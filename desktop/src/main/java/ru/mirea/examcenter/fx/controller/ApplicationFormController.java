package ru.mirea.examcenter.fx.controller;

import java.time.*;
import java.util.Comparator;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import ru.mirea.examcenter.fx.AppContext;
import ru.mirea.examcenter.fx.support.Cells;
import ru.mirea.examcenter.model.*;
import ru.mirea.examcenter.service.*;

public final class ApplicationFormController extends BaseFormController {
  @FXML private Label titleLabel, detailsLabel;
  @FXML private ComboBox<Applicant> applicantField;
  @FXML private TextField examField, hourField, minuteField;
  @FXML private DatePicker dateField;
  private ExamApplication original;

  public ApplicationFormController(AppContext context) {
    super(context);
  }

  public static void open(
      AppContext context, ExamApplication row, Long applicantId, boolean readOnly) {
    AppContext.View<ApplicationFormController> view = context.load("application-form.fxml");
    var controller = view.controller();
    controller.configure(row, applicantId, readOnly);
    controller.attach(context.dialog(view.root(), controller.titleLabel.getText(), context.stage));
    if (readOnly) controller.readOnly();
    controller.stage.show();
  }

  private void configure(ExamApplication row, Long requestedApplicant, boolean readOnly) {
    original = row;
    titleLabel.setText(
        readOnly ? "Карточка заявки" : row == null ? "Новая заявка" : "Изменить заявку");
    Long selectedId = row == null ? requestedApplicant : Long.valueOf(row.getApplicantId());
    applicantField
        .getItems()
        .setAll(
            context.applicants.stream()
                .filter(
                    a ->
                        a.getStatus() == ApplicantStatus.ACTIVE
                            || selectedId != null && a.getId() == selectedId)
                .sorted(Comparator.comparing(Applicant::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList());
    applicantField.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(Applicant a) {
            return a == null
                ? ""
                : a.getFullName()
                    + " · "
                    + a.getEmail()
                    + (a.getStatus() == ApplicantStatus.BLOCKED ? " (заблокирован)" : "");
          }

          @Override
          public Applicant fromString(String value) {
            throw new UnsupportedOperationException();
          }
        });
    if (selectedId != null)
      applicantField.getItems().stream()
          .filter(a -> a.getId() == selectedId)
          .findFirst()
          .ifPresent(applicantField::setValue);
    examField.setText(row == null ? "" : row.getExamName());
    LocalDateTime date =
        row == null ? LocalDate.now(context.clock).plusDays(1).atTime(9, 0) : row.getScheduledAt();
    Cells.datePicker(dateField);
    dateField.setValue(date.toLocalDate());
    hourField.setText(String.format("%02d", date.getHour()));
    minuteField.setText(String.format("%02d", date.getMinute()));
    detailsLabel.setText(
        row == null
            ? "Новая заявка создаётся в статусе «Новая». Выберите активного кандидата и будущее"
                + " время."
            : "Статус: "
                + Labels.status(row.getStatus())
                + " · Балл: "
                + (row.getScore() == null ? "—" : row.getScore())
                + "\nСоздана: "
                + Cells.DATE.format(row.getCreatedAt()));
    if (readOnly) {
      applicantField.setDisable(true);
      examField.setEditable(false);
      dateField.setDisable(true);
      hourField.setEditable(false);
      minuteField.setEditable(false);
    }
    fields.put("applicant", applicantField);
    fields.put("exam", examField);
    fields.put("date", dateField);
    fields.put("hour", hourField);
    fields.put("minute", minuteField);
  }

  private int number(TextField field, String key, int max) {
    try {
      int value = Integer.parseInt(field.getText().strip());
      if (value < 0 || value > max) throw new NumberFormatException();
      return value;
    } catch (NumberFormatException failure) {
      throw new ValidationException(
          key, (key.equals("hour") ? "Часы" : "Минуты") + ": целое число от 0 до " + max + ".");
    }
  }

  @FXML
  private void save() {
    clearError();
    try {
      var applicant =
          Rules.required(
              "applicant",
              applicantField.getValue(),
              "Выберите кандидата. Если список пуст, сначала добавьте кандидата.");
      if (applicant.getStatus() == ApplicantStatus.BLOCKED)
        throw new ValidationException(
            "applicant", "Для записи на экзамен выберите активного кандидата.");
      String exam = Rules.text("exam", examField.getText(), "Название экзамена", 2, 120);
      var day = Rules.required("date", dateField.getValue(), "Выберите дату экзамена.");
      LocalDateTime entered =
          day.atTime(number(hourField, "hour", 23), number(minuteField, "minute", 59));
      // При редактировании других полей сохраняем секунды старой записи КР1.
      final LocalDateTime scheduled =
          original != null && original.getScheduledAt().withSecond(0).withNano(0).equals(entered)
              ? original.getScheduledAt()
              : entered;
      Rules.future(scheduled, context.clock);
      final long applicantId = applicant.getId();
      submit(
          () -> {
            if (original == null)
              return context.applicationService.createApplication(applicantId, exam, scheduled);
            context.applicationService.updateApplication(
                original.getId(), applicantId, exam, scheduled);
            return original.getId();
          },
          "Заявка сохранена");
    } catch (ValidationException failure) {
      showFailure(failure);
    }
  }
}
