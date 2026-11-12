package io.github.lelena1.flatfinder.source;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Встроенная демо-база: вымышленные объявления, которые показывают все случаи —
 * подходящие квартиры, «уточнить у продавца» и отказы по разным причинам.
 * Работает без интернета и без ключей, поэтому проект можно запустить сразу после клонирования.
 */
@Component
@ConditionalOnProperty(name = "app.listings.demo-enabled", havingValue = "true", matchIfMissing = true)
public class DemoListingSource extends JsonListingSource {

    static final String RESOURCE = "data/demo-listings.json";

    public DemoListingSource() {
        try {
            load(new ClassPathResource(RESOURCE).getInputStream(), RESOURCE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String name() {
        return "Демо-база";
    }
}
