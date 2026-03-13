package com.training.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
public class TestDbController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/test-db")
    public String testDb() {
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList("SELECT 1");
            return "✅ Подключение к PostgreSQL работает!";
        } catch (Exception e) {
            return "❌ Ошибка: " + e.getMessage();
        }
    }
}