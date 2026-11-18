package com.smartfinance.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartfinance.config.CurrencyProperties;
import com.smartfinance.dto.response.CurrencyDTO;
import com.smartfinance.dto.response.CurrencyOptionsResponse;

@RestController
@RequestMapping("/api/meta")
public class MetaController {

    private final CurrencyProperties currencyProperties;

    public MetaController(CurrencyProperties currencyProperties) {
        this.currencyProperties = currencyProperties;
    }

    // Public: the register page needs it before anyone is logged in. It only lists currency names.
    @GetMapping("/currencies")
    public CurrencyOptionsResponse currencies() {
        List<CurrencyDTO> items = currencyProperties.getSupported().stream()
                .map(item -> new CurrencyDTO(item.getCode(), item.getSymbol(), item.getName()))
                .toList();
        return new CurrencyOptionsResponse(currencyProperties.getDefault(), items);
    }
}