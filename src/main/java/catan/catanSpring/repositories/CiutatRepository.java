package catan.catanSpring.repositories;

import catan.catanSpring.entities.Ciutat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CiutatRepository extends JpaRepository<Ciutat, Integer> {
    long countByPropietariId(int idJugador);
}
