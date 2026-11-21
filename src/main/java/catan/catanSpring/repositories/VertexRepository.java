package catan.catanSpring.repositories;

import catan.catanSpring.entities.Vertex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VertexRepository extends JpaRepository<Vertex, Integer> {
}
