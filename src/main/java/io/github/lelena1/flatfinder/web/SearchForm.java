package io.github.lelena1.flatfinder.web;

import io.github.lelena1.flatfinder.domain.DealType;
import io.github.lelena1.flatfinder.domain.District;
import io.github.lelena1.flatfinder.domain.SearchCriteria;

import java.util.EnumSet;
import java.util.Set;

/** Параметры формы поиска. Пустые поля заменяются значениями по умолчанию для выбранного сценария. */
public class SearchForm {

    private DealType deal = DealType.SALE;
    private Set<District> districts = EnumSet.allOf(District.class);
    private Long maxPrice;
    private boolean web;

    public SearchCriteria toCriteria() {
        long budget = maxPrice == null || maxPrice <= 0 ? deal.defaultMaxPrice() : maxPrice;
        return new SearchCriteria(deal, districts, budget, SearchCriteria.DEFAULT_MIN_KITCHEN_AREA, false);
    }

    public DealType getDeal() {
        return deal;
    }

    public void setDeal(DealType deal) {
        this.deal = deal == null ? DealType.SALE : deal;
    }

    public Set<District> getDistricts() {
        return districts;
    }

    public void setDistricts(Set<District> districts) {
        this.districts = districts == null || districts.isEmpty() ? EnumSet.allOf(District.class) : districts;
    }

    public Long getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Long maxPrice) {
        this.maxPrice = maxPrice;
    }

    public boolean isWeb() {
        return web;
    }

    public void setWeb(boolean web) {
        this.web = web;
    }
}
