package catan.catanSpring.services;

import catan.catanSpring.entities.*;
import catan.catanSpring.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Servei encarregat de llegir els CSV de topologia (src/main/resources/topologia/)
 * i persistir vèrtexs, arestes i hexàgons associats a una partida concreta.
 * Assigna els terrenys i números de forma aleatòria, i situa el lladre al desert.
 */
@Service
@Transactional
public class TaulerLoaderService {

    @Autowired
    private VertexRepository vertexRepository;
    @Autowired
    private ArestaRepository arestaRepository;
    @Autowired
    private HexagonRepository hexagonRepository;
    @Autowired
    private HexagonVertexRepository hexagonVertexRepository;
    @Autowired
    private PartidaRepository partidaRepository;

    // Distribució estàndard del Catan (19 hexàgons)
    private static final TipusTerreny[] TERRENYS = {
        TipusTerreny.BOSC,     TipusTerreny.BOSC,     TipusTerreny.BOSC,     TipusTerreny.BOSC,
        TipusTerreny.TURONS,   TipusTerreny.TURONS,   TipusTerreny.TURONS,
        TipusTerreny.PASTURES, TipusTerreny.PASTURES, TipusTerreny.PASTURES,  TipusTerreny.PASTURES,
        TipusTerreny.CAMPS,    TipusTerreny.CAMPS,    TipusTerreny.CAMPS,     TipusTerreny.CAMPS,
        TipusTerreny.MUNTANYES,TipusTerreny.MUNTANYES,TipusTerreny.MUNTANYES,
        TipusTerreny.DESERT
    };

    // Números de producció estàndard (18 valors, el desert no en té)
    private static final int[] NUMEROS = {
        2, 3, 3, 4, 4, 5, 5, 6, 6, 8, 8, 9, 9, 10, 10, 11, 11, 12
    };

