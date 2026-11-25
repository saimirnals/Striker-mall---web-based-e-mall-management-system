package com.emall.model;
import java.math.BigDecimal;
public class CartItem {
    private final Product product;
    private int qty;
    public CartItem(Product p, int qty) { this.product = p; this.qty = qty; }
    public Product product() { return product; }
    public int qty() { return qty; }
    public void setQty(int q) { this.qty = q; }
    public BigDecimal lineTotal() { return product.price().multiply(BigDecimal.valueOf(qty)); }
}
