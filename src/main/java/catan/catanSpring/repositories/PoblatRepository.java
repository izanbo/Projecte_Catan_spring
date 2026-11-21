package catan.catanSpring.repositories;

import catan.catanSpring.entities.Poblat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PoblatRepository extends JpaRepository<Poblat, Integer> {
    long countByPropietariId(int idJugador);
    Optional<Poblat> findByVertexIdAndPropietariId(int idVertex, int idJugador);
    long countByVertexId(int idVertex);
}
