package api.loans;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import api.support.APITests;
import api.support.builders.EndSessionBuilder;
import api.support.http.InventoryItemResource;
import io.vertx.core.json.JsonObject;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.Test;

import org.folio.circulation.domain.notice.session.PatronActionType;
import org.folio.circulation.support.http.client.IndividualResource;

public class EndExpiredPatronActionSessionTests extends APITests {

  @Test
  public void expiredEndSessionAfterCheckOut() {

    IndividualResource james = usersFixture.james();
    loansFixture.checkOutByBarcode(itemsFixture.basedUponNod(), james);
    loansFixture.checkOutByBarcode(itemsFixture.basedUponInterestingTimes(), james);
    expiredEndSessionClient.deleteAll();

    List<JsonObject> sessions = patronSessionRecordsClient.getAll();
    assertThat(sessions, Matchers.hasSize(2));

    String patronId = sessions.stream()
      .findFirst()
      .map(session -> session.getString("patronId"))
      .orElse("");

    expiredEndSessionClient.create(new EndSessionBuilder()
      .withPatronId(patronId)
      .withActionType("Check-out"));

    expiredSessionProcessingClient.runRequestExpiredSessionsProcessing(204);
    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(patronSessionRecordsClient::getAll, empty());
  }

  @Test
  public void patronHasSomeSessionsAndOnlySessionsWithSameActionTypeShouldBeExpiredByTimeout() {

    IndividualResource james = usersFixture.james();
    InventoryItemResource nod = itemsFixture.basedUponNod();
    InventoryItemResource interestingTimes = itemsFixture.basedUponInterestingTimes();

    loansFixture.checkOutByBarcode(nod, james);
    loansFixture.checkOutByBarcode(interestingTimes, james);
    loansFixture.checkInByBarcode(nod);
    loansFixture.checkInByBarcode(interestingTimes);
    expiredEndSessionClient.deleteAll();

    List<JsonObject> sessions = patronSessionRecordsClient.getAll();
    assertThat(sessions, Matchers.hasSize(4));

    String patronId = sessions.stream()
      .filter(session -> session.getMap().get("actionType")
        .equals(PatronActionType.CHECK_IN.getRepresentation()))
      .findFirst()
      .map(session -> session.getString("patronId"))
      .orElse("");

    expiredEndSessionClient.create(new EndSessionBuilder()
      .withPatronId(patronId)
      .withActionType("Check-in"));

    expiredSessionProcessingClient.runRequestExpiredSessionsProcessing(204);
    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(patronSessionRecordsClient::getAll,  Matchers.hasSize(2));
  }

  @Test
  public void patronHasSeveralSessionsAndOnlyOneShouldBeExpiredByTimeout() {

    IndividualResource james = usersFixture.james();
    InventoryItemResource nod = itemsFixture.basedUponNod();
    InventoryItemResource interestingTimes = itemsFixture.basedUponInterestingTimes();

    loansFixture.checkOutByBarcode(nod, james);
    loansFixture.checkOutByBarcode(interestingTimes, james);
    loansFixture.checkInByBarcode(nod);
    expiredEndSessionClient.deleteAll();

    List<JsonObject> sessions = patronSessionRecordsClient.getAll();
    assertThat(sessions, Matchers.hasSize(3));

    String patronId = sessions.stream()
      .filter(session -> session.getMap().get("actionType")
        .equals(PatronActionType.CHECK_IN.getRepresentation()))
      .findFirst()
      .map(session -> session.getString("patronId"))
      .orElse("");

    expiredEndSessionClient.create(new EndSessionBuilder()
      .withPatronId(patronId)
      .withActionType("Check-in"));

    expiredSessionProcessingClient.runRequestExpiredSessionsProcessing(204);
    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(patronSessionRecordsClient::getAll,  Matchers.hasSize(2));
  }

  @Test
  public void noExpiredEndSessionAfterCheckOut() {

    IndividualResource james = usersFixture.james();
    loansFixture.checkOutByBarcode(itemsFixture.basedUponNod(), james);
    loansFixture.checkOutByBarcode(itemsFixture.basedUponInterestingTimes(), james);
    expiredEndSessionClient.deleteAll();

    List<JsonObject> sessions = patronSessionRecordsClient.getAll();
    assertThat(sessions, Matchers.hasSize(2));

    expiredEndSessionClient.create(new EndSessionBuilder());
    expiredSessionProcessingClient.runRequestExpiredSessionsProcessing(204);

    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(patronSessionRecordsClient::getAll, Matchers.hasSize(2));
  }

