package io.github.lelena1.flatfinder.web;

import io.github.lelena1.flatfinder.agent.AgentReport;
import io.github.lelena1.flatfinder.agent.AgentBackend;
import io.github.lelena1.flatfinder.agent.AgentService;
import io.github.lelena1.flatfinder.analysis.ListingSearchService;
import io.github.lelena1.flatfinder.analysis.Verdict;
import io.github.lelena1.flatfinder.domain.DealType;
import io.github.lelena1.flatfinder.domain.District;
import io.github.lelena1.flatfinder.domain.SearchCriteria;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class SearchController {

    private static final Parser MARKDOWN = Parser.builder().build();
    // escapeHtml: в ответе агента могут оказаться фрагменты чужих страниц — сырой HTML не пропускаем
    private static final HtmlRenderer HTML = HtmlRenderer.builder().escapeHtml(true).sanitizeUrls(true).build();

    private final ListingSearchService searchService;
    private final AgentService agent;

    public SearchController(ListingSearchService searchService, AgentService agent) {
        this.searchService = searchService;
        this.agent = agent;
    }

    @ModelAttribute
    public void common(Model model) {
        model.addAttribute("dealTypes", DealType.values());
        model.addAttribute("allDistricts", District.values());
        model.addAttribute("verdicts", Verdict.values());
        model.addAttribute("agentAvailable", agent.available());
        model.addAttribute("agentName", agent.active().map(AgentBackend::name).orElse(null));
    }

    @GetMapping("/")
    public String index(@ModelAttribute("form") SearchForm form) {
        return "index";
    }

    @GetMapping("/search")
    public String search(@ModelAttribute("form") SearchForm form, Model model) {
        SearchCriteria criteria = form.toCriteria();
        model.addAttribute("result", searchService.search(criteria));
        return "results";
    }

    @PostMapping("/agent")
    public String runAgent(@ModelAttribute("form") SearchForm form, Model model) {
        SearchCriteria criteria = form.toCriteria();
        AgentReport report = agent.run(criteria, form.isWeb());
        model.addAttribute("result", searchService.search(criteria));
        model.addAttribute("report", report);
        if (report.success()) {
            model.addAttribute("reportHtml", HTML.render(MARKDOWN.parse(report.markdown())));
        }
        return "results";
    }
}
