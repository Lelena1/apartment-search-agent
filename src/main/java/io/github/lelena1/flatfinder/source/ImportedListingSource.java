package io.github.lelena1.flatfinder.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Реальные объявления, которые пользователь сохранил сам (например, выгрузил свою подборку с площадки)
 * в JSON того же формата, что и демо-база. Путь задаётся свойством {@code app.listings.import-file}.
 * Такой файл личный и в репозиторий не попадает.
 */
@Component
@ConditionalOnExpression("!'${app.listings.import-file:}'.isBlank()")
public class ImportedListingSource extends JsonListingSource {

    private static final Logger log = LoggerFactory.getLogger(ImportedListingSource.class);

    private final Path file;

    public ImportedListingSource(@Value("${app.listings.import-file}") String file) {
        this.file = Path.of(file);
        try {
            load(Files.newInputStream(this.file), file);
        } catch (IOException e) {
            throw new UncheckedIOException("Файл импорта не найден или не читается: " + file, e);
        }
        log.info("Загружено {} объявлений из {}", size(), file);
    }

    @Override
    public String name() {
        return "Импорт: " + file.getFileName();
    }
}
