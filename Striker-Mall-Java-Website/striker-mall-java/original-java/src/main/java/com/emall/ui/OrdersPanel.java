package com.emall.ui;

import com.emall.dao.OrderDao;
import com.emall.model.*;
import com.emall.service.Receipt;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Purchase history (replaces the old "Cart" menu that listed earlier purchases). */
class OrdersPanel extends JPanel {
    private final User user; private final Runnable onChanged;
    private final OrderDao dao = new OrderDao();
    private final DefaultTableModel model = Ui.model("Bill #", "Date", "Subtotal", "GST", "Total", "Status");
    private final JTable table = Ui.table(model);
    private List<OrderSummary> rows = List.of();

    OrdersPanel(User user, Runnable onChanged) {
        super(new BorderLayout(8, 8));
        this.user = user; this.onChanged = onChanged;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton view = Ui.button("View bill"), cancel = Ui.button("Cancel order");
        b.add(view); b.add(cancel); add(b, BorderLayout.SOUTH);
        view.addActionListener(e -> Ui.guard(this, () -> {
            OrderSummary o = selected();
            Ui.showText(this, "Bill #" + o.id(), Receipt.forOrder(o, dao.lines(o.id()), user.fullName()));
        }));
        cancel.addActionListener(e -> Ui.guard(this, () -> {
            OrderSummary o = selected();
            if (Ui.confirm(this, "Cancel bill #" + o.id() + "? Items will be returned to stock.")) {
                dao.cancel(user.id(), o.id()); refresh(); onChanged.run();
            }
        }));
        refresh();
    }
    private OrderSummary selected() {
        int r = Ui.selectedModelRow(table);
        if (r < 0) throw new IllegalArgumentException("Select a bill first.");
        return rows.get(r);
    }
    void refresh() {
        Ui.guard(this, () -> {
            rows = dao.history(user.id());
            model.setRowCount(0);
            DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            for (OrderSummary o : rows)
                model.addRow(new Object[]{o.id(), o.time().format(f), Ui.inr(o.subtotal()), Ui.inr(o.gst()), Ui.inr(o.total()), o.status()});
        });
    }
}
