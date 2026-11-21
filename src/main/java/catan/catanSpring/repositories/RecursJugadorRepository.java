package catan.catanSpring.repositories;

import catan.catanSpring.entities.RecursJugador;
import catan.catanSpring.entities.TipusRecurs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecursJugadorRepository extends JpaRepository<RecursJugador, Integer> {
    List<RecursJugador> findByJugadorId(int idJugador);
    Optional<RecursJugador> findByJugadorIdAndTipusRecurs(int idJugador, TipusRecurs tipusRecurs);
}
