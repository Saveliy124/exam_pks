package ru.mirea.examcenter.export;
import java.util.*;
/** Immutable value snapshot. A background export never reads live JavaFX objects. */
public record ExportTable(String name, List<String> headers, List<List<Object>> rows) {
    public ExportTable {
        headers = List.copyOf(headers);
        rows = rows.stream().map(row -> Collections.unmodifiableList(new ArrayList<>(row))).toList();
        int width = headers.size();
        if (rows.stream().anyMatch(row -> row.size() != width))
            throw new IllegalArgumentException("Количество полей строки не соответствует заголовкам.");
    }
}
