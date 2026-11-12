package io.github.lelena1.flatfinder.domain;

/** Русские окончания после числительных: 1 спальня, 2 спальни, 5 спален. */
public final class Plural {

    private Plural() {
    }

    public static String of(int n, String one, String few, String many) {
        int mod100 = Math.abs(n) % 100;
        int mod10 = mod100 % 10;
        String word = mod100 >= 11 && mod100 <= 14 ? many
                : mod10 == 1 ? one
                : mod10 >= 2 && mod10 <= 4 ? few
                : many;
        return n + " " + word;
    }

    public static String bedrooms(int n) {
        return of(n, "спальня", "спальни", "спален");
    }

    public static String rooms(int n) {
        return of(n, "комната", "комнаты", "комнат");
    }
}
