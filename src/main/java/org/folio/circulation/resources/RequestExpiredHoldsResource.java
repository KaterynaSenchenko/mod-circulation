package org.folio.circulation.resources;

import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import org.folio.circulation.domain.ExpiredHoldsContext;
import org.folio.circulation.domain.ExpiredHoldsRequest;
import org.folio.circulation.domain.Item;
import org.folio.circulation.domain.MultipleRecords;
import org.folio.circulation.domain.Request;
import org.folio.circulation.domain.RequestStatus;
import org.folio.circulation.support.Clients;
import org.folio.circulation.support.CollectionResourceClient;
import org.folio.circulation.support.CqlQuery;
import org.folio.circulation.support.OkJsonResponseResult;
import org.folio.circulation.support.Result;
import org.folio.circulation.support.RouteRegistration;
import org.folio.circulation.support.http.client.Response;
import org.folio.circulation.support.http.server.WebContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.folio.circulation.domain.ItemStatus.AWAITING_PICKUP;
import static org.folio.circulation.domain.RequestStatus.OPEN_AWAITING_PICKUP;
import static org.folio.circulation.support.CqlQuery.exactMatch;
import static org.folio.circulation.support.CqlSortBy.descending;

public class RequestExpiredHoldsResource extends Resource {

  /**
   * The optimal number of identifiers that will not exceed the permissible length
   * of the URI in according to the RFC 2616
   */
  private static final int BATCH_SIZE = 40;

  /**
   * Default limit value on a query
   */
  private static final int PAGE_LIMIT = 100;
  private static final String SERVICE_POINT_ID_PARAM = "servicePointId";
  private static final String ITEMS_KEY = "items";
  private static final String ITEM_ID_KEY = "itemId";
  private static final String REQUESTS_KEY = "requests";
  private static final String STATUS_KEY = "status";
  private static final String STATUS_NAME_KEY = "status.name";
  private static final String TOTAL_RECORDS_KEY = "totalRecords";
  private static final String SERVICE_POINT_ID_KEY = "pickupServicePointId";
  private static final String REQUEST_CLOSED_DATE_KEY = "awaitingPickupRequestClosedDate";

  private final String rootPath;

  public RequestExpiredHoldsResource(String rootPath, HttpClient client) {
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

    final CollectionResourceClient itemsStorageClient = clients.itemsStorage();
    final CollectionResourceClient requestsStorage = clients.requestsStorage();

    final String servicePointId = routingContext.request().getParam(SERVICE_POINT_ID_PARAM);

    findAllAwaitingPickupItems(itemsStorageClient)
      .thenComposeAsync(r -> r.after(this::mapContextToItemIdList))
      .thenComposeAsync(r -> r.after(this::mapItemIdsInBatchItemIds))
      .thenComposeAsync(r -> findExpiredOrCancelledItemsIds(requestsStorage, servicePointId, r.value()))
      .thenComposeAsync(r -> findExpiredOrCancelledRequestByItemIds(requestsStorage, servicePointId, r.value()))
      .thenApply(this::mapResultToExpiredHoldsRequests)
      .thenApply(OkJsonResponseResult::from)
      .thenAccept(r -> r.writeTo(routingContext.response()));
  }

  private CompletableFuture<Result<ExpiredHoldsContext>> findAllAwaitingPickupItems(CollectionResourceClient client) {
    CompletableFuture<Result<ExpiredHoldsContext>> future = new CompletableFuture<>();
    ExpiredHoldsContext initialContext = new ExpiredHoldsContext(0, new ArrayList<>());
    fetchNextPage(client, initialContext, future);
    return future;
  }

  private void fetchNextPage(CollectionResourceClient client, ExpiredHoldsContext initialContext,
                             CompletableFuture<Result<ExpiredHoldsContext>> future) {
    findAwaitingPickupItemsByQuery(client, initialContext)
      .thenApply(itemRecords -> {
          ExpiredHoldsContext context = fillExpiredHoldsContext(initialContext, itemRecords);
          int totalRecords = itemRecords.value().getTotalRecords();

          if (totalRecords > context.getPageOffset(PAGE_LIMIT)) {
            fetchNextPage(client, context, future);
          } else {
            future.complete(Result.of(() -> context));
          }

          return itemRecords;
        }
      );
  }

