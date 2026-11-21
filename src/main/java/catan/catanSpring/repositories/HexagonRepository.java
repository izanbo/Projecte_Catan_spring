package catan.catanSpring.repositories;

import catan.catanSpring.entities.Hexagon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HexagonRepository extends JpaRepository<Hexagon, Integer> {
    @Query("SELECT DISTINCT h FROM Hexagon h LEFT JOIN FETCH h.vertexs hv LEFT JOIN FETCH hv.vertex WHERE h.partida.id = :idPartida ORDER BY h.codi ASC")
    List<Hexagon> findByPartidaIdWithVertexs(@Param("idPartida") int idPartida);

    @Query("SELECT h FROM Hexagon h WHERE h.partida.id = :idPartida AND h.numero = :numero AND h.id <> :idLladre")
    List<Hexagon> findByPartidaAndNumeroSenseLladre(@Param("idPartida") int idPartida, @Param("numero") int numero, @Param("idLladre") int idLladre);
}
