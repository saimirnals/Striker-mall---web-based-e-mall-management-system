package com.emall.ui;

import com.emall.dao.*;
import com.emall.model.*;
import com.emall.service.Cart;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/** Browse every shop (tech, restaurants, supermarket, supplements) and add items to the cart. */
class ShopPanel extends JPanel {
    private final ProductDao productDao = new ProductDao();
    private final Cart cart;
    private final Runnable onCartChanged;
    private final JComboBox<Shop> shopBox = new JComboBox<>();
    private final JComboBox<String> sectionBox = new JComboBox<>();
    private final JTextField search = new JTextField(14);
    private final DefaultTableModel model = Ui.model("Section", "Product", "Price", "In stock");
    private final JTable table = Ui.table(model);
    private final JTextArea details = new JTextArea();
    private final JSpinner qty = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));
    private List<Product> all = new ArrayList<>(), shown = new ArrayList<>();
    private boolean loading;

    ShopPanel(Cart cart, Runnable onCartChanged) {
        super(new BorderLayout(8, 8));
        this.cart = cart; this.onCartChanged = onCartChanged;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.add(new JLabel("Shop:")); top.add(shopBox);
        top.add(new JLabel("Section:")); top.add(sectionBox);
        top.add(new JLabel("Search:")); top.add(search);
        JButton find = Ui.button("Search all shops");
        top.add(find);
        add(top, BorderLayout.NORTH);

        details.setEditable(false); details.setLineWrap(true); details.setWrapStyleWord(true);
        details.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane dsp = new JScrollPane(details);
        dsp.setBorder(BorderFactory.createTitledBorder("Details"));
        dsp.setPreferredSize(new Dimension(260, 100));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(table), dsp);
        split.setResizeWeight(0.72);
        add(split, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottom.add(new JLabel("Quantity:")); bottom.add(qty);
        JButton add = Ui.primary("Add to cart");
        bottom.add(add);
        add(bottom, BorderLayout.SOUTH);

        shopBox.addActionListener(e -> { if (!loading) Ui.guard(this, this::loadShop); });
        sectionBox.addActionListener(e -> { if (!loading) applyFilter(); });
        search.addActionListener(e -> applyFilter());
        find.addActionListener(e -> Ui.guard(this, () -> {
            String t = search.getText().trim();
            if (t.isEmpty()) return;
            all = productDao.search(t); refillSections(); showRows(all);
        }));
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) showDetails(); });
        add.addActionListener(e -> Ui.guard(this, this::addToCart));

        Ui.guard(this, () -> {
            loading = true;
            new ShopDao().all().forEach(shopBox::addItem);
            loading = false;
            loadShop();
        });
    }

    void reload() { Ui.guard(this, this::loadShop); }

    private void loadShop() {
        Shop s = (Shop) shopBox.getSelectedItem();
        if (s == null) return;
        search.setText("");
        all = productDao.byShop(s.id(), false);
        refillSections();
        showRows(all);
    }
    private void refillSections() {
        loading = true;
        sectionBox.removeAllItems();
        sectionBox.addItem("All");
        all.stream().map(Product::section).distinct().forEach(sectionBox::addItem);
        loading = false;
    }
    private void applyFilter() {
        String sec = (String) sectionBox.getSelectedItem();
        if (sec == null || sec.equals("All")) showRows(all);
        else showRows(all.stream().filter(p -> p.section().equals(sec)).toList());
    }
    private void showRows(List<Product> rows) {
        shown = rows;
        model.setRowCount(0);
        for (Product p : rows)
            model.addRow(new Object[]{p.section(), p.name() + (shopBox.getSelectedItem() != null && search.getText().isBlank() ? "" : "  (" + p.shopName() + ")"),
                    Ui.inr(p.price()), p.stock() == 0 ? "Out of stock" : p.stock()});
        details.setText("");
    }
    private Product selected() {
        int r = Ui.selectedModelRow(table);
        return r < 0 ? null : shown.get(r);
    }
    private void showDetails() {
        Product p = selected();
        if (p == null) { details.setText(""); return; }
        StringBuilder sb = new StringBuilder(p.name()).append("\n").append(p.shopName()).append(" / ").append(p.section())
                .append("\nPrice: ").append(Ui.inr(p.price())).append("\n\n");
        Map<String, String> specs = productDao.specs(p.id());
        if (specs.isEmpty()) sb.append("No extra specifications.");
        else specs.forEach((k, v) -> sb.append(k).append(": ").append(v).append('\n'));
        details.setText(sb.toString()); details.setCaretPosition(0);
    }
    private void addToCart() {
        Product p = selected();
        if (p == null) throw new IllegalArgumentException("Select a product first.");
        if (p.stock() == 0) throw new IllegalArgumentException(p.name() + " is out of stock.");
        cart.add(p, (Integer) qty.getValue());
        onCartChanged.run();
        Ui.info(this, p.name() + " added to cart.");
    }
}
