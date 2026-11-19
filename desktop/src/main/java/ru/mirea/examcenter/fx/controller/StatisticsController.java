package ru.mirea.examcenter.fx.controller;

import java.util.Locale;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import ru.mirea.examcenter.fx.AppContext;
import ru.mirea.examcenter.fx.support.Cells;
import ru.mirea.examcenter.model.ApplicationStatus;
import ru.mirea.examcenter.service.*;

public final class StatisticsController {
  private final AppContext context;
  @FXML private DatePicker fromDate, toDate;
  @FXML
  private Label totalValue,
      upcomingValue,
      averageValue,
      minValue,
      maxValue,
      percentValue,
      applicantsValue,
      examinersValue,
      periodLabel,
      filterError;
  @FXML private BarChart<String, Number> statusChart;

  public StatisticsController(AppContext context) {
    this.context = context;
  }

  @FXML
  private void initialize() {
    Cells.datePicker(fromDate);
    Cells.datePicker(toDate);
    fromDate.valueProperty().addListener((o, a, b) -> calculate());
    toDate.valueProperty().addListener((o, a, b) -> calculate());
    context.snapshot.addListener((o, a, b) -> calculate());
    filterError.managedProperty().bind(filterError.visibleProperty());
    statusChart.setAnimated(false);
    statusChart.setLegendVisible(false);
    ((NumberAxis) statusChart.getYAxis()).setForceZeroInRange(true);
    calculate();
  }

  private long requestSequence;

  private void calculate() {
    final long sequence = ++requestSequence;
    try {
      var from = fromDate.getValue();
      var to = toDate.getValue();
      Filters.period(from, to);
      context.tasks.submit(
          () -> context.api.statistics(from, to),
          stats -> {
            if (sequence != requestSequence) return;
            filterError.setVisible(false);
            totalValue.setText(String.valueOf(stats.total()));
            upcomingValue.setText(String.valueOf(stats.upcoming()));
            averageValue.setText(
                stats.average() != null
                    ? String.format(Locale.forLanguageTag("ru"), "%.1f", stats.average())
                    : "—");
            minValue.setText(stats.minimum() != null ? String.valueOf(stats.minimum()) : "—");
            maxValue.setText(stats.maximum() != null ? String.valueOf(stats.maximum()) : "—");
            percentValue.setText(
                String.format(Locale.forLanguageTag("ru"), "%.1f %%", stats.completedPercent()));
            applicantsValue.setText(String.valueOf(stats.applicants()));
            examinersValue.setText(String.valueOf(stats.activeExaminers()));
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            for (var status : ApplicationStatus.values())
              series
                  .getData()
                  .add(
                      new XYChart.Data<>(
                          Labels.status(status) + " · " + stats.byStatus().get(status),
                          stats.byStatus().get(status)));
            statusChart.getData().setAll(java.util.List.of(series));
            periodLabel.setText(
                "По дате экзамена · "
                    + (fromDate.getValue() == null
                        ? "с начала учёта"
                        : "с " + Cells.DAY.format(fromDate.getValue()))
                    + (toDate.getValue() == null
                        ? " · без конечной даты"
                        : " по " + Cells.DAY.format(toDate.getValue())));
          },
          error -> {
            if (sequence != requestSequence) return;
            filterError.setText(ru.mirea.examcenter.fx.support.Errors.message(error));
            filterError.setVisible(true);
            clearValues();
          });
    } catch (ValidationException error) {
      filterError.setText(error.getMessage());
      filterError.setVisible(true);
      clearValues();
      periodLabel.setText("Исправьте период для расчёта");
    }
  }

  private void clearValues() {
    for (var value :
        new Label[] {
          totalValue,
          upcomingValue,
          averageValue,
          minValue,
          maxValue,
          percentValue,
          applicantsValue,
          examinersValue
        }) value.setText("—");
    statusChart.getData().clear();
  }

  @FXML
  private void reset() {
    fromDate.setValue(null);
    toDate.setValue(null);
    calculate();
  }
}
