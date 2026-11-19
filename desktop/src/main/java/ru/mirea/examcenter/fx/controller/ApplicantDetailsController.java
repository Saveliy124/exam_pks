package ru.mirea.examcenter.fx.controller;

import java.time.LocalDateTime;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import ru.mirea.examcenter.fx.AppContext;
import ru.mirea.examcenter.fx.support.Cells;
import ru.mirea.examcenter.model.*;
import ru.mirea.examcenter.service.Labels;

public final class ApplicantDetailsController {
  private final AppContext context;
  private Applicant applicant;
  @FXML private Label nameLabel, emailLabel, phoneLabel, statusLabel, totalLabel;
  @FXML private TableView<ExamApplication> linkedTable;
  @FXML private TableColumn<ExamApplication, String> examColumn;
  @FXML private TableColumn<ExamApplication, LocalDateTime> dateColumn;
  @FXML private TableColumn<ExamApplication, ApplicationStatus> statusColumn;
  @FXML private TableColumn<ExamApplication, Integer> scoreColumn;
  @FXML private Button newApplicationButton;

  public ApplicantDetailsController(AppContext context) {
    this.context = context;
  }

  public void configure(Applicant applicant) {
    this.applicant = applicant;
    nameLabel.setText(applicant.getFullName());
    emailLabel.setText(applicant.getEmail());
    phoneLabel.setText(applicant.getPhone().isBlank() ? "Телефон не указан" : applicant.getPhone());
    statusLabel.setText(Labels.status(applicant.getStatus()));
    Cells.column(examColumn, ExamApplication::getExamName);
    Cells.date(dateColumn, ExamApplication::getScheduledAt);
    Cells.status(statusColumn, ExamApplication::getStatus);
    Cells.column(scoreColumn, ExamApplication::getScore);
    linkedTable.setItems(
        FXCollections.observableArrayList(
            context.applications.stream()
                .filter(a -> a.getApplicantId() == applicant.getId())
                .toList()));
    linkedTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    linkedTable.setPlaceholder(new Label("У кандидата пока нет заявок"));
    totalLabel.setText("Связанные заявки: " + linkedTable.getItems().size());
    dateColumn.setSortType(TableColumn.SortType.DESCENDING);
    linkedTable.getSortOrder().add(dateColumn);
    newApplicationButton.setDisable(applicant.getStatus() == ApplicantStatus.BLOCKED);
  }

  @FXML
  private void close() {
    ((Stage) nameLabel.getScene().getWindow()).close();
  }

  @FXML
  private void newApplication() {
    close();
    context.navigation.newApplication(applicant.getId());
  }

  @FXML
  private void allApplications() {
    close();
    context.navigation.applications(applicant.getId());
  }
}
