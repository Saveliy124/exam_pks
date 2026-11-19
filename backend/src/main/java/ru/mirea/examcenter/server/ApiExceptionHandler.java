package ru.mirea.examcenter.server;

import java.sql.SQLException;
import org.slf4j.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import ru.mirea.examcenter.api.ApiContracts.ApiError;
import ru.mirea.examcenter.service.ValidationException;

@RestControllerAdvice
public class ApiExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  private ResponseEntity<ApiError> error(HttpStatus status, String field, String text) {
    return ResponseEntity.status(status).body(new ApiError(status.name(), field, text));
  }

  @ExceptionHandler(ValidationException.class)
  ResponseEntity<ApiError> validation(ValidationException e) {
    return error(HttpStatus.BAD_REQUEST, e.field(), e.getMessage());
  }

  @ExceptionHandler({NotFoundException.class, NoResourceFoundException.class})
  ResponseEntity<ApiError> missing(Exception e) {
    return error(HttpStatus.NOT_FOUND, "", "Запись или адрес не найдены. Обновите данные.");
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<ApiError> malformed(Exception e) {
    return error(
        HttpStatus.BAD_REQUEST,
        "",
        "Некорректный запрос. Проверьте поля, дату, статус и числовые значения.");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ApiError> method(Exception e) {
    return error(HttpStatus.METHOD_NOT_ALLOWED, "", "Этот HTTP-метод не поддерживается.");
  }

  @ExceptionHandler(SQLException.class)
  ResponseEntity<ApiError> sql(SQLException e) {
    String s = e.getSQLState();
    LOG.warn("SQL operation failed: {}", s);
    if ("23505".equals(s))
      return error(
          HttpStatus.CONFLICT, "", "Такая запись уже существует: email или активная заявка.");
    if ("23503".equals(s))
      return error(
          HttpStatus.CONFLICT,
          "",
          "Запись связана с другими данными. Кандидата с заявками удалить нельзя.");
    if ("40001".equals(s))
      return error(HttpStatus.CONFLICT, "", "Запись изменена или удалена. Обновите данные.");
    if (s != null && (s.startsWith("22") || s.startsWith("23")))
      return error(HttpStatus.BAD_REQUEST, "", "Данные нарушают ограничения базы.");
    return error(
        HttpStatus.SERVICE_UNAVAILABLE,
        "",
        "Нет соединения с PostgreSQL или база временно недоступна. Проверьте Docker.");
  }

  @ExceptionHandler({CannotCreateTransactionException.class, DataAccessException.class})
  ResponseEntity<ApiError> database(Exception e) {
    LOG.warn("Database unavailable: {}", e.getClass().getSimpleName());
    return error(
        HttpStatus.SERVICE_UNAVAILABLE,
        "",
        "Нет соединения с PostgreSQL. Проверьте Docker и повторите действие.");
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> unexpected(Exception e) {
    LOG.error("Unhandled API error", e);
    return error(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "",
        "Сервер не смог выполнить действие. Подробности в журнале backend.");
  }
}
