package io.github.lelena1.flatfinder.analysis;

/** Виды «балконов», которые встречаются в объявлениях. */
public enum BalconyKind {
    BALCONY("балкон", true),
    LOGGIA("лоджия", true),
    TERRACE("терраса", true),
    /** Окно от пола с ограждением: створку открыть можно, а выйти на площадку — нет. */
    FRENCH("французский балкон", false);

    private final String title;
    private final boolean walkOut;

    BalconyKind(String title, boolean walkOut) {
        this.title = title;
        this.walkOut = walkOut;
    }

    public String title() {
        return title;
    }

    /** Можно ли выйти наружу на площадку. */
    public boolean walkOut() {
        return walkOut;
    }
}
