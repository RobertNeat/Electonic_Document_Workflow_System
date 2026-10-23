package robert_neat.dms_service.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Przygotowuje katalogi przechowywania (szablony/dokumenty) pod dms.storage.base-path przy starcie
 * aplikacji i sprawdza zapisywalność katalogu bazowego - jeśli wolumen nie jest zapisywalny dla
 * bieżącego użytkownika procesu, aplikacja nie startuje (fail-fast), zamiast ukrytego EACCES przy
 * pierwszym uploadzie. Fail-safe ogólnego przeznaczenia - niezależny od konkretnej przyczyny
 * niezapisywalności (np. ręczna zmiana uprawnień, zamontowanie wolumenu jako read-only). */
@Configuration
@EnableConfigurationProperties(DmsStorageProperties.class)
public class StorageInitializer implements ApplicationRunner {

    private final DmsStorageProperties storageProperties;

    public StorageInitializer(DmsStorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        Path base = Path.of(storageProperties.getBasePath());
        Path szablony = base.resolve("szablony");
        Path dokumenty = base.resolve("dokumenty");
        try {
            Files.createDirectories(szablony);
            Files.createDirectories(dokumenty);
            if (!Files.isWritable(base)) {
                throw new IOException("Katalog bazowy dms.storage.base-path (" + base + ") istnieje, ale nie jest zapisywalny dla bieżącego użytkownika procesu.");
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Nie udało się przygotować katalogu przechowywania DMS pod " + base
                            + ". Jeśli wolumen dms-data został już zamontowany i jego punkt montowania"
                            + " należy do root (np. przy pierwszym tworzeniu przez Docker), konieczna jest"
                            + " korekta własności na poziomie docker-compose (init/chown), niezależnie od"
                            + " tej inicjalizacji aplikacyjnej.", e);
        }
    }
}
