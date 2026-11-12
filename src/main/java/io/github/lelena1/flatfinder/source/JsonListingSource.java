package io.github.lelena1.flatfinder.source;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lelena1.flatfinder.domain.Listing;
import io.github.lelena1.flatfinder.domain.SearchCriteria;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Объявления из JSON-массива в формате {@link Listing}. Данные читаются один раз при старте. */
public abstract class JsonListingSource implements ListingSource {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private List<Listing> listings = List.of();

    protected void load(InputStream in, String origin) {
        try (in) {
            listings = List.copyOf(MAPPER.readValue(in, new TypeReference<List<Listing>>() { }));
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать объявления из " + origin, e);
        }
    }

    @Override
    public List<Listing> fetch(SearchCriteria criteria) {
        return listings.stream()
                .filter(l -> l.dealType() == criteria.dealType())
                .filter(criteria::matchesDistrict)
                .toList();
    }

    @Override
    public Set<String> districts() {
        return listings.stream().map(Listing::district).filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public int size() {
        return listings.size();
    }
}
