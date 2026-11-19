package ru.mirea.examcenter.export;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.examcenter.model.*;
import ru.mirea.examcenter.service.ExportService;

public final class XlsxExporter implements DataExporter {
  @Override
  public void export(
      Path file,
      List<Applicant> applicants,
      List<ExamApplication> applications,
      List<Examiner> examiners)
      throws IOException {
    write(
        file,
        List.of(
            ExportService.applicants(applicants),
            ExportService.applications(applications),
            ExportService.examiners(examiners)));
  }

  @Override
  public void exportTable(Path file, ExportTable table) throws IOException {
    write(file, List.of(table));
  }

  private void write(Path destination, List<ExportTable> tables) throws IOException {
    Path target = destination.toAbsolutePath();
    if (!target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xlsx"))
      throw new IOException("Выберите файл с расширением .xlsx.");
    Path parent = target.getParent();
    if (parent == null || !Files.isDirectory(parent))
      throw new IOException("Папка сохранения не найдена.");
    Path temp = Files.createTempFile(parent, ".exam-export-", ".xlsx");
    try {
      try (Workbook book = new XSSFWorkbook();
          OutputStream out = Files.newOutputStream(temp)) {
        CellStyle header = book.createCellStyle();
        header.setFillForegroundColor(IndexedColors.TEAL.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = book.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        header.setFont(font);
        CellStyle date = book.createCellStyle();
        date.setDataFormat(
            book.getCreationHelper().createDataFormat().getFormat("dd.mm.yyyy hh:mm"));
        for (ExportTable data : tables) {
          Sheet sheet = book.createSheet(data.name());
          Row first = sheet.createRow(0);
          first.setHeightInPoints(24);
          for (int c = 0; c < data.headers().size(); c++) {
            Cell cell = first.createCell(c);
            cell.setCellValue(data.headers().get(c));
            cell.setCellStyle(header);
          }
          for (int r = 0; r < data.rows().size(); r++) {
            Row row = sheet.createRow(r + 1);
            for (int c = 0; c < data.headers().size(); c++) {
              Cell cell = row.createCell(c);
              Object value = data.rows().get(r).get(c);
              if (value instanceof Number n) cell.setCellValue(n.doubleValue());
              else if (value instanceof LocalDateTime time) {
                cell.setCellValue(time);
                cell.setCellStyle(date);
              } else if (value != null) cell.setCellValue(value.toString());
            }
          }
          sheet.createFreezePane(0, 1);
          sheet.setAutoFilter(
              new org.apache.poi.ss.util.CellRangeAddress(
                  0, Math.max(0, data.rows().size()), 0, data.headers().size() - 1));
          for (int c = 0; c < data.headers().size(); c++) {
            sheet.autoSizeColumn(c);
            sheet.setColumnWidth(
                c, Math.min(48 * 256, Math.max(15 * 256, sheet.getColumnWidth(c) + 600)));
          }
        }
        book.write(out);
      }
      try {
        Files.move(
            temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException ex) {
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temp);
    }
  }
}
