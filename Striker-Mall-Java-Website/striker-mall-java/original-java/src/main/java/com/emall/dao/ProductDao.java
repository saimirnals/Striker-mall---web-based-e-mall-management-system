package com.emall.dao;

import com.emall.model.Product;
import com.emall.util.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class ProductDao {
    private static final String BASE =
        "SELECT p.product_id,p.shop_id,s.name,s.category,p.section,p.name,p.price,p.stock,p.active " +
        "FROM products p JOIN shops s ON s.shop_id=p.shop_id ";

    private Product map(ResultSet rs) throws SQLException {
        return new Product(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getString(6), rs.getBigDecimal(7), rs.getInt(8), rs.getBoolean(9));
    }

    /** customer view: only active products. */
    public List<Product> byShop(int shopId, boolean includeInactive) {
        String sql = BASE + "WHERE p.shop_id=? " + (includeInactive ? "" : "AND p.active=TRUE ")
                   + "ORDER BY p.section,p.name";
        List<Product> out = new ArrayList<>();
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, shopId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        } catch (SQLException e) { throw new AppException("Could not load products", e); }
        return out;
    }

    public List<Product> search(String text) {
        List<Product> out = new ArrayList<>();
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement(BASE + "WHERE p.active=TRUE AND (p.name LIKE ? OR p.section LIKE ?) ORDER BY p.name")) {
            String like = "%" + text + "%";
            ps.setString(1, like); ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        } catch (SQLException e) { throw new AppException("Search failed", e); }
        return out;
    }

    public Map<String, String> specs(int productId) {
        Map<String, String> m = new LinkedHashMap<>();
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement("SELECT spec_key,spec_value FROM product_specs WHERE product_id=? ORDER BY spec_key")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) m.put(rs.getString(1), rs.getString(2)); }
        } catch (SQLException e) { throw new AppException("Could not load specs", e); }
        return m;
    }

    public void add(int shopId, String section, String name, BigDecimal price, int stock) {
        exec("INSERT INTO products(shop_id,section,name,price,stock) VALUES(?,?,?,?,?)",
             shopId, section, name, price, stock);
    }
    public void update(int productId, String section, String name, BigDecimal price, int stock) {
        exec("UPDATE products SET section=?,name=?,price=?,stock=? WHERE product_id=?",
             section, name, price, stock, productId);
    }
    public void setActive(int productId, boolean active) {
        exec("UPDATE products SET active=? WHERE product_id=?", active, productId);
    }
    public void saveSpec(int productId, String key, String value) {
        exec("INSERT INTO product_specs(product_id,spec_key,spec_value) VALUES(?,?,?) " +
             "ON DUPLICATE KEY UPDATE spec_value=VALUES(spec_value)", productId, key, value);
    }
    public void deleteSpec(int productId, String key) {
        exec("DELETE FROM product_specs WHERE product_id=? AND spec_key=?", productId, key);
    }

    private void exec(String sql, Object... args) {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            ps.executeUpdate();
        } catch (SQLException e) { throw new AppException("Database error: " + e.getMessage(), e); }
    }
}
