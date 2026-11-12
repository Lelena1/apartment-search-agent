package io.github.lelena1.flatfinder.analysis;

import io.github.lelena1.flatfinder.domain.Listing;
import io.github.lelena1.flatfinder.domain.Plural;

/**
 * Что удалось понять об объявлении: из структурированных полей и из текста описания.
 *
 * @param bedrooms      сколько спален (0 — студия; {@code null} — из объявления не понять)
 * @param kitchenLiving кухня объединена с гостиной (европланировка)
 */
public record ListingAnalysis(
        Integer bedrooms,
        boolean kitchenLiving,
        Tri balcony,
        Tri balconyFromKitchen,
        Tri storage,
        Tri readyFinish,
        Tri furniture
) {
    /** Планировка человеческим языком, например «2 спальни + кухня-гостиная». */
    public String layoutTitle(Listing listing) {
        if (bedrooms == null) {
            return "Планировка не ясна из объявления";
        }
        if (bedrooms == 0) {
            return "Студия";
        }
        if (kitchenLiving) {
            return Plural.bedrooms(bedrooms) + " + кухня-гостиная";
        }
        return Plural.rooms(bedrooms) + " + отдельная кухня";
    }
}
