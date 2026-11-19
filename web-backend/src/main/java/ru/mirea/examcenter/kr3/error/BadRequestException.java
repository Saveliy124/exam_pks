package ru.mirea.examcenter.kr3.error;

/** Сигнал некорректного прикладного запроса. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}
