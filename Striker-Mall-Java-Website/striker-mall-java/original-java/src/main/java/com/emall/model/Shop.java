package com.emall.model;
public record Shop(int id, String name, String category, String description) {
    @Override public String toString() { return name; }
}
