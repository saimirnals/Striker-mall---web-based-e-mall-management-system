package com.emall.model;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record OrderSummary(int id, LocalDateTime time, BigDecimal subtotal, BigDecimal gst,
                           BigDecimal total, String status) {}
