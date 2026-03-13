package com.training.app.controller;

import com.training.app.model.TrainingSession;
import com.training.app.service.TrainingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private TrainingService trainingService;

    @GetMapping
    public String adminPage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) YearMonth month,
            Principal principal,
            Model model) {

        if (date == null) {
            date = LocalDate.now();
        }
        if (month == null) {
            month = YearMonth.from(date);
        }

        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        List<TrainingSession> monthSessions = trainingService.getSessionsBetweenDates(monthStart, monthEnd);
        List<TrainingSession> daySessions = trainingService.getSessionsForDate(date);

        // Подготовка статистики для календаря
        Map<String, int[]> calendarStats = new HashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (TrainingSession ts : monthSessions) {
            String key = ts.getDateTime().format(fmt);
            int[] stats = calendarStats.getOrDefault(key, new int[]{0, 0});
            stats[0]++; // всего тренировок в этот день
            if (ts.isBooked()) {
                stats[1]++; // занято
            }
            calendarStats.put(key, stats);
        }

        model.addAttribute("username", principal.getName());
        model.addAttribute("selectedDate", date);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("monthSessions", monthSessions);
        model.addAttribute("daySessions", daySessions);
        model.addAttribute("calendarStats", calendarStats);
        model.addAttribute("prevMonth", month.minusMonths(1));
        model.addAttribute("nextMonth", month.plusMonths(1));
        model.addAttribute("today", LocalDate.now());

        return "admin";
    }

    @PostMapping("/create-slots")
    public String createSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime endTime,
            @RequestParam int durationMinutes,
            @RequestParam int breakMinutes,
            @RequestParam(required = false) List<Integer> daysOfWeek,
            RedirectAttributes redirectAttributes) {

        try {
            trainingService.createSlots(startDate, endDate, startTime, endTime, durationMinutes, breakMinutes, daysOfWeek);
            redirectAttributes.addFlashAttribute("successMessage", "Слоты успешно созданы");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при создании слотов: " + e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/delete-slots")
    public String deleteSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            RedirectAttributes redirectAttributes) {

        try {
            int deleted = trainingService.deleteSlots(startDate, endDate);
            redirectAttributes.addFlashAttribute("successMessage", "Удалено " + deleted + " слотов");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при удалении слотов: " + e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/cancel-booking/{id}")
    public String cancelBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        boolean cancelled = trainingService.cancelBooking(id);
        if (cancelled) {
            redirectAttributes.addFlashAttribute("successMessage", "Бронь отменена");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Не удалось отменить бронь");
        }
        return "redirect:/admin";
    }
}