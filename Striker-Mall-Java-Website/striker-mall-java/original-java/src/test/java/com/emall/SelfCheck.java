package com.emall;

import com.emall.model.*;
import com.emall.service.*;
import com.emall.util.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.regex.*;

/** Dependency-free checks for logic that does not need MySQL. Run: scripts/selfcheck.sh */
public class SelfCheck {
    static int fails = 0;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS  " : "FAIL  ") + name); if (!ok) fails++; }

    public static void main(String[] a) throws Exception {
        // password hashing matches the hashes written in seed.sql
        String seed = Files.readString(Path.of("database/seed.sql"));
        Matcher m = Pattern.compile("\\('(\\w+ ?\\w*)','\\d+','(\\w+)','([0-9a-f]{64})','([0-9a-f]{32})','\\w+'\\)").matcher(seed);
        String[][] pw = {{"admin","admin123"},{"techadmin","tech123"},{"demo","demo123"}};
        int found = 0;
        while (m.find()) for (String[] p : pw) if (p[0].equals(m.group(2))) {
            found++; check("seed hash for " + p[0], PasswordUtil.matches(p[1], m.group(4), m.group(3)));
            check("wrong password rejected for " + p[0], !PasswordUtil.matches("nope", m.group(4), m.group(3)));
        }
        check("all 3 seed users found", found == 3);
        check("salts are unique", !PasswordUtil.newSalt().equals(PasswordUtil.newSalt()));

        // validation
        check("phone ok", Validator.phone("9876543210")); check("phone bad", !Validator.phone("98765"));
        check("username ok", Validator.username("sai_01")); check("username bad", !Validator.username("a b"));
        check("password short", !Validator.password("123"));

        // cart + GST
        Product phone = new Product(1, 1, "Tech Showroom", "TECH", "Apple", "iPhone 15", new BigDecimal("69900.00"), 5, true);
        Product dish  = new Product(2, 2, "Veg", "RESTAURANT", "Starters", "Paneer Tikka", new BigDecimal("220.00"), 3, true);
        Cart c = new Cart();
        check("empty cart", c.isEmpty() && c.total().signum() == 0);
        c.add(phone, 1); c.add(dish, 2); c.add(dish, 1);
        check("merge same product qty=3", c.items().stream().filter(i -> i.product().id() == 2).findFirst().get().qty() == 3);
        check("subtotal", c.subtotal().compareTo(new BigDecimal("70560.00")) == 0);
        check("GST 18% tech + 5% food = 12582 + 33", c.gst().compareTo(new BigDecimal("12615.00")) == 0);
        check("total", c.total().compareTo(new BigDecimal("83175.00")) == 0);
        boolean blocked = false; try { c.add(dish, 1); } catch (IllegalArgumentException e) { blocked = true; }
        check("cannot exceed stock", blocked);
        c.setQty(2, 0); check("qty 0 removes item", c.items().size() == 1);
        c.clear(); check("clear", c.isEmpty());

        // receipt renders
        String r = Receipt.forOrder(new OrderSummary(7, java.time.LocalDateTime.now(), new BigDecimal("100.00"), new BigDecimal("5.00"),
                new BigDecimal("105.00"), "PAID"), java.util.List.of(new OrderLine("Tea", "Cafe", 2, new BigDecimal("50.00"))), "Tester");
        check("receipt has total", r.contains("105.00") && r.contains("Bill No : 7"));
        System.out.println(fails == 0 ? "\nALL CHECKS PASSED" : "\n" + fails + " CHECK(S) FAILED");
        System.exit(fails == 0 ? 0 : 1);
    }
}
