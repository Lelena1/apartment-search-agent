package io.github.lelena1.flatfinder.domain;

import java.util.List;
import java.util.Locale;

/**
 * Требования пользователя к квартире. Все параметры задаются в форме.
 *
 * <p>Обязательные условия (нарушение — квартира отсеивается): цена, число спален, апартаменты,
 * а также те условия, которые пользователь отметил как обязательные (балкон, отделка, мебель).
 * Желательные условия только добавляют баллы.
 *
 * @param districts              районы или ЖК; пусто — все районы из базы. Сравнение по вхождению подстроки
 * @param bedrooms               сколько нужно спален (0 — студия); {@code null} — неважно
 * @param minKitchenArea         с какой площади отдельная кухня считается «большой», м²
 * @param requireBalcony         балкон или лоджия обязательны
 * @param preferKitchenBalcony   желательно, чтобы выход на балкон был из кухни
 * @param preferStorage          желательны гардеробная / постирочная / кладовая
 * @param requireFinish          нужна готовая отделка, чтобы сразу заехать
 * @param requireFurniture       нужна мебель
 * @param allowApartments        апартаменты допустимы
 */
public record SearchCriteria(
        DealType dealType,
        List<String> districts,
        long maxPrice,
        Integer bedrooms,
        double minKitchenArea,
        boolean requireBalcony,
        boolean preferKitchenBalcony,
        boolean preferStorage,
        boolean requireFinish,
        boolean requireFurniture,
        boolean allowApartments
) {
    public static final double DEFAULT_MIN_KITCHEN_AREA = 12.0;

    public SearchCriteria {
        districts = districts == null ? List.of() : districts.stream()
                .map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
    }

    /** Пример из README: две спальни, балкон, готовая отделка; для аренды — с мебелью. */
    public static SearchCriteria defaultsFor(DealType dealType) {
        return new SearchCriteria(dealType, List.of(), dealType.defaultMaxPrice(), 2,
                DEFAULT_MIN_KITCHEN_AREA, true, true, true, true, dealType == DealType.RENT, false);
    }

    public SearchCriteria withDistricts(List<String> newDistricts) {
        return new SearchCriteria(dealType, newDistricts, maxPrice, bedrooms, minKitchenArea, requireBalcony,
                preferKitchenBalcony, preferStorage, requireFinish, requireFurniture, allowApartments);
    }

    public boolean matchesDistrict(Listing listing) {
        if (districts.isEmpty()) {
            return true;
        }
        String where = (listing.district() + " " + listing.address()).toLowerCase(Locale.ROOT).replace('ё', 'е');
        return districts.stream()
                .map(d -> d.toLowerCase(Locale.ROOT).replace('ё', 'е'))
                .anyMatch(where::contains);
    }

    /** Требования человеческим языком — для промпта агента и подписи на странице. */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        if (bedrooms != null) {
            sb.append(bedrooms == 0 ? "- планировка: студия;\n"
                    : "- планировка: РОВНО " + Plural.bedrooms(bedrooms)
                    + " + кухня-гостиная или отдельная кухня от " + format(minKitchenArea) + " м²;\n");
        }
        sb.append(requireBalcony ? "- обязательно балкон или лоджия" : "- балкон или лоджия — плюс, но не обязательно");
        sb.append(preferKitchenBalcony ? "; желательно с выходом из кухни;\n" : ";\n");
        if (preferStorage) {
            sb.append("- желательно места хранения: гардеробная, постирочная, кладовая;\n");
        }
        if (requireFinish) {
            sb.append("- нужна готовая отделка, чтобы сразу заехать;\n");
        }
        if (requireFurniture) {
            sb.append("- обязательно мебель;\n");
        }
        if (!allowApartments) {
            sb.append("- апартаменты не подходят;\n");
        }
        return sb.toString();
    }

    private static String format(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v).replace('.', ',');
    }
}
