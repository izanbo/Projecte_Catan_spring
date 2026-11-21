package catan.catanSpring.dto;

import java.util.List;

import catan.catanSpring.entities.ColorJugador;
import catan.catanSpring.entities.Jugador;
import catan.catanSpring.entities.RecursJugador;

public record JugadorRecursosResposta(
        int id,
        String nom,
        ColorJugador color,
        int ordreTorn,
        int idPartida,
        List<RecursResposta> recursos) {

    public static JugadorRecursosResposta de(Jugador j, List<RecursJugador> inventari) {
        List<RecursResposta> recursosDto = inventari.stream()
                .map(RecursResposta::de)
                .toList();

        return new JugadorRecursosResposta(
                j.getId(),
                j.getNom(),
                j.getColor(),
                j.getOrdreTorn(),
                j.getPartida().getId(),
                recursosDto);
    }
}