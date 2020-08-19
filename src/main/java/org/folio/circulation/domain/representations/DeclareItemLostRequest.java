package org.folio.circulation.domain.representations;

import static org.folio.circulation.support.JsonPropertyFetcher.getDateTimeProperty;
import static org.folio.circulation.support.JsonPropertyFetcher.getProperty;
import static org.folio.circulation.support.results.Result.succeeded;
import static org.folio.circulation.support.ValidationErrorFailure.failedValidation;

import io.vertx.core.json.JsonObject;
import org.folio.circulation.support.results.Result;
import org.joda.time.DateTime;

public class DeclareItemLostRequest {
  private static final String COMMENT = "comment";
  private static final String DECLARED_LOST_DATETIME  = "declaredLostDateTime";

  private final String loanId;
  private final DateTime declaredLostDateTime;
  private final String comment;
  private final String servicePointId;

  private DeclareItemLostRequest(String comment, DateTime declaredLostDateTime,
    String loanId, String servicePointId) {

    this.comment = comment;
    this.declaredLostDateTime = declaredLostDateTime;
    this.loanId = loanId;
    this.servicePointId = servicePointId;
  }

  public static Result<DeclareItemLostRequest> from(JsonObject json,
    String loanId) {
    final String comment = getProperty(json, COMMENT);

    final DateTime dateTime;
    try {
      dateTime = getDateTimeProperty(json, DECLARED_LOST_DATETIME);
    } catch (Exception e) {
        return failedValidation(
          e.getMessage(), DECLARED_LOST_DATETIME,
          getProperty(json, DECLARED_LOST_DATETIME));
      }

    return succeeded(new DeclareItemLostRequest(comment, dateTime, loanId,
      getProperty(json, "servicePointId")));
  }

  public String getComment() {
    return comment;
  }

  public DateTime getDeclaredLostDateTime() {
    return declaredLostDateTime;
  }

  public String getLoanId() {
    return loanId;
  }

  public String getServicePointId() {
    return servicePointId;
  }
}
