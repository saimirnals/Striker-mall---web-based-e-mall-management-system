package com.emall.dao;

import com.emall.model.Shop;
import com.emall.util.*;
import java.sql.*;
import java.util.*;

public class ShopDao {
    public List<Shop> all() {
        List<Shop> out = new ArrayList<>();
        try (Connection c = Db.get(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT shop_id,name,category,description FROM shops ORDER BY shop_id")) {
            while (rs.next())
                out.add(new Shop(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)));
        } catch (SQLException e) { throw new AppException("Could not load shops", e); }
        return out;
    }
}
