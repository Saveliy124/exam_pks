package ru.mirea.examcenter.kr3.config;
import java.time.*;
import org.springframework.context.annotation.*;
@Configuration
public class ClockConfiguration {
 @Bean public Clock examClock() { return Clock.system(ZoneId.of("Europe/Moscow")); }
}
