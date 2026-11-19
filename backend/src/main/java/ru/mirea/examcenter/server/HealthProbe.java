package ru.mirea.examcenter.server;

/** Standalone JRE-only healthcheck for Docker; no shell, curl or Spring classpath needed. */
public final class HealthProbe {
  public static void main(String[] args) throws Exception {
    var request =
        java.net.http.HttpRequest.newBuilder(
                java.net.URI.create("http://127.0.0.1:8081/actuator/health"))
            .timeout(java.time.Duration.ofSeconds(3))
            .build();
    var response =
        java.net.http.HttpClient.newHttpClient()
            .send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200 || !response.body().contains("\"UP\"")) System.exit(1);
  }
}
