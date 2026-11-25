package com.emall.util;

public final class Validator {
    private Validator() {}
    public static boolean phone(String s)    { return s != null && s.matches("\\d{10}"); }
    public static boolean username(String s) { return s != null && s.matches("[A-Za-z0-9_]{4,20}"); }
    public static boolean password(String s) { return s != null && s.length() >= 6; }
    public static boolean name(String s)     { return s != null && s.trim().length() >= 2; }
}
