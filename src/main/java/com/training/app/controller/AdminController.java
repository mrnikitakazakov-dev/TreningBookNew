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
import java.time.LocalDateTime;
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

    @PostMapping("/delete-slot")
    public String deleteSlot(@RequestParam Long slotId, 
                            RedirectAttributes redirectAttributes) {
        try {
            // Получаем слот по ID
            TrainingSession session = trainingService.getSessionById(slotId);
            
            if (session == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Слот не найден");
            } else if (session.isBooked()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Нельзя удалить забронированный слот");
            } else {
                trainingService.deleteSlot(slotId);
                redirectAttributes.addFlashAttribute("successMessage", "Слот успешно удален");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при удалении слота: " + e.getMessage());
        }
        return "redirect:/admin";
    }

    /**
 * Создание одной тренировки на выбранный день
 */
    @PostMapping("/create-slot-for-day")
    public String createSlotForDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam(required = false, defaultValue = "60") Integer durationMinutes,
            RedirectAttributes redirectAttributes) {
        
        try {
            LocalDateTime dateTime = LocalDateTime.of(date, startTime);
            
            // Проверяем, не существует ли уже такой слот
            boolean created = trainingService.createSingleSlot(dateTime);
            
            if (created) {
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Тренировка успешно создана на " + 
                    date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + " в " + 
                    startTime.format(DateTimeFormatter.ofPattern("HH:mm")));
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Тренировка на это время уже существует");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Ошибка при создании тренировки: " + e.getMessage());
        }
        
        return "redirect:/admin";
    }


}