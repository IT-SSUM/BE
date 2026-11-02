package com.itssum.domain.item.controller;

import com.itssum.domain.item.entity.ItemCategory;
import com.itssum.domain.item.repository.ItemCategoryRepository;
import com.itssum.support.SecurityTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemCategoryController.class)
@SecurityTestConfig
class ItemCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemCategoryRepository itemCategoryRepository;

    @Test
    void categoriesArePublicAndOrdered() throws Exception {
        List<ItemCategory> categories = List.of(category(1L, "생활용품"), category(2L, "의류·잡화"));
        when(itemCategoryRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(categories);

        mockMvc.perform(get("/api/item-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ITEM_200_1"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].categoryId").value(1))
                .andExpect(jsonPath("$.data[0].name").value("생활용품"))
                .andExpect(jsonPath("$.data[1].name").value("의류·잡화"));
    }

    private static ItemCategory category(Long id, String name) {
        ItemCategory category = mock(ItemCategory.class);
        when(category.getId()).thenReturn(id);
        when(category.getName()).thenReturn(name);
        return category;
    }
}
