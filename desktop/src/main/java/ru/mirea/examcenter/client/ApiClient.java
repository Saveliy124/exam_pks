package ru.mirea.examcenter.client;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.time.*;
import ru.mirea.examcenter.api.ApiContracts.*;
import ru.mirea.examcenter.model.CenterSnapshot;
import ru.mirea.examcenter.service.ValidationException;

/** Only HTTP; desktop has no PostgreSQL driver or backend classes. */
public final class ApiClient {
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  private final ObjectMapper mapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  public String baseUrl() {
    String url =
        System.getProperty(
            "exam.api.url", System.getenv().getOrDefault("EXAM_API_URL", "http://127.0.0.1:8081"));
    URI uri = URI.create(url);
    if (!java.util.Set.of("http", "https").contains(uri.getScheme())
        || uri.getHost() == null
        || uri.getUserInfo() != null
        || uri.getQuery() != null
        || uri.getFragment() != null)
      throw new ApiException(0, "Неверный адрес backend в EXAM_API_URL.");
    return url.replaceAll("/+$", "");
  }

  public <T> T call(String method, String path, Object body, Class<T> type) {
    try {
      var builder =
          HttpRequest.newBuilder(URI.create(baseUrl() + path))
              .timeout(Duration.ofSeconds(20))
              .header("Accept", "application/json");
      if (body != null) builder.header("Content-Type", "application/json");
      builder.method(
          method,
          body == null
              ? HttpRequest.BodyPublishers.noBody()
              : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
      var response =
          http.send(
              builder.build(),
              HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
      if (response.statusCode() >= 400) {
        ApiError error;
        try {
          error = mapper.readValue(response.body(), ApiError.class);
        } catch (IOException invalid) {
          throw new ApiException(
              response.statusCode(),
              "Backend вернул ошибку HTTP "
                  + response.statusCode()
                  + ". Проверьте журнал сервера.");
        }
        if (error.field() != null && !error.field().isBlank())
          throw new ValidationException(error.field(), error.message());
        throw new ApiException(response.statusCode(), error.message());
      }
      return type == Void.class ? null : mapper.readValue(response.body(), type);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(
          0, "Ожидание ответа backend прервано. Обновите данные перед повтором.");
    } catch (IOException e) {
      throw new ApiException(
          0,
          "Нет соединения с backend или ответ не получен. Проверьте Docker и EXAM_API_URL. Если"
              + " выполнялось сохранение, сначала обновите данные.");
    }
  }

  public CenterSnapshot snapshot() {
    return call("GET", "/api/snapshot", null, CenterSnapshot.class);
  }

  public Statistics statistics(LocalDate from, LocalDate to) {
    return call(
        "GET",
        "/api/statistics?"
            + (from == null ? "" : "from=" + from + "&")
            + (to == null ? "" : "to=" + to),
        null,
        Statistics.class);
  }
}
