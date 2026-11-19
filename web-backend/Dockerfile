FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --chmod=0444 web-backend/target/exam-web-backend-3.0.0.jar /app/app.jar
COPY --chmod=0555 web-backend/target/classes/ru/mirea/examcenter/kr3/config/HealthProbe.class /app/probe/ru/mirea/examcenter/kr3/config/HealthProbe.class
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java","-Duser.timezone=Europe/Moscow","-jar","/app/app.jar"]
