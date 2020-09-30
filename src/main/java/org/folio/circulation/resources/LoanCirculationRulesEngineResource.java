package org.folio.circulation.resources;

import static org.folio.circulation.support.results.Result.succeeded;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonArray;

import org.folio.circulation.domain.Location;
import org.folio.circulation.rules.CirculationRuleMatch;
import org.folio.circulation.rules.Drools;
import org.folio.circulation.support.results.Result;

/**
 * The circulation rules engine calculates the loan policy based on
 * item type, loan type, patron type and location.
 */
public class LoanCirculationRulesEngineResource extends AbstractCirculationRulesEngineResource {

  public LoanCirculationRulesEngineResource(String applyPath, String applyAllPath, HttpClient client) {
    super(applyPath, applyAllPath, client);
  }

  @Override
  protected CompletableFuture<Result<CirculationRuleMatch>> getPolicyIdAndRuleMatch(
    MultiMap params, Drools drools, Location location) {
    return CompletableFuture.completedFuture(Result.succeeded(LoanCirculationRulesProcessor.getLoanPolicyAndMatch(drools, params, location)));
  }

  @Override
  protected String getPolicyIdKey() {
    return "loanPolicyId";
  }

  @Override
  protected CompletableFuture<Result<JsonArray>> getPolicies(MultiMap params, Drools drools, Location location) {
    return CompletableFuture.completedFuture(succeeded(drools.loanPolicies(params, location)));
  }

}
