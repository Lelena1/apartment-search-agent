package io.github.lelena1.flatfinder.analysis;

public enum Verdict {
    MATCH("Подходит"),
    CHECK("Подходит, если подтвердится"),
    REJECT("Не подходит");

    private final String title;

    Verdict(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
