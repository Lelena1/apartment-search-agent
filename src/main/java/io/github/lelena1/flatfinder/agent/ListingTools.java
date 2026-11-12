package io.github.lelena1.flatfinder.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.lelena1.flatfinder.analysis.ListingSearchService;
import io.github.lelena1.flatfinder.analysis.Match;
import io.github.lelena1.flatfinder.analysis.Verdict;
import io.github.lelena1.flatfinder.domain.Listing;
import io.github.lelena1.flatfinder.domain.SearchCriteria;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Инструменты, которые агент вызывает сам. Это обычный Java-код поверх {@link ListingSearchService}:
 * Claude решает, что и когда смотреть, а данные и проверку критериев даёт приложение.
 */
@Component
public class ListingTools {

    public static final String SEARCH = "search_listings";
    public static final String DETAILS = "get_listing_details";

    private final ListingSearchService searchService;
    private final ObjectMapper mapper = new ObjectMapper();

    public ListingTools(ListingSearchService searchService) {
        this.searchService = searchService;
    }

    /** Схемы инструментов в формате JSON Schema — их получает модель. */
    public Map<String, ToolSpec> specs() {
        return Map.of(
                SEARCH, new ToolSpec(SEARCH,
                        "Ищет объявления в подключённых базах по текущим критериям пользователя (сделка, районы, бюджет) " +
                        "и возвращает их с оценкой приложения: verdict MATCH (все обязательные условия подтверждены), " +
                        "CHECK (что-то обязательное не указано в объявлении), REJECT (точно не подходит, с причиной). " +
                        "Вызывай первым шагом. Отклонённые возвращаются только если include_rejected = true.",
                        Map.of("include_rejected", Map.of("type", "boolean",
                                "description", "Вернуть ли также отклонённые объявления (по умолчанию нет)")),
                        List.of()),
                DETAILS, new ToolSpec(DETAILS,
                        "Возвращает полное описание объявления и разбор по каждому критерию. " +
                        "Используй, чтобы перепроверить спорные случаи по тексту объявления.",
                        Map.of("id", Map.of("type", "string", "description", "id объявления из search_listings")),
                        List.of("id")));
    }

    public String search(SearchCriteria criteria, boolean includeRejected) {
        ArrayNode out = mapper.createArrayNode();
        for (Match m : searchService.search(criteria).matches()) {
            if (m.verdict() == Verdict.REJECT && !includeRejected) {
                continue;
            }
            out.add(summary(m));
        }
        return out.toString();
    }

    public String details(SearchCriteria criteria, String id) {
        return searchService.findById(id, criteria)
                .map(m -> {
                    ObjectNode node = summary(m);
                    node.put("description", m.listing().descriptionOrEmpty());
                    node.put("layout", m.analysis().layout().title());
                    node.put("balcony", m.analysis().balcony().name());
                    node.put("balcony_from_kitchen", m.analysis().balconyFromKitchen().name());
                    node.put("storage", m.analysis().storage().name());
                    node.put("ready_finish", m.analysis().readyFinish().name());
                    node.put("furniture", m.analysis().furniture().name());
                    return node.toString();
                })
                .orElse("{\"error\":\"Объявление с id " + id + " не найдено\"}");
    }

    private ObjectNode summary(Match m) {
        Listing l = m.listing();
        ObjectNode node = mapper.createObjectNode();
        node.put("id", l.id());
        node.put("source", l.source());
        if (l.url() != null) {
            node.put("url", l.url());
        }
        node.put("district", l.district().title());
        node.put("address", l.address());
        node.put("price_rub", l.price());
        node.put("total_area_m2", l.totalArea());
        if (l.kitchenArea() != null) {
            node.put("kitchen_area_m2", l.kitchenArea());
        }
        if (l.floor() != null && l.floorsTotal() != null) {
            node.put("floor", l.floor() + "/" + l.floorsTotal());
        }
        node.put("verdict", m.verdict().name());
        node.put("score", m.score());
        node.set("pros", mapper.valueToTree(m.pros()));
        node.set("to_check", mapper.valueToTree(m.toCheck()));
        node.set("cons", mapper.valueToTree(m.cons()));
        return node;
    }

    /** Описание инструмента, не зависящее от SDK. */
    public record ToolSpec(String name, String description, Map<String, Object> properties, List<String> required) {
    }
}
