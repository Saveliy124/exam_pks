package ru.mirea.examcenter.export;

import ru.mirea.examcenter.model.Applicant;
import ru.mirea.examcenter.model.ExamApplication;
import ru.mirea.examcenter.model.Examiner;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface DataExporter {
    void export(Path file, List<Applicant> applicants, List<ExamApplication> applications,
                List<Examiner> examiners)
            throws IOException;
}
