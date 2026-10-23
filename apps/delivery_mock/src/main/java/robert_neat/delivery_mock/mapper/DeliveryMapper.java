package robert_neat.delivery_mock.mapper;

import robert_neat.delivery_mock.dto.PrzesylkaDostawy;
import robert_neat.delivery_mock.dto.WpisStatusuDostawy;
import robert_neat.delivery_mock.entity.PrzesylkaDostawyEntity;
import robert_neat.delivery_mock.entity.WpisStatusuDostawyEntity;

public final class DeliveryMapper {

    private DeliveryMapper() {
    }

    public static PrzesylkaDostawy toDto(PrzesylkaDostawyEntity entity) {
        return new PrzesylkaDostawy(
                entity.getId(),
                entity.getNumerReferencyjny(),
                entity.getKanal(),
                entity.getAdresAde(),
                entity.getStatus(),
                entity.getDataNadania(),
                entity.getDataStatusu()
        );
    }

    public static WpisStatusuDostawy toDto(WpisStatusuDostawyEntity entity) {
        return new WpisStatusuDostawy(
                entity.getId(),
                entity.getPrzesylkaDostawyId(),
                entity.getStatus(),
                entity.getData(),
                entity.getSzczegoly()
        );
    }
}
