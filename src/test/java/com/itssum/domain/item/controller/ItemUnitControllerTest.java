package com.itssum.domain.item.controller;

import com.itssum.support.SecurityTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemUnitController.class)
@SecurityTestConfig
class ItemUnitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unitsArePublic() throws Exception {
        mockMvc.perform(get("/api/item-units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ITEM_200_2"))
                .andExpect(jsonPath("$.data.length()").value(9))
                .andExpect(jsonPath("$.data[0]").value("개"))
                .andExpect(jsonPath("$.data[1]").value("권"))
                .andExpect(jsonPath("$.data[8]").value("기타"));
    }
}
