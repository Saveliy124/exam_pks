package ru.mirea.examcenter;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mirea.examcenter.export.*;
import ru.mirea.examcenter.model.*;
import ru.mirea.examcenter.service.*;

class ExportTest {
  @TempDir Path folder;

  @Test
  void exportsExactOrderNativeDatesScoresAndLiteralFormulaText() throws Exception {
    var date = LocalDateTime.of(2026, 10, 16, 11, 30);
    var rows =
        List.of(
            new ExamApplication(
                2,
                1,
                "=1+1",
                "a@b.ru",
                "Физика",
                date,
                ApplicationStatus.COMPLETED,
                0,
                date.minusDays(1)),
            new ExamApplication(
                1,
                1,
                "Иван",
                "a@b.ru",
                "Математика",
                date.plusDays(1),
                ApplicationStatus.NEW,
                null,
                date.minusDays(1)));
    var file = folder.resolve("visible.xlsx");
    new XlsxExporter().exportTable(file, ExportService.applications(rows));
    try (var in = Files.newInputStream(file);
        var book = new XSSFWorkbook(in)) {
      assertThat(book.getNumberOfSheets()).isEqualTo(1);
      var sheet = book.getSheet("Заявки");
      assertThat(sheet.getLastRowNum()).isEqualTo(2);
      assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("=1+1");
      assertThat(sheet.getRow(1).getCell(0).getCellType()).isEqualTo(CellType.STRING);
      assertThat(sheet.getRow(1).getCell(3).getLocalDateTimeCellValue()).isEqualTo(date);
      assertThat(DateUtil.isCellDateFormatted(sheet.getRow(1).getCell(3))).isTrue();
      assertThat(sheet.getRow(1).getCell(5).getNumericCellValue()).isZero();
      assertThat(sheet.getRow(2).getCell(5).getCellType()).isEqualTo(CellType.BLANK);
      assertThat(sheet.getPaneInformation().isFreezePane()).isTrue();
    }
    try (var files = Files.list(folder)) {
      assertThat(files.toList()).containsExactly(file);
    }
  }

  @Test
  void emptySelectionStillHasHeaders() throws Exception {
    var file = folder.resolve("empty.xlsx");
    new XlsxExporter().exportTable(file, ExportService.applicants(List.of()));
    try (var in = Files.newInputStream(file);
        var book = new XSSFWorkbook(in)) {
      assertThat(book.getSheetAt(0).getLastRowNum()).isZero();
      assertThat(book.getSheetAt(0).getRow(0).getCell(0).getStringCellValue()).isEqualTo("ФИО");
    }
  }

  @Test
  void refusesWrongExtensionAndMissingDirectory() {
    var exporter = new XlsxExporter();
    var data = ExportService.applicants(List.of());
    assertThatThrownBy(() -> exporter.exportTable(folder.resolve("file.csv"), data))
        .isInstanceOf(java.io.IOException.class);
    assertThatThrownBy(() -> exporter.exportTable(folder.resolve("absent/file.xlsx"), data))
        .isInstanceOf(java.io.IOException.class);
  }

  @Test
  void immutableSnapshotRetainsNullCells() {
    var values = new ArrayList<Object>(Arrays.asList("Иван", null));
    var data = new ExportTable("Данные", List.of("Имя", "Балл"), List.of(values));
    values.set(0, "Изменено");
    assertThat(data.rows().getFirst()).containsExactly("Иван", null);
    assertThatThrownBy(() -> data.rows().getFirst().set(0, "x"))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