    /**
     * Carrega la topologia del tauler des dels CSV i la associa a la partida donada.
     * L'ordre és: vèrtexs → arestes → hexàgons → relacions hexàgon-vèrtex → lladre al desert.
     */
    public void carregarDades(Partida partida) {
        try {
            // 1. Llegir i persistir vèrtexs (codi -> Vertex)
            Map<Integer, Vertex> vertexMap = carregarVertexs(partida);

            // 2. Llegir i persistir arestes
            carregarArestes(partida, vertexMap);

            // 3. Preparar distribució aleatòria de terrenys i números
            List<TipusTerreny> terrenysBarrejats = new ArrayList<>(Arrays.asList(TERRENYS));
            Collections.shuffle(terrenysBarrejats);

            List<Integer> numerosBarrejats = new ArrayList<>();
            for (int n : NUMEROS) numerosBarrejats.add(n);
            Collections.shuffle(numerosBarrejats);

            // 4. Llegir hexàgons del CSV, assignar terreny/número i persistir
            Map<Integer, Hexagon> hexagonMap = carregarHexagons(partida, terrenysBarrejats, numerosBarrejats);

            // 5. Llegir relacions hexàgon-vèrtex i persistir
            carregarHexagonVertex(hexagonMap, vertexMap);

            // 6. Situar el lladre al desert
            Hexagon desert = hexagonMap.values().stream()
                    .filter(h -> h.getTipusTerreny() == TipusTerreny.DESERT)
                    .findFirst()
                    .orElse(null);
            if (desert != null) {
                partida.setHexagonLladre(desert);
                partidaRepository.save(partida);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error carregant dades del tauler: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Mètodes privats de càrrega
    // -------------------------------------------------------------------------

    private Map<Integer, Vertex> carregarVertexs(Partida partida) throws Exception {
        Map<Integer, Vertex> vertexMap = new LinkedHashMap<>();

        try (BufferedReader reader = obrirCsv("topologia/vertexs.csv")) {
            reader.readLine(); // saltar capçalera: id_vertex,x,y
            String linia;
            while ((linia = reader.readLine()) != null) {
                linia = linia.trim();
                if (linia.isEmpty()) continue;

                String[] parts = linia.split(",");
                int    codi = Integer.parseInt(parts[0].trim());
                double x    = Double.parseDouble(parts[1].trim());
                double y    = Double.parseDouble(parts[2].trim());

                Vertex vertex = new Vertex(0, codi, x, y);
                vertex.setPartida(partida);
                vertexRepository.save(vertex);
                partida.getVertexs().add(vertex);
                vertexMap.put(codi, vertex);
            }
        }
        return vertexMap;
    }

    private void carregarArestes(Partida partida, Map<Integer, Vertex> vertexMap) throws Exception {
        try (BufferedReader reader = obrirCsv("topologia/arestes.csv")) {
            reader.readLine(); // saltar capçalera: id_aresta,id_vertex_a,id_vertex_b
            String linia;
            while ((linia = reader.readLine()) != null) {
                linia = linia.trim();
                if (linia.isEmpty()) continue;

                String[] parts   = linia.split(",");
                int codi         = Integer.parseInt(parts[0].trim());
                int codiVertexA  = Integer.parseInt(parts[1].trim());
                int codiVertexB  = Integer.parseInt(parts[2].trim());

                Vertex vertexA = vertexMap.get(codiVertexA);
                Vertex vertexB = vertexMap.get(codiVertexB);

                if (vertexA == null || vertexB == null) {
                    throw new RuntimeException(
                        "Vèrtex no trobat per aresta codi=" + codi +
                        " (vertexA=" + codiVertexA + ", vertexB=" + codiVertexB + ")"
                    );
                }

                Aresta aresta = new Aresta(0, codi, vertexA, vertexB);
                aresta.setPartida(partida);
                arestaRepository.save(aresta);
                partida.getArestes().add(aresta);
            }
        }
    }

    private Map<Integer, Hexagon> carregarHexagons(
            Partida partida,
            List<TipusTerreny> terrenysBarrejats,
            List<Integer> numerosBarrejats) throws Exception {

        Map<Integer, Hexagon> hexagonMap = new LinkedHashMap<>();
        int indexTerreny = 0;
        int indexNumero  = 0;

        try (BufferedReader reader = obrirCsv("topologia/hexagons.csv")) {
            reader.readLine(); // saltar capçalera: id_hexagon,q,r
            String linia;
            while ((linia = reader.readLine()) != null) {
                linia = linia.trim();
                if (linia.isEmpty()) continue;

                String[] parts = linia.split(",");
                int codi = Integer.parseInt(parts[0].trim());
                int q    = Integer.parseInt(parts[1].trim());
                int r    = Integer.parseInt(parts[2].trim());

                TipusTerreny terreny = terrenysBarrejats.get(indexTerreny++);
                Integer numero = (terreny == TipusTerreny.DESERT) ? null : numerosBarrejats.get(indexNumero++);

                Hexagon hexagon = new Hexagon();
                hexagon.setCodi(codi);
                hexagon.setQ(q);
                hexagon.setR(r);
                hexagon.setTipusTerreny(terreny);
                hexagon.setNumero(numero);
                hexagon.setPartida(partida);
                hexagonRepository.save(hexagon);
                partida.getHexagons().add(hexagon);
                hexagonMap.put(codi, hexagon);
            }
        }
        return hexagonMap;
    }

    private void carregarHexagonVertex(
            Map<Integer, Hexagon> hexagonMap,
            Map<Integer, Vertex> vertexMap) throws Exception {

        try (BufferedReader reader = obrirCsv("topologia/hexagon_vertex.csv")) {
            reader.readLine(); // saltar capçalera: id_hexagon,id_vertex,posicio
            String linia;
            while ((linia = reader.readLine()) != null) {
                linia = linia.trim();
                if (linia.isEmpty()) continue;

                String[] parts   = linia.split(",");
                int codiHexagon  = Integer.parseInt(parts[0].trim());
                int codiVertex   = Integer.parseInt(parts[1].trim());
                int posicio      = Integer.parseInt(parts[2].trim());

                Hexagon hexagon = hexagonMap.get(codiHexagon);
                Vertex  vertex  = vertexMap.get(codiVertex);

                if (hexagon == null || vertex == null) {
                    throw new RuntimeException(
                        "Hexàgon o vèrtex no trobat: hexagon=" + codiHexagon + ", vertex=" + codiVertex
                    );
                }

                HexagonVertex hv = new HexagonVertex();
                hv.setHexagon(hexagon);
                hv.setVertex(vertex);
                hv.setPosicio(posicio);
                hexagonVertexRepository.save(hv);
                hexagon.getVertexs().add(hv);
            }
        }
    }

    private BufferedReader obrirCsv(String rutaRelativa) throws Exception {
        ClassPathResource resource = new ClassPathResource(rutaRelativa);
        return new BufferedReader(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
        );
    }
}
