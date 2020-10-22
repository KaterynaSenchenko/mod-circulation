package api.requests;

import static api.support.JsonCollectionAssistant.getRecordById;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeZone.UTC;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;

import org.folio.circulation.support.http.client.Response;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import api.support.APITests;
import api.support.builders.CheckOutByBarcodeRequestBuilder;
import api.support.builders.DeclareItemLostRequestBuilder;
import api.support.builders.ItemBuilder;
import api.support.http.IndividualResource;
import api.support.http.ItemResource;
import api.support.http.ResourceClient;
import api.support.matchers.ItemMatchers;
import api.support.spring.clients.ScheduledJobClient;
import io.vertx.core.json.JsonObject;

public class ItemsLostRequiringActualCostsTests extends APITests {

  @Autowired
  private ScheduledJobClient scheduledAgeToLostClient;

  public ItemsLostRequiringActualCostsTests() {
    super(true, true);
  }

  @Test
  public void reportIsEmptyWhenThereAreNoItemsLostRequringActualCosts() {
    List<JsonObject> items = ResourceClient.forItemsLostRequiringActualCosts().getAll();

    assertTrue(items.isEmpty());
  }

  @Test
  public void itemLostRequiringActualCosts() {

    useLostItemPolicy(lostItemFeePoliciesFixture.chargeFee().getId());


    final ItemResource closedItem = itemsFixture.basedUponNod(ItemBuilder::withRandomBarcode);
    checkOutFixture.checkOutByBarcode(closedItem, usersFixture.jessica());
    checkInFixture.checkInByBarcode(closedItem);


    final ItemResource openItem = itemsFixture.basedUponInterestingTimes(ItemBuilder::withRandomBarcode);
    checkOutFixture.checkOutByBarcode(openItem, usersFixture.charlotte());


    final ItemResource declaredLostItem = itemsFixture.basedUponSmallAngryPlanet(ItemBuilder::withRandomBarcode);
    final IndividualResource checkOutDeclaredLost = checkOutFixture
      .checkOutByBarcode(declaredLostItem, usersFixture.charlotte());
    
    final DeclareItemLostRequestBuilder declaredLostLoanBuilder = new DeclareItemLostRequestBuilder()
      .forLoanId(checkOutDeclaredLost.getId());

    Response declareLostResponse = declareLostFixtures.declareItemLost(declaredLostLoanBuilder);
    assertThat(declareLostResponse.getStatusCode(), CoreMatchers.is(204));

    JsonObject declaredLostLoan = loansFixture.getLoanById(checkOutDeclaredLost.getId()).getJson();
    assertThat(declaredLostLoan.getJsonObject("item"), ItemMatchers.isDeclaredLost());


    useLostItemPolicy(lostItemFeePoliciesFixture.ageToLostAfterOneMinute().getId());

    final IndividualResource agedToLostItem = itemsFixture.basedUponNod(ItemBuilder::withRandomBarcode);
    checkOutFixture.checkOutByBarcode(
      new CheckOutByBarcodeRequestBuilder()
        .forItem(agedToLostItem)
        .at(servicePointsFixture.cd1())
        .to(usersFixture.james())
        .on(now(UTC).minusWeeks(3)));

    scheduledAgeToLostClient.triggerJob();


    Collection<JsonObject> items = ResourceClient.forItemsLostRequiringActualCosts().getAll();

    assertThat(items.size(), is(2));

    JsonObject itemJson = getRecordById(items, declaredLostItem.getId()).get();
    assertThat(itemJson.getString("id"), is(declaredLostItem.getId().toString()));
    assertThat(itemJson, ItemMatchers.isDeclaredLost());
  }

}
