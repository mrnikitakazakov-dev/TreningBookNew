-- Таблица для хранения тренировок
CREATE TABLE IF NOT EXISTS training_sessions (
    id BIGSERIAL PRIMARY KEY,
    date_time TIMESTAMP NOT NULL,
    is_booked BOOLEAN DEFAULT FALSE,
    client_first_name VARCHAR(255),
    client_last_name VARCHAR(255),
    client_phone VARCHAR(50)
);

-- Индекс для быстрого поиска по датам
CREATE INDEX idx_training_sessions_date_time ON training_sessions(date_time);

-- Индекс для поиска по забронированным
CREATE INDEX idx_training_sessions_is_booked ON training_sessions(is_booked);