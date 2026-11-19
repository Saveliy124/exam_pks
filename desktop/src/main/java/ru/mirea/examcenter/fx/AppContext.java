package ru.mirea.examcenter.fx;

import java.io.IOException;
import java.time.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.*;
import ru.mirea.examcenter.client.*;
import ru.mirea.examcenter.export.*;
import ru.mirea.examcenter.fx.support.*;
import ru.mirea.examcenter.model.*;
import ru.mirea.examcenter.service.*;

public final class AppContext implements AutoCloseable {
  public final Clock clock;
  public final ApiClient api = new ApiClient();
  public final ApplicantClient applicantService = new ApplicantClient(api);
  public final ExaminerClient examinerService = new ExaminerClient(api);
  public final ApplicationClient applicationService = new ApplicationClient(api);
  public final DataExporter exporter = new XlsxExporter();
  public final AsyncRunner tasks = new AsyncRunner();
  public final Dialogs dialogs;
  public final ObservableList<Applicant> applicants = FXCollections.observableArrayList();
  public final ObservableList<ExamApplication> applications = FXCollections.observableArrayList();
  public final ObservableList<Examiner> examiners = FXCollections.observableArrayList();
  public final ObjectProperty<CenterSnapshot> snapshot =
      new SimpleObjectProperty<>(CenterSnapshot.empty());
  public final StringProperty status = new SimpleStringProperty("Подключение к backend…");
  public final BooleanProperty loaded = new SimpleBooleanProperty(false);
  public final BooleanProperty refreshing = new SimpleBooleanProperty(false);
  public Stage stage;
  public Navigation navigation;
  private boolean refreshAgain;
  private final java.util.List<Runnable> afterRefresh = new java.util.ArrayList<>();

  public interface Navigation {
    void applications(Long applicantId);

    void applicant(long id);

    void newApplication(Long applicantId);
  }

  public AppContext() {
    this(Clock.systemDefaultZone(), new Dialogs());
  }

  public AppContext(Clock clock, Dialogs dialogs) {
    this.clock = clock;
    this.dialogs = dialogs;
  }

  public void refresh(Runnable after) {
    if (after != null) afterRefresh.add(after);
    if (refreshing.get()) {
      refreshAgain = true;
      return;
    }
    refreshing.set(true);
    status.set("Обновление данных…");
    tasks.submit(
        api::snapshot,
        data -> {
          applicants.setAll(data.applicants());
          applications.setAll(data.applications());
          examiners.setAll(data.examiners());
          snapshot.set(data);
          loaded.set(true);
          refreshing.set(false);
          status.set(
              "Данные обновлены в "
                  + LocalTime.now(clock)
                      .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
          if (refreshAgain) {
            refreshAgain = false;
            refresh(null);
          } else {
            var callbacks = java.util.List.copyOf(afterRefresh);
            afterRefresh.clear();
            callbacks.forEach(Runnable::run);
          }
        },
        error -> {
          refreshing.set(false);
          refreshAgain = false;
          afterRefresh.clear();
          status.set(Errors.message(error));
          dialogs.error(stage, error);
        });
  }

  public <T> void save(
      Callable<T> operation, String message, Consumer<T> success, Consumer<Throwable> failure) {
    tasks.submit(
        operation,
        result -> {
          success.accept(result);
          refresh(() -> status.set(message));
        },
        failure);
  }

  public record View<T>(Parent root, T controller) {}

  public <T> View<T> load(String resource) {
    try {
      FXMLLoader loader =
          new FXMLLoader(AppContext.class.getResource("/ru/mirea/examcenter/fx/" + resource));
      loader.setClassLoader(AppContext.class.getClassLoader());
      loader.setControllerFactory(
          type -> {
            try {
              return type.getConstructor(AppContext.class).newInstance(this);
            } catch (ReflectiveOperationException ex) {
              throw new IllegalStateException(
                  "Не удалось создать контроллер " + type.getSimpleName(), ex);
            }
          });
      Parent root = loader.load();
      return new View<>(root, loader.getController());
    } catch (IOException ex) {
      throw new IllegalStateException("Не удалось загрузить окно " + resource, ex);
    }
  }

  public Stage dialog(Parent root, String title, Window owner) {
    Stage dialog = new Stage();
    dialog.initOwner(owner);
    dialog.initModality(Modality.WINDOW_MODAL);
    dialog.setTitle(title);
    Scene scene = new Scene(root);
    style(scene);
    dialog.setScene(scene);
    dialog.setResizable(false);
    return dialog;
  }

  public static void style(Scene scene) {
    scene
        .getStylesheets()
        .add(AppContext.class.getResource("/ru/mirea/examcenter/fx/app.css").toExternalForm());
  }

  @Override
  public void close() {
    tasks.close();
  }
}
