package ru.mirea.examcenter.export;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.examcenter.model.Applicant;
import ru.mirea.examcenter.model.ExamApplication;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Экспорт текущих данных PostgreSQL в обычный файл Excel. */
public final class XlsxExporter implements DataExporter {
    @Override
    public void export(Path file, List<Applicant> applicants, List<ExamApplication> applications)
            throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);

        try (Workbook workbook = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(file)) {
            CellStyle header = workbook.createCellStyle();
            header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            header.setFont(font);

            CellStyle date = workbook.createCellStyle();
            date.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("dd.mm.yyyy hh:mm"));

            Sheet people = sheet(workbook, "Кандидаты", header,
                    "ID", "ФИО", "Email", "Телефон");
            for (int i = 0; i < applicants.size(); i++) {
                Applicant person = applicants.get(i);
                Row row = people.createRow(i + 1);
                row.createCell(0).setCellValue(person.getId());
                row.createCell(1).setCellValue(person.getFullName());
                row.createCell(2).setCellValue(person.getEmail());
                row.createCell(3).setCellValue(person.getPhone());
            }
            widths(people, 10, 30, 34, 22);

            Sheet requests = sheet(workbook, "Заявки", header,
                    "ID", "ID кандидата", "Кандидат", "Email", "Экзамен",
                    "Дата экзамена", "Статус", "Балл", "Создана");
            for (int i = 0; i < applications.size(); i++) {
                ExamApplication application = applications.get(i);
                Row row = requests.createRow(i + 1);
                row.createCell(0).setCellValue(application.getId());
                row.createCell(1).setCellValue(application.getApplicantId());
                row.createCell(2).setCellValue(application.getApplicantName());
                row.createCell(3).setCellValue(application.getApplicantEmail());
                row.createCell(4).setCellValue(application.getExamName());
                row.createCell(5).setCellValue(application.getScheduledAt());
                row.getCell(5).setCellStyle(date);
                row.createCell(6).setCellValue(application.getStatus().name());
                if (application.getScore() != null)
                    row.createCell(7).setCellValue(application.getScore());
                row.createCell(8).setCellValue(application.getCreatedAt());
                row.getCell(8).setCellStyle(date);
            }
            widths(requests, 10, 14, 30, 34, 30, 22, 18, 12, 22);
            workbook.write(out);
        }
    }

    private Sheet sheet(Workbook workbook, String name, CellStyle header, String... labels) {
        Sheet sheet = workbook.createSheet(name);
        Row row = sheet.createRow(0);
        for (int i = 0; i < labels.length; i++) {
            row.createCell(i).setCellValue(labels[i]);
            row.getCell(i).setCellStyle(header);
        }
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, labels.length - 1));
        return sheet;
    }

    private void widths(Sheet sheet, int... characters) {
        for (int i = 0; i < characters.length; i++)
            sheet.setColumnWidth(i, characters[i] * 256);
    }
}
