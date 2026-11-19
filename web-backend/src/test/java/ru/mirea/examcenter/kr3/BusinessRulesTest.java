package ru.mirea.examcenter.kr3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.mirea.examcenter.kr3.domain.*;
import ru.mirea.examcenter.kr3.service.*;
import ru.mirea.examcenter.kr3.security.PasswordPolicy;
import ru.mirea.examcenter.kr3.filter.ApplicationSpecifications;
import ru.mirea.examcenter.kr3.web.PageableFactory;
import ru.mirea.examcenter.kr3.error.BadRequestException;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class BusinessRulesTest {
    @ParameterizedTest
    @CsvSource({"NEW,0,false,CRITICAL","NEW,29,false,CRITICAL","NEW,30,false,HIGH","NEW,100,false,HIGH","APPROVED,49,true,HIGH","APPROVED,50,true,MEDIUM","NEW,79,true,MEDIUM","NEW,80,true,LOW","NEW,100,true,LOW","COMPLETED,0,false,NOT_APPLICABLE","CANCELLED,100,true,NOT_APPLICABLE"})
    void riskBoundaries(ApplicationStatus status,int value,boolean documents,RiskLevel expected) {
        assertThat(new RiskCalculator().calculate(status,value,documents)).isEqualTo(expected);
    }
    @Test void transitionGraphAndZeroScore() {
        var rules=new ExamRules(Clock.fixed(Instant.parse("2030-01-01T10:00:00Z"),ZoneId.of("Europe/Moscow")));
        rules.transition(ApplicationStatus.NEW,ApplicationStatus.APPROVED);
        rules.transition(ApplicationStatus.APPROVED,ApplicationStatus.COMPLETED);
        rules.score(ApplicationStatus.COMPLETED,0,LocalDateTime.of(2029,1,1,10,0));
        assertThatThrownBy(()->rules.transition(ApplicationStatus.COMPLETED,ApplicationStatus.NEW)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(()->rules.transition(ApplicationStatus.NEW,ApplicationStatus.COMPLETED)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(()->rules.score(ApplicationStatus.NEW,0,LocalDateTime.of(2029,1,1,10,0))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(()->rules.score(ApplicationStatus.COMPLETED,80,LocalDateTime.of(2031,1,1,10,0))).isInstanceOf(BadRequestException.class);
    }
    @Test void futureBoundary() {
        var rules=new ExamRules(Clock.fixed(Instant.parse("2030-01-01T10:00:00Z"),ZoneId.of("Europe/Moscow")));
        assertThatThrownBy(()->rules.future(LocalDateTime.of(2030,1,1,13,0))).isInstanceOf(BadRequestException.class);
        rules.future(LocalDateTime.of(2030,1,1,13,0,1));
    }
    @Test void passwordsRespectBcryptByteLimitAndComplexity() {
        var policy=new PasswordPolicy();policy.validate("A1!abcdefghijkl","user@example.org");
        for(String password:List.of("short","lowercaseonly","UserA1!abcdefghijk","Яя1!"+"я".repeat(40)))
            assertThatThrownBy(()->policy.validate(password,"user@example.org")).isInstanceOf(BadRequestException.class);
    }
    @Test void filteringEscapesWildcardsAndSortingRejectsInjection() {
        assertThat(ApplicationSpecifications.escape("A%_\\B")).isEqualTo("a\\%\\_\\\\b");
        var pages=new PageableFactory();assertThat(pages.applications(0,100,"scheduledAt,asc").getPageSize()).isEqualTo(100);
        assertThatThrownBy(()->pages.applications(0,101,"scheduledAt,asc")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(()->pages.applications(0,10,"externalId;drop table,asc")).isInstanceOf(BadRequestException.class);
    }
    @Test void tagsNormalizeAndCsvCellsAreSpreadsheetSafe() {
        assertThat(ApplicationService.tags(Set.of(" Важное ","важное"))).containsExactly("важное");
        assertThat(CsvApplicationService.safe("=1+1")).isEqualTo("'=1+1");
        assertThat(CsvApplicationService.safe("text")).isEqualTo("text");
        assertThatThrownBy(()->ApplicationService.tags(Set.of(" "))).isInstanceOf(BadRequestException.class);
    }
}
