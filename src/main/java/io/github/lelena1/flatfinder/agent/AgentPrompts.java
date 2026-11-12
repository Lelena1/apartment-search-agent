package io.github.lelena1.flatfinder.agent;

import io.github.lelena1.flatfinder.domain.SearchCriteria;

import java.util.List;

/** Тексты промптов — общие для обоих способов подключения Claude. Критерии берутся из формы. */
final class AgentPrompts {

    private AgentPrompts() {
    }

    private static String intro(SearchCriteria c) {
        return "Ты — помощник по подбору квартиры. Человек ищет квартиру для себя, поэтому важны точность и честность: "
                + "не выдумывай объявления, цены и характеристики.\n\nТребования пользователя:\n" + c.describe();
    }

    private static final String FORMAT = """
            Формат ответа (Markdown, по-русски, не длиннее 450 слов):
            ## Лучшие варианты — до 3 квартир: адрес, цена, площадь, чем подходит, что уточнить.
            ## Уточнить у продавца — короткий список вопросов по главным кандидатам.
            ## Что ещё нашлось в интернете — только если был веб-поиск. Каждый пункт — ссылкой \
            в формате Markdown [название](https://...) на конкретное объявление или страницу выдачи.
            ## Итог — одна-две фразы с рекомендацией.
            """;

    /** Для Claude API: данные агент получает сам через инструменты приложения. */
    static String system(SearchCriteria c, List<String> domains) {
        return intro(c) + """

                Как работать:
                1. Сначала вызови search_listings — приложение уже проверило объявления по правилам.
                2. Для лучших кандидатов и спорных случаев вызови get_listing_details и перепроверь вывод \
                приложения по тексту описания: правила на регулярных выражениях могут ошибаться.
                3. %s

                """.formatted(webRule(c, domains)) + FORMAT;
    }

    /** Для Claude Code: данные приложение передаёт прямо в запросе. */
    static String withData(SearchCriteria c, List<String> domains, String task, String listingsJson) {
        return intro(c) + """

                Как работать:
                1. Ниже — объявления, которые приложение уже проверило по правилам (поля verdict, pros, to_check, cons) \
                вместе с полным текстом. Перепроверь выводы по тексту описания: правила на регулярных выражениях могут ошибаться.
                2. %s
                3. Не читай и не изменяй файлы, не запускай команды — нужен только ответ.

                """.formatted(webRule(c, domains)) + FORMAT
                + "\n" + task + "\n\nОбъявления (JSON):\n" + listingsJson;
    }

    static String task(SearchCriteria c, boolean webSearch) {
        String districts = c.districts().isEmpty() ? "любые (все из базы приложения)" : String.join(", ", c.districts());
        String price = String.format("%,d", c.maxPrice()).replace(',', ' ');
        return "Сценарий: %s. Районы: %s. Бюджет: до %s %s. %s"
                .formatted(c.dealType().title().toLowerCase(), districts, price, c.dealType().priceUnit(),
                        webSearch ? "Веб-поиск разрешён." : "Веб-поиск не используй — только данные приложения.");
    }

    private static String webRule(SearchCriteria c, List<String> domains) {
        String where = c.districts().isEmpty()
                ? "Районы не заданы, поэтому в интернете не ищи — работай только с данными приложения."
                : "Ищи только в районах: " + String.join(", ", c.districts()) + ".";
        return "Если доступен веб-поиск — найди свежие объявления, подходящие под требования, только на сайтах: "
                + String.join(", ", domains) + ". " + where
                + " Давай ссылку на каждое найденное объявление и честно помечай, что его не проверяло приложение. "
                + "Не утверждай того, чего нет в результатах поиска.";
    }
}
