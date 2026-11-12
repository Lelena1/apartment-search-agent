package io.github.lelena1.flatfinder.domain;

/** Сценарий поиска: купить квартиру или снять в аренду. */
public enum DealType {
    SALE("Покупка", 45_000_000L, "₽"),
    RENT("Аренда", 150_000L, "₽/мес");

    private final String title;
    private final long defaultMaxPrice;
    private final String priceUnit;

    DealType(String title, long defaultMaxPrice, String priceUnit) {
        this.title = title;
        this.defaultMaxPrice = defaultMaxPrice;
        this.priceUnit = priceUnit;
    }

    public String title() {
        return title;
    }

    public long defaultMaxPrice() {
        return defaultMaxPrice;
    }

    public String priceUnit() {
        return priceUnit;
    }
}
