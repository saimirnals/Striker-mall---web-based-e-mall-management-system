package com.emall.model;
import java.math.BigDecimal;
public record SalesRow(String shop, long units, BigDecimal revenue) {}
