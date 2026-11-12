package io.github.lelena1.flatfinder.analysis;

import io.github.lelena1.flatfinder.domain.Listing;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Разбирает объявление на признаки, важные для подбора.
 *
 * <p>Структурированным полям площадок доверять нельзя: число балконов часто стоит «0» по умолчанию,
 * а евротрёшку продавцы записывают то как «2-комн.», то как «3-комн.». Поэтому анализатор
 * опирается на текст описания, а поля использует только как подтверждение.
 */
@Component
public class ListingAnalyzer {

    /** «евро-3», «евротрёшка», «евро-4х» — число комнат вместе с кухней-гостиной. */
    private static final Pattern EURO = rx("евро-?\\s?(\\d)|евро-?\\s?(двух|двушк|трех|трешк|четыр|пяти)|евро(двушк|трешк)");
    private static final Pattern N_BEDROOMS = rx(
            "(\\d|одна|одной|две|двумя|двух|три|тремя|трех|четыре)\\s+((изолированн|отдельн|раздельн)\\w*\\s+)?(спальн[яиюе]|спален)");
    private static final Pattern N_ISOLATED_ROOMS = rx(
            "(\\d|две|двумя|двух|три|тремя|трех|четыре)\\s+(изолированн|отдельн|раздельн)\\w*\\s+комнат");
    private static final Pattern N_ROOMS = rx("(\\d|две|три|четыре)\\s+комнат");
    private static final Pattern ONE_BEDROOM = rx("(изолированн|отдельн)(ая|ую|ой)\\s+спальн");
    private static final Pattern SEPARATE_KITCHEN = rx("отдельн\\w*\\s+кухн|кухн\\w*\\s+\\(?отдельн");
    private static final Pattern STUDIO = rx("студи[яюи]");
    /** Отдельная кухня такой площади и больше без пояснений в тексте может оказаться кухней-гостиной. */
    private static final double AMBIGUOUS_KITCHEN_AREA = 15.0;
    private static final Pattern KITCHEN_LIVING = rx("кухн\\w*[- ]?(гостин|столов)|гостин\\w*[- ]кухн");

    private static final Pattern FRENCH_BALCONY = rx("французск\\w*\\s*балкон");
    private static final Pattern NO_BALCONY = rx("без балкона|без лоджии|нет балкона|нет лоджии|балкона нет|лоджии нет");
    private static final Pattern BALCONY = rx("лодж|балкон|террас");
    private static final Pattern LOGGIA = rx("лодж");
    private static final Pattern PLAIN_BALCONY = rx("балкон");
    private static final Pattern TERRACE = rx("террас");
    private static final Pattern BALCONY_FROM_KITCHEN = rx(
            "кухн\\w*[^.;]{0,40}\\s(с|на)\\s(лодж|балкон|террас)|" +
            "(лодж|балкон|террас)\\w*[^.;]{0,30}из кухн|" +
            "выход\\w*\\s*на\\s*(лодж|балкон|террас)\\w*\\s*из\\s*кухн");

    private static final Pattern COMMON_STORAGE = rx("хранени\\w*\\s*(велосипед|коляс|самокат)\\w*|колясочн\\w*|келлер\\w*");
    private static final Pattern STORAGE = rx("гардероб|постироч|прачечн|кладов|систем\\w*\\s*хранени|мест\\w*\\s*(для\\s*)?хранени|встроенн\\w*\\s*шкаф");

    private static final Pattern NOT_FINISHED = rx("без отделк|чернов\\w*\\s*отделк|отделк\\w*\\s*чернов|в бетоне|white ?box|вайт ?бокс|предчистов|без стен|под самоотделк");
    private static final Pattern NO_REPAIR = rx("без ремонта|без готового ремонта|под ремонт|требует ремонт");
    private static final Pattern FINISHED = rx("ремонт|чистов\\w*\\s*отделк|отделк\\w*\\s*чистов|под ключ|заезжай|можно сразу заехать|готова к (проживанию|заселению)");

    private static final Pattern NO_FURNITURE = rx("без мебели");
    private static final Pattern FURNITURE = rx("мебел|меблирован");

    public ListingAnalysis analyze(Listing listing) {
        String text = normalize(listing.descriptionOrEmpty());
        Layout layout = layout(listing, text);
        return new ListingAnalysis(
                layout.bedrooms(),
                layout.kitchenLiving(),
                balcony(listing, text),
                balconyKinds(listing, text),
                balconyFromKitchen(text),
                storage(text),
                finish(listing, text),
                furniture(listing, text));
    }

    record Layout(Integer bedrooms, boolean kitchenLiving) {
    }

