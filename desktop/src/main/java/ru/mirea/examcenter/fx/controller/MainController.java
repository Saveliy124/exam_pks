package ru.mirea.examcenter.fx.controller;

import java.util.HashMap;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import ru.mirea.examcenter.fx.AppContext;

/** Навигация и общий статус. Контроллеры разделов загружаются один раз. */
public final class MainController implements AppContext.Navigation {
  private final AppContext context;
  private final Map<String, AppContext.View<?>> views = new HashMap<>();
  @FXML private StackPane content;
  @FXML private ToggleButton applicationsNav, applicantsNav, examinersNav, statisticsNav;
  @FXML private Button refreshButton;
  @FXML private Label statusLabel;
  @FXML private Label modeLabel;
  @FXML private ProgressIndicator progress;
  private ToggleGroup navigation;

  public MainController(AppContext context) {
    this.context = context;
  }

  @FXML
  private void initialize() {
    boolean demoMode = "demo".equals(System.getenv("EXAM_APP_MODE"));
    modeLabel.setText(demoMode ? "Демонстрационная база" : "");
    modeLabel.setVisible(demoMode);
    modeLabel.setManaged(demoMode);
    navigation = new ToggleGroup();
    for (var button :
        new ToggleButton[] {applicationsNav, applicantsNav, examinersNav, statisticsNav})
      button.setToggleGroup(navigation);
    statusLabel.textProperty().bind(context.status);
    progress.visibleProperty().bind(context.tasks.activeProperty().greaterThan(0));
    refreshButton.disableProperty().bind(context.tasks.activeProperty().greaterThan(0));
    context.navigation = this;
    applicationsNav.setSelected(true);
    show("applications-view.fxml", applicationsNav);
  }

  private AppContext.View<?> show(String file, ToggleButton button) {
    var view = views.computeIfAbsent(file, context::load);
    content.getChildren().setAll(view.root());
    button.setSelected(true);
    return view;
  }

  @FXML
  private void showApplications() {
    show("applications-view.fxml", applicationsNav);
  }

  @FXML
  private void showApplicants() {
    show("applicants-view.fxml", applicantsNav);
  }

  @FXML
  private void showExaminers() {
    show("examiners-view.fxml", examinersNav);
  }

  @FXML
  private void showStatistics() {
    show("statistics-view.fxml", statisticsNav);
  }

  @FXML
  private void refresh() {
    context.refresh(null);
  }

  @Override
  public void applications(Long applicantId) {
    var view = show("applications-view.fxml", applicationsNav);
    ((ApplicationsController) view.controller()).filterApplicant(applicantId);
  }

  @Override
  public void applicant(long id) {
    var item = context.applicants.stream().filter(a -> a.getId() == id).findFirst();
    item.ifPresent(
        a -> {
          AppContext.View<ApplicantDetailsController> view = context.load("applicant-details.fxml");
          view.controller().configure(a);
          context.dialog(view.root(), "Карточка кандидата", context.stage).show();
        });
  }

  @Override
  public void newApplication(Long applicantId) {
    show("applications-view.fxml", applicationsNav);
    ApplicationFormController.open(context, null, applicantId, false);
  }
}
