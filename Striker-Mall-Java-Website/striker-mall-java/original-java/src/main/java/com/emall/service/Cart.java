package com.emall.service;

import com.emall.model.*;
import java.math.BigDecimal;
import java.util.*;

/** In-memory shopping cart for one logged-in customer. */
public class Cart {
    private final Map<Integer, CartItem> items = new LinkedHashMap<>();

    public void add(Product p, int qty) {
        if (qty <= 0) throw new IllegalArgumentException("Quantity must be positive");
        CartItem ci = items.get(p.id());
        int newQty = (ci == null ? 0 : ci.qty()) + qty;
        if (newQty > p.stock())
            throw new IllegalArgumentException("Only " + p.stock() + " in stock for " + p.name());
        if (ci == null) items.put(p.id(), new CartItem(p, newQty)); else ci.setQty(newQty);
    }
    public void setQty(int productId, int qty) {
        CartItem ci = items.get(productId);
        if (ci == null) return;
        if (qty <= 0) items.remove(productId);
        else if (qty > ci.product().stock())
            throw new IllegalArgumentException("Only " + ci.product().stock() + " in stock");
        else ci.setQty(qty);
    }
    public void remove(int productId) { items.remove(productId); }
    public void clear() { items.clear(); }
    public boolean isEmpty() { return items.isEmpty(); }
    public List<CartItem> items() { return new ArrayList<>(items.values()); }

    public BigDecimal subtotal() {
        return items.values().stream().map(CartItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    public BigDecimal gst() {
        return items.values().stream()
            .map(i -> Tax.gst(i.lineTotal(), i.product().category()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    public BigDecimal total() { return subtotal().add(gst()); }
}
