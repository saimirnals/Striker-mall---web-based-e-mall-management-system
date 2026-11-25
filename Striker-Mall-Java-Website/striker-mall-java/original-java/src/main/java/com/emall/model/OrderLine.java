package com.emall.model;
import java.math.BigDecimal;
public record OrderLine(String name, String shop, int qty, BigDecimal unitPrice) {
    public BigDecimal total() { return unitPrice.multiply(BigDecimal.valueOf(qty)); }
}
