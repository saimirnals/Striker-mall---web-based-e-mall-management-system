package com.emall.service;

import java.math.*;

/** GST slabs: food & grocery 5%, electronics & supplements 18%. */
public final class Tax {
    private Tax() {}
    public static BigDecimal rate(String shopCategory) {
        return switch (shopCategory) {
            case "RESTAURANT", "GROCERY" -> new BigDecimal("0.05");
            default -> new BigDecimal("0.18");
        };
    }
    public static BigDecimal gst(BigDecimal amount, String shopCategory) {
        return amount.multiply(rate(shopCategory)).setScale(2, RoundingMode.HALF_UP);
    }
}