  private CompletableFuture<Result<MultipleRecords<Item>>> findAwaitingPickupItemsByQuery(CollectionResourceClient client,
                                                                                          ExpiredHoldsContext expiredHoldsContext) {
    final Result<CqlQuery> itemStatusQuery = exactMatch(STATUS_NAME_KEY, AWAITING_PICKUP.getValue());
    int pageOffset = expiredHoldsContext.getCurrPageNumber() * PAGE_LIMIT;

    return itemStatusQuery
      .after(query -> client.getMany(query, PAGE_LIMIT, pageOffset))
      .thenApply(result -> result.next(this::mapResponseToItems));
  }

  private ExpiredHoldsContext fillExpiredHoldsContext(ExpiredHoldsContext initialContext,
                                                      Result<MultipleRecords<Item>> itemRecords) {
    List<Result<MultipleRecords<Item>>> itemIds = initialContext.getItemIds();
    itemIds.add(itemRecords);
    int newPageNumber = initialContext.getCurrPageNumber() + 1;
    return new ExpiredHoldsContext(newPageNumber, itemIds);
  }

  private CompletableFuture<Result<List<String>>> mapContextToItemIdList(ExpiredHoldsContext expiredHoldsContext) {
    List<String> itemIds = expiredHoldsContext.getItemIds().stream()
      .flatMap(records -> records.value().getRecords().stream())
      .filter(item -> StringUtils.isNoneBlank(item.getItemId()))
      .map(Item::getItemId)
      .collect(Collectors.toList());
    return CompletableFuture.completedFuture(Result.succeeded(itemIds));
  }

  private CompletableFuture<Result<List<List<String>>>> mapItemIdsInBatchItemIds(List<String> itemIds) {
    return CompletableFuture.completedFuture(Result.succeeded(splitIds(itemIds)));
  }

  private List<List<String>> splitIds(List<String> itemsIds) {
    int size = itemsIds.size();
    if (size <= 0) {
      return new ArrayList<>();
    }

    int fullChunks = (size - 1) / BATCH_SIZE;
    return IntStream.range(0, fullChunks + 1)
      .mapToObj(n ->
        itemsIds.subList(n * BATCH_SIZE, n == fullChunks
          ? size
          : (n + 1) * BATCH_SIZE))
      .collect(Collectors.toList());
  }

  private CompletableFuture<Result<List<String>>> findExpiredOrCancelledItemsIds(CollectionResourceClient client,
                                                                                 String servicePointId,
                                                                                 List<List<String>> batchItemIds) {
    List<Result<MultipleRecords<Request>>> recordsResult = finAwaitingPickupRequests(client, servicePointId, batchItemIds);
    List<String> expiredItemsIds = findExpiredOrCancelledItemsIdsInRequests(recordsResult);
    return CompletableFuture.completedFuture(Result.succeeded(
      findDifferenceBetweenAvailableItemsAndExpired(batchItemIds, expiredItemsIds)));
  }

  private List<Result<MultipleRecords<Request>>> finAwaitingPickupRequests(CollectionResourceClient client,
                                                                           String servicePointId,
                                                                           List<List<String>> batchItemIds) {
    return batchItemIds.stream()
      .map(batch -> {
        final Result<CqlQuery> servicePointQuery = exactMatch(SERVICE_POINT_ID_KEY, servicePointId);
        final Result<CqlQuery> statusQuery = exactMatch(STATUS_KEY, OPEN_AWAITING_PICKUP.getValue());
        final Result<CqlQuery> itemIdsQuery = CqlQuery.exactMatchAny(ITEM_ID_KEY, batch);

        Result<CqlQuery> cqlQueryResult = servicePointQuery
          .combine(statusQuery, CqlQuery::and)
          .combine(itemIdsQuery, CqlQuery::and);

        return finRequestsByCqlQuery(client, cqlQueryResult);
      })
      .map(CompletableFuture::join)
      .collect(Collectors.toList());
  }

