package ru.mirea.examcenter.fx.support;

import java.io.IOException;
import java.util.concurrent.*;
import org.slf4j.*;
import ru.mirea.examcenter.client.ApiException;
import ru.mirea.examcenter.service.ValidationException;

/** Desktop receives API errors; database details are handled only on the backend. */
public final class Errors {
  private static final Logger LOG = LoggerFactory.getLogger(Errors.class);

  private Errors() {}

  public static Throwable unwrap(Throwable error) {
    while ((error instanceof CompletionException || error instanceof ExecutionException)
        && error.getCause() != null) error = error.getCause();
    return error;
  }

  public static String message(Throwable error) {
    error = unwrap(error);
    if (error instanceof ValidationException || error instanceof ApiException)
      return error.getMessage();
    if (error instanceof IOException) {
      LOG.warn("File operation failed ({})", error.getClass().getSimpleName());
      return "Не удалось сохранить файл. Проверьте папку, права доступа и свободное место.";
    }
    LOG.error("Unexpected application error", error);
    return "Не удалось выполнить действие. Повторите попытку; подробности записаны в журнал.";
  }
}
