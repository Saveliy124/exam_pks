package ru.mirea.examcenter.fx.support;

import java.nio.file.Path;
import java.time.LocalDate;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class Dialogs {
  public boolean confirm(Window owner, String title, String message) {
    Alert alert =
        new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
    alert.initOwner(owner);
    alert.setTitle(title);
    alert.setHeaderText(title);
    style(alert);
    ((Button) alert.getDialogPane().lookupButton(ButtonType.OK)).setText("Подтвердить");
    ((Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Отмена");
    return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
  }

  public void error(Window owner, Throwable failure) {
    Alert alert = new Alert(Alert.AlertType.ERROR, Errors.message(failure), ButtonType.OK);
    alert.initOwner(owner);
    alert.setTitle("Не удалось выполнить действие");
    alert.setHeaderText("Проверьте данные и повторите попытку");
    style(alert);
    alert.show();
  }

  private void style(Alert alert) {
    alert
        .getDialogPane()
        .getStylesheets()
        .add(Dialogs.class.getResource("/ru/mirea/examcenter/fx/app.css").toExternalForm());
  }

  public Path exportFile(Window owner, String title) {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Экспорт — " + title);
    chooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("Книга Excel (*.xlsx)", "*.xlsx"));
    chooser.setInitialFileName(title + "_" + LocalDate.now() + ".xlsx");
    var selected = chooser.showSaveDialog(owner);
    if (selected == null) return null;
    Path path = selected.toPath();
    if (!path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx"))
      path = path.resolveSibling(path.getFileName() + ".xlsx");
    if (java.nio.file.Files.exists(path)
        && !confirm(owner, "Заменить файл?", "Файл «" + path.getFileName() + "» уже существует."))
      return null;
    return path;
  }
}
