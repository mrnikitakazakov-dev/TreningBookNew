package com.training.app.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class ScheduleGenerationRequest {
    private List<DayOfWeek> daysOfWeek;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate; // начало недели или конкретный день
    private LocalTime startTime;
    private LocalTime endTime;
    private int sessionDuration; // в минутах
    private int breakDuration;   // в минутах
    private boolean generateForWeek; // если true, генерировать на неделю от startDate, иначе только на startDate

    // геттеры и сеттеры
    public List<DayOfWeek> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(List<DayOfWeek> daysOfWeek) { this.daysOfWeek = daysOfWeek; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public int getSessionDuration() { return sessionDuration; }
    public void setSessionDuration(int sessionDuration) { this.sessionDuration = sessionDuration; }
    public int getBreakDuration() { return breakDuration; }
    public void setBreakDuration(int breakDuration) { this.breakDuration = breakDuration; }
    public boolean isGenerateForWeek() { return generateForWeek; }
    public void setGenerateForWeek(boolean generateForWeek) { this.generateForWeek = generateForWeek; }
}