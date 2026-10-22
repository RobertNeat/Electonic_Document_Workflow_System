package robert_neat.document_api.mapper;

import robert_neat.document_api.dto.sprawy.DefinicjaObiegu;
import robert_neat.document_api.dto.sprawy.KategoriaArchiwalna;
import robert_neat.document_api.dto.sprawy.PrzejscieObiegu;
import robert_neat.document_api.dto.sprawy.Sprawa;
import robert_neat.document_api.dto.sprawy.WpisMetryki;
import robert_neat.document_api.entity.DefinicjaObieguEntity;
import robert_neat.document_api.entity.PrzejscieObieguEntity;
import robert_neat.document_api.entity.SprawaEntity;
import robert_neat.document_api.entity.WpisMetrykiEntity;

import java.util.List;

public final class SprawyMapper {

    private SprawyMapper() {
    }

    public static Sprawa toDto(SprawaEntity entity) {
        return new Sprawa(
                entity.getId(),
                entity.getZnakSprawy(),
                entity.getTytul(),
                entity.getJrwa(),
                entity.getDefinicjaObiegu().getId(),
                entity.getStan(),
                entity.getObywatel().getId(),
                entity.getWydzial(),
                entity.getReferent(),
                entity.getDataWszczecia(),
                entity.getTerminUstawowy(),
                KategoriaArchiwalna.valueOf(entity.getKategoriaArchiwalna()),
                entity.isZamknieta()
        );
    }

    public static WpisMetryki toDto(WpisMetrykiEntity entity) {
        return new WpisMetryki(
                entity.getId(),
                entity.getSprawa().getId(),
                entity.getData(),
                entity.getCzynnosc(),
                entity.getOsoba(),
                entity.getOpis()
        );
    }

    public static PrzejscieObiegu toDto(PrzejscieObieguEntity entity) {
        return new PrzejscieObiegu(
                entity.getZstanu(),
                List.copyOf(entity.getDoStanow()),
                entity.getWymaganaRola(),
                List.copyOf(entity.getWymaganeTypyDokumentow())
        );
    }

    public static DefinicjaObiegu toDto(DefinicjaObieguEntity entity, List<PrzejscieObieguEntity> przejscia) {
        return new DefinicjaObiegu(
                entity.getId(),
                entity.getNazwa(),
                entity.getOpis(),
                List.copyOf(entity.getStany()),
                entity.getStanPoczatkowy(),
                przejscia.stream().map(SprawyMapper::toDto).toList()
        );
    }
}
