package api.support.fixtures;

import org.folio.circulation.support.http.client.IndividualResource;

import api.support.builders.FeeFineBuilder;
import api.support.http.ResourceClient;

public final class FeeFineTypeFixture extends RecordCreator {
  public FeeFineTypeFixture(ResourceClient client) {
    super(client, json -> json.getString("feeFineType"));
  }

  public IndividualResource lostItemFee() {
    return createIfAbsent(new FeeFineBuilder()
      .withFeeFineType("Lost item fee")
      .withAutomatic(true));
  }

  public IndividualResource lostItemProcessingFee() {
    return createIfAbsent(new FeeFineBuilder()
      .withFeeFineType("Lost item processing fee")
      .withAutomatic(true));
  }
}