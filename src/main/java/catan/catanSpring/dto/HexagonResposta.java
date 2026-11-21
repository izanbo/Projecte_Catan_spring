package catan.catanSpring.dto;

import catan.catanSpring.entities.Hexagon;
import catan.catanSpring.entities.TipusTerreny;

public record HexagonResposta(
        int id,
        int codi,
        TipusTerreny tipusTerreny,
        Integer numero) {

    public static HexagonResposta de(Hexagon h) {
        return new HexagonResposta(h.getId(), h.getCodi(), h.getTipusTerreny(), h.getNumero());
    }
}