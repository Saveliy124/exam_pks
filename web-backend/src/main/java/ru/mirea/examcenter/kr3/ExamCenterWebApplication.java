package ru.mirea.examcenter.kr3;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.mirea.examcenter.kr3.config.AppProperties;
@SpringBootApplication(exclude=org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties(AppProperties.class)
public class ExamCenterWebApplication {
    public static void main(String[] args) { SpringApplication.run(ExamCenterWebApplication.class, args); }
}
