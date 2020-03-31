package api.support.matchers;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.core.Is.is;

import java.util.UUID;

import org.folio.circulation.domain.representations.ItemProperties;
import org.folio.circulation.support.http.client.IndividualResource;
import org.hamcrest.Matcher;

import io.vertx.core.json.JsonObject;

public class OverdueFineMatcher {

  public static Matcher<JsonObject> isValidOverdueFine(IndividualResource loan,
    IndividualResource item, String location, UUID ownerId, UUID feeFineId, Double amount) {
    return JsonObjectMatcher.allOfPaths(
      hasJsonPath("ownerId", UUIDMatcher.is(ownerId)),
      hasJsonPath("feeFineId", is(feeFineId.toString())),
      hasJsonPath("amount", is(amount)),
      hasJsonPath("remaining", is(amount)),
      hasJsonPath("feeFineType", is("Overdue fine")),
      hasJsonPath("feeFineOwner", is("fee-fine-owner")),
      hasJsonPath("title", is(loan.getJson().getJsonObject("item").getString("title"))),
      hasJsonPath("barcode", is(item.getJson().getString("barcode"))),
      hasJsonPath("callNumber", is(item.getJson().getJsonObject("effectiveCallNumberComponents")
        .getString("callNumber"))),
      hasJsonPath("location", is(location)),
      hasJsonPath("materialTypeId", is(item.getJson().getString(ItemProperties.MATERIAL_TYPE_ID))),
      hasJsonPath("materialType", is(loan.getJson().getJsonObject("item")
        .getJsonObject("materialType").getString("name"))),
      hasJsonPath("loanId", UUIDMatcher.is(loan.getId())),
      hasJsonPath("userId", is(loan.getJson().getString("userId"))),
      hasJsonPath("itemId", UUIDMatcher.is(item.getId()))
    );
  }

}
