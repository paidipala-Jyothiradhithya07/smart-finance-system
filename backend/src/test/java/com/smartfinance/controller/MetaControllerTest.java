package com.smartfinance.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.smartfinance.config.CurrencyProperties;

class MetaControllerTest {

    @Test
    void returnsTheConfiguredCurrencies() throws Exception {
        CurrencyProperties properties = new CurrencyProperties();
        properties.setDefault("INR");
        properties.setSupported(List.of(item("INR", "Indian Rupee"), item("USD", "US Dollar")));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MetaController(properties)).build();

        mockMvc.perform(get("/api/meta/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultCurrency").value("INR"))
                .andExpect(jsonPath("$.currencies.length()").value(2))
                .andExpect(jsonPath("$.currencies[0].code").value("INR"))
                .andExpect(jsonPath("$.currencies[1].name").value("US Dollar"));
    }

    private static CurrencyProperties.Item item(String code, String name) {
        CurrencyProperties.Item item = new CurrencyProperties.Item();
        item.setCode(code);
        item.setSymbol(code);
        item.setName(name);
        return item;
    }
}