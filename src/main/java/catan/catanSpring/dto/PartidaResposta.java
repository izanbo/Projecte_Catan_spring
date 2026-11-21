package catan.catanSpring.dto;

import java.sql.Date;
import java.util.List;

import catan.catanSpring.entities.EstatPartida;
import catan.catanSpring.entities.Partida;

public record PartidaResposta(
        int id,
        EstatPartida estat,
        Date dataInici,
        int ronda,
        int ultimaTirada,
        int puntsPerGuanyar,
        boolean dausTirats,
        boolean lladrePendent,
        Integer idJugadorActiu,
        Integer idHexagonLladre,
        List<JugadorResumResposta> jugadors,
        List<HexagonResposta> hexagons) {

    public static PartidaResposta de(Partida p) {
        Integer idActiu = p.getJugadorActiu() != null ? p.getJugadorActiu().getId() : null;
        Integer idLladre = p.getHexagonLladre() != null ? p.getHexagonLladre().getId() : null;

        List<JugadorResumResposta> jugadorsDto = p.getJugadors().stream()
                .map(JugadorResumResposta::de)
                .toList();

        List<HexagonResposta> hexagonsDto = p.getHexagons().stream()
                .map(HexagonResposta::de)
                .toList();

        return new PartidaResposta(
                p.getId(),
                p.getEstat(),
                p.getDataInici(),
                p.getRonda(),
                p.getUltimaTirada(),
                p.getPuntsPerGuanyar(),
                p.isDausTirats(),
                p.isLladre(),
                idActiu,
                idLladre,
                jugadorsDto,
                hexagonsDto);
    }
}
