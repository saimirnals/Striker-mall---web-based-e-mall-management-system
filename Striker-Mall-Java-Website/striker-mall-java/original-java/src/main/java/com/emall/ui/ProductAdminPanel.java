package com.emall.ui;

import com.emall.dao.ProductDao;
import com.emall.model.*;
import com.emall.util.AppException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;

/** Shop admins maintain products, prices, stock and (for tech items) specifications. */
class ProductAdminPanel extends JPanel {
    private final ProductDao dao = new ProductDao();
    private final JComboBox<Shop> shopBox = new JComboBox<>();
    private final DefaultTableModel model = Ui.model("ID", "Section", "Name", "Price", "Stock", "Active");
    private final JTable table = Ui.table(model);
    private List<Product> rows = List.of();

    ProductAdminPanel(List<Shop> managed) {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        managed.forEach(shopBox::addItem);
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.add(new JLabel("Shop:")); top.add(shopBox);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton add = Ui.primary("Add product"), edit = Ui.button("Edit"), restock = Ui.button("Restock"),
                toggle = Ui.button("Activate / Deactivate"), specs = Ui.button("Specifications");
        for (JButton x : new JButton[]{add, edit, restock, toggle, specs}) b.add(x);
        add(b, BorderLayout.SOUTH);

        shopBox.addActionListener(e -> refresh());
        add.addActionListener(e -> Ui.guard(this, () -> form(null)));
        edit.addActionListener(e -> Ui.guard(this, () -> form(selected())));
        restock.addActionListener(e -> Ui.guard(this, () -> {
            Product p = selected();
            String s = JOptionPane.showInputDialog(this, "Units to add to stock of " + p.name() + ":", "10");
            if (s == null) return;
            int n = parseInt(s, "units");
            if (n <= 0) throw new IllegalArgumentException("Enter a positive number.");
            dao.update(p.id(), p.section(), p.name(), p.price(), p.stock() + n); refresh();
        }));
        toggle.addActionListener(e -> Ui.guard(this, () -> { Product p = selected(); dao.setActive(p.id(), !p.active()); refresh(); }));
        specs.addActionListener(e -> Ui.guard(this, this::specDialog));
        refresh();
    }

    private Product selected() {
        int r = Ui.selectedModelRow(table);
        if (r < 0) throw new IllegalArgumentException("Select a product first.");
        return rows.get(r);
    }
    private static int parseInt(String s, String what) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid " + what + "."); }
    }
    void refresh() {
        Ui.guard(this, () -> {
            Shop s = (Shop) shopBox.getSelectedItem();
            if (s == null) return;
            rows = dao.byShop(s.id(), true);
            model.setRowCount(0);
            for (Product p : rows) model.addRow(new Object[]{p.id(), p.section(), p.name(), Ui.inr(p.price()), p.stock(), p.active() ? "Yes" : "No"});
        });
    }

    private void form(Product p) {
        JTextField section = new JTextField(p == null ? "" : p.section(), 18), name = new JTextField(p == null ? "" : p.name(), 18),
                   price = new JTextField(p == null ? "" : p.price().toPlainString()), stock = new JTextField(p == null ? "0" : String.valueOf(p.stock()));
        JPanel f = new JPanel(new GridLayout(0, 2, 6, 6));
        f.add(new JLabel("Section / brand")); f.add(section); f.add(new JLabel("Name")); f.add(name);
        f.add(new JLabel("Price (INR)")); f.add(price); f.add(new JLabel("Stock")); f.add(stock);
        if (JOptionPane.showConfirmDialog(this, f, p == null ? "Add product" : "Edit product", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (section.getText().isBlank() || name.getText().isBlank()) throw new IllegalArgumentException("Section and name are required.");
        BigDecimal pr;
        try { pr = new BigDecimal(price.getText().trim()); } catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid price."); }
        if (pr.signum() < 0) throw new IllegalArgumentException("Price cannot be negative.");
        int st = parseInt(stock.getText(), "stock");
        if (st < 0) throw new IllegalArgumentException("Stock cannot be negative.");
        if (p == null) dao.add(((Shop) shopBox.getSelectedItem()).id(), section.getText().trim(), name.getText().trim(), pr, st);
        else dao.update(p.id(), section.getText().trim(), name.getText().trim(), pr, st);
        refresh();
    }

    private void specDialog() {
        Product p = selected();
        DefaultTableModel sm = Ui.model("Specification", "Value");
        JTable st = Ui.table(sm);
        Runnable load = () -> { sm.setRowCount(0); dao.specs(p.id()).forEach((k, v) -> sm.addRow(new Object[]{k, v})); };
        load.run();
        JButton add = Ui.button("Add / update"), del = Ui.button("Delete");
        add.addActionListener(e -> Ui.guard(this, () -> {
            JTextField k = new JTextField(), v = new JTextField();
            JPanel f = new JPanel(new GridLayout(0, 2, 6, 6));
            f.add(new JLabel("Name (e.g. RAM)")); f.add(k); f.add(new JLabel("Value")); f.add(v);
            if (JOptionPane.showConfirmDialog(this, f, "Specification", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                if (k.getText().isBlank() || v.getText().isBlank()) throw new IllegalArgumentException("Both fields are required.");
                dao.saveSpec(p.id(), k.getText().trim(), v.getText().trim()); load.run();
            }
        }));
        del.addActionListener(e -> Ui.guard(this, () -> {
            int r = Ui.selectedModelRow(st);
            if (r < 0) throw new IllegalArgumentException("Select a specification.");
            dao.deleteSpec(p.id(), (String) sm.getValueAt(r, 0)); load.run();
        }));
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.add(new JScrollPane(st), BorderLayout.CENTER);
        JPanel bt = new JPanel(new FlowLayout(FlowLayout.RIGHT)); bt.add(add); bt.add(del);
        panel.add(bt, BorderLayout.SOUTH);
        panel.setPreferredSize(new Dimension(460, 320));
        JOptionPane.showMessageDialog(this, panel, "Specifications - " + p.name(), JOptionPane.PLAIN_MESSAGE);
    }
}
