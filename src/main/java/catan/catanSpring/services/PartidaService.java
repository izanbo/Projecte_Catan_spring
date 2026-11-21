package catan.catanSpring.services;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import catan.catanSpring.dto.ConstruccioAdjacenciaResposta;
import catan.catanSpring.dto.NovaPartidaPeticio;
import catan.catanSpring.dto.PartidaResposta;
import catan.catanSpring.dto.TaulerHexagonResposta;
import catan.catanSpring.entities.Assentament;
import catan.catanSpring.entities.CartaDesenvolupament;
import catan.catanSpring.entities.Ciutat;
import catan.catanSpring.entities.ColorJugador;
import catan.catanSpring.entities.EstatPartida;
import catan.catanSpring.entities.Hexagon;
import catan.catanSpring.entities.HexagonVertex;
import catan.catanSpring.entities.Jugador;
import catan.catanSpring.entities.Partida;
import catan.catanSpring.entities.RecursJugador;
import catan.catanSpring.entities.TipusCarta;
import catan.catanSpring.entities.TipusRecurs;
import catan.catanSpring.entities.TipusTerreny;
import catan.catanSpring.entities.Vertex;
import catan.catanSpring.repositories.AssentamentRepository;
import catan.catanSpring.repositories.CarreteraRepository;
import catan.catanSpring.repositories.CartaDesenvolupamentRepository;
import catan.catanSpring.repositories.CiutatRepository;
import catan.catanSpring.repositories.HexagonRepository;
import catan.catanSpring.repositories.JugadorRepository;
import catan.catanSpring.repositories.PartidaRepository;
import catan.catanSpring.repositories.PoblatRepository;
import catan.catanSpring.repositories.RecursJugadorRepository;

@Service
@Transactional
public class PartidaService {

    @Autowired
    private PartidaRepository partidaRepository;
    @Autowired
    private JugadorRepository jugadorRepository;
    @Autowired
    private RecursJugadorRepository recursJugadorRepository;
    @Autowired
    private HexagonRepository hexagonRepository;
    @Autowired
    private AssentamentRepository assentamentRepository;
    @Autowired
    private CartaDesenvolupamentRepository cartaRepository;
    @Autowired
    private PoblatRepository poblatRepository;
    @Autowired
    private CiutatRepository ciutatRepository;
    @Autowired
    private CarreteraRepository carreteraRepository;
    @Autowired
    private TaulerLoaderService taulerLoaderService;

