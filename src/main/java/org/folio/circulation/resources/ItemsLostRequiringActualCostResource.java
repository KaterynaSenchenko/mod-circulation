package org.folio.circulation.resources;

import static org.folio.circulation.domain.ItemStatus.AGED_TO_LOST;
import static org.folio.circulation.domain.ItemStatus.DECLARED_LOST;
import static org.folio.circulation.support.AsyncCoordinationUtil.allOf;
import static org.folio.circulation.support.http.client.CqlQuery.matchAny;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collector;

import org.folio.circulation.domain.FeeFine;
import org.folio.circulation.domain.Item;
import org.folio.circulation.domain.ItemLostRequiringCostsEntry;
import org.folio.circulation.infrastructure.storage.feesandfines.FeeFineActionRepository;
import org.folio.circulation.infrastructure.storage.inventory.ItemRepository;
import org.folio.circulation.infrastructure.storage.loans.LoanRepository;
import org.folio.circulation.support.Clients;
import org.folio.circulation.support.RouteRegistration;
import org.folio.circulation.support.http.server.JsonHttpResponse;
import org.folio.circulation.support.http.server.WebContext;
import org.folio.circulation.support.results.Result;

import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class ItemsLostRequiringActualCostResource extends Resource {

  private final String rootPath;

  public ItemsLostRequiringActualCostResource(String rootPath, HttpClient client) {
    super(client);
    this.rootPath = rootPath;
  }

  @Override
  public void register(Router router) {
    RouteRegistration routeRegistration = new RouteRegistration(rootPath, router);
    routeRegistration.getMany(this::getMany);
  }

  private void getMany(RoutingContext routingContext) {
    final WebContext context = new WebContext(routingContext);
    final Clients clients = Clients.create(context, client);
    final ItemRepository itemRepository = new ItemRepository(clients, true, true, true);
    final LoanRepository loanRepository = new LoanRepository(clients);
    final FeeFineActionRepository feeFineActionRepository = new FeeFineActionRepository(clients);

    final Collection<String> fieldValues = List.of(
      DECLARED_LOST.getValue(),
      AGED_TO_LOST.getValue());
    
    itemRepository.findByQuery(matchAny("status.name", fieldValues))
      .thenCompose(r -> r.after(items -> fetchItemsLoanAndFeeFine(
        items, itemRepository, loanRepository, feeFineActionRepository)))
      .thenApply(this::mapResultToJson)
      .thenApply(r -> r.map(JsonHttpResponse::ok))
      .thenAccept(context::writeResultToHttpResponse);
  }

  private CompletableFuture<Result<List<JsonObject>>> fetchItemsLoanAndFeeFine(
    Collection<Item> items, ItemRepository itemRepository, 
    LoanRepository loanRepository, FeeFineActionRepository feeFineActionRepository) {

    return allOf(items, item -> fetchItemLoanAndFeeFine(
        new ItemLostRequiringCostsEntry(item),
        loanRepository, feeFineActionRepository)
      );
  }
  
  private CompletableFuture<Result<JsonObject>> fetchItemLoanAndFeeFine(
    ItemLostRequiringCostsEntry entry,
    LoanRepository loanRepository, FeeFineActionRepository feeActionFineRepository) {

    return CompletableFuture.completedFuture(Result.succeeded(entry))
      .thenCompose(r -> feeActionFineRepository.findById(entry.getItem().getHoldingsRecordId()))
      .thenApply(r -> entry.withFeeFineAction(r.value()))
      .thenCompose(e -> loanRepository.findOpenLoanForItem(e.getItem()))
      .thenApply(r -> entry.withLoan(r.value()))
      .thenApply(r -> mapEntryToItem(r));
  }

  private Result<JsonObject> mapEntryToItem(ItemLostRequiringCostsEntry entry) {
    if (entry.getLoan() != null && entry.getLoan().isOpen()) {
      if (!entry.getItem().getStatusName().equals(AGED_TO_LOST.getValue()) || entry.getLoan().getLostItemHasBeenBilled()) {
        if (entry.getFeeFineAction() == null || entry.getFeeFineAction().getActionType() != FeeFine.LOST_ITEM_FEE_ACTUAL_COSTS_TYPE) {
          return Result.of(() -> entry.getItem().getItem());
        }
      }
    }
    
    return null;
  }

  private Result<JsonObject> mapResultToJson(Result<List<JsonObject>> items) {
    Result<JsonArray> result = items.map(r -> r.stream()
      .filter(Objects::nonNull)
      .collect(Collector.of(JsonArray::new, JsonArray::add, JsonArray::add)));

    return result.next(jsonArray -> Result.succeeded(new JsonObject()
      .put("items", jsonArray)
      .put("totalRecords", jsonArray.size())));
  }
}
