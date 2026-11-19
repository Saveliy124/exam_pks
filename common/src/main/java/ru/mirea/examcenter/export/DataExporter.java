package ru.mirea.examcenter.export;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import ru.mirea.examcenter.model.Applicant;
import ru.mirea.examcenter.model.ExamApplication;
import ru.mirea.examcenter.model.Examiner;

public interface DataExporter {
  void exportTable(Path file, ExportTable table) throws IOException;

  void export(
      Path file,
      List<Applicant> applicants,
      List<ExamApplication> applications,
      List<Examiner> examiners)
      throws IOException;
}
