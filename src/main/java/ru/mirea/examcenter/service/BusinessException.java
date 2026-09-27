package ru.mirea.examcenter.service;

/** Ошибка предметной области: неверные данные, ID или переход статуса. */
public final class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
