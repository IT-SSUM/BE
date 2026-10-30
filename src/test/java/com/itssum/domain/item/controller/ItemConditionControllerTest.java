package com.itssum.domain.item.controller;

import com.itssum.support.SecurityTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemConditionController.class)
@SecurityTestConfig
class ItemConditionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsConditionGradesFromBestToWorst() throws Exception {
        mockMvc.perform(get("/api/item-conditions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].code").value("HIGH"))
                .andExpect(jsonPath("$.data[0].description").value("상·미개봉"))
                .andExpect(jsonPath("$.data[1].code").value("MID"))
                .andExpect(jsonPath("$.data[2].code").value("LOW"));
    }
}
