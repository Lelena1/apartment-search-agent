package io.github.lelena1.flatfinder.analysis;

/** Тип планировки с точки зрения критерия «ровно две спальни + кухня-гостиная или большая кухня». */
public enum LayoutType {
    EURO_THREE("Евротрёшка: 2 спальни + кухня-гостиная", true),
    TWO_BEDROOMS_BIG_KITCHEN("2 комнаты + большая кухня", true),
    TWO_BEDROOMS_SMALL_KITCHEN("2 комнаты, но кухня небольшая", true),
    EURO_TWO("Евродвушка: 1 спальня + кухня-гостиная", false),
    THREE_BEDROOMS("3 спальни и больше", false),
    UNKNOWN("Планировка не ясна из объявления", true);

    private final String title;
    private final boolean acceptable;

    LayoutType(String title, boolean acceptable) {
        this.title = title;
        this.acceptable = acceptable;
    }

    public String title() {
        return title;
    }

    /** {@code false} — планировка точно не подходит, квартиру можно отсеять. */
    public boolean acceptable() {
        return acceptable;
    }
}
