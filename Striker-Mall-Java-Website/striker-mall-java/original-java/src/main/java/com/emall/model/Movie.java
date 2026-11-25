package com.emall.model;
public record Movie(int id, String title, String director, String genre, int durationMin) {
    @Override public String toString() { return title; }
}
