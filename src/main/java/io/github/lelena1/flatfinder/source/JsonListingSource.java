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
                .filter(l -> criteria.districts().contains(l.district()))
                .toList();
    }

    public int size() {
        return listings.size();
    }
}
