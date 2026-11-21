package catan.catanSpring.dto;

import catan.catanSpring.entities.RecursJugador;
import catan.catanSpring.entities.TipusRecurs;

public record RecursResposta(
        TipusRecurs tipusRecurs,
        int quantitat) {

    public static RecursResposta de(RecursJugador r) {
        return new RecursResposta(r.getTipusRecurs(), r.getQuantitat());
    }
}