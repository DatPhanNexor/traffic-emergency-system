package com.example.suco.controller.admin;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class TestPassword {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String hash = encoder.encode("Admin123@");
        System.out.println(hash);
    }
}
// admin@system.com