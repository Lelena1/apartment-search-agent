package io.github.lelena1.flatfinder;

import io.github.lelena1.flatfinder.analysis.ListingAnalysis;
import io.github.lelena1.flatfinder.analysis.ListingAnalyzer;
import io.github.lelena1.flatfinder.analysis.Tri;
import io.github.lelena1.flatfinder.domain.DealType;
import io.github.lelena1.flatfinder.domain.Listing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ListingAnalyzerTest {

    private final ListingAnalyzer analyzer = new ListingAnalyzer();

    static Listing listing(int rooms, Double kitchen, String description) {
        return new Listing("t", "test", null, DealType.SALE, "Район", "адрес", 40_000_000, 60, kitchen,
                5, 16, rooms, false, null, null, null, null, description);
    }

    private ListingAnalysis analyze(int rooms, Double kitchen, String description) {
        return analyzer.analyze(listing(rooms, kitchen, description));
    }

    @ParameterizedTest(name = "{2} → спален: {3}, кухня-гостиная: {4}")
    @CsvSource(delimiter = '|', nullValues = "null", value = {
            "2 | 18.0 | Продаётся ЕВРОТРЁШКА, две изолированные комнаты          | 2    | true",
            "2 | 18.0 | Две изолированные спальни и большая кухня-гостиная      | 2    | true",
            "3 | 20.0 | Евро-3 комнатная с видом на аллею                        | 2    | true",
            "2 | 20.0 | Евродвушка: отдельная спальня и кухня-гостиная          | 1    | true",
            "2 | 20.0 | Изолированная спальня и просторная кухня-гостиная       | 1    | true",
            "3 | 22.0 | Сдается евро-4х комнатная квартира                       | 3    | true",
            "3 | 12.0 | Три спальни и кухня                                      | 3    | false",
            "2 | 13.0 | Отличная квартира с ремонтом                             | 2    | false",
            "2 | 9.5  | Отличная квартира с ремонтом                             | 2    | false",
            "2 | 12.0 | Изолированная спальня, гостиная и отдельная кухня 12 м² | 2    | false",
            "1 | null | Уютная студия после ремонта                              | 0    | true",
            "3 | 17.0 | Просторная квартира в новом доме                         | null | false",
            "2 | 15.0 | Диван и два спальных места                               | null | false",
    })
    void countsBedrooms(int rooms, Double kitchen, String text, Integer bedrooms, boolean kitchenLiving) {
        ListingAnalysis a = analyze(rooms, kitchen, text);
        assertThat(a.bedrooms()).isEqualTo(bedrooms);
        assertThat(a.kitchenLiving()).isEqualTo(kitchenLiving);
    }

    @Test
    void frenchBalconyIsNotABalcony() {
        assertThat(analyze(2, 15.0, "Панорамные окна с французским балконом").balcony()).isEqualTo(Tri.NO);
    }

    @Test
    void zeroInBalconyFieldMeansUnknownNotNo() {
        Listing l = new Listing("t", "test", null, DealType.SALE, "Район", "адрес", 1, 60, 15.0,
                5, 16, 2, false, 0, 0, null, null, "Хорошая квартира");
        assertThat(analyzer.analyze(l).balcony()).isEqualTo(Tri.UNKNOWN);
    }

    @Test
    void loggiaInKitchenIsRecognised() {
        ListingAnalysis a = analyze(2, 12.0, "Кухня с лоджией 12 м², 2 спальни по 15 м²");
        assertThat(a.balcony()).isEqualTo(Tri.YES);
        assertThat(a.balconyFromKitchen()).isEqualTo(Tri.YES);
    }

    @Test
    void bicycleStorageInTheBuildingDoesNotCount() {
        assertThat(analyze(2, 15.0, "На этаже помещение для хранения велосипедов и колясок").storage())
                .isEqualTo(Tri.UNKNOWN);
        assertThat(analyze(2, 15.0, "Есть гардеробная и постирочная").storage()).isEqualTo(Tri.YES);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(delimiter = '|', value = {
            "Квартира без отделки, можно сделать ремонт под себя | NO",
            "Состояние «в бетоне» даёт свободу                   | NO",
            "Отделка white box, есть дизайн-проект                | NO",
            "Дизайнерский ремонт, мебель остаётся                 | YES",
            "Отделка чистовая, высота потолков 3,15 м             | YES",
            "Один взрослый собственник                            | UNKNOWN",
    })
    void recognisesFinish(String text, Tri expected) {
        assertThat(analyze(2, 15.0, text).readyFinish()).isEqualTo(expected);
    }

    @Test
    void furnitureFromText() {
        assertThat(analyze(2, 15.0, "С ремонтом, но без мебели").furniture()).isEqualTo(Tri.NO);
        assertThat(analyze(2, 15.0, "Полностью меблирована").furniture()).isEqualTo(Tri.YES);
    }
}
