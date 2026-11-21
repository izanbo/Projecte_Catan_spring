package catan.catanSpring.repositories;

import catan.catanSpring.entities.HexagonVertex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HexagonVertexRepository extends JpaRepository<HexagonVertex, Integer> {
    @Query("SELECT hv FROM HexagonVertex hv JOIN FETCH hv.hexagon WHERE hv.vertex.id = :idVertex")
    List<HexagonVertex> findByVertexId(@Param("idVertex") int idVertex);
}
