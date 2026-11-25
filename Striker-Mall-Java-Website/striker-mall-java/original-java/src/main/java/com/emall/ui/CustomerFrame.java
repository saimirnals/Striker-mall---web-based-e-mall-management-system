package com.emall.ui;

import com.emall.model.User;
import com.emall.service.Cart;
import javax.swing.*;
import java.awt.*;

public class CustomerFrame extends JFrame {
    public CustomerFrame(User user) {
        super("K.M.S. E-Mall - " + user.fullName());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Cart cart = new Cart();
        JTabbedPane tabs = new JTabbedPane();
        OrdersPanel[] orders = new OrdersPanel[1];
        ShopPanel[] shop = new ShopPanel[1];
        CartPanel[] cartPanel = new CartPanel[1];
        shop[0] = new ShopPanel(cart, () -> cartPanel[0].refresh());
        cartPanel[0] = new CartPanel(cart, user, () -> { orders[0].refresh(); shop[0].reload(); });
        orders[0] = new OrdersPanel(user, () -> shop[0].reload());
        TheatrePanel theatre = new TheatrePanel(user);
        tabs.addTab("Shops", shop[0]);
        tabs.addTab("IMAX Theatre", theatre);
        tabs.addTab("My Cart", cartPanel[0]);
        tabs.addTab("My Orders", orders[0]);
        tabs.addChangeListener(e -> {
            Component c = tabs.getSelectedComponent();
            if (c == cartPanel[0]) cartPanel[0].refresh();
            else if (c == orders[0]) orders[0].refresh();
            else if (c == theatre) theatre.refresh();
        });
        setLayout(new BorderLayout());
        add(Ui.header("K.M.S. E-Mall", "Hello, " + user.fullName(), () -> { dispose(); new LoginFrame().setVisible(true); }), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        setSize(980, 640);
        setLocationRelativeTo(null);
    }
}
