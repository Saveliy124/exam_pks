package ru.mirea.examcenter;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import ru.mirea.examcenter.server.BackendApplication;

/** Real embedded HTTP server, exclusively connected to an explicitly selected *_test database. */
final class TestServer implements AutoCloseable {
  final ConfigurableApplicationContext context;
  final String externalUrl;

  TestServer() {
    String url = System.getenv("EXAM_TEST_DB_URL");
    if (url == null || !url.matches("jdbc:postgresql://[^/]+/[a-zA-Z0-9_]+_test"))
      throw new IllegalStateException("Отказ запускать тестовый сервер на рабочей БД");
    externalUrl = System.getenv("EXAM_TEST_API_URL");
    if (externalUrl != null) {
      try {
        var response =
            java.net.http.HttpClient.newHttpClient()
                .send(
                    java.net.http.HttpRequest.newBuilder(
                            java.net.URI.create(externalUrl + "/api/info"))
                        .build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString());
        String database =
            new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response.body())
                .path("database")
                .asText();
        if (!database.endsWith("_test") || !url.endsWith("/" + database))
          throw new IllegalStateException(
              "Внешний тестовый API должен использовать именно EXAM_TEST_DB_URL с суффиксом _test");
      } catch (Exception e) {
        throw new IllegalStateException(
            "Отказ использовать непроверенный внешний API для тестов", e);
      }
      context = null;
      return;
    }
    context =
        new SpringApplicationBuilder(BackendApplication.class)
            .run(
                "--server.port=0",
                "--spring.datasource.url=" + url,
                "--spring.datasource.username=" + System.getenv("EXAM_DB_USER"),
                "--spring.datasource.password=" + System.getenv("EXAM_DB_PASSWORD"),
                "--logging.level.root=WARN");
  }

  String url() {
    return externalUrl != null
        ? externalUrl
        : "http://127.0.0.1:" + ((WebServerApplicationContext) context).getWebServer().getPort();
  }

  @Override
  public void close() {
    if (context != null) context.close();
  }
}
