package catan.catanSpring.dto;

import java.util.List;

import catan.catanSpring.entities.TipusTerreny;

public record TaulerHexagonResposta(
        int id,
        int codi,
        TipusTerreny tipusTerreny,
        Integer numero,
        boolean teLladre,
        List<ConstruccioAdjacenciaResposta> construccions) {
}