FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --chmod=0444 backend/target/exam-backend-3.0.0-exec.jar /app/app.jar
COPY --chmod=0555 backend/target/classes/ru/mirea/examcenter/server/HealthProbe.class /app/probe/ru/mirea/examcenter/server/HealthProbe.class
USER 10001:10001
EXPOSE 8081
ENTRYPOINT ["java","-Duser.timezone=Europe/Moscow","-jar","/app/app.jar"]
