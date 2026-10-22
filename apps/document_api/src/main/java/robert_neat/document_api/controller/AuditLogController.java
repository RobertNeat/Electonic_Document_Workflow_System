package robert_neat.document_api.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import robert_neat.document_api.dto.administracja.WpisDziennikaZdarzen;
import robert_neat.document_api.service.AuditLogService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/audit-log")
    public List<WpisDziennikaZdarzen> lista(
            @RequestParam(required = false) String typOperacji,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate od,
            @RequestParam(name = "do", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate doData) {
        return auditLogService.lista(typOperacji, od, doData);
    }
}
