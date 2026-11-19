package ru.mirea.examcenter.fx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class ExamCenterApplication extends Application {
  private AppContext context;

  @Override
  public void init() {
    java.util.Locale.setDefault(java.util.Locale.forLanguageTag("ru-RU"));
  }

  @Override
  public void start(Stage stage) {
    context = new AppContext();
    context.stage = stage;
    var view = context.load("main-view.fxml");
    Scene scene = new Scene(view.root(), 1320, 850);
    AppContext.style(scene);
    stage.setTitle(
        "Экзаменационный центр"
            + ("demo".equals(System.getenv("EXAM_APP_MODE")) ? " · Демонстрация" : ""));
    stage.setScene(scene);
    stage.setMinWidth(1100);
    stage.setMinHeight(740);
    stage.show();
    context.refresh(null);
  }

  @Override
  public void stop() {
    if (context != null) context.close();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
