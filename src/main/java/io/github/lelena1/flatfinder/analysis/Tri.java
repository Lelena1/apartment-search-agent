package io.github.lelena1.flatfinder.analysis;

/**
 * Трёхзначная логика для признаков, которые продавец мог не указать.
 * В объявлениях о недвижимости «не указано» встречается постоянно, и это не то же самое, что «нет».
 */
public enum Tri {
    YES, NO, UNKNOWN;

    public static Tri of(boolean value) {
        return value ? YES : NO;
    }
}
