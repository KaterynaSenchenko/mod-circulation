package org.folio.circulation.domain;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.circulation.support.Result.succeeded;
import static org.folio.circulation.support.fetching.MultipleCqlIndexValuesCriteria.byIndex;
import static org.folio.circulation.support.fetching.RecordFetching.findWithCqlQuery;
import static org.folio.circulation.support.fetching.RecordFetching.findWithMultipleCqlIndexValues;
import static org.folio.circulation.support.http.ResponseMapping.forwardOnFailure;
import static org.folio.circulation.support.http.ResponseMapping.mapUsingJson;
import static org.folio.circulation.support.http.client.CqlQuery.exactMatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.folio.circulation.domain.representations.AccountStorageRepresentation;
import org.folio.circulation.support.Clients;
import org.folio.circulation.support.CollectionResourceClient;
import org.folio.circulation.support.FindWithMultipleCqlIndexValues;
import org.folio.circulation.support.GetManyRecordsClient;
import org.folio.circulation.support.Result;
import org.folio.circulation.support.http.client.ResponseInterpreter;
import org.folio.circulation.support.results.CommonFailures;

public class AccountRepository {
  private static final String LOAN_ID_FIELD_NAME = "loanId";
  private static final String ACCOUNT_ID_FIELD_NAME = "accountId";
  private static final String ACCOUNTS_COLLECTION_PROPERTY_NAME = "accounts";

  private final CollectionResourceClient accountsStorageClient;
  private final GetManyRecordsClient feefineActionsStorageClient;

  public AccountRepository(Clients clients) {
    accountsStorageClient = clients.accountsStorageClient();
    feefineActionsStorageClient = clients.feeFineActionsStorageClient();
  }

  public CompletableFuture<Result<Loan>> findAccountsForLoan(Result<Loan> loanResult) {
    return loanResult.after(loan -> {
      if (loan == null) {
        return completedFuture(loanResult);
      }
      return loanResult.combineAfter(r -> fetchAccountsForLoan(loan.getId()), Loan::withAccounts);
    });
  }

  private CompletableFuture<Result<Collection<Account>>> fetchAccountsForLoan(String loanId) {
    return findWithCqlQuery(accountsStorageClient, ACCOUNTS_COLLECTION_PROPERTY_NAME, Account::from)
      .findByQuery(exactMatch(LOAN_ID_FIELD_NAME, loanId))
      .thenCompose(r -> r.after(this::findFeeFineActionsForAccounts))
      .thenApply(r -> r.map(MultipleRecords::getRecords));
  }

  public CompletableFuture<Result<MultipleRecords<Loan>>> findAccountsForLoans(
    MultipleRecords<Loan> multipleLoans) {

    if (multipleLoans.getRecords().isEmpty()) {
      return completedFuture(succeeded(multipleLoans));
    }

    return getAccountsForLoans(multipleLoans.getRecords())
      .thenApply(r -> r.map(accountMap -> multipleLoans.mapRecords(
        loan -> loan.withAccounts(accountMap.getOrDefault(loan.getId(),
          new ArrayList<>())))));
  }

  private CompletableFuture<Result<Map<String, List<Account>>>> getAccountsForLoans(Collection<Loan> loans) {

    final Set<String> loanIds =
      loans.stream()
        .filter(Objects::nonNull)
        .map(Loan::getId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    return findWithMultipleCqlIndexValues(accountsStorageClient,
        ACCOUNTS_COLLECTION_PROPERTY_NAME, Account::from)
      .find(byIndex(LOAN_ID_FIELD_NAME, loanIds))
      .thenCompose(r -> r.after(this::findFeeFineActionsForAccounts))
      .thenComposeAsync(r -> r.after(multipleRecords -> completedFuture(succeeded(multipleRecords.getRecords()
        .stream()
        .collect(Collectors.groupingBy(Account::getLoanId))))));
  }

  public CompletableFuture<Result<MultipleRecords<Account>>> findFeeFineActionsForAccounts(
      MultipleRecords<Account> multipleLoans) {

    if (multipleLoans.getRecords().isEmpty()) {
      return completedFuture(succeeded(multipleLoans));
    }

    return getFeeFineActionsForAccounts(multipleLoans.getRecords())
        .thenApply(r -> r.map(accountMap -> multipleLoans.mapRecords(
            loan -> loan.withFeeFineActions(accountMap.getOrDefault(loan.getId(),
                new ArrayList<>())))));
  }

  private CompletableFuture<Result<Map<String, List<FeeFineAction>>>> getFeeFineActionsForAccounts(
    Collection<Account> accounts) {

    final Set<String> loanIds =
    accounts.stream()
      .filter(Objects::nonNull)
      .map(Account::getId)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());

    return createFeeFineActionFetcher().find(byIndex(ACCOUNT_ID_FIELD_NAME, loanIds))
        .thenComposeAsync(r -> r.after(multipleRecords -> completedFuture(succeeded(
            multipleRecords.getRecords().stream().collect(
                Collectors.groupingBy(FeeFineAction::getAccountId))))));
  }

  private FindWithMultipleCqlIndexValues<FeeFineAction> createFeeFineActionFetcher() {
    return findWithMultipleCqlIndexValues(feefineActionsStorageClient,
      "feefineactions", FeeFineAction::from);
  }

  public CompletableFuture<Result<Account>> create(AccountStorageRepresentation account) {
    final ResponseInterpreter<Account> interpreter = new ResponseInterpreter<Account>()
      .flatMapOn(201, mapUsingJson(Account::from))
      .otherwise(forwardOnFailure());

    return accountsStorageClient.post(account)
      .thenApply(interpreter::flatMap);
  }

  public CompletableFuture<Result<Void>> createAll(Collection<AccountStorageRepresentation> accounts) {
    return allOf(accounts.stream()
      .map(this::create)
      .toArray(CompletableFuture[]::new))
      .thenApply(Result::succeeded)
      .exceptionally(CommonFailures::failedDueToServerError);
  }
}
