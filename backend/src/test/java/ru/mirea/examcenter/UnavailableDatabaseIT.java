package ru.mirea.examcenter;

import static org.assertj.core.api.Assertions.*;

import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import ru.mirea.examcenter.server.BackendApplication;

class UnavailableDatabaseIT {
  @Test
  void unavailableDatabaseReturns503WithoutExposingDetails() throws Exception {
    try (var context =
        new SpringApplicationBuilder(BackendApplication.class)
            .run(
                "--server.port=0",
                "--spring.datasource.url=jdbc:postgresql://127.0.0.1:1/unreachable_test",
                "--spring.datasource.username=test",
                "--spring.datasource.password=not-a-real-secret",
                "--spring.datasource.hikari.initialization-fail-timeout=-1",
                "--spring.datasource.hikari.connection-timeout=500",
                "--logging.level.root=ERROR")) {
      int port = ((WebServerApplicationContext) context).getWebServer().getPort();
      var client = HttpClient.newHttpClient();
      var response =
          client.send(
              HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/snapshot"))
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      assertThat(response.statusCode()).isEqualTo(503);
      assertThat(response.body())
          .contains("message")
          .doesNotContain("password", "stackTrace", "SQLException", "not-a-real-secret");
      var health =
          client.send(
              HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/actuator/health"))
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      assertThat(health.statusCode()).isEqualTo(503);
    }
  }
}
