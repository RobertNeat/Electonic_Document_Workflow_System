package robert_neat.sign_service.mapper;

import robert_neat.sign_service.dto.CertyfikatDto;
import robert_neat.sign_service.entity.CertyfikatEntity;

public final class PkiMapper {

    private PkiMapper() {
    }

    public static CertyfikatDto toDto(CertyfikatEntity entity) {
        return new CertyfikatDto(
                entity.getId(),
                entity.getPodmiot(),
                entity.getNumerSeryjny(),
                entity.getDataWydania(),
                entity.getDataWygasniecia(),
                entity.getStatus(),
                entity.getWydawca()
        );
    }
}
