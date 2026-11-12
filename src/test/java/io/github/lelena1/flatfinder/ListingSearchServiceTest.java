package io.github.lelena1.flatfinder;

import io.github.lelena1.flatfinder.analysis.ListingAnalyzer;
import io.github.lelena1.flatfinder.analysis.ListingSearchService;
import io.github.lelena1.flatfinder.analysis.ListingSearchService.SearchResult;
import io.github.lelena1.flatfinder.analysis.Match;
import io.github.lelena1.flatfinder.analysis.MatchScorer;
import io.github.lelena1.flatfinder.analysis.Verdict;
import io.github.lelena1.flatfinder.domain.DealType;
import io.github.lelena1.flatfinder.domain.District;
import io.github.lelena1.flatfinder.domain.SearchCriteria;
import io.github.lelena1.flatfinder.source.DemoListingSource;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Проверяет полный конвейер «источник → анализ → оценка» на демо-базе. */
class ListingSearchServiceTest {

    private final ListingSearchService service = new ListingSearchService(
            List.of(new DemoListingSource()), new ListingAnalyzer(), new MatchScorer());

    private Map<String, Match> run(DealType deal) {
        return service.search(SearchCriteria.defaultsFor(deal)).byId();
    }

    @Test
    void saleVerdicts() {
        Map<String, Match> m = run(DealType.SALE);
        assertThat(m.get("demo-zs-01").verdict()).isEqualTo(Verdict.MATCH);   // евротрёшка, лоджия из кухни
        assertThat(m.get("demo-rs-01").verdict()).isEqualTo(Verdict.MATCH);   // кухня с лоджией
        assertThat(m.get("demo-zs-02").verdict()).isEqualTo(Verdict.CHECK);   // балкон не указан
        assertThat(m.get("demo-zs-03").cons()).contains("евродвушка — только одна спальня");
        assertThat(m.get("demo-zs-04").verdict()).isEqualTo(Verdict.REJECT);  // три спальни
        assertThat(m.get("demo-zs-05").cons()).contains("нет готовой отделки");
        assertThat(m.get("demo-zs-06").cons()).contains("цена выше бюджета");
        assertThat(m.get("demo-rs-03").cons()).contains("апартаменты, а не квартира");
        assertThat(m.get("demo-rs-06").cons()).contains("нет балкона и лоджии"); // только французский балкон
    }

    @Test
    void smallKitchenIsAMinusButNotARejection() {
        Match m = run(DealType.SALE).get("demo-rs-04");
        assertThat(m.verdict()).isEqualTo(Verdict.MATCH);
        assertThat(m.cons()).anyMatch(c -> c.startsWith("кухня всего"));
        assertThat(m.score()).isLessThan(run(DealType.SALE).get("demo-rs-01").score());
    }

    @Test
    void rentRequiresFurniture() {
        Map<String, Match> m = run(DealType.RENT);
        assertThat(m.get("demo-zr-04").cons()).contains("сдаётся без мебели");
        assertThat(m.get("demo-rr-04").verdict()).isEqualTo(Verdict.MATCH);
        assertThat(m.get("demo-rr-02").verdict()).isEqualTo(Verdict.CHECK);
    }

    @Test
    void bestMatchComesFirst() {
        SearchResult result = service.search(SearchCriteria.defaultsFor(DealType.RENT));
        assertThat(result.matches().get(0).listing().id()).isEqualTo("demo-rr-04"); // лоджия из кухни + хранение
        assertThat(result.matches()).last().extracting(Match::verdict).isEqualTo(Verdict.REJECT);
    }

    @Test
    void filtersByDistrictAndDeal() {
        SearchCriteria onlyRiver = new SearchCriteria(DealType.SALE, EnumSet.of(District.RIVER_PARK),
                45_000_000, 12, false);
        assertThat(service.search(onlyRiver).matches())
                .allMatch(m -> m.listing().district() == District.RIVER_PARK)
                .allMatch(m -> m.listing().dealType() == DealType.SALE)
                .hasSize(6);
    }
}
