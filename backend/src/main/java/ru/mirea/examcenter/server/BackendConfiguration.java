package ru.mirea.examcenter.server;

import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import ru.mirea.examcenter.repository.*;
import ru.mirea.examcenter.service.*;

@Configuration
public class BackendConfiguration {
  @Bean
  Clock clock() {
    return Clock.systemDefaultZone();
  }

  @Bean
  ApplicantService applicants(DataSource source) {
    return new ApplicantService(
        new ApplicantRepository(new TransactionAwareDataSourceProxy(source)));
  }

  @Bean
  ExaminerService examiners(DataSource source) {
    return new ExaminerService(new ExaminerRepository(new TransactionAwareDataSourceProxy(source)));
  }

  @Bean
  ExamApplicationService applications(DataSource source, ApplicantService applicants, Clock clock) {
    return new ExamApplicationService(
        new ExamApplicationRepository(new TransactionAwareDataSourceProxy(source)),
        applicants,
        clock);
  }

  @Bean
  CenterService center(DataSource source) {
    return new CenterService(new CenterRepository(source));
  }

  @Bean
  StatisticsService statistics(Clock clock) {
    return new StatisticsService(clock);
  }
}
