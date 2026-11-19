package ru.mirea.examcenter.fx.controller;

import java.time.LocalDateTime;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ru.mirea.examcenter.export.ExportTable;
import ru.mirea.examcenter.fx.AppContext;
import ru.mirea.examcenter.fx.support.Cells;
import ru.mirea.examcenter.model.*;
import ru.mirea.examcenter.service.*;

public final class ApplicationsController extends BaseTableController<ExamApplication> {
  @FXML private TableColumn<ExamApplication, String> nameColumn, emailColumn, examColumn;
  @FXML private TableColumn<ExamApplication, LocalDateTime> dateColumn, createdColumn;
  @FXML private TableColumn<ExamApplication, ApplicationStatus> statusColumn;
  @FXML private TableColumn<ExamApplication, Integer> scoreColumn;
  @FXML private ComboBox<ApplicationStatus> statusFilter;
  @FXML private DatePicker fromDate, toDate;
  @FXML private Button approveButton, completeButton, cancelButton, applicantButton;
  @FXML private Label applicantFilterLabel;
  private Long applicantId;

  public ApplicationsController(AppContext context) {
    super(context);
  }

  @FXML
  private void initialize() {
    Cells.column(nameColumn, ExamApplication::getApplicantName);
    Cells.column(emailColumn, ExamApplication::getApplicantEmail);
    Cells.column(examColumn, ExamApplication::getExamName);
    Cells.date(dateColumn, ExamApplication::getScheduledAt);
    Cells.date(createdColumn, ExamApplication::getCreatedAt);
    Cells.status(statusColumn, ExamApplication::getStatus);
    Cells.column(scoreColumn, ExamApplication::getScore);
    statusFilter.getItems().add(null);
    statusFilter.getItems().addAll(ApplicationStatus.values());
    statusFilter.setConverter(Cells.statusConverter());
    initializeTable(context.applications);
    dateColumn.setSortType(TableColumn.SortType.DESCENDING);
    table.getSortOrder().add(dateColumn);
    // Выбор календарём исключает неоднозначный текст даты и незавершённый ввод.
    Cells.datePicker(fromDate);
    Cells.datePicker(toDate);
    statusFilter.valueProperty().addListener((o, a, b) -> applyFilters());
    fromDate.valueProperty().addListener((o, a, b) -> applyFilters());
    toDate.valueProperty().addListener((o, a, b) -> applyFilters());
    applicantFilterLabel.managedProperty().bind(applicantFilterLabel.visibleProperty());
    applicantFilterLabel.setVisible(false);
    bindTransition(approveButton, ApplicationStatus.APPROVED);
    bindTransition(completeButton, ApplicationStatus.COMPLETED);
    bindTransition(cancelButton, ApplicationStatus.CANCELLED);
    applicantButton
        .disableProperty()
        .bind(busy.or(table.getSelectionModel().selectedItemProperty().isNull()));
    editButton.disableProperty().unbind();
    editButton
        .disableProperty()
        .bind(
            busy.or(
                Bindings.createBooleanBinding(
                    () ->
                        selected() == null
                            || selected().getStatus() == ApplicationStatus.COMPLETED
                            || selected().getStatus() == ApplicationStatus.CANCELLED,
                    table.getSelectionModel().selectedItemProperty())));
  }

  private void bindTransition(Button button, ApplicationStatus target) {
    button
        .disableProperty()
        .bind(
            busy.or(
                Bindings.createBooleanBinding(
                    () ->
                        selected() == null || !Rules.canTransition(selected().getStatus(), target),
                    table.getSelectionModel().selectedItemProperty())));
  }

  @Override
  protected long id(ExamApplication row) {
    return row.getId();
  }

  @Override
  protected ExportTable exportData(List<ExamApplication> rows) {
    return ExportService.applications(rows);
  }

  @Override
  protected void applyFilters() {
    try {
      filter(
          Filters.applications(
              searchField.getText(),
              statusFilter.getValue(),
              fromDate.getValue(),
              toDate.getValue(),
              applicantId));
    } catch (ValidationException error) {
      filterFailure(error);
    }
  }

  public void filterApplicant(Long id) {
    reset();
    applicantId = id;
    applicantFilterLabel.setVisible(id != null);
    applicantFilterLabel.setText(
        id == null
            ? ""
            : "Кандидат: "
                + context.applicants.stream()
                    .filter(a -> a.getId() == id)
                    .map(Applicant::getFullName)
                    .findFirst()
                    .orElse("не найден")
                + " · «Сбросить» покажет всех");
    applyFilters();
  }

  @FXML
  private void reset() {
    applicantId = null;
    searchField.clear();
    statusFilter.setValue(null);
    fromDate.setValue(null);
    toDate.setValue(null);
    applicantFilterLabel.setVisible(false);
    applyFilters();
  }

  @FXML
  private void add() {
    ApplicationFormController.open(context, null, applicantId, false);
  }

  @FXML
  private void edit() {
    if (selected() != null) ApplicationFormController.open(context, selected(), null, false);
  }

  @FXML
  private void view() {
    if (selected() != null) ApplicationFormController.open(context, selected(), null, true);
  }

  @FXML
  private void applicant() {
    if (selected() != null) context.navigation.applicant(selected().getApplicantId());
  }

  @FXML
  private void approve() {
    transition(ApplicationStatus.APPROVED);
  }

  @FXML
  private void complete() {
    transition(ApplicationStatus.COMPLETED);
  }

  @FXML
  private void cancel() {
    transition(ApplicationStatus.CANCELLED);
  }

  private void transition(ApplicationStatus target) {
    if (selected() != null) StatusFormController.open(context, selected(), target);
  }

  @FXML
  private void deleteSelected() {
    var row = selected();
    if (row == null) return;
    delete(
        "Заявка: " + row.getApplicantName() + " — " + row.getExamName(),
        () -> {
          context.applicationService.deleteApplication(row.getId());
          return null;
        });
  }
}
