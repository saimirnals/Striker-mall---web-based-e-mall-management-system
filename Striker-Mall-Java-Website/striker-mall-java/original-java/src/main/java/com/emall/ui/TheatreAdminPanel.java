package com.emall.ui;

import com.emall.dao.TheatreDao;
import com.emall.model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.*;
import java.util.List;

/** Theatre admin: movies and show timings (replaces theatre_admin()/theatre_changes()). */
class TheatreAdminPanel extends JPanel {
    private static final DateTimeFormatter IN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final TheatreDao dao = new TheatreDao();
    private final DefaultTableModel mm = Ui.model("ID", "Title", "Director", "Genre", "Minutes");
    private final DefaultTableModel sm = Ui.model("Show #", "Movie", "Screen", "Time", "Booked / Total", "Price");
    private final JTable mt = Ui.table(mm), stbl = Ui.table(sm);
    private List<Movie> movies = List.of(); private List<Show> shows = List.of();

    TheatreAdminPanel() {
        super(new GridLayout(2, 1, 8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel a = new JPanel(new BorderLayout(6, 6)); a.setBorder(BorderFactory.createTitledBorder("Movies"));
        a.add(new JScrollPane(mt), BorderLayout.CENTER);
        JPanel ab = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton addM = Ui.primary("Add movie"), editM = Ui.button("Edit movie"); ab.add(addM); ab.add(editM);
        a.add(ab, BorderLayout.SOUTH);
        JPanel b = new JPanel(new BorderLayout(6, 6)); b.setBorder(BorderFactory.createTitledBorder("Upcoming shows"));
        b.add(new JScrollPane(stbl), BorderLayout.CENTER);
        JPanel bb = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton addS = Ui.primary("Schedule show"), delS = Ui.button("Delete show"); bb.add(addS); bb.add(delS);
        b.add(bb, BorderLayout.SOUTH);
        add(a); add(b);

        addM.addActionListener(e -> Ui.guard(this, () -> movieForm(null)));
        editM.addActionListener(e -> Ui.guard(this, () -> {
            int r = Ui.selectedModelRow(mt);
            if (r < 0) throw new IllegalArgumentException("Select a movie.");
            movieForm(movies.get(r));
        }));
        addS.addActionListener(e -> Ui.guard(this, this::showForm));
        delS.addActionListener(e -> Ui.guard(this, () -> {
            int r = Ui.selectedModelRow(stbl);
            if (r < 0) throw new IllegalArgumentException("Select a show.");
            if (Ui.confirm(this, "Delete show #" + shows.get(r).id() + "?")) { dao.deleteShow(shows.get(r).id()); refresh(); }
        }));
        refresh();
    }

    void refresh() {
        Ui.guard(this, () -> {
            movies = dao.movies(); mm.setRowCount(0);
            for (Movie m : movies) mm.addRow(new Object[]{m.id(), m.title(), m.director(), m.genre(), m.durationMin()});
            shows = dao.upcomingShows(); sm.setRowCount(0);
            for (Show s : shows) sm.addRow(new Object[]{s.id(), s.title(), s.screenNo(), s.time().format(IN),
                    s.bookedSeats() + " / " + s.totalSeats(), Ui.inr(s.price())});
        });
    }

    private void movieForm(Movie m) {
        JTextField t = new JTextField(m == null ? "" : m.title(), 18), d = new JTextField(m == null ? "" : m.director()),
                   g = new JTextField(m == null ? "" : m.genre()), n = new JTextField(m == null ? "120" : String.valueOf(m.durationMin()));
        JPanel f = new JPanel(new GridLayout(0, 2, 6, 6));
        f.add(new JLabel("Title")); f.add(t); f.add(new JLabel("Director")); f.add(d);
        f.add(new JLabel("Genre")); f.add(g); f.add(new JLabel("Duration (min)")); f.add(n);
        if (JOptionPane.showConfirmDialog(this, f, m == null ? "Add movie" : "Edit movie", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (t.getText().isBlank()) throw new IllegalArgumentException("Title is required.");
        int mins;
        try { mins = Integer.parseInt(n.getText().trim()); } catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid duration."); }
        if (m == null) dao.addMovie(t.getText().trim(), d.getText().trim(), g.getText().trim(), mins);
        else dao.updateMovie(m.id(), t.getText().trim(), d.getText().trim(), g.getText().trim(), mins);
        refresh();
    }

    private void showForm() {
        if (movies.isEmpty()) throw new IllegalArgumentException("Add a movie first.");
        JComboBox<Movie> mv = new JComboBox<>(movies.toArray(new Movie[0]));
        JTextField screen = new JTextField("1"), when = new JTextField(LocalDateTime.now().plusDays(1).withMinute(0).withSecond(0).withNano(0).format(IN)),
                   seats = new JTextField("100"), price = new JTextField("300");
        JPanel f = new JPanel(new GridLayout(0, 2, 6, 6));
        f.add(new JLabel("Movie")); f.add(mv); f.add(new JLabel("Screen no.")); f.add(screen);
        f.add(new JLabel("Date & time (yyyy-MM-dd HH:mm)")); f.add(when);
        f.add(new JLabel("Total seats")); f.add(seats); f.add(new JLabel("Ticket price (INR)")); f.add(price);
        if (JOptionPane.showConfirmDialog(this, f, "Schedule show", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            LocalDateTime t = LocalDateTime.parse(when.getText().trim(), IN);
            if (t.isBefore(LocalDateTime.now())) throw new IllegalArgumentException("Show time must be in the future.");
            int sc = Integer.parseInt(screen.getText().trim()), se = Integer.parseInt(seats.getText().trim());
            BigDecimal pr = new BigDecimal(price.getText().trim());
            if (sc < 1 || se < 1 || pr.signum() < 0) throw new IllegalArgumentException("Screen, seats and price must be positive.");
            dao.addShow(((Movie) mv.getSelectedItem()).id(), sc, t, se, pr);
        } catch (DateTimeParseException e) { throw new IllegalArgumentException("Use the format yyyy-MM-dd HH:mm.");
        } catch (NumberFormatException e) { throw new IllegalArgumentException("Screen, seats and price must be numbers."); }
        refresh();
    }
}
