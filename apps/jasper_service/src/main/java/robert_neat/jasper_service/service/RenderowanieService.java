package robert_neat.jasper_service.service;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import org.springframework.stereotype.Service;
import robert_neat.jasper_service.client.DmsServiceClient;
import robert_neat.jasper_service.dto.SzablonMetadane;
import robert_neat.jasper_service.exception.BadRequestException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Renderuje PDF z szablonu .jrxml pobranego z dms_service i danych JSON żądania. */
@Service
public class RenderowanieService {

    private final DmsServiceClient dmsServiceClient;

    public RenderowanieService(DmsServiceClient dmsServiceClient) {
        this.dmsServiceClient = dmsServiceClient;
    }

    public WynikRenderowania renderujPdf(String szablonId, Map<String, Object> dane) {
        SzablonMetadane metadane = dmsServiceClient.pobierzMetadaneSzablonu(szablonId);
        byte[] jrxml = dmsServiceClient.pobierzPlikSzablonu(szablonId);

        try (InputStream jrxmlStream = new ByteArrayInputStream(jrxml)) {
            JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);
            JRDataSource dataSource = new JREmptyDataSource();
            // fillReport może zapisywać do mapy parametrów wpisy wbudowane (np. REPORT_PARAMETERS_MAP) -
            // kopia mutowalna, bo Map.of()/niemodyfikowalna mapa z żądania wywołałaby UnsupportedOperationException.
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, new HashMap<>(dane), dataSource);
            byte[] pdf = JasperExportManager.exportReportToPdf(jasperPrint);
            return new WynikRenderowania(pdf, metadane.nazwa());
        } catch (JRException ex) {
            throw new BadRequestException("Nie udało się wyrenderować szablonu " + szablonId + ": " + ex.getMessage());
        } catch (RuntimeException | java.io.IOException ex) {
            // JasperReports 7.x (loader Jackson) zgłasza dla uszkodzonego/niepoprawnego .jrxml niecheckowane
            // wyjątki (np. JacksonRuntimeException), nie tylko JRException - traktujemy to tak samo jako 400.
            throw new BadRequestException("Nie udało się wyrenderować szablonu " + szablonId + ": " + ex.getMessage());
        }
    }

    public record WynikRenderowania(byte[] pdf, String nazwaSzablonu) {
    }
}
