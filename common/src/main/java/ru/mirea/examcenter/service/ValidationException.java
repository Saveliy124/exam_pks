package ru.mirea.examcenter.service;

/** A domain error with an optional form field key; works in both console and GUI. */
public class ValidationException extends RuntimeException {
  private final String field;

  public ValidationException(String message) {
    this("", message);
  }

  public ValidationException(String field, String message) {
    super(message);
    this.field = field;
  }

  public String field() {
    return field;
  }
}