  @Test
  public void noExpiredEndSessionAfterCheckIn() {

    IndividualResource james = usersFixture.james();
    InventoryItemResource nod = itemsFixture.basedUponNod();
    InventoryItemResource interestingTimes = itemsFixture.basedUponInterestingTimes();

    loansFixture.checkOutByBarcode(nod, james);
    loansFixture.checkOutByBarcode(interestingTimes, james);
    loansFixture.checkInByBarcode(nod);
    loansFixture.checkInByBarcode(interestingTimes);
    expiredEndSessionClient.deleteAll();

    List<JsonObject> sessions = patronSessionRecordsClient.getAll();
    assertThat(sessions, Matchers.hasSize(4));

    expiredEndSessionClient.create(new EndSessionBuilder());
    expiredSessionProcessingClient.runRequestExpiredSessionsProcessing(204);

    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(patronSessionRecordsClient::getAll, Matchers.hasSize(4));
  }

  @Test
  public void notFailEndSessionProcessingWhenServerIsNotResponding() {

    IndividualResource james = usersFixture.james();
    InventoryItemResource nod = itemsFixture.basedUponNod();
    InventoryItemResource interestingTimes = itemsFixture.basedUponInterestingTimes();
    loansFixture.checkOutByBarcode(itemsFixture.basedUponNod(), james);
    loansFixture.checkOutByBarcode(itemsFixture.basedUponInterestingTimes(), james);
    loansFixture.checkInByBarcode(nod);
    loansFixture.checkInByBarcode(interestingTimes);
    expiredEndSessionClient.deleteAll();

    List<JsonObject> sessions = patronSessionRecordsClient.getAll();
    assertThat(sessions, Matchers.hasSize(4));

    expiredSessionProcessingClient.runRequestExpiredSessionsProcessing(204);

    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(patronSessionRecordsClient::getAll, Matchers.hasSize(4));
  }

  @Test
  public void patronsHaveSessionsAndAllShouldBeExpiredByTimeout() {

    IndividualResource james = usersFixture.james();
    IndividualResource jessica = usersFixture.jessica();
    IndividualResource steve = usersFixture.steve();
    InventoryItemResource nod = itemsFixture.basedUponNod();
    InventoryItemResource interestingTimes = itemsFixture.basedUponInterestingTimes();
    InventoryItemResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();

    loansFixture.checkOutByBarcode(nod, james);
    loansFixture.checkOutByBarcode(interestingTimes, jessica);
    loansFixture.checkOutByBarcode(smallAngryPlanet, steve);
    loansFixture.checkInByBarcode(nod);
    loansFixture.checkInByBarcode(interestingTimes);
    loansFixture.checkInByBarcode(smallAngryPlanet);
    expiredEndSessionClient.deleteAll();

    List<JsonObject> sessions = patronSessionRecordsClient.getAll();
    assertThat(sessions, Matchers.hasSize(6));

    sessions.stream()
      .filter(session -> session.getMap().get("actionType")
        .equals(PatronActionType.CHECK_IN.getRepresentation()))
      .map(session -> session.getString("patronId"))
      .forEach(patronId -> expiredEndSessionClient.create(new EndSessionBuilder()
        .withPatronId(patronId).withActionType("Check-in")));

    expiredSessionProcessingClient.runRequestExpiredSessionsProcessing(204);
    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(patronSessionRecordsClient::getAll,  Matchers.hasSize(3));

    expiredEndSessionClient.deleteAll();

    sessions.stream()
      .filter(session -> session.getMap().get("actionType")
        .equals(PatronActionType.CHECK_OUT.getRepresentation()))
      .map(session -> session.getString("patronId"))
      .forEach(patronId -> expiredEndSessionClient.create(new EndSessionBuilder()
        .withPatronId(patronId).withActionType("Check-out")));

    expiredSessionProcessingClient.runRequestExpiredSessionsProcessing(204);
    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(patronSessionRecordsClient::getAll,  Matchers.hasSize(0));
  }
}
