package io.github.lelena1.flatfinder.domain;

/** Районы (жилые кварталы), в которых ведётся поиск. */
public enum District {
    ZILART("ЗИЛАРТ", "Даниловский район, м. ЗИЛ"),
    RIVER_PARK("Ривер Парк Коломенское", "Нагатинский затон, ул. Корабельная");

    private final String title;
    private final String location;

    District(String title, String location) {
        this.title = title;
        this.location = location;
    }

    public String title() {
        return title;
    }

    public String location() {
        return location;
    }
}