    public ResponseEntity<?> iniciarPartida(NovaPartidaPeticio peticio) {
        if (peticio.nomsJugadors() == null || peticio.nomsJugadors().size() < 2 || peticio.nomsJugadors().size() > 4) {
            return ResponseEntity.badRequest().body("Hi ha d'haver entre 2 i 4 jugadors.");
        }

        Partida partida = new Partida();
        partida.IniciarPartida();
        if (peticio.puntsPerGuanyar() != null) {
            partida.setPuntsPerGuanyar(peticio.puntsPerGuanyar());
        }
        partidaRepository.save(partida);

        List<String> nomsBarrejats = new ArrayList<>(peticio.nomsJugadors());
        Collections.shuffle(nomsBarrejats);

        ColorJugador[] colors = ColorJugador.values();
        for (int i = 0; i < nomsBarrejats.size(); i++) {
            Jugador jugador = new Jugador(nomsBarrejats.get(i), colors[i], i + 1, partida);
            for (TipusRecurs tipus : TipusRecurs.values()) {
                RecursJugador recurs = new RecursJugador(0, jugador, tipus, 0);
                jugador.getRecursos().add(recurs);
            }
            partida.addJugador(jugador);
        }

        Jugador primerJugador = partida.getJugadors().stream()
                .filter(j -> j.getOrdreTorn() == 1)
                .findFirst()
                .orElse(partida.getJugadors().get(0));
        partida.setJugadorActiu(primerJugador);

        carregarDades(partida);
        
        crearPilaCartes(partida);

        partidaRepository.save(partida);

        return ResponseEntity.created(URI.create("/partida/" + partida.getId()))
                .body(PartidaResposta.de(partida));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> obtenirTauler(int idPartida) {
        Optional<Partida> partidaOpt = partidaRepository.findById(idPartida);
        if (partidaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No s'ha trobat cap partida amb ID: " + idPartida);
        }

        Partida partida = partidaOpt.get();
        List<Hexagon> hexagons = hexagonRepository.findByPartidaIdWithVertexs(idPartida);
        Hexagon hexLladre = partida.getHexagonLladre();

        List<TaulerHexagonResposta> resultat = new ArrayList<>();

        for (Hexagon hex : hexagons) {
            boolean hiHaLladre = (hexLladre != null && hexLladre.getId() == hex.getId());

            List<ConstruccioAdjacenciaResposta> construccions = new ArrayList<>();
            List<HexagonVertex> hexVertexList = new ArrayList<>(hex.getVertexs());
            hexVertexList.sort(Comparator.comparingInt(HexagonVertex::getPosicio));

            for (HexagonVertex hv : hexVertexList) {
                int posicio = hv.getPosicio();
                int vertexCodi = hv.getVertex().getCodi();
                int vertexId = hv.getVertex().getId();

                Optional<Assentament> assentamentOpt = assentamentRepository.findByVertexId(vertexId);

                if (assentamentOpt.isPresent()) {
                    Assentament assentament = assentamentOpt.get();
                    construccions.add(new ConstruccioAdjacenciaResposta(
                            posicio,
                            vertexId,
                            vertexCodi,
                            assentament.getClass().getSimpleName(),
                            assentament.getPropietari().getNom()));
                }
            }

            resultat.add(new TaulerHexagonResposta(
                    hex.getId(),
                    hex.getCodi(),
                    hex.getTipusTerreny(),
                    hex.getNumero(),
                    hiHaLladre,
                    construccions));
        }

        return ResponseEntity.ok(resultat);
    }

    private void crearPilaCartes(Partida partida) {
        List<TipusCarta> tipusCartes = new ArrayList<>();
        for (int i = 0; i < 14; i++) tipusCartes.add(TipusCarta.CAVALLER);
        for (int i = 0; i < 5; i++) tipusCartes.add(TipusCarta.PUNT_VICTORIA);
        for (int i = 0; i < 2; i++) tipusCartes.add(TipusCarta.MONOPOLI);
        Collections.shuffle(tipusCartes);

        for (TipusCarta tipus : tipusCartes) {
            CartaDesenvolupament carta = new CartaDesenvolupament(tipus, partida);
            partida.getCartes().add(carta);
            cartaRepository.save(carta);
        }
    }

    // Placeholder - la lógica real de carga de CSV se mantiene externamente
    private void carregarDades(Partida partida) {
        taulerLoaderService.carregarDades(partida);
    }

    @Transactional(readOnly = true)
    public void mostrarTauler(int idPartida) {
        Partida partida = partidaRepository.findById(idPartida)
                .orElseThrow(() -> new IllegalArgumentException("No s'ha trobat cap partida amb ID: " + idPartida));

        List<Hexagon> hexagons = hexagonRepository.findByPartidaIdWithVertexs(idPartida);

        if (hexagons.isEmpty()) {
            System.out.println("No s'han trobat hexàgons per a la partida amb ID: " + idPartida);
            return;
        }

        System.out.println("    --------------------");
        System.out.println("    Tauler de la Partida (" + idPartida + ")");
        System.out.println("    --------------------");

        Hexagon hexLladre = partida.getHexagonLladre();

        for (Hexagon hex : hexagons) {
            boolean hiHaLladre = (hexLladre != null && hexLladre.getId() == hex.getId());
            System.out.printf("Hexàgon [Codi: %d] - Terreny: %10s | Número: %-2s | Lladre: %s%n",
                    hex.getCodi(), hex.getTipusTerreny(),
                    (hex.getNumero() != null ? hex.getNumero() : "N/A"),
                    (hiHaLladre ? "Sí" : "NO"));

            List<HexagonVertex> hexVertexList = hex.getVertexs();
            if (hexVertexList == null || hexVertexList.isEmpty()) {
                System.out.println("(Sense vèrtexs associats)");
            } else {
                for (HexagonVertex hv : hexVertexList) {
                    int posicio = hv.getPosicio();
                    int vertexCodi = hv.getVertex().getCodi();
                    int vertexId = hv.getVertex().getId();

                    Optional<Assentament> assentamentOpt = assentamentRepository.findByVertexId(vertexId);

                    if (assentamentOpt.isPresent()) {
                        Assentament assentament = assentamentOpt.get();
                        String tipusConstruccio = assentament.getClass().getSimpleName();
                        String propietariNom = assentament.getPropietari().getNom();
                        System.out.printf("  └─ Vèrtex pos %d (Codi %d): %s de %s%n", posicio, vertexCodi, tipusConstruccio, propietariNom);
                    } else {
                        System.out.printf("  └─ Vèrtex pos %d (Codi %d): [BUIT]%n", posicio, vertexCodi);
                    }
                }
            }
            System.out.println("--------------------");
        }
    }

    @Transactional(readOnly = true)
    public void mostrarPartida(int idPartida) {
        Partida partida = partidaRepository.findById(idPartida)
                .orElseThrow(() -> new IllegalArgumentException("No s'ha trobat cap partida amb ID: " + idPartida));

        System.out.println("    --------------------");
        System.out.println("    Estat de la Partida (" + partida.getId() + ")");
        System.out.println("    --------------------");

        System.out.println("Estat: " + partida.getEstat());
        System.out.println("Ronda: " + partida.getRonda());
        System.out.println("Jugador actiu: " + (partida.getJugadorActiu() != null ? partida.getJugadorActiu().getNom() : "cap"));
        System.out.println("Última tirada: " + partida.getUltimaTirada());
        System.out.println("------------------");

        List<Jugador> jugadors = partida.getJugadors();
        if (jugadors == null || jugadors.isEmpty()) {
            System.out.println("Aquesta partida no té jugadors registrats.");
        } else {
            Integer idExercitMesGran = trobarIdExercitMesGran(partida);

            for (Jugador j : jugadors) {
                long poblats = poblatRepository.countByPropietariId(j.getId());
                long ciutats = ciutatRepository.countByPropietariId(j.getId());
                long carreteres = carreteraRepository.countByPropietariId(j.getId());
                long puntsCartes = cartaRepository.countByJugadorIdAndTipus(j.getId(), TipusCarta.PUNT_VICTORIA);

                int puntsVictoria = (int) (poblats + (ciutats * 2) + puntsCartes);
                if (idExercitMesGran != null && idExercitMesGran == j.getId()) {
                    puntsVictoria += 2;
                }

                List<RecursJugador> recursos = recursJugadorRepository.findByJugadorId(j.getId());
                long totalRecursos = recursos.stream().mapToLong(RecursJugador::getQuantitat).sum();

                System.out.printf("Jugador: %s (Color: %s)%n", j.getNom(), j.getColor());
                System.out.printf(" └─ Punts de Victòria: %d%n", puntsVictoria);
                System.out.printf(" └─ Construccions: %d Poblat(s), %d Ciutat(s), %d Carretera(s)%n", poblats, ciutats, carreteres);
                System.out.printf(" └─ Total de recursos: %d%n", totalRecursos);
                System.out.println("--------------------------------------------------");
            }
        }
    }

    public void tirarDaus(int idPartida) {
        Partida partida = partidaRepository.findById(idPartida)
                .orElseThrow(() -> new IllegalStateException("La partida no existeix."));

        if (partida.getEstat() != EstatPartida.EN_CURS)
            throw new IllegalStateException("La partida no està en curs.");
        if (partida.isDausTirats())
            throw new IllegalStateException("Ja s'han tirat els daus en aquest torn.");
        if (partida.isLladrePendent())
            throw new IllegalStateException("El lladre està pendent de moure.");

        int dau1 = (int) (Math.random() * 6) + 1;
        int dau2 = (int) (Math.random() * 6) + 1;
        int tirada = dau1 + dau2;
        partida.setUltimaTirada(tirada);
        partida.setDausTirats(true);

        System.out.println("Tirada de daus: " + dau1 + " + " + dau2 + " = " + tirada);

        if (tirada == 7) {
            partida.setLladrePendent(true);
            System.out.println("Ha sortit un 7! No hi ha producció. El lladre queda pendent.");

            for (Jugador jugador : partida.getJugadors()) {
                List<RecursJugador> recursos = recursJugadorRepository.findByJugadorId(jugador.getId());
                int total = recursos.stream().mapToInt(RecursJugador::getQuantitat).sum();

                if (total > 7) {
                    int aDescartar = total / 2;
                    List<TipusRecurs> bossa = new ArrayList<>();
                    for (RecursJugador r : recursos) {
                        for (int i = 0; i < r.getQuantitat(); i++) bossa.add(r.getTipusRecurs());
                    }
                    Collections.shuffle(bossa);

                    Map<TipusRecurs, Integer> descartats = new HashMap<>();
                    for (int i = 0; i < aDescartar; i++) {
                        TipusRecurs t = bossa.get(i);
                        descartats.put(t, descartats.getOrDefault(t, 0) + 1);
                    }

                    for (RecursJugador r : recursos) {
                        int desc = descartats.getOrDefault(r.getTipusRecurs(), 0);
                        if (desc > 0) r.setQuantitat(r.getQuantitat() - desc);
                    }
                    System.out.println(jugador.getNom() + " tenia " + total + " recursos i n'ha descartat " + aDescartar + ": " + descartats);
                }
            }
            partidaRepository.save(partida);
            return;
        }

        // Tirada diferent de 7
        int idLladre = (partida.getHexagonLladre() != null) ? partida.getHexagonLladre().getId() : -1;
        List<Hexagon> hexagons = hexagonRepository.findByPartidaAndNumeroSenseLladre(idPartida, tirada, idLladre);

        Map<String, Map<TipusRecurs, Integer>> resumRecursos = new HashMap<>();

        for (Hexagon hexagon : hexagons) {
            TipusTerreny terreny = hexagon.getTipusTerreny();
            TipusRecurs tipus = obtenirRecurs(terreny);
            if (tipus == null) continue;

            for (HexagonVertex hv : hexagon.getVertexs()) {
                Vertex vertex = hv.getVertex();
                Optional<Assentament> assentamentOpt = assentamentRepository.findByVertexId(vertex.getId());
                if (assentamentOpt.isEmpty()) continue;

                Assentament assentament = assentamentOpt.get();
                int quantitat = (assentament instanceof Ciutat) ? 2 : 1;
                Jugador propietari = assentament.getPropietari();

                Optional<RecursJugador> recursOpt = recursJugadorRepository.findByJugadorIdAndTipusRecurs(propietari.getId(), tipus);
                recursOpt.ifPresent(r -> {
                    r.setQuantitat(r.getQuantitat() + quantitat);
                    resumRecursos.putIfAbsent(propietari.getNom(), new HashMap<>());
                    resumRecursos.get(propietari.getNom()).merge(tipus, quantitat, Integer::sum);
                });
            }
        }

        System.out.println("Resum de producció per la tirada " + tirada + ":");
        if (resumRecursos.isEmpty()) {
            System.out.println("  Cap jugador ha rebut recursos.");
        } else {
            resumRecursos.forEach((nom, mapa) -> System.out.println("  " + nom + " ha rebut: " + mapa));
        }

        partidaRepository.save(partida);
    }

    public void comprovarVictoria(int idPartida) {
        Partida partida = partidaRepository.findById(idPartida)
                .orElseThrow(() -> new IllegalStateException("La partida no existeix."));

        if (partida.getEstat() == EstatPartida.FINALITZADA) return;

        Integer idExercitMesGran = trobarIdExercitMesGran(partida);

        for (Jugador jugador : partida.getJugadors()) {
            long poblats = poblatRepository.countByPropietariId(jugador.getId());
            long ciutats = ciutatRepository.countByPropietariId(jugador.getId());
            long cartesVictoria = cartaRepository.countByJugadorIdAndTipus(jugador.getId(), TipusCarta.PUNT_VICTORIA);

            int puntsVictoria = (int) (poblats + (2 * ciutats) + cartesVictoria);
            if (idExercitMesGran != null && idExercitMesGran == jugador.getId()) puntsVictoria += 2;

            if (puntsVictoria >= partida.getPuntsPerGuanyar()) {
                partida.setGuanyador(jugador);
                partida.setEstat(EstatPartida.FINALITZADA);
                System.out.println("La partida " + idPartida + " ha estat guanyada per " + jugador.getNom() + " amb " + puntsVictoria + " punts de victòria.");
                break;
            }
        }

        partidaRepository.save(partida);
    }

    private Integer trobarIdExercitMesGran(Partida partida) {
        List<Jugador> jugadors = partida.getJugadors();
        if (jugadors == null || jugadors.isEmpty()) return null;

        Jugador maxJugador = null;
        int maxCavallers = 2;
        boolean empat = false;

        for (Jugador j : jugadors) {
            int c = j.getCavallersJugats();
            if (c > maxCavallers) {
                maxCavallers = c;
                maxJugador = j;
                empat = false;
            } else if (c == maxCavallers && maxCavallers >= 3) {
                empat = true;
            }
        }

        return (maxJugador != null && !empat) ? maxJugador.getId() : null;
    }

    private TipusRecurs obtenirRecurs(TipusTerreny terreny) {
        if (terreny == null) return null;
        return switch (terreny) {
            case BOSC -> TipusRecurs.FUSTA;
            case TURONS -> TipusRecurs.MAO;
            case PASTURES -> TipusRecurs.LLANA;
            case CAMPS -> TipusRecurs.BLAT;
            case MUNTANYES -> TipusRecurs.MINERAL;
            default -> null;
        };
    }

    public void MostrarTauler(int idPartida) {
        mostrarTauler(idPartida);
    }

    public void MostrarPartida(int idPartida) {
        mostrarPartida(idPartida);
    }

    public void TirarDaus(int idPartida) {
        tirarDaus(idPartida);
    }

    public void ComprovarVictoria(int idPartida) {
        comprovarVictoria(idPartida);
    }
}
