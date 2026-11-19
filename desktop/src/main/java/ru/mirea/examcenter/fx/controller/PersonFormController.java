package ru.mirea.examcenter.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import ru.mirea.examcenter.fx.AppContext;
import ru.mirea.examcenter.model.*;
import ru.mirea.examcenter.service.*;

/** Одинаковые поля справочников обслуживает одна форма без дублирования CRUD. */
public final class PersonFormController extends BaseFormController {
  @FXML private Label titleLabel, subtitleLabel, extraLabel;
  @FXML private TextField fullNameField, emailField, extraField;
  @FXML private ComboBox<Enum<?>> statusField;
  private boolean examiner;
  private long id;

  public PersonFormController(AppContext context) {
    super(context);
  }

  public static void openApplicant(AppContext context, Applicant row) {
    open(context, row, null, false, false);
  }

  public static void openExaminer(AppContext context, Examiner row, boolean readOnly) {
    open(context, null, row, true, readOnly);
  }

  private static void open(
      AppContext context,
      Applicant applicant,
      Examiner examiner,
      boolean isExaminer,
      boolean readOnly) {
    AppContext.View<PersonFormController> view = context.load("person-form.fxml");
    view.controller().configure(applicant, examiner, isExaminer, readOnly);
    view.controller().show(context, view, readOnly);
  }

  private void show(
      AppContext context, AppContext.View<PersonFormController> view, boolean readOnly) {
    attach(context.dialog(view.root(), titleLabel.getText(), context.stage));
    if (readOnly) {
      readOnly();
      fullNameField.setEditable(false);
      emailField.setEditable(false);
      extraField.setEditable(false);
      statusField.setDisable(true);
    }
    stage.show();
  }

  private void configure(Applicant applicant, Examiner row, boolean isExaminer, boolean readOnly) {
    examiner = isExaminer;
    id = row != null ? row.getId() : applicant != null ? applicant.getId() : 0;
    titleLabel.setText(
        (readOnly ? "Карточка" : id == 0 ? "Новый" : "Изменить")
            + (examiner ? " экзаменатора" : " кандидата"));
    if (id == 0) titleLabel.setText(examiner ? "Новый экзаменатор" : "Новый кандидат");
    subtitleLabel.setText("ФИО и email обязательны. Email должен быть уникальным.");
    extraLabel.setText(examiner ? "Предмет *" : "Телефон");
    extraField.setPromptText(examiner ? "Например, Математика" : "Необязательное поле");
    statusField.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(Enum<?> value) {
            return Labels.status(value);
          }

          @Override
          public Enum<?> fromString(String value) {
            throw new UnsupportedOperationException();
          }
        });
    if (examiner) statusField.getItems().setAll(ExaminerStatus.values());
    else statusField.getItems().setAll(ApplicantStatus.values());
    fullNameField.setText(
        row != null ? row.getFullName() : applicant != null ? applicant.getFullName() : "");
    emailField.setText(
        row != null ? row.getEmail() : applicant != null ? applicant.getEmail() : "");
    extraField.setText(
        row != null ? row.getSubject() : applicant != null ? applicant.getPhone() : "");
    statusField.setValue(
        row != null
            ? row.getStatus()
            : applicant != null
                ? applicant.getStatus()
                : examiner ? ExaminerStatus.ACTIVE : ApplicantStatus.ACTIVE);
    fields.put("fullName", fullNameField);
    fields.put("email", emailField);
    fields.put("status", statusField);
    fields.put(examiner ? "subject" : "phone", extraField);
  }

  @FXML
  private void save() {
    clearError();
    try {
      String name = Rules.text("fullName", fullNameField.getText(), "ФИО", 2, 120);
      String email = Rules.email(emailField.getText());
      String extra =
          Rules.text(
              examiner ? "subject" : "phone",
              extraField.getText(),
              examiner ? "Предмет" : "Телефон",
              examiner ? 2 : 0,
              examiner ? 120 : 30);
      Enum<?> status = Rules.required("status", statusField.getValue(), "Выберите статус.");
      submit(
          () -> {
            if (examiner) {
              if (id == 0)
                return context.examinerService.createExaminer(
                    name, email, extra, (ExaminerStatus) status);
              context.examinerService.updateExaminer(
                  id, name, email, extra, (ExaminerStatus) status);
            } else {
              if (id == 0)
                return context.applicantService.createApplicant(
                    name, email, extra, (ApplicantStatus) status);
              context.applicantService.updateApplicant(
                  id, name, email, extra, (ApplicantStatus) status);
            }
            return id;
          },
          "Данные сохранены");
    } catch (ValidationException failure) {
      showFailure(failure);
    }
  }
}
