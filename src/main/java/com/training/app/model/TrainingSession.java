// Модель данных для тренировки
// src/main/java/com/training/app/model/TrainingSession.java
package com.training.app.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "training_sessions")
public class TrainingSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private LocalDateTime dateTime;
    
    @Column(nullable = false)
    private boolean isBooked = false;
    
    private String clientFirstName;
    
    private String clientLastName;
    
    private String clientPhone;
    
    // Конструкторы
    public TrainingSession() {}
    
    public TrainingSession(LocalDateTime dateTime) {
        this.dateTime = dateTime;
        this.isBooked = false;
    }
    
    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LocalDateTime getDateTime() {
        return dateTime;
    }
    
    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }
    
    public boolean isBooked() {
        return isBooked;
    }
    
    public void setBooked(boolean booked) {
        isBooked = booked;
    }
    
    public String getClientFirstName() {
        return clientFirstName;
    }
    
    public void setClientFirstName(String clientFirstName) {
        this.clientFirstName = clientFirstName;
    }
    
    public String getClientLastName() {
        return clientLastName;
    }
    
    public void setClientLastName(String clientLastName) {
        this.clientLastName = clientLastName;
    }
    
    public String getClientPhone() {
        return clientPhone;
    }
    
    public void setClientPhone(String clientPhone) {
        this.clientPhone = clientPhone;
    }
}