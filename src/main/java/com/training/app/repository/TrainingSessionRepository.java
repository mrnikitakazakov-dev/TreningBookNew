package com.training.app.repository;

import com.training.app.model.TrainingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {
    
    List<TrainingSession> findByDateTimeBetweenOrderByDateTime(LocalDateTime start, LocalDateTime end);
    
   @Query("SELECT t FROM TrainingSession t WHERE YEAR(t.dateTime) = :year AND MONTH(t.dateTime) = :month AND DAY(t.dateTime) = :day ORDER BY t.dateTime ASC")
    List<TrainingSession> findByDateComponentsOrderByDateTime(
        @Param("year") int year, 
        @Param("month") int month, 
        @Param("day") int day
    );
    
    boolean existsByDateTime(LocalDateTime dateTime);
    
    List<TrainingSession> findByIsBooked(boolean isBooked);
    List<TrainingSession> findByDateTimeBefore(LocalDateTime dateTime);
}