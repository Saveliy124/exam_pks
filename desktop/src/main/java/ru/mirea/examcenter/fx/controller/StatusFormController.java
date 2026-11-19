package ru.mirea.examcenter.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import ru.mirea.examcenter.fx.AppContext;
import ru.mirea.examcenter.fx.support.Cells;
import ru.mirea.examcenter.model.*;
import ru.mirea.examcenter.service.*;

public final class StatusFormController extends BaseFormController {
  @FXML private Label titleLabel, summaryLabel;
  @FXML private TextField scoreField;
  @FXML private VBox scoreBox;
  private ExamApplication application;
  private ApplicationStatus target;

  public StatusFormController(AppContext context) {
    super(context);
  }

  public static void open(
      AppContext context, ExamApplication application, ApplicationStatus target) {
    AppContext.View<StatusFormController> view = context.load("status-form.fxml");
    var c = view.controller();
    c.application = application;
    c.target = target;
    c.titleLabel.setText(
        switch (target) {
          case APPROVED -> "Согласовать заявку";
          case COMPLETED -> "Записать результат";
          case CANCELLED -> "Отменить заявку";
          default -> "Статус заявки";
        });
    c.summaryLabel.setText(
        application.getApplicantName()
            + "\n"
            + application.getExamName()
            + " · "
            + Cells.DATE.format(application.getScheduledAt())
            + "\n"
            + Labels.status(application.getStatus())
            + " → "
            + Labels.status(target));
    c.scoreBox.setVisible(target == ApplicationStatus.COMPLETED);
    c.scoreBox.setManaged(target == ApplicationStatus.COMPLETED);
    c.fields.put("score", c.scoreField);
    c.attach(context.dialog(view.root(), c.titleLabel.getText(), context.stage));
    c.stage.show();
  }

  @FXML
  private void save() {
    clearError();
    try {
      Integer score = null;
      if (target == ApplicationStatus.COMPLETED) {
        try {
          score = Integer.valueOf(scoreField.getText().strip());
        } catch (NumberFormatException failure) {
          throw new ValidationException("score", "Укажите целый балл от 0 до 100.");
        }
      }
      Rules.transition(
          application.getStatus(), target, application.getScheduledAt(), score, context.clock);
      Integer result = score;
      submit(
          () -> {
            context.applicationService.changeStatus(application.getId(), target, result);
            return application.getId();
          },
          "Статус заявки изменён");
    } catch (ValidationException failure) {
      showFailure(failure);
    }
  }
}
