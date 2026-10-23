package robert_neat.delivery_mock.dto;

import java.util.List;

/** Szczegóły przesyłki dostawy wraz z chronologiczną historią statusów - odpowiedź
 * `GET /api/przesylki-dostawy/{id}`. */
public record PrzesylkaDostawyZHistoria(
        PrzesylkaDostawy przesylka,
        List<WpisStatusuDostawy> historiaStatusow
) {
}
