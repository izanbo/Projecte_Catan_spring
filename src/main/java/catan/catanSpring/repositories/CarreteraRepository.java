package catan.catanSpring.repositories;

import catan.catanSpring.entities.Carretera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CarreteraRepository extends JpaRepository<Carretera, Integer> {
    long countByArestaId(int idAresta);
    long countByPropietariId(int idJugador);

    @Query("SELECT COUNT(c) FROM Carretera c WHERE c.propietari.id = :idJugador AND (c.aresta.vertexA.id = :vId OR c.aresta.vertexB.id = :vId)")
    long countByPropietariIdAndVertexId(@Param("idJugador") int idJugador, @Param("vId") int vId);

    @Query("SELECT COUNT(c) FROM Carretera c WHERE c.propietari.id = :idJugador AND c.aresta.id <> :idAresta AND (c.aresta.vertexA.id = :vA OR c.aresta.vertexB.id = :vA OR c.aresta.vertexA.id = :vB OR c.aresta.vertexB.id = :vB)")
    long countByPropietariIdConnectedTo(@Param("idJugador") int idJugador, @Param("idAresta") int idAresta, @Param("vA") int vA, @Param("vB") int vB);
}
