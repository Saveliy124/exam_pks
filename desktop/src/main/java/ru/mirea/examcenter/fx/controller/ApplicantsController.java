package ru.mirea.examcenter.fx.controller;

import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ru.mirea.examcenter.export.ExportTable;
import ru.mirea.examcenter.fx.AppContext;
import ru.mirea.examcenter.fx.support.Cells;
import ru.mirea.examcenter.model.*;
import ru.mirea.examcenter.service.*;

public final class ApplicantsController extends BaseTableController<Applicant> {
  @FXML private TableColumn<Applicant, String> nameColumn, emailColumn, phoneColumn;
  @FXML private TableColumn<Applicant, ApplicantStatus> statusColumn;
  @FXML private ComboBox<ApplicantStatus> statusFilter;
  @FXML private TextField domainFilter;

  public ApplicantsController(AppContext context) {
    super(context);
  }

  @FXML
  private void initialize() {
    Cells.column(nameColumn, Applicant::getFullName);
    Cells.column(emailColumn, Applicant::getEmail);
    Cells.column(phoneColumn, Applicant::getPhone);
    Cells.status(statusColumn, Applicant::getStatus);
    statusFilter.getItems().add(null);
    statusFilter.getItems().addAll(ApplicantStatus.values());
    statusFilter.setConverter(Cells.statusConverter());
    initializeTable(context.applicants);
    table.getSortOrder().add(nameColumn);
    statusFilter.valueProperty().addListener((o, a, b) -> applyFilters());
    domainFilter.textProperty().addListener((o, a, b) -> applyFilters());
  }

  @Override
  protected long id(Applicant row) {
    return row.getId();
  }

  @Override
  protected ExportTable exportData(List<Applicant> rows) {
    return ExportService.applicants(rows);
  }

  @Override
  protected void applyFilters() {
    filter(
        Filters.applicants(searchField.getText(), statusFilter.getValue(), domainFilter.getText()));
  }

  @FXML
  private void reset() {
    searchField.clear();
    statusFilter.setValue(null);
    domainFilter.clear();
  }

  @FXML
  private void add() {
    PersonFormController.openApplicant(context, null);
  }

  @FXML
  private void edit() {
    if (selected() != null) PersonFormController.openApplicant(context, selected());
  }

  @FXML
  private void view() {
    if (selected() != null) context.navigation.applicant(selected().getId());
  }

  @FXML
  private void deleteSelected() {
    var row = selected();
    if (row == null) return;
    delete(
        "Кандидат: " + row.getFullName(),
        () -> {
          context.applicantService.deleteApplicant(row.getId());
          return null;
        });
  }
}
