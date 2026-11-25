package com.emall.model;
public record User(int id, String fullName, String phone, String username, Role role) {
    public boolean isAdmin() { return role == Role.ADMIN; }
}
