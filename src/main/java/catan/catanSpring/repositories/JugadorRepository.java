package catan.catanSpring.repositories;

import catan.catanSpring.entities.Jugador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JugadorRepository extends JpaRepository<Jugador, Integer> {
    @Query("SELECT j FROM Jugador j WHERE j.partida.id = :idPartida ORDER BY j.ordreTorn ASC")
    List<Jugador> findByPartidaIdOrderByOrdreTornAsc(@Param("idPartida") int idPartida);
}
