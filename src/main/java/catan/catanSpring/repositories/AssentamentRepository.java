package catan.catanSpring.repositories;

import catan.catanSpring.entities.Assentament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssentamentRepository extends JpaRepository<Assentament, Integer> {
    Optional<Assentament> findByVertexId(int idVertex);

    @Query("SELECT COUNT(a) FROM Assentament a WHERE a.propietari.id = :idJugador AND (a.vertex.id = :vA OR a.vertex.id = :vB)")
    long countByPropietariIdAndVertexAOrVertexB(@Param("idJugador") int idJugador, @Param("vA") int vA, @Param("vB") int vB);

    @Query("SELECT COUNT(a) FROM Assentament a WHERE a.vertex.id IN (" +
           "SELECT e.vertexB.id FROM Aresta e WHERE e.vertexA.id = :idVertex" +
           ") OR a.vertex.id IN (" +
           "SELECT e.vertexA.id FROM Aresta e WHERE e.vertexB.id = :idVertex)")
    long countAssentamentsVeins(@Param("idVertex") int idVertex);
}
