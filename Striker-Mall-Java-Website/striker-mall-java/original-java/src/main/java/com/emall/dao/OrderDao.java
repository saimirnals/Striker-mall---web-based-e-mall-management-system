package com.emall.dao;

import com.emall.model.*;
import com.emall.service.Tax;
import com.emall.util.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class OrderDao {

    /** Atomically validates stock, decrements it and stores the order. Returns the new order id. */
    public int checkout(int userId, List<CartItem> items) {
        if (items.isEmpty()) throw new AppException("Your cart is empty.");
        try (Connection c = Db.get()) {
            c.setAutoCommit(false);
            try {
                BigDecimal subtotal = BigDecimal.ZERO, gst = BigDecimal.ZERO;
                List<Object[]> lines = new ArrayList<>();
                // lock rows in id order to avoid deadlocks between concurrent customers
                List<CartItem> sorted = new ArrayList<>(items);
                sorted.sort(Comparator.comparingInt(i -> i.product().id()));
                for (CartItem ci : sorted) {
                    try (PreparedStatement ps = c.prepareStatement(
                        "SELECT p.price,p.stock,p.active,s.category FROM products p JOIN shops s ON s.shop_id=p.shop_id " +
                        "WHERE p.product_id=? FOR UPDATE")) {
                        ps.setInt(1, ci.product().id());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next() || !rs.getBoolean("active"))
                                throw new AppException(ci.product().name() + " is no longer available.");
                            if (rs.getInt("stock") < ci.qty())
                                throw new AppException("Only " + rs.getInt("stock") + " left of " + ci.product().name());
                            BigDecimal price = rs.getBigDecimal("price");   // always trust the DB price
                            BigDecimal line = price.multiply(BigDecimal.valueOf(ci.qty()));
                            subtotal = subtotal.add(line);
                            gst = gst.add(Tax.gst(line, rs.getString("category")));
                            lines.add(new Object[]{ci.product().id(), ci.qty(), price});
                        }
                    }
                    try (PreparedStatement ps = c.prepareStatement("UPDATE products SET stock=stock-? WHERE product_id=?")) {
                        ps.setInt(1, ci.qty()); ps.setInt(2, ci.product().id()); ps.executeUpdate();
                    }
                }
                int orderId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO orders(user_id,subtotal,gst,total) VALUES(?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, userId); ps.setBigDecimal(2, subtotal); ps.setBigDecimal(3, gst);
                    ps.setBigDecimal(4, subtotal.add(gst));
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); orderId = rs.getInt(1); }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO order_items(order_id,product_id,qty,unit_price) VALUES(?,?,?,?)")) {
                    for (Object[] l : lines) {
                        ps.setInt(1, orderId); ps.setInt(2, (Integer) l[0]);
                        ps.setInt(3, (Integer) l[1]); ps.setBigDecimal(4, (BigDecimal) l[2]);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                c.commit();
                return orderId;
            } catch (AppException | SQLException e) {
                c.rollback();
                if (e instanceof AppException ae) throw ae;
                throw new AppException("Checkout failed: " + e.getMessage(), e);
            }
        } catch (SQLException e) { throw new AppException("Checkout failed: " + e.getMessage(), e); }
    }

    public List<OrderSummary> history(int userId) {
        List<OrderSummary> out = new ArrayList<>();
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT order_id,order_time,subtotal,gst,total,status FROM orders WHERE user_id=? ORDER BY order_time DESC, order_id DESC")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new OrderSummary(rs.getInt(1), rs.getTimestamp(2).toLocalDateTime(),
                        rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getBigDecimal(5), rs.getString(6)));
            }
        } catch (SQLException e) { throw new AppException("Could not load order history", e); }
        return out;
    }

    public OrderSummary find(int orderId) {
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT order_id,order_time,subtotal,gst,total,status FROM orders WHERE order_id=?")) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new AppException("Order not found");
                return new OrderSummary(rs.getInt(1), rs.getTimestamp(2).toLocalDateTime(),
                        rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getBigDecimal(5), rs.getString(6));
            }
        } catch (SQLException e) { throw new AppException("Could not load order", e); }
    }

    public List<OrderLine> lines(int orderId) {
        List<OrderLine> out = new ArrayList<>();
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT p.name,s.name,oi.qty,oi.unit_price FROM order_items oi " +
                 "JOIN products p ON p.product_id=oi.product_id JOIN shops s ON s.shop_id=p.shop_id " +
                 "WHERE oi.order_id=? ORDER BY oi.item_id")) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new OrderLine(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getBigDecimal(4)));
            }
        } catch (SQLException e) { throw new AppException("Could not load order items", e); }
        return out;
    }

    /** Cancels a PAID order and puts the stock back. */
    public void cancel(int userId, int orderId) {
        try (Connection c = Db.get()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT status FROM orders WHERE order_id=? AND user_id=? FOR UPDATE")) {
                    ps.setInt(1, orderId); ps.setInt(2, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new AppException("Order not found.");
                        if (!"PAID".equals(rs.getString(1))) throw new AppException("Order is already cancelled.");
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE products p JOIN order_items oi ON oi.product_id=p.product_id " +
                        "SET p.stock=p.stock+oi.qty WHERE oi.order_id=?")) {
                    ps.setInt(1, orderId); ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE orders SET status='CANCELLED' WHERE order_id=?")) {
                    ps.setInt(1, orderId); ps.executeUpdate();
                }
                c.commit();
            } catch (AppException | SQLException e) {
                c.rollback();
                if (e instanceof AppException ae) throw ae;
                throw new AppException("Cancel failed: " + e.getMessage(), e);
            }
        } catch (SQLException e) { throw new AppException("Cancel failed: " + e.getMessage(), e); }
    }

    public List<SalesRow> salesByShop() {
        List<SalesRow> out = new ArrayList<>();
        try (Connection c = Db.get(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT shop,units,revenue FROM v_sales_by_shop ORDER BY revenue DESC")) {
            while (rs.next()) out.add(new SalesRow(rs.getString(1), rs.getLong(2), rs.getBigDecimal(3)));
        } catch (SQLException e) { throw new AppException("Could not load sales report", e); }
        return out;
    }
}
