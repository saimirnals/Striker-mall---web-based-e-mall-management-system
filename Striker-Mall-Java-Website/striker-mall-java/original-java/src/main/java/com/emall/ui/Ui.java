package com.emall.ui;

import com.emall.util.AppException;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Small shared Swing helpers: colours, formatted money, error handling, read-only tables. */
final class Ui {
    private Ui() {}
    static final Color PRIMARY = new Color(0x1F3A5F);
    static final Color ACCENT  = new Color(0xE07A1F);
    static final Color BG      = new Color(0xF4F6FA);
    private static final NumberFormat INR = NumberFormat.getCurrencyInstance(Locale.of("en", "IN"));

    static String inr(BigDecimal v) { return INR.format(v); }

    static void installLookAndFeel() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        UIManager.put("defaultFont", new Font("SansSerif", Font.PLAIN, 13));
    }

    static DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }
    static JTable table(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(24);
        t.setAutoCreateRowSorter(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setReorderingAllowed(false);
        t.setFillsViewportHeight(true);
        return t;
    }
    static JButton button(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        return b;
    }
    static JButton primary(String text) {
        JButton b = button(text);
        b.setBackground(ACCENT); b.setForeground(Color.WHITE); b.setOpaque(true);
        b.setBorderPainted(false);
        return b;
    }
    static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 22f));
        l.setForeground(PRIMARY);
        return l;
    }

    /** Runs an action; any AppException/IllegalArgumentException is shown to the user instead of crashing. */
    static void guard(Component parent, Runnable r) {
        try { r.run(); }
        catch (AppException | IllegalArgumentException e) {
            JOptionPane.showMessageDialog(parent, e.getMessage(), "Notice", JOptionPane.WARNING_MESSAGE);
        } catch (RuntimeException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent, "Unexpected error: " + e, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    static void info(Component p, String msg) { JOptionPane.showMessageDialog(p, msg, "E-Mall", JOptionPane.INFORMATION_MESSAGE); }
    static boolean confirm(Component p, String msg) {
        return JOptionPane.showConfirmDialog(p, msg, "Please confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
    static void showText(Component parent, String title, String text) {
        JTextArea a = new JTextArea(text);
        a.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        a.setEditable(false);
        JScrollPane sp = new JScrollPane(a);
        sp.setPreferredSize(new Dimension(520, 420));
        JOptionPane.showMessageDialog(parent, sp, title, JOptionPane.PLAIN_MESSAGE);
    }
    static int selectedModelRow(JTable t) {
        int r = t.getSelectedRow();
        return r < 0 ? -1 : t.convertRowIndexToModel(r);
    }
    static JPanel header(String text, String right, Runnable onLogout) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(PRIMARY);
        p.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel l = new JLabel(text); l.setForeground(Color.WHITE);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 20f));
        p.add(l, BorderLayout.WEST);
        JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); r.setOpaque(false);
        JLabel who = new JLabel(right); who.setForeground(Color.WHITE); r.add(who);
        JButton out = button("Log out"); out.addActionListener(e -> onLogout.run()); r.add(out);
        p.add(r, BorderLayout.EAST);
        return p;
    }
}
