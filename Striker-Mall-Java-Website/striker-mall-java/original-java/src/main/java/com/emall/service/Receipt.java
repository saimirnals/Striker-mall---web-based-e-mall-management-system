package com.emall.service;

import com.emall.model.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Builds the plain-text bill shown after checkout (replaces bill() of the Python project). */
public final class Receipt {
    private Receipt() {}
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static String forOrder(OrderSummary o, List<OrderLine> lines, String customer) {
        StringBuilder sb = new StringBuilder();
        String bar = "=".repeat(62);
        sb.append(bar).append('\n').append(center("K.M.S. E-MALL", 62)).append('\n').append(bar).append('\n');
        sb.append("Bill No : ").append(o.id()).append('\n');
        sb.append("Date    : ").append(o.time().format(F)).append('\n');
        sb.append("Customer: ").append(customer).append('\n');
        sb.append("Status  : ").append(o.status()).append('\n');
        sb.append("-".repeat(62)).append('\n');
        sb.append(String.format("%-28s %-4s %9s %12s%n", "ITEM", "QTY", "RATE", "AMOUNT"));
        sb.append("-".repeat(62)).append('\n');
        for (OrderLine l : lines)
            sb.append(String.format("%-28s %-4d %9.2f %12.2f%n", cut(l.name(), 28), l.qty(), l.unitPrice(), l.total()));
        sb.append("-".repeat(62)).append('\n');
        sb.append(String.format("%-43s %18.2f%n", "Subtotal", o.subtotal()));
        sb.append(String.format("%-43s %18.2f%n", "GST", o.gst()));
        sb.append(String.format("%-43s %18.2f%n", "TOTAL (INR)", o.total()));
        sb.append(bar).append('\n').append(center("Thank you! Visit again.", 62)).append('\n');
        return sb.toString();
    }

    public static String forBooking(Booking b, String customer) {
        String bar = "=".repeat(48);
        return bar + "\n" + center("K.M.S. E-MALL  -  IMAX TICKET", 48) + "\n" + bar + "\n"
            + "Booking : " + b.id() + "\nCustomer: " + customer + "\nMovie   : " + b.title()
            + "\nScreen  : " + b.screenNo() + "\nShow    : " + b.showTime().format(F)
            + "\nTickets : " + b.tickets() + "\nTotal   : INR " + b.total() + "\nStatus  : " + b.status() + "\n" + bar + "\n";
    }
    private static String cut(String s, int n) { return s.length() <= n ? s : s.substring(0, n - 1) + "."; }
    private static String center(String s, int w) { int p = Math.max(0, (w - s.length()) / 2); return " ".repeat(p) + s; }
}
