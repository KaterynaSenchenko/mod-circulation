package org.folio.circulation.domain.anonymization.service;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import org.folio.circulation.domain.Loan;
import org.folio.circulation.support.results.Result;

public interface LoanAnonymizationFinderService {
  CompletableFuture<Result<Collection<Loan>>> findLoansToAnonymize();
}
