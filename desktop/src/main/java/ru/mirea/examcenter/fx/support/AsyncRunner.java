package ru.mirea.examcenter.fx.support;

import java.util.concurrent.*;
import java.util.function.Consumer;
import javafx.beans.property.*;
import javafx.concurrent.Task;

public final class AsyncRunner implements AutoCloseable {
  private final ExecutorService executor =
      Executors.newFixedThreadPool(
          3,
          r -> {
            Thread t = new Thread(r, "exam-background");
            t.setDaemon(true);
            return t;
          });
  private final IntegerProperty active = new SimpleIntegerProperty();

  public ReadOnlyIntegerProperty activeProperty() {
    return active;
  }

  public <T> void submit(Callable<T> action, Consumer<T> success, Consumer<Throwable> failure) {
    Task<T> task =
        new Task<>() {
          @Override
          protected T call() throws Exception {
            return action.call();
          }
        };
    active.set(active.get() + 1);
    task.setOnSucceeded(
        e -> {
          active.set(active.get() - 1);
          success.accept(task.getValue());
        });
    task.setOnFailed(
        e -> {
          active.set(active.get() - 1);
          failure.accept(task.getException());
        });
    task.setOnCancelled(e -> active.set(active.get() - 1));
    executor.execute(task);
  }

  @Override
  public void close() {
    executor.shutdownNow();
  }
}
