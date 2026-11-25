package com.emall.ui;

import com.emall.dao.TheatreDao;
import com.emall.model.*;
import com.emall.service.Receipt;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

class TheatrePanel extends JPanel {
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private final User user;
    private final TheatreDao dao = new TheatreDao();
    private final DefaultTableModel showModel = Ui.model("Show #", "Movie", "Screen", "Time", "Seats left", "Price / ticket");
    private final DefaultTableModel bookModel = Ui.model("Booking #", "Movie", "Screen", "Time", "Tickets", "Total", "Status");
    private final JTable showTable = Ui.table(showModel), bookTable = Ui.table(bookModel);
    private final JSpinner tickets = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
    private List<Show> shows = List.of(); private List<Booking> bookings = List.of();

    TheatrePanel(User user) {
        super(new GridLayout(2, 1, 8, 8));
        this.user = user;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new BorderLayout(6, 6));
        top.setBorder(BorderFactory.createTitledBorder("IMAX - upcoming shows"));
        top.add(new JScrollPane(showTable), BorderLayout.CENTER);
        JPanel tb = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tb.add(new JLabel("Tickets (max 10):")); tb.add(tickets);
        JButton book = Ui.primary("Book tickets"); tb.add(book);
        top.add(tb, BorderLayout.SOUTH);

        JPanel bot = new JPanel(new BorderLayout(6, 6));
        bot.setBorder(BorderFactory.createTitledBorder("My bookings"));
        bot.add(new JScrollPane(bookTable), BorderLayout.CENTER);
        JPanel bb = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton view = Ui.button("View ticket"), cancel = Ui.button("Cancel booking");
        bb.add(view); bb.add(cancel); bot.add(bb, BorderLayout.SOUTH);
        add(top); add(bot);

        book.addActionListener(e -> Ui.guard(this, () -> {
            int r = Ui.selectedModelRow(showTable);
            if (r < 0) throw new IllegalArgumentException("Select a show first.");
            Show s = shows.get(r); int n = (Integer) tickets.getValue();
            if (!Ui.confirm(this, "Book " + n + " ticket(s) for " + s.title() + " = " + Ui.inr(s.price().multiply(java.math.BigDecimal.valueOf(n))) + "?")) return;
            int id = dao.book(user.id(), s.id(), n);
            refresh();
            bookings.stream().filter(b -> b.id() == id).findFirst()
                    .ifPresent(b -> Ui.showText(this, "Ticket #" + id, Receipt.forBooking(b, user.fullName())));
        }));
        view.addActionListener(e -> Ui.guard(this, () -> {
            Booking b = selectedBooking();
            Ui.showText(this, "Ticket #" + b.id(), Receipt.forBooking(b, user.fullName()));
        }));
        cancel.addActionListener(e -> Ui.guard(this, () -> {
            Booking b = selectedBooking();
            if (Ui.confirm(this, "Cancel booking #" + b.id() + "?")) { dao.cancel(user.id(), b.id()); refresh(); }
        }));
        refresh();
    }
    private Booking selectedBooking() {
        int r = Ui.selectedModelRow(bookTable);
        if (r < 0) throw new IllegalArgumentException("Select a booking first.");
        return bookings.get(r);
    }
    void refresh() {
        Ui.guard(this, () -> {
            shows = dao.upcomingShows(); showModel.setRowCount(0);
            for (Show s : shows)
                showModel.addRow(new Object[]{s.id(), s.title(), s.screenNo(), s.time().format(F),
                        s.available() == 0 ? "SOLD OUT" : s.available(), Ui.inr(s.price())});
            bookings = dao.bookingsFor(user.id()); bookModel.setRowCount(0);
            for (Booking b : bookings)
                bookModel.addRow(new Object[]{b.id(), b.title(), b.screenNo(), b.showTime().format(F), b.tickets(), Ui.inr(b.total()), b.status()});
        });
    }
}
