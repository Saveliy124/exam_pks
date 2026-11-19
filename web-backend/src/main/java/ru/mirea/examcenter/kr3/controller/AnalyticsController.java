package ru.mirea.examcenter.kr3.controller;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;
import ru.mirea.examcenter.kr3.service.AnalyticsService;
import ru.mirea.examcenter.kr3.dto.ExamDtos.AnalyticsResponse;
@RestController @RequestMapping("/api/analytics") @SecurityRequirement(name="bearerAuth")
public class AnalyticsController {
 private final AnalyticsService analytics;public AnalyticsController(AnalyticsService a){analytics=a;}
 @GetMapping("/summary") public AnalyticsResponse summary(){return analytics.summary();}
}
