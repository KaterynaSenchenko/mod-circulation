package org.folio.circulation.support.http.client;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.apache.http.HttpHeaders.ACCEPT;
import static org.folio.circulation.support.Result.failed;
import static org.folio.circulation.support.Result.succeeded;
import static org.folio.circulation.support.http.OkapiHeader.OKAPI_URL;
import static org.folio.circulation.support.http.OkapiHeader.REQUEST_ID;
import static org.folio.circulation.support.http.OkapiHeader.TENANT;
import static org.folio.circulation.support.http.OkapiHeader.TOKEN;
import static org.folio.circulation.support.http.OkapiHeader.USER_ID;
import static org.folio.circulation.support.http.client.Response.responseFrom;

import java.net.URL;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.folio.circulation.support.Result;
import org.folio.circulation.support.ServerErrorFailure;

import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class VertxWebClientOkapiHttpClient {
  private static final Duration DEFAULT_TIMEOUT = Duration.of(5, SECONDS);

  private final WebClient webClient;
  private final URL okapiUrl;
  private String tenantId;
  private String token;
  private final String userId;
  private String requestId;

  public static VertxWebClientOkapiHttpClient createClientUsing(
    HttpClient httpClient, URL okapiUrl, String tenantId, String token,
    String userId, String requestId) {

    return new VertxWebClientOkapiHttpClient(WebClient.wrap(httpClient),
      okapiUrl, tenantId, token, userId, requestId);
  }

  private VertxWebClientOkapiHttpClient(WebClient webClient, URL okapiUrl,
    String tenantId, String token, String userId, String requestId) {

    this.webClient = webClient;
    this.okapiUrl = okapiUrl;
    this.tenantId = tenantId;
    this.token = token;
    this.userId = userId;
    this.requestId = requestId;
  }

  public CompletableFuture<Result<Response>> get(String url,
    Duration timeout, QueryParameter... queryParameters) {

    final CompletableFuture<AsyncResult<HttpResponse<Buffer>>> futureResponse
      = new CompletableFuture<>();

    final HttpRequest<Buffer> request = withStandardHeaders(
      webClient.getAbs(url));

    Stream.of(queryParameters)
      .forEach(parameter -> parameter.writeTo(request));

    request
      .timeout(timeout.toMillis())
      .send(futureResponse::complete);

    return futureResponse
      .thenApply(asyncResult -> mapAsyncResultToResult(url, asyncResult));
  }

  public CompletableFuture<Result<Response>> get(URL url,
    QueryParameter... queryParameters) {
    return get(url.toString(), queryParameters);
  }

  public CompletableFuture<Result<Response>> get(String url,
    QueryParameter... queryParameters) {

    return get(url, DEFAULT_TIMEOUT, queryParameters);
  }

  public CompletableFuture<Result<Response>> delete(URL url,
    QueryParameter... queryParameters) {

    return delete(url.toString(), queryParameters);
  }

  public CompletableFuture<Result<Response>> delete(String url,
    QueryParameter... queryParameters) {

    return delete(url, DEFAULT_TIMEOUT, queryParameters);
  }

  public CompletableFuture<Result<Response>> delete(String url,
    Duration timeout, QueryParameter... queryParameters) {

    final CompletableFuture<AsyncResult<HttpResponse<Buffer>>> futureResponse
      = new CompletableFuture<>();

    final HttpRequest<Buffer> request = withStandardHeaders(
      webClient.deleteAbs(url));

    Stream.of(queryParameters)
      .forEach(parameter -> parameter.writeTo(request));

    request
      .timeout(timeout.toMillis())
      .send(futureResponse::complete);

    return futureResponse
      .thenApply(asyncResult -> mapAsyncResultToResult(url, asyncResult));
  }

  private HttpRequest<Buffer> withStandardHeaders(HttpRequest<Buffer> request) {
    return request
      .putHeader(ACCEPT, "application/json, text/plain")
      .putHeader(OKAPI_URL, okapiUrl.toString())
      .putHeader(TENANT, this.tenantId)
      .putHeader(TOKEN, this.token)
      .putHeader(USER_ID, this.userId)
      .putHeader(REQUEST_ID, this.requestId);
  }

  private static Result<Response> mapAsyncResultToResult(String url,
    AsyncResult<HttpResponse<Buffer>> asyncResult) {

    return asyncResult.succeeded()
      ? succeeded(responseFrom(url, asyncResult.result()))
      : failed(new ServerErrorFailure(asyncResult.cause()));
  }
}