  private List<String> findExpiredOrCancelledItemsIdsInRequests(List<Result<MultipleRecords<Request>>> multipleRecordsResult) {
    return multipleRecordsResult.stream()
      .flatMap(records -> records.value().getRecords().stream())
      .filter(isNotOpenAwaitingPickupRequest())
      .map(Request::getItemId)
      .collect(Collectors.toList());
  }

  private Predicate<Request> isNotOpenAwaitingPickupRequest() {
    return r -> RequestStatus.OPEN_AWAITING_PICKUP != r.getStatus();
  }

  private List<String> findDifferenceBetweenAvailableItemsAndExpired(List<List<String>> batchItemIds,
                                                                     List<String> expiredItemsIds) {
    List<String> awaitingPickupItemsIds = batchItemIds.stream()
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
    awaitingPickupItemsIds.removeAll(expiredItemsIds);
    return awaitingPickupItemsIds;
  }

  private CompletableFuture<Result<List<Request>>> findExpiredOrCancelledRequestByItemIds(CollectionResourceClient client,
                                                                                          String servicePointId, List<String> itemIds) {
    List<Result<MultipleRecords<Request>>> requestList = findRequestsSortedByClosedDate(client, servicePointId, itemIds);
    return CompletableFuture.completedFuture(
      Result.succeeded(getFirstRequestFromList(requestList)));
  }

  /**
   * Find for each item ids requests sorted by awaitingPickupRequestClosedDate
   */
  private List<Result<MultipleRecords<Request>>> findRequestsSortedByClosedDate(CollectionResourceClient client,
                                                                                String servicePointId,
                                                                                List<String> itemIds) {
    return itemIds.stream()
      .map(itemId -> {
        final Result<CqlQuery> servicePointQuery = exactMatch(SERVICE_POINT_ID_KEY, servicePointId);
        final Result<CqlQuery> itemIdQuery = CqlQuery.exactMatch(ITEM_ID_KEY, itemIds.get(0));
        Result<CqlQuery> cqlQueryResult = servicePointQuery
          .combine(itemIdQuery, CqlQuery::and)
          .map(q -> q.sortBy(descending(REQUEST_CLOSED_DATE_KEY)));

        return finRequestsByCqlQuery(client, cqlQueryResult);
      }).map(CompletableFuture::join)
      .collect(Collectors.toList());
  }

  private List<Request> getFirstRequestFromList(List<Result<MultipleRecords<Request>>> multipleRecordsList) {
    return multipleRecordsList.stream()
      .map(r -> r.value().getRecords().stream().findFirst())
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(Collectors.toList());
  }

  private CompletableFuture<Result<MultipleRecords<Request>>> finRequestsByCqlQuery(CollectionResourceClient client,
                                                                                    Result<CqlQuery> cqlQueryResult) {
    return cqlQueryResult
      .after(query -> client.getMany(query, PAGE_LIMIT))
      .thenApply(result -> result.next(this::mapResponseToRequest));
  }

  private Result<JsonObject> mapResultToExpiredHoldsRequests(Result<List<Request>> requests) {
    return requests.map(r -> {
      JsonArray jsonArray = r.stream()
        .map(ExpiredHoldsRequest::new)
        .map(ExpiredHoldsRequest::asJson)
        .collect(Collector.of(JsonArray::new, JsonArray::add, JsonArray::add));
      return new JsonObject()
        .put(REQUESTS_KEY, jsonArray)
        .put(TOTAL_RECORDS_KEY, r.size());
    });
  }

  private Result<MultipleRecords<Item>> mapResponseToItems(Response response) {
    return MultipleRecords.from(response, Item::from, ITEMS_KEY);
  }

  private Result<MultipleRecords<Request>> mapResponseToRequest(Response response) {
    return MultipleRecords.from(response, Request::from, REQUESTS_KEY);
  }
}
