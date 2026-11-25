package com.emall.ui;

import com.emall.dao.*;
import com.emall.model.*;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Admin console. A shop admin only sees the shops listed for them in shop_admins.
 * An admin who manages every shop (the mall owner) also gets theatre management and the sales report.
 */
public class AdminFrame extends JFrame {
    public AdminFrame(User admin) {
        super("K.M.S. E-Mall - Admin console");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        List<Shop> all = new ShopDao().all();
        List<Integer> ids = new UserDao().managedShopIds(admin.id());
        List<Shop> mine = all.stream().filter(s -> ids.contains(s.id())).toList();
        boolean owner = mine.size() == all.size();

        JTabbedPane tabs = new JTabbedPane();
        if (!mine.isEmpty()) tabs.addTab("Products & stock", new ProductAdminPanel(mine));
        if (owner) {
            tabs.addTab("IMAX Theatre", new TheatreAdminPanel());
            tabs.addTab("Sales report", new SalesPanel());
        }
        if (tabs.getTabCount() == 0)
            tabs.addTab("No access", new JLabel("  This admin account is not assigned to any shop.", SwingConstants.CENTER));
        setLayout(new BorderLayout());
        add(Ui.header("E-Mall Admin", admin.fullName() + (owner ? " (owner)" : ""), () -> { dispose(); new LoginFrame().setVisible(true); }), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        setSize(980, 640);
        setLocationRelativeTo(null);
    }
}
