package com.emall.dao;

import com.emall.model.*;
import com.emall.util.*;
import java.sql.*;
import java.util.*;

public class UserDao {
    public User register(String fullName, String phone, String username, String password) {
        String salt = PasswordUtil.newSalt();
        String sql = "INSERT INTO users(full_name,phone,username,password_hash,salt,role) VALUES(?,?,?,?,?,'CUSTOMER')";
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fullName.trim());
            ps.setString(2, phone);
            ps.setString(3, username);
            ps.setString(4, PasswordUtil.hash(password, salt));
            ps.setString(5, salt);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return new User(rs.getInt(1), fullName.trim(), phone, username, Role.CUSTOMER);
            }
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("23"))
                throw new AppException("That user ID is already taken. Please choose another.");
            throw new AppException("Could not create account: " + e.getMessage(), e);
        }
    }

    /** Returns the user when credentials are valid, otherwise empty. */
    public Optional<User> authenticate(String username, String password) {
        String sql = "SELECT user_id,full_name,phone,username,password_hash,salt,role FROM users WHERE username=?";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                if (!PasswordUtil.matches(password, rs.getString("salt"), rs.getString("password_hash")))
                    return Optional.empty();
                return Optional.of(new User(rs.getInt("user_id"), rs.getString("full_name"),
                        rs.getString("phone"), rs.getString("username"), Role.valueOf(rs.getString("role"))));
            }
        } catch (SQLException e) { throw new AppException("Login failed: " + e.getMessage(), e); }
    }

    public List<Integer> managedShopIds(int userId) {
        List<Integer> ids = new ArrayList<>();
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement("SELECT shop_id FROM shop_admins WHERE user_id=?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) ids.add(rs.getInt(1)); }
        } catch (SQLException e) { throw new AppException("Could not load admin shops", e); }
        return ids;
    }
}
