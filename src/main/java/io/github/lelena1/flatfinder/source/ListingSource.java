package io.github.lelena1.flatfinder.source;

import io.github.lelena1.flatfinder.domain.Listing;
import io.github.lelena1.flatfinder.domain.SearchCriteria;

import java.util.List;
import java.util.Set;

/** Откуда берутся объявления. Новую площадку можно подключить, реализовав этот интерфейс. */
public interface ListingSource {

    String name();

    /** Объявления нужного типа сделки в нужных районах. Фильтр по критериям квартиры — не здесь. */
    List<Listing> fetch(SearchCriteria criteria);

    /** Районы, которые есть в источнике, — подсказки для формы. Источник без списка возвращает пустое множество. */
    default Set<String> districts() {
        return Set.of();
    }
}
