package io.github.lelena1.flatfinder.analysis;

import io.github.lelena1.flatfinder.domain.Listing;
import org.springframework.stereotype.Component;

import java.util.Locale;
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

    private static final Pattern EURO_TWO = rx("евро-?\\s?2|евро-?\\s?двух|евродвушк|евро двушк");
    private static final Pattern EURO_THREE = rx("евро-?\\s?3|евро-?\\s?тр[её]х|евротр[её]шк|евро тр[её]шк");
    private static final Pattern EURO_FOUR = rx("евро-?\\s?4|евро-?\\s?четыр");
    private static final Pattern TWO_BEDROOMS = rx(
            "(две|2)\\s+((изолированн|отдельн|раздельн)\\w*\\s+)?(спальн|комнат)|двумя\\s+спальн");
    private static final Pattern THREE_BEDROOMS = rx("(три|3)\\s*(изолированн\\w*\\s*)?спальн|(три|3)\\s*изолированн\\w*\\s*комнат");
    private static final Pattern ONE_BEDROOM = rx("(изолированн|отдельн)(ая|ую)\\s*спальн|одна спальн|1 спальн");
    private static final Pattern KITCHEN_LIVING = rx("кухн\\w*[- ]?(гостин|столов)|гостин\\w*[- ]кухн");

    private static final Pattern FRENCH_BALCONY = rx("французск\\w*\\s*балкон");
    private static final Pattern NO_BALCONY = rx("без балкона|без лоджии|нет балкона|нет лоджии|балкона нет|лоджии нет");
    private static final Pattern BALCONY = rx("лодж|балкон|террас");
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

    public ListingAnalysis analyze(Listing listing, double minKitchenArea) {
        String text = normalize(listing.descriptionOrEmpty());
        return new ListingAnalysis(
                layout(listing, text, minKitchenArea),
                balcony(listing, text),
                balconyFromKitchen(text),
                storage(text),
                finish(listing, text),
                furniture(listing, text));
    }

    LayoutType layout(Listing listing, String text, double minKitchenArea) {
        boolean kitchenLiving = KITCHEN_LIVING.matcher(text).find();
        boolean twoBedrooms = TWO_BEDROOMS.matcher(text).find();

        if (EURO_FOUR.matcher(text).find() || THREE_BEDROOMS.matcher(text).find()) {
            return LayoutType.THREE_BEDROOMS;
        }
        if (EURO_THREE.matcher(text).find() || (twoBedrooms && kitchenLiving)) {
            return LayoutType.EURO_THREE;
        }
        if (EURO_TWO.matcher(text).find() || (ONE_BEDROOM.matcher(text).find() && kitchenLiving)) {
            return LayoutType.EURO_TWO;
        }
        if (listing.rooms() == 2 && !kitchenLiving && listing.kitchenArea() != null) {
            return listing.kitchenArea() >= minKitchenArea
                    ? LayoutType.TWO_BEDROOMS_BIG_KITCHEN
                    : LayoutType.TWO_BEDROOMS_SMALL_KITCHEN;
        }
        if (listing.rooms() >= 4) {
            return LayoutType.THREE_BEDROOMS;
        }
        // «3-комн.» без описания может оказаться и евротрёшкой, и обычной трёшкой;
        // «2-комн.» с кухней-гостиной — и евротрёшкой, и евродвушкой. Решает человек.
        return LayoutType.UNKNOWN;
    }

    Tri balcony(Listing listing, String text) {
        if (NO_BALCONY.matcher(text).find()) {
            return Tri.NO;
        }
        String withoutFrench = FRENCH_BALCONY.matcher(text).replaceAll(" ");
        if (BALCONY.matcher(withoutFrench).find() || positive(listing.balconies()) || positive(listing.loggias())) {
            return Tri.YES;
        }
        if (FRENCH_BALCONY.matcher(text).find()) {
            return Tri.NO; // французский балкон — это окно в пол с ограждением, выйти на него нельзя
        }
        return Tri.UNKNOWN; // «0» в поле площадки ничего не доказывает: его часто не заполняют
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
