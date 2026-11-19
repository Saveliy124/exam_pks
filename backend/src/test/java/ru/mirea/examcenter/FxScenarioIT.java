package ru.mirea.examcenter;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.stage.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import ru.mirea.examcenter.fx.AppContext;
import ru.mirea.examcenter.fx.support.Dialogs;
import ru.mirea.examcenter.model.*;

/** Реальные FXML, события JavaFX и PostgreSQL; FileChooser заменён для воспроизводимости. */
class FxScenarioIT {
  static TestServer server;
  static AppContext context;
  static Stage main;
  static TestDialogs dialogs;
  static final Path QA = Path.of("target/qa");

  static final class TestDialogs extends Dialogs {
    boolean confirm = true;
    int confirmations;
    Path destination;
    final List<String> errors = new ArrayList<>();

    @Override
    public boolean confirm(Window owner, String title, String message) {
      confirmations++;
      return confirm;
    }

    @Override
    public Path exportFile(Window owner, String title) {
      return destination;
    }

    @Override
    public void error(Window owner, Throwable failure) {
      errors.add(ru.mirea.examcenter.fx.support.Errors.message(failure));
    }
  }

  static <T> T fx(Callable<T> call) throws Exception {
    var task = new FutureTask<T>(call);
    Platform.runLater(task);
    return task.get(15, TimeUnit.SECONDS);
  }

  static void action(Runnable action) throws Exception {
    fx(
        () -> {
          action.run();
          return null;
        });
  }

