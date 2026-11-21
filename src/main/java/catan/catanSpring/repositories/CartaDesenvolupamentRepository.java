package catan.catanSpring.repositories;

import catan.catanSpring.entities.CartaDesenvolupament;
import catan.catanSpring.entities.TipusCarta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartaDesenvolupamentRepository extends JpaRepository<CartaDesenvolupament, Integer> {
    long countByJugadorIdAndTipus(int idJugador, TipusCarta tipus);
    List<CartaDesenvolupament> findByPartidaIdAndJugadorIsNull(int idPartida);
}
