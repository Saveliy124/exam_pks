package ru.mirea.examcenter.fx.controller;

import java.util.*;
import java.util.concurrent.Callable;
import javafx.beans.property.*;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.mirea.examcenter.fx.AppContext;
import ru.mirea.examcenter.fx.support.Errors;
import ru.mirea.examcenter.service.ValidationException;

/** Сохранение в фоне и единая обратная связь во всех формах. */
public abstract class BaseFormController {
  protected final AppContext context;
  protected Stage stage;
  @FXML protected VBox formBody;
  @FXML protected Label errorLabel;
  @FXML protected Button saveButton, closeButton;
  @FXML protected ProgressIndicator formProgress;
  protected final BooleanProperty saving = new SimpleBooleanProperty();
  protected final Map<String, Control> fields = new HashMap<>();
  private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");

  protected BaseFormController(AppContext context) {
    this.context = context;
  }

  protected void attach(Stage window) {
    stage = window;
    errorLabel.managedProperty().bind(errorLabel.visibleProperty());
    errorLabel.setVisible(false);
    saveButton.disableProperty().bind(saving);
    closeButton.disableProperty().bind(saving);
    formBody.disableProperty().bind(saving);
    formProgress.visibleProperty().bind(saving);
    stage.setOnCloseRequest(
        e -> {
          if (saving.get()) e.consume();
        });
  }

  protected void clearError() {
    errorLabel.setVisible(false);
    fields.values().forEach(control -> control.pseudoClassStateChanged(INVALID, false));
  }

  protected void showFailure(Throwable error) {
    saving.set(false);
    errorLabel.setText(Errors.message(error));
    errorLabel.setVisible(true);
    Throwable cause = Errors.unwrap(error);
    if (cause instanceof ValidationException validation) {
      Control control = fields.get(validation.field());
      if (control != null) {
        control.pseudoClassStateChanged(INVALID, true);
        control.requestFocus();
      }
    }
    stage.sizeToScene();
  }

  protected <T> void submit(Callable<T> action, String message) {
    saving.set(true);
    context.save(
        action,
        message,
        result -> {
          saving.set(false);
          stage.close();
        },
        this::showFailure);
  }

  protected void readOnly() {
    saveButton.setVisible(false);
    saveButton.setManaged(false);
    closeButton.setText("Закрыть");
  }

  @FXML
  protected void close() {
    if (!saving.get()) stage.close();
  }
}
