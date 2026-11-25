package com.emall.model;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record Booking(int id, String title, int screenNo, LocalDateTime showTime,
                      int tickets, BigDecimal total, String status) {}
