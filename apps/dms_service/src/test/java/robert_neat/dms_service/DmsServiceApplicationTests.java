package robert_neat.dms_service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DmsServiceApplicationTests {

    // base-path nadpisany, żeby StorageInitializer (ApplicationRunner) nie próbował tworzyć /data/dms
    // przy starcie kontekstu w testach.
    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void overrideStorage(DynamicPropertyRegistry registry) {
        registry.add("dms.storage.base-path", () -> tempDir.toString());
    }

    @Test
    void contextLoads() {
    }

}
