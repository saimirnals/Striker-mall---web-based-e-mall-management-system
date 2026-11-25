package com.emall.model;
import java.math.BigDecimal;
public record Product(int id, int shopId, String shopName, String category, String section,
                      String name, BigDecimal price, int stock, boolean active) {
    @Override public String toString() { return name; }
}
