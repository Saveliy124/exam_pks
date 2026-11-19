package ru.mirea.examcenter.fx.controller;

import java.util.List;
import java.util.function.Predicate;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.collections.transformation.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ru.mirea.examcenter.export.ExportTable;
import ru.mirea.examcenter.fx.AppContext;
import ru.mirea.examcenter.service.ValidationException;

/** Общая механика таблиц: фильтр → сортировка → видимая выборка → экспорт. */
public abstract class BaseTableController<T> {
  protected final AppContext context;
  @FXML protected TableView<T> table;
  @FXML protected TextField searchField;
  @FXML protected Label countLabel, filterError;
  @FXML protected Button addButton, editButton, deleteButton, viewButton, exportButton;
  protected FilteredList<T> filtered;
  protected SortedList<T> sorted;
  protected BooleanBinding busy;
  protected final BooleanProperty invalidFilter = new SimpleBooleanProperty();
  private Long rememberedId;

  protected BaseTableController(AppContext context) {
    this.context = context;
  }

  protected abstract long id(T row);

  protected abstract ExportTable exportData(List<T> rows);

  protected abstract void applyFilters();

  protected void initializeTable(ObservableList<T> source) {
    filtered = new FilteredList<>(source);
    sorted = new SortedList<>(filtered);
    sorted.comparatorProperty().bind(table.comparatorProperty());
    table.setItems(sorted);
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    table.setPlaceholder(new Label("Нет записей. Измените фильтры или добавьте запись."));
    busy = context.tasks.activeProperty().greaterThan(0).or(context.loaded.not());
    addButton.disableProperty().bind(busy);
    var unavailable = busy.or(table.getSelectionModel().selectedItemProperty().isNull());
    editButton.disableProperty().bind(unavailable);
    deleteButton.disableProperty().bind(unavailable);
    viewButton.disableProperty().bind(unavailable);
    exportButton.disableProperty().bind(busy.or(invalidFilter));
    table
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, old, row) -> {
              if (row != null) rememberedId = id(row);
            });
    context.snapshot.addListener((o, old, snapshot) -> restoreSelection());
    sorted.addListener((javafx.collections.ListChangeListener<T>) change -> updateCount());
    searchField.textProperty().addListener((o, old, value) -> applyFilters());
    filterError.managedProperty().bind(filterError.visibleProperty());
    filterError.setVisible(false);
    updateCount();
  }

  private void restoreSelection() {
    if (rememberedId != null) {
      T found = sorted.stream().filter(r -> id(r) == rememberedId).findFirst().orElse(null);
      table.getSelectionModel().select(found);
      if (found == null) rememberedId = null;
    }
    updateCount();
  }

  private void updateCount() {
    countLabel.setText("Показано: " + sorted.size() + " из " + filtered.getSource().size());
  }

  protected void filter(Predicate<T> predicate) {
    invalidFilter.set(false);
    filterError.setVisible(false);
    filtered.setPredicate(predicate);
  }

  protected void filterFailure(ValidationException error) {
    invalidFilter.set(true);
    filterError.setText(error.getMessage());
    filterError.setVisible(true);
    filtered.setPredicate(row -> false);
  }

  @FXML
  protected void export() {
    var data = exportData(List.copyOf(sorted));
    var destination = context.dialogs.exportFile(context.stage, data.name());
    if (destination == null) return;
    context.tasks.submit(
        () -> {
          context.exporter.exportTable(destination, data);
          return destination;
        },
        path -> context.status.set("Экспортировано записей: " + data.rows().size() + " → " + path),
        failure -> context.dialogs.error(context.stage, failure));
  }

  protected T selected() {
    return table.getSelectionModel().getSelectedItem();
  }

  protected void delete(String label, java.util.concurrent.Callable<Void> action) {
    if (!context.dialogs.confirm(
        context.stage, "Удалить запись?", label + "\nЭто действие удалит запись из базы данных."))
      return;
    context.save(
        action,
        "Запись удалена",
        result -> table.getSelectionModel().clearSelection(),
        failure -> context.dialogs.error(context.stage, failure));
  }
}
