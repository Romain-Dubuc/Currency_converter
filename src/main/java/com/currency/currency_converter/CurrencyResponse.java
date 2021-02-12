package com.currency.currency_converter;

import java.io.Serializable;
import java.util.Map;

public class CurrencyResponse implements Serializable {
    private Map<String, Float> rates;
    private String base;
    private String date;

    public Map<String, Float> getRates() {
        return rates;
    }

    public String getBase() {
        return base;
    }

    public String getDate() {
        return date;
    }
}