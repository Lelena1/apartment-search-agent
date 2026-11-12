package io.github.lelena1.flatfinder.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Требования к квартире.
 *
 * <p>Обязательные условия (нарушение — квартира отсеивается): цена, планировка «ровно две спальни
 * + кухня-гостиная или большая кухня», готовая отделка, наличие балкона/лоджии, для аренды — мебель.
 * Желательные условия только добавляют баллы: балкон из кухни, гардеробная/постирочная/кладовая.
 *
 * @param minKitchenArea с какой площади отдельная кухня считается «большой», м²
 */
public record SearchCriteria(
        DealType dealType,
        Set<District> districts,
        long maxPrice,
        double minKitchenArea,
        boolean allowApartments
) {
    public static final double DEFAULT_MIN_KITCHEN_AREA = 12.0;

    public SearchCriteria {
        districts = districts == null || districts.isEmpty()
                ? EnumSet.allOf(District.class)
                : EnumSet.copyOf(districts);
    }

    public static SearchCriteria defaultsFor(DealType dealType) {
        return new SearchCriteria(dealType, EnumSet.allOf(District.class),
                dealType.defaultMaxPrice(), DEFAULT_MIN_KITCHEN_AREA, false);
    }

    public boolean furnitureRequired() {
        return dealType == DealType.RENT;
    }
}
