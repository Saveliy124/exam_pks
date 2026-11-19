package ru.mirea.examcenter.server;

public final class NotFoundException extends RuntimeException {
  public NotFoundException() {
    super("Запись не найдена. Обновите данные.");
  }
}
