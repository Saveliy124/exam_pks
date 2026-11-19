package ru.mirea.examcenter.kr3.config;
public final class HealthProbe {
 public static void main(String[] args)throws Exception {
  var req=java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:8080/actuator/health")).timeout(java.time.Duration.ofSeconds(3)).build();
  var r=java.net.http.HttpClient.newHttpClient().send(req,java.net.http.HttpResponse.BodyHandlers.ofString());
  if(r.statusCode()!=200||!r.body().contains("\"UP\""))System.exit(1);
 }
}
