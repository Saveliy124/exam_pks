package ru.mirea.examcenter.fx.support;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import ru.mirea.examcenter.service.Labels;

public final class Cells {
  public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
  public static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy");

  private Cells() {}

  public static void datePicker(DatePicker picker) {
    picker.setEditable(false);
    picker.setConverter(new javafx.util.converter.LocalDateStringConverter(DAY, DAY));
  }

  public static <T, V> void column(TableColumn<T, V> column, Function<T, V> getter) {
    column.setCellValueFactory(
        value -> new ReadOnlyObjectWrapper<>(getter.apply(value.getValue())));
  }

  public static <T> void date(
      TableColumn<T, LocalDateTime> column, Function<T, LocalDateTime> getter) {
    column(column, getter);
    column.setCellFactory(
        c ->
            new TableCell<>() {
              @Override
              protected void updateItem(LocalDateTime value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : DATE.format(value));
              }
            });
  }

  public static <T, E extends Enum<E>> void status(
      TableColumn<T, E> column, Function<T, E> getter) {
    column(column, getter);
    column.setCellFactory(
        c ->
            new TableCell<>() {
              @Override
              protected void updateItem(E value, boolean empty) {
                super.updateItem(value, empty);
                setText(null);
                if (empty || value == null) {
                  setGraphic(null);
                  return;
                }
                Label label = new Label(Labels.status(value));
                label
                    .getStyleClass()
                    .addAll(
                        "status-pill", "status-" + value.name().toLowerCase(java.util.Locale.ROOT));
                setGraphic(label);
              }
            });
  }

  public static <E extends Enum<E>> StringConverter<E> statusConverter() {
    return new StringConverter<>() {
      public String toString(E value) {
        return Labels.status(value);
      }

      public E fromString(String value) {
        throw new UnsupportedOperationException();
      }
    };
  }
}
