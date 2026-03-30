package com.training.app.service;

import com.training.app.dto.BookingRequest;
import com.training.app.model.TrainingSession;
import com.training.app.repository.TrainingSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;


import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TrainingService {

    @Autowired
    private TrainingSessionRepository trainingSessionRepository;

    // ==================== Существующие методы (ПОЛНОСТЬЮ СОХРАНЕНЫ) ====================

    public List<TrainingSession> getSessionsBetweenDates(LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);
        return trainingSessionRepository.findByDateTimeBetweenOrderByDateTime(startDateTime, endDateTime);
    }

    public List<TrainingSession> getSessionsForDate(LocalDate date) {
        return trainingSessionRepository.findByDateComponentsOrderByDateTime(
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
        // Этот метод оставлен для обратной совместимости
        List<LocalTime> times = List.of(
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                LocalTime.of(18, 0)
        );
        
        // Оптимизированная версия для этого метода
        List<LocalDateTime> allDateTimes = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            for (LocalTime time : times) {
                allDateTimes.add(LocalDateTime.of(currentDate, time));
            }
        }
        
        // Получаем существующие слоты
        LocalDateTime startOfWeek = startDate.atStartOfDay();
        LocalDateTime endOfWeek = startDate.plusDays(7).atTime(LocalTime.MAX);
        Set<LocalDateTime> existingDateTimes = trainingSessionRepository
                .findByDateTimeBetweenOrderByDateTime(startOfWeek, endOfWeek)
                .stream()
                .map(TrainingSession::getDateTime)
                .collect(Collectors.toSet());
        
        // Собираем только новые слоты
        List<TrainingSession> sessionsToSave = new ArrayList<>();
        for (LocalDateTime dateTime : allDateTimes) {
            if (!existingDateTimes.contains(dateTime)) {
                sessionsToSave.add(new TrainingSession(dateTime));
            }
        }
        
        // Сохраняем всё одной пачкой
        if (!sessionsToSave.isEmpty()) {
            trainingSessionRepository.saveAll(sessionsToSave);
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

    // ==================== ОПТИМИЗИРОВАННЫЕ МЕТОДЫ ====================

    /**
     * СОЗДАЁТ СЛОТЫ (тренировки) в заданном диапазоне дат с учётом времени, длительности и перерывов.
     * ПОЛНОСТЬЮ ПЕРЕПИСАНА ДЛЯ МАКСИМАЛЬНОЙ ПРОИЗВОДИТЕЛЬНОСТИ
     */
    @Transactional
    public void createSlots(LocalDate startDate, LocalDate endDate,
                            LocalTime dayStartTime, LocalTime dayEndTime,
                            int slotDuration, int breakDuration,
                            List<Integer> daysOfWeek) {
        
        // Шаг 1: Генерируем все потенциальные даты и времена
        List<LocalDateTime> potentialDateTimes = generatePotentialDateTimes(
                startDate, endDate, dayStartTime, dayEndTime, 
                slotDuration, breakDuration, daysOfWeek
        );
        
        if (potentialDateTimes.isEmpty()) {
            return;
        }
        
        // Шаг 2: Загружаем уже существующие слоты за этот период (ОДИН запрос!)
        LocalDateTime periodStart = startDate.atStartOfDay();
        LocalDateTime periodEnd = endDate.atTime(23, 59, 59);
        
        Set<LocalDateTime> existingDateTimes = trainingSessionRepository
                .findByDateTimeBetweenOrderByDateTime(periodStart, periodEnd)
                .stream()
                .map(TrainingSession::getDateTime)
                .collect(Collectors.toSet());
        
        // Шаг 3: Собираем только новые слоты (проверка в памяти, а не в БД!)
        List<TrainingSession> sessionsToSave = new ArrayList<>();
        for (LocalDateTime dateTime : potentialDateTimes) {
            if (!existingDateTimes.contains(dateTime)) {
                sessionsToSave.add(new TrainingSession(dateTime));
            }
        }
        
        // Шаг 4: Сохраняем всё одной пачкой (ОДИН INSERT!)
        if (!sessionsToSave.isEmpty()) {
            trainingSessionRepository.saveAll(sessionsToSave);
        }
    }

    /**
     * ВСПОМОГАТЕЛЬНЫЙ МЕТОД: генерирует все потенциальные даты и времена для слотов
     */
    private List<LocalDateTime> generatePotentialDateTimes(LocalDate startDate, LocalDate endDate,
                                                           LocalTime dayStartTime, LocalTime dayEndTime,
                                                           int slotDuration, int breakDuration,
                                                           List<Integer> daysOfWeek) {
        List<LocalDateTime> result = new ArrayList<>();
        LocalDate currentDate = startDate;
        Set<Integer> allowedDays = (daysOfWeek == null || daysOfWeek.isEmpty()) 
                ? null 
                : new HashSet<>(daysOfWeek);
        
        while (!currentDate.isAfter(endDate)) {
            // Проверяем, нужно ли создавать слоты в этот день
            if (allowedDays == null || allowedDays.contains(currentDate.getDayOfWeek().getValue())) {
                LocalTime currentTime = dayStartTime;
                while (currentTime.plusMinutes(slotDuration).isBefore(dayEndTime) ||
                        currentTime.plusMinutes(slotDuration).equals(dayEndTime)) {
                    
                    result.add(LocalDateTime.of(currentDate, currentTime));
                    currentTime = currentTime.plusMinutes(slotDuration + breakDuration);
                }
            }
            currentDate = currentDate.plusDays(1);
        }
        return result;
    }

    /**
     * УДАЛЯЕТ слоты в заданном диапазоне дат, которые НЕ забронированы.
     * ОПТИМИЗИРОВАНО: удаление одной пачкой, а не по одному
     */
    @Transactional
    public int deleteSlots(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        
        // Загружаем все слоты за период
        List<TrainingSession> allSessions = trainingSessionRepository
                .findByDateTimeBetweenOrderByDateTime(start, end);
        
        // Отбираем только незабронированные для удаления
        List<TrainingSession> sessionsToDelete = allSessions.stream()
                .filter(session -> !session.isBooked())
                .collect(Collectors.toList());
        
        int deletedCount = sessionsToDelete.size();
        
        // Удаляем одной пачкой, если есть что удалять
        if (deletedCount > 0) {
            trainingSessionRepository.deleteAll(sessionsToDelete);
        }
        
        return deletedCount;
    }

    /**
 * Получить тренировку по ID
 */
    public TrainingSession getSessionById(Long id) {
        return trainingSessionRepository.findById(id).orElse(null);
    }

    /**
     * Удалить слот (только если он не забронирован)
     */
    @Transactional
    public void deleteSlot(Long id) {
        TrainingSession session = getSessionById(id);
        if (session != null && !session.isBooked()) {
            trainingSessionRepository.delete(session);
        }
    }

    @Transactional
    public boolean createSingleSlot(LocalDateTime dateTime) {
        if (!trainingSessionRepository.existsByDateTime(dateTime)) {
            TrainingSession session = new TrainingSession(dateTime);
            trainingSessionRepository.save(session);
            return true;
        }
        return false;
    }


    @Transactional
    @Scheduled(cron = "0 0 0 * * ?") // Каждый день в полночь
    public void deletePastSessions() {
        LocalDateTime now = LocalDateTime.now();
        List<TrainingSession> pastSessions = trainingSessionRepository.findByDateTimeBefore(now);
        
        int deletedCount = pastSessions.size();
        trainingSessionRepository.deleteAll(pastSessions);
        
        System.out.println("Удалено " + deletedCount + " прошедших тренировок");
    }

    /**
     * Получить все тренировки до указанной даты
     */
    public List<TrainingSession> getSessionsBefore(LocalDateTime dateTime) {
        return trainingSessionRepository.findByDateTimeBefore(dateTime);
    }




}