package io.github.lelena1.flatfinder;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebLayerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void homePageRenders() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Подобрать по правилам")));
    }

    @Test
    void searchShowsDemoListings() throws Exception {
        mvc.perform(get("/search").param("deal", "RENT"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Аренда: просмотрено 10 объявлений")))
                .andExpect(content().string(containsString("выход на балкон/лоджию из кухни")));
    }
}
