package ru.mirea.examcenter.service;

import java.sql.SQLException;
import ru.mirea.examcenter.model.CenterSnapshot;
import ru.mirea.examcenter.repository.CenterRepository;

/** Обновляет связанные списки из одного согласованного снимка БД. */
public final class CenterService {
  private final CenterRepository repository;

  public CenterService(CenterRepository repository) {
    this.repository = repository;
  }

  public CenterSnapshot load() throws SQLException {
    return repository.load();
  }
}
