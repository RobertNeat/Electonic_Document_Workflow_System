package robert_neat.dms_service.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Przygotowuje katalogi przechowywania (szablony/dokumenty) pod dms.storage.base-path przy starcie
 * aplikacji i sprawdza zapisywalność katalogu bazowego - jeśli wolumen nie jest zapisywalny dla
 * bieżącego użytkownika procesu, aplikacja nie startuje (fail-fast), zamiast ukrytego EACCES przy
 * pierwszym uploadzie. Fail-safe ogólnego przeznaczenia - niezależny od konkretnej przyczyny
 * niezapisywalności (np. ręczna zmiana uprawnień, zamontowanie wolumenu jako read-only).
 * Dogrywa też pliki .jrxml demo (seed w 100-seed.xml wstawia tylko metadane w bazie, bez treści
 * pliku) - kopiuje z classpath na wolumen, tylko jeśli plik docelowy jeszcze nie istnieje, żeby nie
 * nadpisać szablonu wgranego/zmienionego realnym uploadem pod tą samą ścieżką. Analogicznie dogrywa
 * placeholder-PDF-y dla wersji dokumentów wd-0/wd-1/wd-2 (dokumenty-demo) - UWAGA: to są proste
 * placeholdery bez realnego podpisu PAdES. "statusPodpisu" pokazywany na liście/w nagłówku wersji
 * dla wd-2 pochodzi z metadanych bazy (core.wersja_dokumentu.status_podpisu="poprawny"). Jeśli
 * ktoś mimo to wywoła akcję "Szczegóły podpisu" na wd-2, sign_service realnie zweryfikuje bajty
 * pliku, nie znajdzie żadnego podpisu i odpowie wartościami "nieznany"/null (bez błędu/500) -
 * to jest znana, zaakceptowana rozbieżność danych demo, nie błąd tej klasy. */
@Configuration
@EnableConfigurationProperties(DmsStorageProperties.class)
public class StorageInitializer implements ApplicationRunner {

    private static final String[] SZABLONY_DEMO = {
            "decyzja_srodowiskowa_v3.jrxml",
            "wezwanie_do_zaplaty_v1.jrxml"
    };

    /** Ścieżki względne do base-path, odpowiadające sciezka_pliku z seedów core/dms. */
    private static final String[] DOKUMENTY_DEMO = {
            "dokumenty/d-1/wd-0.pdf",
            "dokumenty/d-2/wd-1.pdf",
            "dokumenty/d-2/wd-2.pdf"
    };

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
            seedujSzablonyDemo(szablony);
            seedujDokumentyDemo(base);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Nie udało się przygotować katalogu przechowywania DMS pod " + base
                            + ". Jeśli wolumen dms-data został już zamontowany i jego punkt montowania"
                            + " należy do root (np. przy pierwszym tworzeniu przez Docker), konieczna jest"
                            + " korekta własności na poziomie docker-compose (init/chown), niezależnie od"
                            + " tej inicjalizacji aplikacyjnej.", e);
        }
    }

    private void seedujSzablonyDemo(Path szablony) throws IOException {
        for (String nazwaPliku : SZABLONY_DEMO) {
            Path cel = szablony.resolve(nazwaPliku);
            if (Files.exists(cel)) {
                continue;
            }
            ClassPathResource zasob = new ClassPathResource("szablony-demo/" + nazwaPliku);
            try (var in = zasob.getInputStream()) {
                Files.copy(in, cel);
            }
        }
    }

    /** Dogrywa placeholder-PDF-y dla wersji dokumentów demo (wd-0/wd-1/wd-2) - analogicznie do
     * seedujSzablonyDemo: copy-if-absent, nigdy nie nadpisuje pliku już obecnego na wolumenie
     * (np. realnie podpisanego w trakcie manualnego testu). */
    private void seedujDokumentyDemo(Path base) throws IOException {
        for (String sciezkaWzgledna : DOKUMENTY_DEMO) {
            Path cel = base.resolve(sciezkaWzgledna);
            if (Files.exists(cel)) {
                continue;
            }
            Files.createDirectories(cel.getParent());
            String nazwaPliku = cel.getFileName().toString();
            ClassPathResource zasob = new ClassPathResource("dokumenty-demo/" + nazwaPliku);
            try (var in = zasob.getInputStream()) {
                Files.copy(in, cel);
            }
        }
    }
}
