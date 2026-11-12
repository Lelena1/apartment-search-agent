package io.github.lelena1.flatfinder;

import io.github.lelena1.flatfinder.analysis.ListingAnalyzer;
import io.github.lelena1.flatfinder.analysis.ListingSearchService;
import io.github.lelena1.flatfinder.analysis.ListingSearchService.SearchResult;
import io.github.lelena1.flatfinder.analysis.Match;
import io.github.lelena1.flatfinder.analysis.MatchScorer;
import io.github.lelena1.flatfinder.analysis.Verdict;
import io.github.lelena1.flatfinder.domain.DealType;
import io.github.lelena1.flatfinder.domain.SearchCriteria;
import io.github.lelena1.flatfinder.source.DemoListingSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Проверяет полный конвейер «источник → анализ → оценка» на демо-базе. */
class ListingSearchServiceTest {

    private final ListingSearchService service = new ListingSearchService(
            List.of(new DemoListingSource()), new ListingAnalyzer(), new MatchScorer());

    private Map<String, Match> run(SearchCriteria criteria) {
        return service.search(criteria).byId();
    }

    private Map<String, Match> run(DealType deal) {
        return run(SearchCriteria.defaultsFor(deal));
    }

    @Test
    void saleVerdicts() {
        Map<String, Match> m = run(DealType.SALE);
        assertThat(m.get("demo-zs-01").verdict()).isEqualTo(Verdict.MATCH);   // евротрёшка, лоджия из кухни
        assertThat(m.get("demo-rs-01").verdict()).isEqualTo(Verdict.MATCH);   // кухня с лоджией
        assertThat(m.get("demo-zs-02").verdict()).isEqualTo(Verdict.CHECK);   // балкон не указан
        assertThat(m.get("demo-zs-03").cons()).contains("евродвушка — только одна спальня");
        assertThat(m.get("demo-zs-04").cons()).contains("3 спальни, а нужно 2");
        assertThat(m.get("demo-zs-05").cons()).contains("нет готовой отделки");
        assertThat(m.get("demo-zs-06").cons()).contains("цена выше бюджета");
        assertThat(m.get("demo-rs-03").cons()).contains("апартаменты, а не квартира");
    }

    @Test
    void frenchBalconyIsASeparateKind() {
        // по умолчанию французский балкон подходит, но баллов даёт меньше, чем настоящий балкон
        Map<String, Match> m = run(DealType.SALE);
        assertThat(m.get("demo-rs-06").verdict()).isEqualTo(Verdict.MATCH);
        assertThat(m.get("demo-rs-06").pros()).anyMatch(p -> p.startsWith("французский балкон"));
        assertThat(m.get("demo-rs-06").score()).isLessThan(m.get("demo-zs-01").score());

        // если французский балкон не подходит — квартира отсеивается с понятной причиной
        SearchCriteria strict = new SearchCriteria(DealType.SALE, List.of(), 45_000_000, 2, 12,
                true, false, true, true, true, false, false);
        assertThat(run(strict).get("demo-rs-06").cons()).contains("только французский балкон");
    }

    @Test
    void smallKitchenIsAMinusButNotARejection() {
        Map<String, Match> m = run(DealType.SALE);
        assertThat(m.get("demo-rs-04").verdict()).isEqualTo(Verdict.MATCH);
        assertThat(m.get("demo-rs-04").cons()).anyMatch(c -> c.startsWith("кухня всего"));
        assertThat(m.get("demo-rs-04").score()).isLessThan(m.get("demo-rs-01").score());
    }

    @Test
    void rentRequiresFurnitureByDefault() {
        Map<String, Match> m = run(DealType.RENT);
        assertThat(m.get("demo-zr-04").cons()).contains("без мебели");
        assertThat(m.get("demo-rr-04").verdict()).isEqualTo(Verdict.MATCH);
        assertThat(m.get("demo-rr-02").verdict()).isEqualTo(Verdict.CHECK);
    }

    @Test
    void bestMatchComesFirst() {
        SearchResult result = service.search(SearchCriteria.defaultsFor(DealType.RENT)
                .withDistricts(List.of("ЗИЛАРТ", "Ривер Парк")));
        assertThat(result.matches().get(0).listing().id()).isEqualTo("demo-rr-04"); // лоджия из кухни + хранение
        assertThat(result.matches()).last().extracting(Match::verdict).isEqualTo(Verdict.REJECT);
    }

    @Test
    void filtersByDistrictSubstringIgnoringCase() {
        SearchCriteria riverOnly = SearchCriteria.defaultsFor(DealType.SALE).withDistricts(List.of("ривер"));
        assertThat(service.search(riverOnly).matches())
                .allMatch(m -> m.listing().district().equals("Ривер Парк Коломенское"))
                .allMatch(m -> m.listing().dealType() == DealType.SALE)
                .hasSize(6);
    }

    @Test
    void otherRequirementsWorkToo() {
        // однушка в Хамовниках, балкон не обязателен
        SearchCriteria oneBedroom = new SearchCriteria(DealType.SALE, List.of("Хамовники"), 30_000_000, 1,
                9, false, false, false, false, true, false, false);
        Map<String, Match> m = run(oneBedroom);
        assertThat(m.get("demo-hs-01").verdict()).isEqualTo(Verdict.MATCH);
        assertThat(m.get("demo-hs-03").cons()).contains("студия, а нужно 1 спальня");

        // большая семья: три спальни в аренду в Митино
        SearchCriteria threeBedrooms = new SearchCriteria(DealType.RENT, List.of("Митино"), 100_000, 3,
                12, true, false, false, true, true, true, false);
        Map<String, Match> r = run(threeBedrooms);
        assertThat(r.get("demo-mr-01").verdict()).isEqualTo(Verdict.MATCH);
        assertThat(r.get("demo-mr-02").verdict()).isEqualTo(Verdict.REJECT);
    }

    @Test
    void anyBedroomsWhenNotSpecified() {
        SearchCriteria any = new SearchCriteria(DealType.RENT, List.of("Митино"), 100_000, null,
                12, true, false, false, false, true, true, false);
        assertThat(run(any).values()).noneMatch(m -> m.verdict() == Verdict.REJECT);
    }
}
