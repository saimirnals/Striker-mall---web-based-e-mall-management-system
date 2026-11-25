package com.emall.model;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record Show(int id, int movieId, String title, int screenNo, LocalDateTime time,
                   int totalSeats, int bookedSeats, BigDecimal price) {
    public int available() { return totalSeats - bookedSeats; }
}
