package io.github.lelena1.flatfinder.analysis;

import io.github.lelena1.flatfinder.domain.Listing;
import io.github.lelena1.flatfinder.domain.SearchCriteria;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Применяет критерии к проанализированному объявлению.
 *
 * <p>Обязательное условие, которое точно нарушено, отсеивает квартиру. Обязательное условие,
 * о котором объявление молчит, переводит её в статус «уточнить» — такие квартиры не теряются,
 * потому что продавцы часто не заполняют поля. Желательные условия только меняют баллы.
 */
@Component
public class MatchScorer {

    public Match score(Listing listing, ListingAnalysis a, SearchCriteria criteria) {
        Result r = new Result();

        // --- обязательные условия ---
        if (listing.price() > criteria.maxPrice()) {
            r.reject("цена выше бюджета");
        }
        if (listing.apartments() && !criteria.allowApartments()) {
            r.reject("апартаменты, а не квартира");
        }
        switch (a.layout()) {
            case EURO_THREE -> r.pro("2 спальни + кухня-гостиная", 15);
            case TWO_BEDROOMS_BIG_KITCHEN -> r.pro("2 комнаты + большая кухня " + area(listing.kitchenArea()), 15);
            case TWO_BEDROOMS_SMALL_KITCHEN -> r.minus("кухня всего " + area(listing.kitchenArea()), 10);
            case EURO_TWO -> r.reject("евродвушка — только одна спальня");
            case THREE_BEDROOMS -> r.reject("три спальни и больше — планировка не та");
            case UNKNOWN -> r.ask("планировка: 2 спальни + кухня-гостиная или нет");
        }
        r.mandatory(a.readyFinish(), "готовая отделка, можно сразу заехать", "нет готовой отделки", "есть ли отделка, можно ли сразу заехать", 5);
        r.mandatory(a.balcony(), "есть балкон/лоджия", "нет балкона и лоджии", "есть ли балкон или лоджия", 10);
        if (criteria.furnitureRequired()) {
            r.mandatory(a.furniture(), "сдаётся с мебелью", "сдаётся без мебели", "есть ли мебель", 5);
        }

        // --- желательные условия ---
        if (a.balconyFromKitchen() == Tri.YES) {
            r.pro("выход на балкон/лоджию из кухни", 10);
        } else if (a.balcony() != Tri.NO) {
            r.wish("откуда выход на балкон — из кухни или из комнаты");
        }
        if (a.storage() == Tri.YES) {
            r.pro("есть места хранения (гардеробная/постирочная/кладовая)", 10);
        } else {
            r.wish("есть ли гардеробная, постирочная или кладовая");
        }
        if (listing.kitchenArea() != null && listing.kitchenArea() >= 18) {
            r.score += 5;
        }
        if (listing.price() <= criteria.maxPrice()) {
            // небольшой бонус за запас по бюджету — при прочих равных
            r.score += (int) Math.round(5.0 * (criteria.maxPrice() - listing.price()) / criteria.maxPrice());
        }

        Verdict verdict = r.rejected ? Verdict.REJECT : r.mandatoryUnknown ? Verdict.CHECK : Verdict.MATCH;
        int score = r.rejected ? 0 : Math.min(Math.max(r.score, 0), 100); // шкала 0–100
        return new Match(listing, a, verdict, score,
                List.copyOf(r.pros), List.copyOf(r.toCheck), List.copyOf(r.cons));
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
