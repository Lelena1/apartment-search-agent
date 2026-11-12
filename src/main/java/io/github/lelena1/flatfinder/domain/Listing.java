package io.github.lelena1.flatfinder.domain;

/**
 * Объявление о квартире в нормализованном виде — независимо от того, откуда оно пришло
 * (демо-база, файл импорта и т.п.). Поля, которые продавец не заполнил, равны {@code null}.
 *
 * @param balconies число балконов по данным объявления ({@code null} — не указано)
 * @param loggias   число лоджий по данным объявления ({@code null} — не указано)
 * @param finish    тип отделки из объявления ({@code null} — не указан)
 * @param furnished есть ли мебель ({@code null} — не указано)
 */
public record Listing(
        String id,
        String source,
        String url,
        DealType dealType,
        String district,
        String address,
        long price,
        double totalArea,
        Double kitchenArea,
        Integer floor,
        Integer floorsTotal,
        int rooms,
        boolean apartments,
        Integer balconies,
        Integer loggias,
        Finish finish,
        Boolean furnished,
        String description
) {
    public enum Finish {
        NONE("без отделки"),
        WHITE_BOX("white box / предчистовая"),
        FINE("чистовая"),
        EURO("евроремонт"),
        DESIGN("дизайнерский ремонт");

        private final String title;

        Finish(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }

        public boolean readyToLive() {
            return this == FINE || this == EURO || this == DESIGN;
        }
    }

    public String descriptionOrEmpty() {
        return description == null ? "" : description;
    }
}
