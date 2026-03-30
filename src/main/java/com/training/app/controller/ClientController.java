package com.training.app.controller;

import com.training.app.dto.BookingRequest;
import com.training.app.model.TrainingSession;
import com.training.app.service.TrainingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ClientController {
    
    @Autowired
    private TrainingService trainingService;
    
    @GetMapping("/")
    public String index(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) YearMonth month,
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
        
        model.addAttribute("selectedDate", date);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("monthSessions", monthSessions);
        model.addAttribute("daySessions", daySessions);
        model.addAttribute("calendarStats", calendarStats);
        model.addAttribute("bookingRequest", new BookingRequest());
        model.addAttribute("prevMonth", month.minusMonths(1));
        model.addAttribute("nextMonth", month.plusMonths(1));
        model.addAttribute("today", LocalDate.now());
        
        return "index";
    }
    
    @GetMapping("/book/{id}")
    public String showBookingForm(@PathVariable Long id, Model model) {
        TrainingSession session = trainingService.getSessionById(id);
        
        // Проверяем, не является ли дата прошедшей
        if (session != null && session.getDateTime().toLocalDate().isBefore(LocalDate.now())) {
            return "redirect:/?error=pastDate";
        }
        
        model.addAttribute("sessionId", id);
        model.addAttribute("bookingRequest", new BookingRequest());
        return "booking";
    }

    
    @PostMapping("/book")
    public String bookSession(@Valid @ModelAttribute BookingRequest bookingRequest,
                            BindingResult result,
                            @RequestParam(required = false) Long sessionId,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        
           TrainingSession session = trainingService.getSessionById(
            bookingRequest.getSessionId() != null ? 
            bookingRequest.getSessionId() : sessionId);
        
        if (session != null && session.getDateTime().toLocalDate().isBefore(LocalDate.now())) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Нельзя записаться на прошедшую дату");
            return "redirect:/";
        }


        // Если есть ошибки валидации, возвращаем форму с сохранением sessionId
        if (result.hasErrors()) {
            model.addAttribute("sessionId", sessionId != null ? sessionId : bookingRequest.getSessionId());
            model.addAttribute("bookingRequest", bookingRequest);
            return "booking";
        }
        
        if (bookingRequest.getSessionId() == null) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Ошибка: идентификатор тренировки не указан");
            return "redirect:/";
        }
        
        boolean booked = trainingService.bookSession(bookingRequest);
        
        if (booked) {
            redirectAttributes.addFlashAttribute("successMessage", 
                "Вы успешно записаны на тренировку!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "К сожалению, это время уже занято. Пожалуйста, выберите другое.");
        }
        
        return "redirect:/";
    }

    private Map<String, Object> getPageData(LocalDate date, YearMonth month) {
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

        Map<String, Object> data = new HashMap<>();
        data.put("selectedDate", date);
        data.put("selectedMonth", month);
        data.put("monthSessions", monthSessions);
        data.put("daySessions", daySessions);
        data.put("calendarStats", calendarStats);
        data.put("prevMonth", month.minusMonths(1));
        data.put("nextMonth", month.plusMonths(1));
        data.put("today", LocalDate.now());
        
        return data;
    }

}