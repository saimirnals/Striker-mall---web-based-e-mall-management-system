package com.emall.ui;

import com.emall.dao.OrderDao;
import com.emall.model.SalesRow;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;

class SalesPanel extends JPanel {
    private final DefaultTableModel model = Ui.model("Shop", "Units sold", "Revenue (excl. GST)");
    private final JLabel total = new JLabel();
    SalesPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(new JScrollPane(Ui.table(model)), BorderLayout.CENTER);
        JPanel s = new JPanel(new BorderLayout());
        JButton r = Ui.button("Refresh"); r.addActionListener(e -> refresh());
        total.setFont(total.getFont().deriveFont(Font.BOLD, 15f));
        s.add(total, BorderLayout.WEST); s.add(r, BorderLayout.EAST);
        add(s, BorderLayout.SOUTH);
        refresh();
    }
    void refresh() {
        Ui.guard(this, () -> {
            model.setRowCount(0); BigDecimal sum = BigDecimal.ZERO;
            for (SalesRow r : new OrderDao().salesByShop()) {
                model.addRow(new Object[]{r.shop(), r.units(), Ui.inr(r.revenue())});
                sum = sum.add(r.revenue());
            }
            total.setText("Total revenue: " + Ui.inr(sum));
        });
    }
}
