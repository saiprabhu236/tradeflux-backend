package com.sai.finance.finance_manager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/home")
    public ResponseEntity<?> home() {
        return ResponseEntity.ok(Map.of("message", "Welcome! JWT is valid."));
    }
}