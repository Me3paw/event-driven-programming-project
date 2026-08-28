package service;
public final class CustomerTierCount {
  private final String tier;
  private final long count;
  public CustomerTierCount(String tier, long count) {
    this.tier = tier;
    this.count = count;
  }
  public String getTier() {
    return tier;
  }
  public long getCount() {
    return count;
  }
}
