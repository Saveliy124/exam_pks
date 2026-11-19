package ru.mirea.examcenter.fx.controller;

import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ru.mirea.examcenter.export.ExportTable;
import ru.mirea.examcenter.fx.AppContext;
import ru.mirea.examcenter.fx.support.Cells;
import ru.mirea.examcenter.model.*;
import ru.mirea.examcenter.service.*;

public final class ExaminersController extends BaseTableController<Examiner> {
  @FXML private TableColumn<Examiner, String> nameColumn, emailColumn, subjectColumn;
  @FXML private TableColumn<Examiner, ExaminerStatus> statusColumn;
  @FXML private ComboBox<ExaminerStatus> statusFilter;
  @FXML private TextField subjectFilter;

  public ExaminersController(AppContext context) {
    super(context);
  }

  @FXML
  private void initialize() {
    Cells.column(nameColumn, Examiner::getFullName);
    Cells.column(emailColumn, Examiner::getEmail);
    Cells.column(subjectColumn, Examiner::getSubject);
    Cells.status(statusColumn, Examiner::getStatus);
    statusFilter.getItems().add(null);
    statusFilter.getItems().addAll(ExaminerStatus.values());
    statusFilter.setConverter(Cells.statusConverter());
    initializeTable(context.examiners);
    table.getSortOrder().add(nameColumn);
    statusFilter.valueProperty().addListener((o, a, b) -> applyFilters());
    subjectFilter.textProperty().addListener((o, a, b) -> applyFilters());
  }

  @Override
  protected long id(Examiner row) {
    return row.getId();
  }

  @Override
  protected ExportTable exportData(List<Examiner> rows) {
    return ExportService.examiners(rows);
  }

  @Override
  protected void applyFilters() {
    filter(
        Filters.examiners(searchField.getText(), statusFilter.getValue(), subjectFilter.getText()));
  }

  @FXML
  private void reset() {
    searchField.clear();
    statusFilter.setValue(null);
    subjectFilter.clear();
  }

  @FXML
  private void add() {
    PersonFormController.openExaminer(context, null, false);
  }

  @FXML
  private void edit() {
    if (selected() != null) PersonFormController.openExaminer(context, selected(), false);
  }

  @FXML
  private void view() {
    if (selected() != null) PersonFormController.openExaminer(context, selected(), true);
  }

  @FXML
  private void deleteSelected() {
    var row = selected();
    if (row == null) return;
    delete(
        "Экзаменатор: " + row.getFullName(),
        () -> {
          context.examinerService.deleteExaminer(row.getId());
          return null;
        });
  }
}
