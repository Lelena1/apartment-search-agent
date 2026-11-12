package io.github.lelena1.flatfinder.analysis;

import io.github.lelena1.flatfinder.domain.Listing;
import io.github.lelena1.flatfinder.domain.SearchCriteria;
import io.github.lelena1.flatfinder.source.ListingSource;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Собирает объявления из всех источников, анализирует и сортирует от лучших к худшим. */
@Service
public class ListingSearchService {

    private static final Comparator<Match> BEST_FIRST = Comparator
            .comparing(Match::verdict)                       // MATCH, затем CHECK, затем REJECT
            .thenComparing(Match::score, Comparator.reverseOrder())
            .thenComparing(m -> m.listing().price());

    private final List<ListingSource> sources;
    private final ListingAnalyzer analyzer;
    private final MatchScorer scorer;

    public ListingSearchService(List<ListingSource> sources, ListingAnalyzer analyzer, MatchScorer scorer) {
        this.sources = sources;
        this.analyzer = analyzer;
        this.scorer = scorer;
    }

    public SearchResult search(SearchCriteria criteria) {
        List<Match> matches = sources.stream()
                .flatMap(s -> s.fetch(criteria).stream())
                .map(l -> evaluate(l, criteria))
                .sorted(BEST_FIRST)
                .toList();
        return new SearchResult(criteria, matches, sources.stream().map(ListingSource::name).toList());
    }

    public Match evaluate(Listing listing, SearchCriteria criteria) {
        return scorer.score(listing, analyzer.analyze(listing), criteria);
    }

    /** Все районы из подключённых источников — подсказки для формы. */
    public Set<String> knownDistricts() {
        return sources.stream().flatMap(s -> s.districts().stream())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public Optional<Match> findById(String id, SearchCriteria criteria) {
        return search(criteria).matches().stream().filter(m -> m.listing().id().equals(id)).findFirst();
    }

    public record SearchResult(SearchCriteria criteria, List<Match> matches, List<String> sources) {

        public List<Match> withVerdict(Verdict verdict) {
            return matches.stream().filter(m -> m.verdict() == verdict).toList();
        }

        public Map<Verdict, Long> counts() {
            return matches.stream().collect(Collectors.groupingBy(Match::verdict, Collectors.counting()));
        }

        public long count(Verdict verdict) {
            return counts().getOrDefault(verdict, 0L);
        }

        public Map<String, Match> byId() {
            return matches.stream().collect(Collectors.toMap(m -> m.listing().id(), Function.identity()));
        }
    }
}
