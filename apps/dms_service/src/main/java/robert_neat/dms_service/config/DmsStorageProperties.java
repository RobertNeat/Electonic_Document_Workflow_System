package robert_neat.dms_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dms.storage")
public class DmsStorageProperties {

    /** Katalog bazowy wolumenu dms-data, np. /data/dms. */
    private String basePath;

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
}
