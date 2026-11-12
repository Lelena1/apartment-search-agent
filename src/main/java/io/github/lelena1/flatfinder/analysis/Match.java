package io.github.lelena1.flatfinder.analysis;

import io.github.lelena1.flatfinder.domain.Listing;

import java.util.List;

/**
 * Результат сравнения объявления с критериями.
 *
 * @param pros    что в квартире совпадает с пожеланиями
 * @param toCheck что не указано в объявлении и что надо уточнить у продавца
 * @param cons    почему квартира не подходит (для {@link Verdict#REJECT}) или её минусы
 */
public record Match(
        Listing listing,
        ListingAnalysis analysis,
        Verdict verdict,
        int score,
        List<String> pros,
        List<String> toCheck,
        List<String> cons
) {
}
