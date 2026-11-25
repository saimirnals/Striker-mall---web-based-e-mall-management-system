package com.emall.dao;

import com.emall.model.*;
import com.emall.util.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class TheatreDao {
    private static final int MAX_PER_BOOKING = 10;

    public List<Movie> movies() {
        List<Movie> out = new ArrayList<>();
        try (Connection c = Db.get(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT movie_id,title,director,genre,duration_min FROM movies ORDER BY title")) {
            while (rs.next()) out.add(new Movie(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5)));
        } catch (SQLException e) { throw new AppException("Could not load movies", e); }
        return out;
    }

    public List<Show> upcomingShows() {
        List<Show> out = new ArrayList<>();
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement(
                "SELECT sh.show_id,sh.movie_id,m.title,sh.screen_no,sh.show_time,sh.total_seats,sh.booked_seats,sh.price " +
                "FROM shows sh JOIN movies m ON m.movie_id=sh.movie_id WHERE sh.show_time > ? ORDER BY sh.show_time")) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new Show(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getInt(4),
                        rs.getTimestamp(5).toLocalDateTime(), rs.getInt(6), rs.getInt(7), rs.getBigDecimal(8)));
            }
        } catch (SQLException e) { throw new AppException("Could not load shows", e); }
        return out;
    }

    public int book(int userId, int showId, int tickets) {
        if (tickets < 1 || tickets > MAX_PER_BOOKING)
            throw new AppException("You can book 1 to " + MAX_PER_BOOKING + " tickets at a time.");
        try (Connection c = Db.get()) {
            c.setAutoCommit(false);
            try {
                int total, booked; BigDecimal price; Timestamp when;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT total_seats,booked_seats,price,show_time FROM shows WHERE show_id=? FOR UPDATE")) {
                    ps.setInt(1, showId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new AppException("Show not found.");
                        total = rs.getInt(1); booked = rs.getInt(2); price = rs.getBigDecimal(3); when = rs.getTimestamp(4);
                    }
                }
                if (when.toLocalDateTime().isBefore(LocalDateTime.now())) throw new AppException("This show has already started.");
                if (total - booked < tickets)
                    throw new AppException("Not enough seats. Only " + (total - booked) + " left.");
                try (PreparedStatement ps = c.prepareStatement("UPDATE shows SET booked_seats=booked_seats+? WHERE show_id=?")) {
                    ps.setInt(1, tickets); ps.setInt(2, showId); ps.executeUpdate();
                }
                int id;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO ticket_bookings(user_id,show_id,num_tickets,total) VALUES(?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, userId); ps.setInt(2, showId); ps.setInt(3, tickets);
                    ps.setBigDecimal(4, price.multiply(BigDecimal.valueOf(tickets)));
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); id = rs.getInt(1); }
                }
                c.commit();
                return id;
            } catch (AppException | SQLException e) {
                c.rollback();
                if (e instanceof AppException ae) throw ae;
                throw new AppException("Booking failed: " + e.getMessage(), e);
            }
        } catch (SQLException e) { throw new AppException("Booking failed: " + e.getMessage(), e); }
    }

    public void cancel(int userId, int bookingId) {
        try (Connection c = Db.get()) {
            c.setAutoCommit(false);
            try {
                int showId, n; Timestamp when;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT b.show_id,b.num_tickets,b.status,s.show_time FROM ticket_bookings b " +
                        "JOIN shows s ON s.show_id=b.show_id WHERE b.booking_id=? AND b.user_id=? FOR UPDATE")) {
                    ps.setInt(1, bookingId); ps.setInt(2, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new AppException("Booking not found.");
                        if (!"BOOKED".equals(rs.getString(3))) throw new AppException("Booking is already cancelled.");
                        showId = rs.getInt(1); n = rs.getInt(2); when = rs.getTimestamp(4);
                    }
                }
                if (when.toLocalDateTime().isBefore(LocalDateTime.now())) throw new AppException("Cannot cancel after the show has started.");
                try (PreparedStatement ps = c.prepareStatement("UPDATE shows SET booked_seats=booked_seats-? WHERE show_id=?")) {
                    ps.setInt(1, n); ps.setInt(2, showId); ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE ticket_bookings SET status='CANCELLED' WHERE booking_id=?")) {
                    ps.setInt(1, bookingId); ps.executeUpdate();
                }
                c.commit();
            } catch (AppException | SQLException e) {
                c.rollback();
                if (e instanceof AppException ae) throw ae;
                throw new AppException("Cancel failed: " + e.getMessage(), e);
            }
        } catch (SQLException e) { throw new AppException("Cancel failed: " + e.getMessage(), e); }
    }

    public List<Booking> bookingsFor(int userId) {
        List<Booking> out = new ArrayList<>();
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement(
                "SELECT b.booking_id,m.title,s.screen_no,s.show_time,b.num_tickets,b.total,b.status " +
                "FROM ticket_bookings b JOIN shows s ON s.show_id=b.show_id JOIN movies m ON m.movie_id=s.movie_id " +
                "WHERE b.user_id=? ORDER BY b.booked_at DESC, b.booking_id DESC")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new Booking(rs.getInt(1), rs.getString(2), rs.getInt(3),
                        rs.getTimestamp(4).toLocalDateTime(), rs.getInt(5), rs.getBigDecimal(6), rs.getString(7)));
            }
        } catch (SQLException e) { throw new AppException("Could not load bookings", e); }
        return out;
    }

    // ---------- admin ----------
    public void addMovie(String title, String director, String genre, int minutes) {
        exec("INSERT INTO movies(title,director,genre,duration_min) VALUES(?,?,?,?)", title, director, genre, minutes);
    }
    public void updateMovie(int id, String title, String director, String genre, int minutes) {
        exec("UPDATE movies SET title=?,director=?,genre=?,duration_min=? WHERE movie_id=?", title, director, genre, minutes, id);
    }
    public void addShow(int movieId, int screen, LocalDateTime time, int seats, BigDecimal price) {
        exec("INSERT INTO shows(movie_id,screen_no,show_time,total_seats,price) VALUES(?,?,?,?,?)",
             movieId, screen, Timestamp.valueOf(time), seats, price);
    }
    public void deleteShow(int showId) {
        try (Connection c = Db.get();
             PreparedStatement chk = c.prepareStatement(
                 "SELECT COUNT(*) FROM ticket_bookings WHERE show_id=? AND status='BOOKED'")) {
            chk.setInt(1, showId);
            try (ResultSet rs = chk.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) throw new AppException("Cannot delete: customers have active bookings for this show.");
            }
        } catch (SQLException e) { throw new AppException("Delete failed", e); }
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM shows WHERE show_id=?")) {
            ps.setInt(1, showId); ps.executeUpdate();
        } catch (SQLException e) {
            throw new AppException("Cannot delete a show that has past bookings.", e);
        }
    }

    private void exec(String sql, Object... args) {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            ps.executeUpdate();
        } catch (SQLException e) { throw new AppException("Database error: " + e.getMessage(), e); }
    }
}