  static void await(BooleanSupplier condition) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
    while (!fx(condition::getAsBoolean)) {
      if (System.nanoTime() > deadline)
        fail("Истекло время ожидания JavaFX; ошибки: " + fx(() -> dialogs.errors));
      Thread.sleep(50);
    }
  }

  static void idle() throws Exception {
    await(() -> context.loaded.get() && context.tasks.activeProperty().get() == 0);
  }

  static Parent root() {
    return main.getScene().getRoot();
  }

  @SuppressWarnings("unchecked")
  static <T extends Node> T node(Parent root, String id) {
    Node result = root.lookup("#" + id);
    if (result == null) throw new AssertionError("Элемент не найден: " + id);
    return (T) result;
  }

  static void fire(String id) throws Exception {
    action(() -> ((ButtonBase) node(root(), id)).fire());
  }

  static void dialogFire(String id) throws Exception {
    action(() -> ((ButtonBase) node(dialogRoot(), id)).fire());
  }

  static Stage dialog() {
    return Window.getWindows().stream()
        .filter(w -> w instanceof Stage && w != main && w.isShowing())
        .map(w -> (Stage) w)
        .findFirst()
        .orElseThrow();
  }

  static Parent dialogRoot() {
    return dialog().getScene().getRoot();
  }

  static void field(String id, String value) throws Exception {
    action(() -> ((TextInputControl) node(dialogRoot(), id)).setText(value));
  }

  @SuppressWarnings("unchecked")
  static <T> TableView<T> table() {
    return (TableView<T>) node(root(), "table");
  }

  static void snapshot(String name, boolean modal) throws Exception {
    // Даём оконной системе применить размер Stage и завершить следующий pulse.
    Thread.sleep(250);
    fx(
        () -> {
          Parent root = modal ? dialogRoot() : root();
          root.applyCss();
          root.layout();
          ImageIO.write(
              SwingFXUtils.fromFXImage(root.snapshot(null, null), null),
              "png",
              QA.resolve(name + ".png").toFile());
          return null;
        });
  }

  @BeforeAll
  static void setup() throws Exception {
    TestDatabase.configure();
    TestDatabase.fixture();
    server = new TestServer();
    System.setProperty("exam.api.url", server.url());
    Files.createDirectories(QA);
    var started = new CountDownLatch(1);
    Platform.startup(started::countDown);
    assertThat(started.await(10, TimeUnit.SECONDS)).isTrue();
    action(
        () -> {
          Platform.setImplicitExit(false);
          dialogs = new TestDialogs();
          context = new AppContext(Clock.systemDefaultZone(), dialogs);
          main = new Stage();
          context.stage = main;
          var view = context.load("main-view.fxml");
          var scene = new Scene(view.root(), 1320, 850);
          AppContext.style(scene);
          main.setScene(scene);
          main.setMinWidth(1100);
          main.setMinHeight(740);
          main.setTitle("КР2 — проверка интерфейса");
          main.show();
          context.refresh(null);
        });
    idle();
  }

  @AfterAll
  static void cleanup() throws Exception {
    try {
      if (context != null)
        action(
            () -> {
              new ArrayList<>(Window.getWindows()).forEach(Window::hide);
              context.close();
            });
    } finally {
      Platform.exit();
      if (server != null) server.close();
      TestDatabase.clear();
      System.clearProperty("exam.api.url");
      System.clearProperty("exam.db.url");
    }
  }

  @Test
  void fullUserJourneyWithErrorsFiltersExportAndRecovery() throws Exception {
    assertThat(fx(() -> table().getItems().size())).isEqualTo(10);
    snapshot("01-applications", false);
    action(
        () -> {
          main.setWidth(1100);
          main.setHeight(740);
        });
    snapshot("02-small-window", false);
    action(
        () -> {
          main.setWidth(1320);
          main.setHeight(850);
        });
    // Создание кандидата, обязательные поля, уникальность, редактирование.
    fire("applicantsNav");
    snapshot("03-applicants", false);
    action(
        () -> {
          ((TextField) node(root(), "domainFilter")).setText("example.org");
          ComboBox<ApplicantStatus> status = node(root(), "statusFilter");
          status.setValue(ApplicantStatus.ACTIVE);
        });
    assertThat(fx(() -> table().getItems().size())).isEqualTo(4);
    action(() -> ((TextField) node(root(), "searchField")).setText("Анна"));
    assertThat(fx(() -> table().getItems().size())).isEqualTo(1);
    action(
        () -> {
          ((TextField) node(root(), "searchField")).clear();
          ((TextField) node(root(), "domainFilter")).clear();
          ((ComboBox<?>) node(root(), "statusFilter")).setValue(null);
        });
    fire("addButton");
    dialogFire("saveButton");
    assertThat(fx(() -> ((Label) node(dialogRoot(), "errorLabel")).getText())).contains("ФИО");
    snapshot("04-validation", true);
    field("fullNameField", "Тестовая Кандидатка");
    field("emailField", "bad-email");
    dialogFire("saveButton");
    assertThat(fx(() -> ((Label) node(dialogRoot(), "errorLabel")).getText())).contains("email");
    field("emailField", "anna.demo@example.org");
    dialogFire("saveButton");
    idle();
    assertThat(fx(() -> ((Label) node(dialogRoot(), "errorLabel")).getText()))
        .contains("используется");
    field("emailField", "gui.test@example.org");
    field("extraField", "+7 900 777-77-77");
    dialogFire("saveButton");
    idle();
    long candidateId =
        fx(
            () ->
                context.applicants.stream()
                    .filter(a -> a.getEmail().equals("gui.test@example.org"))
                    .findFirst()
                    .orElseThrow()
                    .getId());
    action(
        () ->
            table()
                .getSelectionModel()
                .select(
                    context.applicants.stream()
                        .filter(a -> a.getId() == candidateId)
                        .findFirst()
                        .orElseThrow()));
    fire("editButton");
    field("fullNameField", "Тестовая Кандидатка Новая");
    dialogFire("saveButton");
    idle();
    assertThat(fx(() -> ((Applicant) table().getSelectionModel().getSelectedItem()).getFullName()))
        .endsWith("Новая");
    fire("viewButton");
    snapshot("05-applicant-details", true);
    action(() -> dialog().close());
    // Удаление можно отменить; удаление связанного кандидата показывает понятную ошибку.
    dialogs.confirm = false;
    fire("deleteButton");
    idle();
    assertThat(fx(() -> context.applicants.stream().anyMatch(a -> a.getId() == candidateId)))
        .isTrue();
    dialogs.confirm = true;
    action(
        () ->
            table()
                .getSelectionModel()
                .select(
                    context.applicants.stream()
                        .filter(a -> a.getEmail().equals("anna.demo@example.org"))
                        .findFirst()
                        .orElseThrow()));
    fire("deleteButton");
    idle();
    assertThat(dialogs.errors).anyMatch(message -> message.contains("заявки"));
    dialogs.errors.clear();
    fire("viewButton");
    assertThat(fx(() -> ((TableView<?>) node(dialogRoot(), "linkedTable")).getItems().size()))
        .isEqualTo(2);
    action(
        () ->
            dialogRoot().lookupAll(".button").stream()
                .filter(
                    n -> n instanceof Button b && b.getText().equals("Показать в разделе заявок"))
                .map(n -> (Button) n)
                .findFirst()
                .orElseThrow()
                .fire());
    assertThat(fx(() -> table().getItems().size())).isEqualTo(2);
    action(() -> context.navigation.applications(null));
    // Новая заявка: кандидат выбирается из объектов ComboBox, ошибочная дата/время не сохраняются.
    fire("applicationsNav");
    fire("addButton");
    snapshot("06-application-form", true);
    action(
        () -> {
          ComboBox<Applicant> box = node(dialogRoot(), "applicantField");
          assertThat(box.getItems()).noneMatch(a -> a.getStatus() == ApplicantStatus.BLOCKED);
          box.setValue(
              box.getItems().stream()
                  .filter(a -> a.getId() == candidateId)
                  .findFirst()
                  .orElseThrow());
        });
    field("examField", "Тестовый экзамен");
    field("hourField", "25");
    dialogFire("saveButton");
    assertThat(fx(() -> ((Label) node(dialogRoot(), "errorLabel")).getText())).contains("Часы");
    field("hourField", "10");
    action(
        () ->
            ((DatePicker) node(dialogRoot(), "dateField")).setValue(LocalDate.now().minusDays(1)));
    dialogFire("saveButton");
    assertThat(fx(() -> ((Label) node(dialogRoot(), "errorLabel")).getText())).contains("будущем");
    action(
        () -> ((DatePicker) node(dialogRoot(), "dateField")).setValue(LocalDate.now().plusDays(2)));
    dialogFire("saveButton");
    idle();
    long applicationId =
        fx(
            () ->
                context.applications.stream()
                    .filter(a -> a.getApplicantId() == candidateId)
                    .findFirst()
                    .orElseThrow()
                    .getId());
    selectApplication(a -> a.getId() == applicationId);
    fire("editButton");
    field("examField", "Тестовый экзамен обновлён");
    dialogFire("saveButton");
    idle();
    selectApplication(a -> a.getId() == applicationId);
    fire("approveButton");
    dialogFire("saveButton");
    idle();
    selectApplication(a -> a.getId() == applicationId);
    fire("completeButton");
    field("scoreField", "90");
    dialogFire("saveButton");
    assertThat(fx(() -> ((Label) node(dialogRoot(), "errorLabel")).getText()))
        .contains("до назначенного");
    dialogFire("closeButton");
    // Завершение прошедшего согласованного экзамена; 101 отвергается, 87 сохраняется.
    selectApplication(
        a ->
            a.getStatus() == ApplicationStatus.APPROVED
                && a.getScheduledAt().isBefore(LocalDateTime.now()));
    fire("completeButton");
    field("scoreField", "101");
    dialogFire("saveButton");
    assertThat(fx(() -> ((Label) node(dialogRoot(), "errorLabel")).getText())).contains("0 до 100");
    field("scoreField", "87");
    snapshot("07-result-form", true);
    dialogFire("saveButton");
    idle();
    assertThat(
            fx(
                () ->
                    context.applications.stream()
                        .anyMatch(a -> Integer.valueOf(87).equals(a.getScore()))))
        .isTrue();
    selectApplication(a -> a.getId() == applicationId);
    fire("cancelButton");
    dialogFire("saveButton");
    idle();
    assertThat(fx(() -> ((Button) node(root(), "editButton")).isDisabled())).isTrue();
    fire("viewButton");
    assertThat(fx(() -> ((Label) node(dialogRoot(), "detailsLabel")).getText()))
        .contains("Отменена");
    dialogFire("closeButton");
    // Поиск + статус + даты + сортировка работают одновременно; Excel содержит только видимые
    // строки.
    action(
        () -> {
          ((TextField) node(root(), "searchField")).setText("demo");
          ComboBox<ApplicationStatus> status = node(root(), "statusFilter");
          status.setValue(ApplicationStatus.COMPLETED);
          ((DatePicker) node(root(), "fromDate")).setValue(LocalDate.now().minusDays(10));
          ((DatePicker) node(root(), "toDate")).setValue(LocalDate.now());
          TableView<ExamApplication> table = table();
          @SuppressWarnings("unchecked")
          TableColumn<ExamApplication, String> column = nodeColumn(table, "nameColumn");
          column.setSortType(TableColumn.SortType.ASCENDING);
          table.getSortOrder().setAll(List.of(column));
        });
    assertThat(fx(() -> table().getItems().size())).isEqualTo(4);
    snapshot("08-filters", false);
    List<String> visibleNames =
        fx(
            () ->
                FxScenarioIT.<ExamApplication>table().getItems().stream()
                    .map(ExamApplication::getApplicantName)
                    .toList());
    dialogs.destination = QA.resolve("filtered.xlsx");
    fire("exportButton");
    idle();
    try (var in = Files.newInputStream(dialogs.destination);
        var book = new org.apache.poi.xssf.usermodel.XSSFWorkbook(in)) {
      var sheet = book.getSheetAt(0);
      assertThat(sheet.getLastRowNum()).isEqualTo(visibleNames.size());
      for (int i = 0; i < visibleNames.size(); i++)
        assertThat(sheet.getRow(i + 1).getCell(0).getStringCellValue())
            .isEqualTo(visibleNames.get(i));
    }
    dialogs.destination = null;
    fire("exportButton");
    idle();
    assertThat(dialogs.errors).isEmpty();
    dialogs.destination = QA.resolve("nonexistent-folder/export.xlsx");
    fire("exportButton");
    idle();
    assertThat(dialogs.errors).anyMatch(message -> message.contains("сохранить файл"));
    dialogs.errors.clear();
    action(() -> ((DatePicker) node(root(), "fromDate")).setValue(LocalDate.now().plusDays(1)));
    assertThat(fx(() -> ((Label) node(root(), "filterError")).isVisible())).isTrue();
    assertThat(fx(() -> ((Button) node(root(), "exportButton")).isDisabled())).isTrue();
    action(() -> context.navigation.applications(null));
    // Экзаменаторы: отдельный справочник с полным CRUD.
    fire("examinersNav");
    snapshot("09-examiners", false);
    action(
        () -> {
          ((TextField) node(root(), "subjectFilter")).setText("матем");
          ComboBox<ExaminerStatus> status = node(root(), "statusFilter");
          status.setValue(ExaminerStatus.ACTIVE);
        });
    assertThat(fx(() -> table().getItems().size())).isEqualTo(1);
    action(
        () -> {
          ((TextField) node(root(), "subjectFilter")).clear();
          ((ComboBox<?>) node(root(), "statusFilter")).setValue(null);
        });
    fire("addButton");
    field("fullNameField", "Тест Экзаменатор");
    field("emailField", "gui.examiner@example.org");
    field("extraField", "Тестовый предмет");
    dialogFire("saveButton");
    idle();
    action(
        () ->
            table()
                .getSelectionModel()
                .select(
                    context.examiners.stream()
                        .filter(a -> a.getEmail().equals("gui.examiner@example.org"))
                        .findFirst()
                        .orElseThrow()));
    fire("editButton");
    field("extraField", "Математика");
    dialogFire("saveButton");
    idle();
    fire("viewButton");
    snapshot("10-examiner-card", true);
    dialogFire("closeButton");
    fire("deleteButton");
    idle();
    assertThat(
            fx(
                () ->
                    context.examiners.stream()
                        .anyMatch(a -> a.getEmail().equals("gui.examiner@example.org"))))
        .isFalse();
    fire("statisticsNav");
    idle();
    snapshot("11-statistics", false);
    assertThat(fx(() -> ((Label) node(root(), "totalValue")).getText())).isEqualTo("11");
    action(() -> ((DatePicker) node(root(), "fromDate")).setValue(LocalDate.now().plusDays(20)));
    idle();
    assertThat(fx(() -> ((Label) node(root(), "totalValue")).getText())).isEqualTo("0");
    assertThat(fx(() -> ((Label) node(root(), "averageValue")).getText())).isEqualTo("—");
    action(() -> ((DatePicker) node(root(), "toDate")).setValue(LocalDate.now()));
    assertThat(fx(() -> ((Label) node(root(), "filterError")).isVisible())).isTrue();
    action(
        () -> {
          ((DatePicker) node(root(), "fromDate")).setValue(null);
          ((DatePicker) node(root(), "toDate")).setValue(null);
        });
    idle();
    // Удаление заявки и кандидата; повторная загрузка сохраняет изменения в PostgreSQL.
    fire("applicationsNav");
    selectApplication(a -> a.getId() == applicationId);
    fire("deleteButton");
    idle();
    fire("applicantsNav");
    action(
        () ->
            table()
                .getSelectionModel()
                .select(
                    context.applicants.stream()
                        .filter(a -> a.getId() == candidateId)
                        .findFirst()
                        .orElseThrow()));
    fire("deleteButton");
    idle();
    assertThat(dialogs.confirmations).isGreaterThanOrEqualTo(5);
    fire("refreshButton");
    idle();
    assertThat(fx(() -> context.applicants.size())).isEqualTo(6);
    // Ошибка соединения не роняет окно, повторное обновление восстанавливает работу.
    String correctUrl = System.getProperty("exam.api.url");
    try {
      System.setProperty("exam.api.url", "http://127.0.0.1:1");
      fire("refreshButton");
      idle();
      assertThat(dialogs.errors).anyMatch(message -> message.contains("Нет соединения"));
    } finally {
      System.setProperty("exam.api.url", correctUrl);
    }
    dialogs.errors.clear();
    fire("refreshButton");
    idle();
    assertThat(dialogs.errors).isEmpty();
    assertThat(fx(() -> context.applications.size())).isEqualTo(10);
  }

  @Test
  void realConfirmationDialogCanCancelDeletion() throws Exception {
    boolean answer =
        fx(
            () -> {
              var delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(400));
              delay.setOnFinished(
                  event -> {
                    DialogPane pane = (DialogPane) dialogRoot();
                    try {
                      ImageIO.write(
                          SwingFXUtils.fromFXImage(pane.snapshot(null, null), null),
                          "png",
                          QA.resolve("12-delete-confirmation.png").toFile());
                    } catch (java.io.IOException failure) {
                      throw new java.io.UncheckedIOException(failure);
                    }
                    ((Button) pane.lookupButton(ButtonType.CANCEL)).fire();
                  });
              delay.play();
              return new Dialogs()
                  .confirm(
                      main,
                      "Удалить запись?",
                      "Кандидат: Тестовый Кандидат\nЭто действие удалит запись из базы данных.");
            });
    assertThat(answer).isFalse();
  }

  static <T, V> TableColumn<T, V> nodeColumn(TableView<T> table, String id) {
    @SuppressWarnings("unchecked")
    var result =
        (TableColumn<T, V>)
            table.getColumns().stream().filter(c -> id.equals(c.getId())).findFirst().orElseThrow();
    return result;
  }

  static void selectApplication(Predicate<ExamApplication> predicate) throws Exception {
    action(
        () -> {
          TableView<ExamApplication> table = table();
          table
              .getSelectionModel()
              .select(table.getItems().stream().filter(predicate).findFirst().orElseThrow());
        });
  }
}
