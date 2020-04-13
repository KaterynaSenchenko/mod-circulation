package org.folio.circulation.domain.representations;

import static org.folio.circulation.support.JsonPropertyFetcher.getDateTimeProperty;
import static org.folio.circulation.support.JsonPropertyFetcher.getProperty;
import static org.folio.circulation.support.Result.succeeded;
import static org.folio.circulation.support.ValidationErrorFailure.failedValidation;

import org.folio.circulation.support.Result;
import org.joda.time.DateTime;

import io.vertx.core.json.JsonObject;

public class ChangeDueDateRequest {
  private static final String DUE_DATE = "dueDate";

  private final String loanId;
  private final DateTime dueDate;

  public ChangeDueDateRequest(String loanId, DateTime dueDate) {
    this.loanId = loanId;
    this.dueDate = dueDate;
  }

  public static Result<ChangeDueDateRequest> from(JsonObject json,
      String loanId) {

    return Result.of(() -> getDateTimeProperty(json, DUE_DATE))
      .map(dueDate -> new ChangeDueDateRequest(loanId, dueDate));
  }

  public String getLoanId() {
    return loanId;
  }

  public DateTime getDueDate() {
    return dueDate;
  }
}
