package com.training.app.service;

import com.training.app.dto.BookingRequest;
import com.training.app.model.TrainingSession;
import com.training.app.repository.TrainingSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TrainingService {

    @Autowired
    private TrainingSessionRepository trainingSessionRepository;

    // ==================== Существующие методы ====================

    public List<TrainingSession> getSessionsBetweenDates(LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);
        return trainingSessionRepository.findByDateTimeBetweenOrderByDateTime(startDateTime, endDateTime);
    }

    public List<TrainingSession> getSessionsForDate(LocalDate date) {
        return trainingSessionRepository.findByDateComponents(
                date.getYear(),
                date.getMonthValue(),
                date.getDayOfMonth()
        );
    }

    public List<TrainingSession> getSessionsForWeek(LocalDate startDate) {
        LocalDateTime startOfWeek = startDate.atStartOfDay();
        LocalDateTime endOfWeek = startDate.plusDays(7).atTime(LocalTime.MAX);
        return trainingSessionRepository.findByDateTimeBetweenOrderByDateTime(startOfWeek, endOfWeek);
    }

    public List<TrainingSession> getAllSessions() {
        return trainingSessionRepository.findAll();
    }

    @Transactional
    public void createWeekSessions(LocalDate startDate) {
        // Этот метод можно оставить для обратной совместимости или удалить
        List<LocalTime> times = List.of(
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                LocalTime.of(18, 0)
        );
        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            for (LocalTime time : times) {
                LocalDateTime dateTime = LocalDateTime.of(currentDate, time);
                if (!trainingSessionRepository.existsByDateTime(dateTime)) {
                    TrainingSession session = new TrainingSession(dateTime);
                    trainingSessionRepository.save(session);
                }
            }
        }
    }

    @Transactional
    public boolean bookSession(BookingRequest bookingRequest) {
        if (bookingRequest.getSessionId() == null) {
            return false;
        }
        Optional<TrainingSession> optionalSession =
                trainingSessionRepository.findById(bookingRequest.getSessionId());
        if (optionalSession.isPresent()) {
            TrainingSession session = optionalSession.get();
            if (!session.isBooked()) {
                session.setBooked(true);
                session.setClientFirstName(bookingRequest.getFirstName());
                session.setClientLastName(bookingRequest.getLastName());
                session.setClientPhone(bookingRequest.getPhone());
                trainingSessionRepository.save(session);
                return true;
            }
        }
        return false;
    }

    @Transactional
    public boolean cancelBooking(Long sessionId) {
        Optional<TrainingSession> optionalSession = trainingSessionRepository.findById(sessionId);
        if (optionalSession.isPresent()) {
            TrainingSession session = optionalSession.get();
            session.setBooked(false);
            session.setClientFirstName(null);
            session.setClientLastName(null);
            session.setClientPhone(null);
            trainingSessionRepository.save(session);
            return true;
        }
        return false;
    }

    public long getBookedSessionsCount() {
        return trainingSessionRepository.findByIsBooked(true).size();
    }

    // ==================== Новые методы для гибкого создания расписания ====================

    /**
     * Создаёт слоты (тренировки) в заданном диапазоне дат с учётом времени, длительности и перерывов.
     *
     * @param startDate       начало диапазона дат (включительно)
     * @param endDate         конец диапазона дат (включительно)
     * @param dayStartTime    время начала рабочего дня (например, 09:00)
     * @param dayEndTime      время окончания рабочего дня (например, 21:00)
     * @param slotDuration    длительность одной тренировки в минутах
     * @param breakDuration   перерыв между тренировками в минутах
     * @param daysOfWeek      список дней недели, для которых создавать слоты (1-пн, 7-вс). Если null или пусто — все дни.
     */
    @Transactional
    public void createSlots(LocalDate startDate, LocalDate endDate,
                            LocalTime dayStartTime, LocalTime dayEndTime,
                            int slotDuration, int breakDuration,
                            List<Integer> daysOfWeek) {
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            // Проверяем, нужно ли создавать слоты в этот день
            if (daysOfWeek == null || daysOfWeek.isEmpty() || daysOfWeek.contains(currentDate.getDayOfWeek().getValue())) {
                createSlotsForDay(currentDate, dayStartTime, dayEndTime, slotDuration, breakDuration);
            }
            currentDate = currentDate.plusDays(1);
        }
    }

    /**
     * Создаёт слоты для конкретного дня.
     */
    private void createSlotsForDay(LocalDate date, LocalTime dayStart, LocalTime dayEnd,
                                    int slotDuration, int breakDuration) {
        LocalTime currentTime = dayStart;
        while (currentTime.plusMinutes(slotDuration).isBefore(dayEnd) ||
                currentTime.plusMinutes(slotDuration).equals(dayEnd)) {
            LocalDateTime slotDateTime = LocalDateTime.of(date, currentTime);
            // Проверяем, что такой слот ещё не существует
            if (!trainingSessionRepository.existsByDateTime(slotDateTime)) {
                TrainingSession session = new TrainingSession(slotDateTime);
                trainingSessionRepository.save(session);
            }
            // Переходим к следующему слоту: длительность + перерыв
            currentTime = currentTime.plusMinutes(slotDuration + breakDuration);
        }
    }

    /**
     * Удаляет все слоты (тренировки) в заданном диапазоне дат, которые НЕ являются забронированными.
     * Если слот забронирован, он не удаляется.
     *
     * @param startDate начало диапазона
     * @param endDate   конец диапазона
     * @return количество удалённых слотов
     */
    @Transactional
    public int deleteSlots(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        List<TrainingSession> sessions = trainingSessionRepository.findByDateTimeBetweenOrderByDateTime(start, end);
        int deletedCount = 0;
        for (TrainingSession session : sessions) {
            if (!session.isBooked()) {
                trainingSessionRepository.delete(session);
                deletedCount++;
            }
        }
        return deletedCount;
    }
}