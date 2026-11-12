package io.github.lelena1.flatfinder.analysis;

import io.github.lelena1.flatfinder.domain.Listing;
import io.github.lelena1.flatfinder.domain.Plural;
import io.github.lelena1.flatfinder.domain.SearchCriteria;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Применяет критерии пользователя к проанализированному объявлению.
 *
 * <p>Обязательное условие, которое точно нарушено, отсеивает квартиру. Обязательное условие,
 * о котором объявление молчит, переводит её в статус «уточнить» — такие квартиры не теряются,
 * потому что продавцы часто не заполняют поля. Желательные условия только меняют баллы.
 */
@Component
public class MatchScorer {

    public Match score(Listing listing, ListingAnalysis a, SearchCriteria c) {
        Result r = new Result();

        // --- обязательные условия ---
        if (listing.price() > c.maxPrice()) {
            r.reject("цена выше бюджета");
        }
        if (listing.apartments() && !c.allowApartments()) {
            r.reject("апартаменты, а не квартира");
        }
        layout(listing, a, c, r);
        if (c.requireFinish()) {
            r.mandatory(a.readyFinish(), "готовая отделка, можно сразу заехать", "нет готовой отделки",
                    "есть ли отделка, можно ли сразу заехать", 5);
        } else if (a.readyFinish() == Tri.YES) {
            r.pro("готовая отделка", 5);
        }
        balcony(a, c, r);
        if (c.requireFurniture()) {
            r.mandatory(a.furniture(), "есть мебель", "без мебели", "есть ли мебель", 5);
        } else if (a.furniture() == Tri.YES) {
            r.pro("есть мебель", 5);
        }

        // --- желательные условия ---
        if (c.preferKitchenBalcony()) {
            if (a.balconyFromKitchen() == Tri.YES) {
                r.pro("выход на балкон/лоджию из кухни", 10);
            } else if (a.balcony() != Tri.NO) {
                r.wish("откуда выход на балкон — из кухни или из комнаты");
            }
        }
        if (c.preferStorage()) {
            if (a.storage() == Tri.YES) {
                r.pro("есть места хранения (гардеробная/постирочная/кладовая)", 10);
            } else {
                r.wish("есть ли гардеробная, постирочная или кладовая");
            }
        }
        if (listing.kitchenArea() != null && listing.kitchenArea() >= 18) {
            r.score += 5;
        }
        if (listing.price() <= c.maxPrice()) {
            // небольшой бонус за запас по бюджету — при прочих равных
            r.score += (int) Math.round(5.0 * (c.maxPrice() - listing.price()) / c.maxPrice());
        }

        Verdict verdict = r.rejected ? Verdict.REJECT : r.mandatoryUnknown ? Verdict.CHECK : Verdict.MATCH;
        int score = r.rejected ? 0 : Math.min(Math.max(r.score, 0), 100); // шкала 0–100
        return new Match(listing, a, verdict, score,
                List.copyOf(r.pros), List.copyOf(r.toCheck), List.copyOf(r.cons));
    }

    /**
     * Балкон, лоджия или терраса — полноценный выход наружу. Французский балкон (окно в пол) засчитывается,
     * только если пользователь отметил, что он подходит, и даёт меньше баллов.
     */
    private static void balcony(ListingAnalysis a, SearchCriteria c, Result r) {
        if (a.balcony() == Tri.YES) {
            String kinds = a.balconyKinds().stream().filter(BalconyKind::walkOut)
                    .map(BalconyKind::title).collect(Collectors.joining(", "));
            r.pro("есть " + (kinds.isEmpty() ? "балкон/лоджия" : kinds)
                    + (a.hasFrenchBalcony() ? " и французский балкон" : ""), 10);
            return;
        }
        if (a.hasFrenchBalcony() && c.allowFrenchBalcony()) {
            r.pro("французский балкон (окно в пол: створку открыть можно, выйти на площадку — нет)",
                    c.requireBalcony() ? 5 : 3);
            return;
        }
        if (!c.requireBalcony()) {
            return;
        }
        if (a.balcony() == Tri.NO) {
            r.reject(a.hasFrenchBalcony() ? "только французский балкон" : "нет балкона и лоджии");
        } else {
            r.ask(c.allowFrenchBalcony() ? "есть ли балкон, лоджия или французский балкон" : "есть ли балкон или лоджия");
        }
    }

    private static void layout(Listing listing, ListingAnalysis a, SearchCriteria c, Result r) {
        Integer wanted = c.bedrooms();
        if (wanted == null) {
            return;
        }
        Integer actual = a.bedrooms();
        if (actual == null) {
            r.ask("планировка: сколько спален и есть ли кухня-гостиная");
            return;
        }
        if (!actual.equals(wanted)) {
            r.reject(actual == 1 && a.kitchenLiving() && wanted > 1 ? "евродвушка — только одна спальня"
                    : actual == 0 ? "студия, а нужно " + Plural.bedrooms(wanted)
                    : Plural.bedrooms(actual) + ", а нужно " + wanted);
            return;
        }
        String title = a.layoutTitle(listing);
        if (wanted == 0 || a.kitchenLiving()) {
            r.pro(title, 15);
        } else if (listing.kitchenArea() == null) {
            r.pro(title, 10);
            r.wish("какая площадь кухни");
        } else if (listing.kitchenArea() >= c.minKitchenArea()) {
            r.pro(title + ", кухня " + area(listing.kitchenArea()), 15);
        } else {
            r.minus("кухня всего " + area(listing.kitchenArea()), 10);
        }
    }

    private static String area(Double area) {
        return area == null ? "" : String.format("%.1f м²", area).replace('.', ',');
    }

    private static final class Result {
        final List<String> pros = new ArrayList<>();
        final List<String> toCheck = new ArrayList<>();
        final List<String> cons = new ArrayList<>();
        int score = 35; // база подобрана так, чтобы идеальная квартира набирала около 100
        boolean rejected;
        boolean mandatoryUnknown;

        void pro(String text, int points) {
            pros.add(text);
            score += points;
        }

        void minus(String text, int points) {
            cons.add(text);
            score -= points;
        }

        void reject(String text) {
            cons.add(text);
            rejected = true;
        }

        void ask(String question) {
            toCheck.add(question);
            mandatoryUnknown = true;
        }

        void wish(String question) {
            toCheck.add(question);
        }

        void mandatory(Tri value, String yes, String no, String question, int points) {
            switch (value) {
                case YES -> pro(yes, points);
                case NO -> reject(no);
                case UNKNOWN -> ask(question);
            }
        }
    }
}
