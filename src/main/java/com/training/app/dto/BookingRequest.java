package com.training.app.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class BookingRequest {
    
    private Long sessionId;
    
    @NotBlank(message = "Имя обязательно")
    private String firstName;
    
    @NotBlank(message = "Фамилия обязательна")
    private String lastName;
    
    @NotBlank(message = "Номер телефона обязателен")
    @Pattern(regexp = "^\\+?[0-9\\-\\s]{10,15}$", message = "Введите корректный номер телефона")
    private String phone;
    
    // Геттеры и сеттеры
    public Long getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
}