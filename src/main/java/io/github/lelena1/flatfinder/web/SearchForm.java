package io.github.lelena1.flatfinder.web;

import io.github.lelena1.flatfinder.domain.DealType;
import io.github.lelena1.flatfinder.domain.SearchCriteria;

import java.util.Arrays;
import java.util.List;

/**
 * Параметры формы поиска. Значения по умолчанию — пример запроса: две спальни, балкон, готовый ремонт.
 * Флажки размечены через th:field, поэтому снятый флажок приходит как {@code false}, а не теряется.
 */
public class SearchForm {

    private DealType deal = DealType.SALE;
    private String districts = "";
    private Long maxPrice;
    private Integer bedrooms = 2;
    private Double minKitchenArea = SearchCriteria.DEFAULT_MIN_KITCHEN_AREA;
    private boolean requireBalcony = true;
    private boolean allowFrenchBalcony = true;
    private boolean preferKitchenBalcony = true;
    private boolean preferStorage = true;
    private boolean requireFinish = true;
    /** {@code null} — форму ещё не отправляли: для аренды мебель по умолчанию нужна, для покупки нет. */
    private Boolean requireFurniture;
    private boolean allowApartments;
    private boolean web;

    public SearchCriteria toCriteria() {
        long budget = maxPrice == null || maxPrice <= 0 ? deal.defaultMaxPrice() : maxPrice;
        double kitchen = minKitchenArea == null || minKitchenArea <= 0
                ? SearchCriteria.DEFAULT_MIN_KITCHEN_AREA : minKitchenArea;
        return new SearchCriteria(deal, districtList(), budget, bedrooms, kitchen, requireBalcony,
                allowFrenchBalcony, preferKitchenBalcony, preferStorage, requireFinish, getRequireFurniture(), allowApartments);
    }

    public List<String> districtList() {
        return Arrays.stream(districts == null ? new String[0] : districts.split("[,;\\n]"))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public DealType getDeal() {
        return deal;
    }

    public void setDeal(DealType deal) {
        this.deal = deal == null ? DealType.SALE : deal;
    }

    public String getDistricts() {
        return districts;
    }

    public void setDistricts(String districts) {
        this.districts = districts == null ? "" : districts;
    }

    public Long getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Long maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Integer getBedrooms() {
        return bedrooms;
    }

    public void setBedrooms(Integer bedrooms) {
        this.bedrooms = bedrooms;
    }

    public Double getMinKitchenArea() {
        return minKitchenArea;
    }

    public void setMinKitchenArea(Double minKitchenArea) {
        this.minKitchenArea = minKitchenArea;
    }

    public boolean isRequireBalcony() {
        return requireBalcony;
    }

    public void setRequireBalcony(boolean requireBalcony) {
        this.requireBalcony = requireBalcony;
    }

    public boolean isAllowFrenchBalcony() {
        return allowFrenchBalcony;
    }

    public void setAllowFrenchBalcony(boolean allowFrenchBalcony) {
        this.allowFrenchBalcony = allowFrenchBalcony;
    }

    public boolean isPreferKitchenBalcony() {
        return preferKitchenBalcony;
    }

    public void setPreferKitchenBalcony(boolean preferKitchenBalcony) {
        this.preferKitchenBalcony = preferKitchenBalcony;
    }

    public boolean isPreferStorage() {
        return preferStorage;
    }

    public void setPreferStorage(boolean preferStorage) {
        this.preferStorage = preferStorage;
    }

    public boolean isRequireFinish() {
        return requireFinish;
    }

    public void setRequireFinish(boolean requireFinish) {
        this.requireFinish = requireFinish;
    }

    public Boolean getRequireFurniture() {
        return requireFurniture == null ? deal == DealType.RENT : requireFurniture;
    }

    public void setRequireFurniture(Boolean requireFurniture) {
        this.requireFurniture = requireFurniture;
    }

    public boolean isAllowApartments() {
        return allowApartments;
    }

    public void setAllowApartments(boolean allowApartments) {
        this.allowApartments = allowApartments;
    }

    public boolean isWeb() {
        return web;
    }

    public void setWeb(boolean web) {
        this.web = web;
    }
}
