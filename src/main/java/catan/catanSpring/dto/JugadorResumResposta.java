package catan.catanSpring.dto;

import catan.catanSpring.entities.ColorJugador;
import catan.catanSpring.entities.Jugador;

public record JugadorResumResposta(
        int id,
        String nom,
        ColorJugador color,
        int ordreTorn) {

    public static JugadorResumResposta de(Jugador j) {
        return new JugadorResumResposta(j.getId(), j.getNom(), j.getColor(), j.getOrdreTorn());
    }
}