    /**
     * Сколько в квартире спален. Число комнат площадки ненадёжно: евротрёшку записывают и как
     * «2-комн.», и как «3-комн.», поэтому сначала смотрим на текст и только потом на поля.
     */
    Layout layout(Listing listing, String text) {
        boolean kitchenLiving = KITCHEN_LIVING.matcher(text).find();
        boolean separateKitchen = SEPARATE_KITCHEN.matcher(text).find();

        Matcher euro = EURO.matcher(text);
        if (euro.find()) {
            Integer total = number(firstGroup(euro));
            if (total != null && total > 1) {
                return new Layout(total - 1, true);
            }
        }
        Matcher m = N_BEDROOMS.matcher(text);
        if (m.find()) {
            return new Layout(number(m.group(1)), kitchenLiving);
        }
        m = N_ISOLATED_ROOMS.matcher(text);
        if (m.find()) {
            return new Layout(number(m.group(1)), kitchenLiving);
        }
        if (ONE_BEDROOM.matcher(text).find() && kitchenLiving) {
            return new Layout(1, true);
        }
        if (STUDIO.matcher(text).find() && listing.rooms() <= 1) {
            return new Layout(0, true);
        }
        if (!kitchenLiving) {
            m = N_ROOMS.matcher(text);
            if (separateKitchen && m.find()) {
                return new Layout(number(m.group(1)), false);
            }
            boolean smallKitchen = listing.kitchenArea() != null && listing.kitchenArea() < AMBIGUOUS_KITCHEN_AREA;
            if (listing.rooms() > 0 && (separateKitchen || smallKitchen)) {
                return new Layout(listing.rooms(), false);
            }
        }
        // «3-комн.» без описания — и евротрёшка, и обычная трёшка; решает человек.
        return new Layout(null, kitchenLiving);
    }

    private static String firstGroup(Matcher m) {
        for (int i = 1; i <= m.groupCount(); i++) {
            if (m.group(i) != null) {
                return m.group(i);
            }
        }
        return null;
    }

    private static Integer number(String word) {
        if (word == null) {
            return null;
        }
        if (word.chars().allMatch(Character::isDigit)) {
            return Integer.parseInt(word);
        }
        if (word.startsWith("одн")) {
            return 1;
        }
        if (word.startsWith("дв")) {
            return 2;
        }
        if (word.startsWith("тр")) {
            return 3;
        }
        if (word.startsWith("четыр")) {
            return 4;
        }
        if (word.startsWith("пят")) {
            return 5;
        }
        return null;
    }

    /**
     * Есть ли настоящий балкон, на который можно выйти: балкон, лоджия или терраса.
     * Французский балкон сюда не входит — это окно в пол с ограждением; он учитывается отдельно,
     * см. {@link #balconyKinds}.
     */
    Tri balcony(Listing listing, String text) {
        if (NO_BALCONY.matcher(text).find()) {
            return Tri.NO;
        }
        String withoutFrench = FRENCH_BALCONY.matcher(text).replaceAll(" ");
        if (BALCONY.matcher(withoutFrench).find() || positive(listing.balconies()) || positive(listing.loggias())) {
            return Tri.YES;
        }
        if (FRENCH_BALCONY.matcher(text).find()) {
            return Tri.NO;
        }
        return Tri.UNKNOWN; // «0» в поле площадки ничего не доказывает: его часто не заполняют
    }

    /** Какие виды балконов упомянуты в объявлении — для показа и для критерия «французский балкон подходит». */
    Set<BalconyKind> balconyKinds(Listing listing, String text) {
        Set<BalconyKind> kinds = EnumSet.noneOf(BalconyKind.class);
        if (FRENCH_BALCONY.matcher(text).find()) {
            kinds.add(BalconyKind.FRENCH);
        }
        String withoutFrench = FRENCH_BALCONY.matcher(text).replaceAll(" ");
        if (NO_BALCONY.matcher(withoutFrench).find()) {
            return kinds;
        }
        if (LOGGIA.matcher(withoutFrench).find() || positive(listing.loggias())) {
            kinds.add(BalconyKind.LOGGIA);
        }
        if (PLAIN_BALCONY.matcher(withoutFrench).find() || positive(listing.balconies())) {
            kinds.add(BalconyKind.BALCONY);
        }
        if (TERRACE.matcher(withoutFrench).find()) {
            kinds.add(BalconyKind.TERRACE);
        }
        return kinds;
    }

    Tri balconyFromKitchen(String text) {
        return BALCONY_FROM_KITCHEN.matcher(text).find() ? Tri.YES : Tri.UNKNOWN;
    }

    Tri storage(String text) {
        String inFlatOnly = COMMON_STORAGE.matcher(text).replaceAll(" ");
        return STORAGE.matcher(inFlatOnly).find() ? Tri.YES : Tri.UNKNOWN;
    }

    Tri finish(Listing listing, String text) {
        if (listing.finish() != null) {
            return Tri.of(listing.finish().readyToLive());
        }
        if (NOT_FINISHED.matcher(text).find()) {
            return Tri.NO;
        }
        if (NO_REPAIR.matcher(text).find()) {
            return Tri.NO;
        }
        return FINISHED.matcher(text).find() ? Tri.YES : Tri.UNKNOWN;
    }

    Tri furniture(Listing listing, String text) {
        if (NO_FURNITURE.matcher(text).find()) {
            return Tri.NO;
        }
        if (listing.furnished() != null) {
            return Tri.of(listing.furnished());
        }
        return FURNITURE.matcher(text).find() ? Tri.YES : Tri.UNKNOWN;
    }

    private static boolean positive(Integer value) {
        return value != null && value > 0;
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replace('ё', 'е').replaceAll("\\s+", " ");
    }

    private static Pattern rx(String regex) {
        // UNICODE_CHARACTER_CLASS: без него \w в Java совпадает только с латиницей
        return Pattern.compile(regex.replace("ё", "е").replace("[ее]", "е"),
                Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
    }
}
