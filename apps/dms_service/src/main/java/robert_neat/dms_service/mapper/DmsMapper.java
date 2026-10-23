package robert_neat.dms_service.mapper;

import robert_neat.dms_service.dto.SzablonJasper;
import robert_neat.dms_service.dto.WersjaDokumentuDms;
import robert_neat.dms_service.entity.SzablonJasperEntity;
import robert_neat.dms_service.entity.WersjaDokumentuDmsEntity;

public final class DmsMapper {

    private DmsMapper() {
    }

    public static SzablonJasper toDto(SzablonJasperEntity entity) {
        return new SzablonJasper(
                entity.getId(),
                entity.getTypDokumentuId(),
                entity.getNazwa(),
                entity.getWersja(),
                entity.getDataWgrania(),
                entity.getAutor()
        );
    }

    public static WersjaDokumentuDms toDto(WersjaDokumentuDmsEntity entity) {
        return new WersjaDokumentuDms(
                entity.getId(),
                entity.getDokumentId(),
                entity.getWersja(),
                entity.isPodpisana(),
                entity.getDataUtworzenia(),
                entity.getAutor(),
                entity.getStatusPodpisu()
        );
    }
}
