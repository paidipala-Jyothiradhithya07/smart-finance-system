package com.smartfinance.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties(prefix = "app.currency")
public class CurrencyProperties {
	private String defaultCurrency;
    private List<Item> supported = new ArrayList<>();

    // YAML key is "default", so Spring looks for getDefault/setDefault
    public String getDefault() { return defaultCurrency; }
    public void setDefault(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }

    public List<Item> getSupported() { return supported; }
    public void setSupported(List<Item> supported) { this.supported = supported; }

    public boolean isSupported(String code) {
        return supported.stream().anyMatch(item -> item.getCode().equalsIgnoreCase(code));
    }

    public String supportedCodes() {
        return supported.stream().map(Item::getCode).collect(Collectors.joining(", "));
    }

    public static class Item {
        private String code;
        private String symbol;
        private String name;

        public String getCode() {
        	return code;
        	}
        public void setCode(String code) { 
        	this.code = code;
        	}

        public String getSymbol() { 
        	return symbol;
        	}
        public void setSymbol(String symbol) { 
        	this.symbol = symbol; 
        	}

        public String getName() { 
        	return name; 
        	}
        public void setName(String name) {
        	this.name = name; 
        	}
    }
}
