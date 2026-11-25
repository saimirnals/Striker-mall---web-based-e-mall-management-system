package com.emall.ui;

import com.emall.dao.*;
import com.emall.model.*;
import com.emall.service.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

class CartPanel extends JPanel {
    private final Cart cart;
    private final User user;
    private final Runnable onCheckedOut;
    private final OrderDao orders = new OrderDao();
    private final DefaultTableModel model = Ui.model("Item", "Shop", "Qty", "Price", "Total");
    private final JTable table = Ui.table(model);
    private final JLabel sub = new JLabel(), gst = new JLabel(), tot = new JLabel();
    private List<CartItem> rows = List.of();

    CartPanel(Cart cart, User user, Runnable onCheckedOut) {
        super(new BorderLayout(8, 8));
        this.cart = cart; this.user = user; this.onCheckedOut = onCheckedOut;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton chg = Ui.button("Change quantity"), rem = Ui.button("Remove"), clr = Ui.button("Clear cart");
        left.add(chg); left.add(rem); left.add(clr);
        JPanel totals = new JPanel(new GridLayout(3, 1)); totals.add(sub); totals.add(gst); totals.add(tot);
        tot.setFont(tot.getFont().deriveFont(Font.BOLD, 16f));
        JButton pay = Ui.primary("Checkout & pay");
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0)); right.add(totals); right.add(pay);
        JPanel south = new JPanel(new BorderLayout()); south.add(left, BorderLayout.WEST); south.add(right, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);

        chg.addActionListener(e -> Ui.guard(this, () -> {
            CartItem ci = selected();
            String s = JOptionPane.showInputDialog(this, "New quantity for " + ci.product().name() + ":", ci.qty());
            if (s == null) return;
            try { cart.setQty(ci.product().id(), Integer.parseInt(s.trim())); }
            catch (NumberFormatException ex) { throw new IllegalArgumentException("Enter a whole number."); }
            refresh();
        }));
        rem.addActionListener(e -> Ui.guard(this, () -> { cart.remove(selected().product().id()); refresh(); }));
        clr.addActionListener(e -> { if (!cart.isEmpty() && Ui.confirm(this, "Remove everything from the cart?")) { cart.clear(); refresh(); } });
        pay.addActionListener(e -> Ui.guard(this, this::checkout));
        refresh();
    }

    private CartItem selected() {
        int r = Ui.selectedModelRow(table);
        if (r < 0) throw new IllegalArgumentException("Select an item in the cart first.");
        return rows.get(r);
    }

    void refresh() {
        rows = cart.items();
        model.setRowCount(0);
        for (CartItem i : rows)
            model.addRow(new Object[]{i.product().name(), i.product().shopName(), i.qty(), Ui.inr(i.product().price()), Ui.inr(i.lineTotal())});
        sub.setText("Subtotal: " + Ui.inr(cart.subtotal()));
        gst.setText("GST: " + Ui.inr(cart.gst()));
        tot.setText("Total: " + Ui.inr(cart.total()));
    }

    private void checkout() {
        if (cart.isEmpty()) throw new IllegalArgumentException("Your cart is empty.");
        if (!Ui.confirm(this, "Pay " + Ui.inr(cart.total()) + " now?")) return;
        int id = orders.checkout(user.id(), cart.items());
        OrderSummary o = orders.find(id);
        Ui.showText(this, "Bill #" + id, Receipt.forOrder(o, orders.lines(id), user.fullName()));
        cart.clear(); refresh(); onCheckedOut.run();
    }
}
