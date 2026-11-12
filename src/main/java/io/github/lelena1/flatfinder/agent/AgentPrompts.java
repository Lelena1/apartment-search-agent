package io.github.lelena1.flatfinder.agent;

import io.github.lelena1.flatfinder.domain.District;
import io.github.lelena1.flatfinder.domain.SearchCriteria;

import java.util.List;
import java.util.stream.Collectors;

/** Тексты промптов — общие для обоих способов подключения Claude. */
final class AgentPrompts {

    private AgentPrompts() {
    }

    private static final String CRITERIA = """
            Ты — помощник по подбору квартиры в Москве. Пользователь ищет квартиру для себя, \
            поэтому важны точность и честность: не выдумывай объявления, цены и характеристики.

            Критерии пользователя:
            - планировка: РОВНО две спальни + кухня-гостиная (евротрёшка) или две комнаты + большая отдельная кухня; \
            евродвушка (одна спальня) и квартиры с тремя спальнями не подходят;
            - обязательно балкон или лоджия; желательно — с выходом из кухни или кухни-гостиной;
            - желательно места хранения: гардеробная, постирочная, кладовая;
            - покупка: готовая отделка, чтобы можно было сразу заехать; аренда: ремонт и обязательно мебель;
            - апартаменты не подходят.
            """;

    private static final String FORMAT = """
            Формат ответа (Markdown, по-русски, не длиннее 400 слов):
            ## Лучшие варианты — до 3 квартир: адрес, цена, площадь, чем подходит, что уточнить.
            ## Уточнить у продавца — короткий список вопросов по главным кандидатам.
            ## Что ещё нашлось в интернете — только если был веб-поиск.
            ## Итог — одна-две фразы с рекомендацией.
            """;

    /** Для Claude API: данные агент получает сам через инструменты приложения. */
    static String system(List<String> domains) {
        return CRITERIA + """

                Как работать:
                1. Сначала вызови search_listings — приложение уже проверило объявления по правилам.
                2. Для лучших кандидатов и спорных случаев вызови get_listing_details и перепроверь вывод \
                приложения по тексту описания: правила на регулярных выражениях могут ошибаться.
                3. %s

                """.formatted(webRule(domains)) + FORMAT;
    }

    /** Для Claude Code: данные приложение передаёт прямо в запросе. */
    static String withData(List<String> domains, String task, String listingsJson) {
        return CRITERIA + """

                Как работать:
                1. Ниже — объявления, которые приложение уже проверило по правилам (поля verdict, pros, to_check, cons) \
                вместе с полным текстом. Перепроверь выводы по тексту описания: правила на регулярных выражениях могут ошибаться.
                2. %s
                3. Не читай и не изменяй файлы, не запускай команды — нужен только ответ.

                """.formatted(webRule(domains)) + FORMAT
                + "\n" + task + "\n\nОбъявления (JSON):\n" + listingsJson;
    }

    static String task(SearchCriteria c, boolean webSearch) {
        String districts = c.districts().stream().map(District::title).collect(Collectors.joining(", "));
        String price = String.format("%,d", c.maxPrice()).replace(',', ' ');
        return "Сценарий: %s. Районы: %s. Бюджет: до %s %s. %s"
                .formatted(c.dealType().title().toLowerCase(), districts, price, c.dealType().priceUnit(),
                        webSearch ? "Веб-поиск разрешён." : "Веб-поиск не используй — только данные приложения.");
    }

    private static String webRule(List<String> domains) {
        return "Если доступен веб-поиск — найди свежие объявления в этих же жилых комплексах только на сайтах: "
                + String.join(", ", domains)
                + ". Указывай ссылку на каждое найденное объявление и честно помечай, что его не проверяло приложение. "
                + "Не утверждай того, чего нет в результатах поиска.";
    }
}
