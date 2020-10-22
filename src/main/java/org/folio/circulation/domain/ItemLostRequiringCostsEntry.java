package org.folio.circulation.domain;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ItemLostRequiringCostsEntry {
  private final Item item;
  private final Loan loan;
  private final FeeFineAction feeFineAction;

  public ItemLostRequiringCostsEntry(Item item) {
    this.item = item;
    this.loan = null;
    this.feeFineAction = null;
  }

  public Item getItem() {
    return item;
  }

  public Loan getLoan() {
    return loan;
  }

  public FeeFineAction getFeeFineAction() {
    return feeFineAction;
  }

  public ItemLostRequiringCostsEntry withItem(Item item) {
    return new ItemLostRequiringCostsEntry(item, this.loan, this.feeFineAction);
  }

  public ItemLostRequiringCostsEntry withFeeFineAction(FeeFineAction feeFineAction) {
    return new ItemLostRequiringCostsEntry(this.item, this.loan, feeFineAction);
  }

  public ItemLostRequiringCostsEntry withLoan(Loan loan) {
    return new ItemLostRequiringCostsEntry(this.item, loan, this.feeFineAction);
  }
}
