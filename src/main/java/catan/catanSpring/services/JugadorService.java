package catan.catanSpring.services;

import catan.catanSpring.entities.*;
import catan.catanSpring.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class JugadorService {

    @Autowired
    private JugadorRepository jugadorRepository;
    @Autowired
    private ArestaRepository arestaRepository;
    @Autowired
    private VertexRepository vertexRepository;
    @Autowired
    private PartidaRepository partidaRepository;
    @Autowired
    private RecursJugadorRepository recursJugadorRepository;
    @Autowired
    private CarreteraRepository carreteraRepository;
    @Autowired
    private PoblatRepository poblatRepository;
    @Autowired
    private CiutatRepository ciutatRepository;
    @Autowired
    private AssentamentRepository assentamentRepository;
    @Autowired
    private CartaDesenvolupamentRepository cartaRepository;
    @Autowired
    private HexagonVertexRepository hexagonVertexRepository;

    @Transactional(readOnly = true)
    public void mostrarRecursos(int idJugador) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElse(null);
        if (jugador == null) {
            System.out.println("No s'ha trobat cap jugador amb ID: " + idJugador);
            return;
        }

        List<RecursJugador> recursos = recursJugadorRepository.findByJugadorId(idJugador);

        System.out.println("    --------------------");
        System.out.println("    Inventari de Recursos - " + jugador.getNom() + " (Color: " + jugador.getColor() + ")");
        System.out.println("    --------------------");

        if (recursos.isEmpty()) {
            System.out.println("El jugador no té cap recurs actualment.");
        } else {
            for (RecursJugador r : recursos) {
                System.out.printf("- %-10s: %d%n", r.getTipusRecurs(), r.getQuantitat());
            }
        }
        System.out.println("--------------------");
    }

    public boolean construirCarretera(int idJugador, int idAresta) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElse(null);
        Aresta aresta = arestaRepository.findById(idAresta).orElse(null);

        if (jugador == null || aresta == null) return false;

        Partida partida = jugador.getPartida();
        if (partida.getEstat() == EstatPartida.FINALITZADA) return false;
        if (partida.getJugadorActiu() == null || partida.getJugadorActiu().getId() != idJugador) return false;
        if (aresta.getPartida() != null && aresta.getPartida().getId() != partida.getId()) return false;

        if (carreteraRepository.countByArestaId(idAresta) > 0) return false;

        boolean preparacio = (partida.getEstat() == EstatPartida.PREPARACIO);
        int vertexAId = aresta.getVertexA().getId();
        int vertexBId = aresta.getVertexB().getId();

        long totalCarreteresJugador = carreteraRepository.countByPropietariId(idJugador);
        long totalPoblatsJugador = poblatRepository.countByPropietariId(idJugador);

        if (preparacio) {
            if (totalPoblatsJugador != totalCarreteresJugador + 1) return false;

            boolean tocaPoblatSenseCarretera = false;
            for (int vId : new int[]{vertexAId, vertexBId}) {
                Optional<Poblat> poblat = poblatRepository.findByVertexIdAndPropietariId(vId, idJugador);
                if (poblat.isPresent()) {
                    long carreteresAdjacents = carreteraRepository.countByPropietariIdAndVertexId(idJugador, vId);
                    if (carreteresAdjacents == 0) {
                        tocaPoblatSenseCarretera = true;
                        break;
                    }
                }
            }
            if (!tocaPoblatSenseCarretera) return false;
        } else {
            if (!partida.isDausTirats() || partida.isLladrePendent()) return false;
            if (totalCarreteresJugador >= 15) return false;

            long assentamentsPropis = assentamentRepository.countByPropietariIdAndVertexAOrVertexB(idJugador, vertexAId, vertexBId);
            long carreteresConnectades = carreteraRepository.countByPropietariIdConnectedTo(idJugador, idAresta, vertexAId, vertexBId);

            if (assentamentsPropis == 0 && carreteresConnectades == 0) return false;

            List<RecursJugador> recursos = recursJugadorRepository.findByJugadorId(idJugador);
            RecursJugador fusta = trobarRecurs(recursos, TipusRecurs.FUSTA);
            RecursJugador mao = trobarRecurs(recursos, TipusRecurs.MAO);

            if (fusta == null || fusta.getQuantitat() < 1 || mao == null || mao.getQuantitat() < 1) return false;

            fusta.setQuantitat(fusta.getQuantitat() - 1);
            mao.setQuantitat(mao.getQuantitat() - 1);
        }

        Carretera carretera = new Carretera(jugador, aresta);
        carreteraRepository.save(carretera);
        jugador.getConstruccions().add(carretera);
        return true;
    }

    public boolean construirPoblat(int idJugador, int idVertex) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElse(null);
        Vertex vertex = vertexRepository.findById(idVertex).orElse(null);

        if (jugador == null || vertex == null) return false;

        Partida partida = jugador.getPartida();
        if (partida.getEstat() == EstatPartida.FINALITZADA) return false;
        if (partida.getJugadorActiu() == null || partida.getJugadorActiu().getId() != idJugador) return false;
        if (vertex.getPartida() != null && vertex.getPartida().getId() != partida.getId()) return false;

        // Vèrtex lliure
        if (assentamentRepository.findByVertexId(idVertex).isPresent()) return false;

        // Regla de la distància: cap vèrtex adjacent pot tenir cap construcció
        if (assentamentRepository.countAssentamentsVeins(idVertex) > 0) return false;
        long poblatsActuals = poblatRepository.countByPropietariId(idJugador);
        long carreteresActuals = carreteraRepository.countByPropietariId(idJugador);

        List<RecursJugador> recursos = recursJugadorRepository.findByJugadorId(idJugador);

        boolean preparacio = (partida.getEstat() == EstatPartida.PREPARACIO);

        if (preparacio) {
            if (poblatsActuals != carreteresActuals || poblatsActuals >= 2) return false;
        } else {
            if (!partida.isDausTirats() || partida.isLladrePendent()) return false;
            if (poblatsActuals >= 5) return false;

            long carreteresPropies = carreteraRepository.countByPropietariIdAndVertexId(idJugador, idVertex);
            if (carreteresPropies == 0) return false;

            TipusRecurs[] cost = {TipusRecurs.FUSTA, TipusRecurs.MAO, TipusRecurs.LLANA, TipusRecurs.BLAT};
            for (TipusRecurs tipus : cost) {
                RecursJugador r = trobarRecurs(recursos, tipus);
                if (r == null || r.getQuantitat() < 1) return false;
            }
            for (TipusRecurs tipus : cost) {
                RecursJugador r = trobarRecurs(recursos, tipus);
                r.setQuantitat(r.getQuantitat() - 1);
            }
        }

        Poblat poblat = new Poblat(jugador, vertex);
        poblatRepository.save(poblat);
        jugador.getConstruccions().add(poblat);

        // Segon poblat de la preparació: rep recursos
        if (preparacio && poblatsActuals == 1) {
            List<HexagonVertex> hvList = hexagonVertexRepository.findByVertexId(idVertex);
            for (HexagonVertex hv : hvList) {
                Hexagon hex = hv.getHexagon();
                TipusRecurs tipus = recursDelTerreny(hex.getTipusTerreny());
                if (tipus != null) {
                    afegirRecurs(jugador, recursos, tipus);
                }
            }
        }

        comprovarVictoriaInternal(partida);
        return true;
    }

    public boolean millorarACiutat(int idJugador, int idVertex) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElse(null);
        Vertex vertex = vertexRepository.findById(idVertex).orElse(null);

        if (jugador == null || vertex == null) return false;

        Partida partida = jugador.getPartida();
        if (partida.getEstat() != EstatPartida.EN_CURS) return false;
        if (partida.getJugadorActiu() == null || partida.getJugadorActiu().getId() != idJugador) return false;
        if (!partida.isDausTirats() || partida.isLladrePendent()) return false;
        if (vertex.getPartida() != null && vertex.getPartida().getId() != partida.getId()) return false;

        long ciutatsCount = ciutatRepository.countByPropietariId(idJugador);
        if (ciutatsCount >= 4) return false;

        Optional<Poblat> poblatOpt = poblatRepository.findByVertexIdAndPropietariId(idVertex, idJugador);
        if (poblatOpt.isEmpty()) return false;

        List<RecursJugador> recursos = recursJugadorRepository.findByJugadorId(idJugador);
        RecursJugador blat = trobarRecurs(recursos, TipusRecurs.BLAT);
        RecursJugador mineral = trobarRecurs(recursos, TipusRecurs.MINERAL);

        if (blat == null || blat.getQuantitat() < 2 || mineral == null || mineral.getQuantitat() < 3) return false;

        blat.setQuantitat(blat.getQuantitat() - 2);
        mineral.setQuantitat(mineral.getQuantitat() - 3);

        Poblat poblat = poblatOpt.get();
        jugador.getConstruccions().remove(poblat);
        poblatRepository.delete(poblat);

        Ciutat ciutat = new Ciutat(jugador, vertex);
        ciutatRepository.save(ciutat);
        jugador.getConstruccions().add(ciutat);

        comprovarVictoriaInternal(partida);
        return true;
    }

    public TipusRecurs comerciarBanca(int idJugador, TipusRecurs dona, TipusRecurs rep) {
        if (dona == null || rep == null || dona == rep) return null;

        Jugador jugador = jugadorRepository.findById(idJugador).orElse(null);
        if (jugador == null) return null;

        Partida partida = jugador.getPartida();
        if (partida.getEstat() != EstatPartida.EN_CURS) return null;
        if (partida.getJugadorActiu() == null || partida.getJugadorActiu().getId() != idJugador) return null;
        if (!partida.isDausTirats() || partida.isLladrePendent()) return null;

        List<RecursJugador> recursos = recursJugadorRepository.findByJugadorId(idJugador);
        RecursJugador recursDona = trobarRecurs(recursos, dona);
        RecursJugador recursRep = trobarRecurs(recursos, rep);

        if (recursDona == null || recursDona.getQuantitat() < 4 || recursRep == null) return null;

        recursDona.setQuantitat(recursDona.getQuantitat() - 4);
        recursRep.setQuantitat(recursRep.getQuantitat() + 1);

        return rep;
    }

    @Transactional(readOnly = true)
    public int calcularPunts(int idJugador) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElse(null);
        if (jugador == null) return 0;

        Integer idExercitMesGran = trobarIdExercitMesGran(jugador.getPartida());
        return calcularPuntsInternal(idJugador, idExercitMesGran);
    }

    public void passarTorn(int idJugador) {
        Jugador jugador = jugadorRepository.findById(idJugador)
                .orElseThrow(() -> new IllegalStateException("El jugador no existeix."));

        Partida partida = jugador.getPartida();
        if (partida.getEstat() == EstatPartida.FINALITZADA)
            throw new IllegalStateException("La partida està finalitzada.");
        if (partida.getJugadorActiu() == null || partida.getJugadorActiu().getId() != idJugador)
            throw new IllegalStateException("No és el teu torn.");
        if (partida.isLladrePendent())
            throw new IllegalStateException("No es pot passar torn amb el lladre pendent.");

        boolean preparacio = (partida.getEstat() == EstatPartida.PREPARACIO);

        if (preparacio) {
            int ronda = partida.getRonda();
            long poblats = poblatRepository.countByPropietariId(idJugador);
            long carreteres = carreteraRepository.countByPropietariId(idJugador);
            if (poblats < ronda || carreteres < ronda)
                throw new IllegalStateException("En fase de preparació cal haver col·locat el poblat i la carretera de la ronda actual.");
        } else {
            if (!partida.isDausTirats())
                throw new IllegalStateException("Cal tirar els daus abans de passar torn.");
        }

        List<Jugador> jugadors = jugadorRepository.findByPartidaIdOrderByOrdreTornAsc(partida.getId());

        int n = jugadors.size();
        int currentOrdre = jugador.getOrdreTorn();

        if (preparacio) {
            if (partida.getRonda() == 1) {
                if (currentOrdre < n) {
                    partida.setJugadorActiu(buscarJugadorPerOrdre(jugadors, currentOrdre + 1));
                } else {
                    partida.setRonda(2);
                    partida.setJugadorActiu(buscarJugadorPerOrdre(jugadors, n));
                }
            } else if (partida.getRonda() == 2) {
                if (currentOrdre > 1) {
                    partida.setJugadorActiu(buscarJugadorPerOrdre(jugadors, currentOrdre - 1));
                } else {
                    partida.setEstat(EstatPartida.EN_CURS);
                    partida.setRonda(1);
                    partida.setJugadorActiu(buscarJugadorPerOrdre(jugadors, 1));
                }
            }
        } else {
            if (currentOrdre < n) {
                partida.setJugadorActiu(buscarJugadorPerOrdre(jugadors, currentOrdre + 1));
            } else {
                partida.setJugadorActiu(buscarJugadorPerOrdre(jugadors, 1));
                partida.setRonda(partida.getRonda() + 1);
            }
        }

        partida.setDausTirats(false);
        partidaRepository.save(partida);
    }

    // ---- Helpers privats ----

    private Jugador buscarJugadorPerOrdre(List<Jugador> jugadors, int ordre) {
        return jugadors.stream()
                .filter(j -> j.getOrdreTorn() == ordre)
                .findFirst()
                .orElse(jugadors.get(0));
    }

    private int calcularPuntsInternal(int idJugador, Integer idExercitMesGran) {
        long poblats = poblatRepository.countByPropietariId(idJugador);
        long ciutats = ciutatRepository.countByPropietariId(idJugador);
        long puntsCartes = cartaRepository.countByJugadorIdAndTipus(idJugador, TipusCarta.PUNT_VICTORIA);

        int punts = (int) (poblats + (ciutats * 2) + puntsCartes);
        if (idExercitMesGran != null && idExercitMesGran == idJugador) punts += 2;
        return punts;
    }

    private void comprovarVictoriaInternal(Partida partida) {
        if (partida.getEstat() == EstatPartida.FINALITZADA) return;
        Integer idExercit = trobarIdExercitMesGran(partida);
        for (Jugador j : partida.getJugadors()) {
            int punts = calcularPuntsInternal(j.getId(), idExercit);
            if (punts >= partida.getPuntsPerGuanyar()) {
                partida.setGuanyador(j);
                partida.setEstat(EstatPartida.FINALITZADA);
                System.out.println("Partida guanyada per " + j.getNom() + " amb " + punts + " punts!");
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

    private RecursJugador trobarRecurs(List<RecursJugador> recursos, TipusRecurs tipus) {
        return recursos.stream()
                .filter(r -> r.getTipusRecurs() == tipus)
                .findFirst()
                .orElse(null);
    }

    private TipusRecurs recursDelTerreny(TipusTerreny terreny) {
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

    private void afegirRecurs(Jugador jugador, List<RecursJugador> recursos, TipusRecurs tipus) {
        RecursJugador recurs = trobarRecurs(recursos, tipus);
        if (recurs == null) {
            recurs = new RecursJugador(0, jugador, tipus, 1);
            recursJugadorRepository.save(recurs);
            recursos.add(recurs);
        } else {
            recurs.setQuantitat(recurs.getQuantitat() + 1);
        }
    }

    // Mètodes de compatibilitat (PascalCase)
    public void MostrarRecursos(int idJugador) {
        mostrarRecursos(idJugador);
    }

    public boolean ConstruirCarretera(int idJugador, int idAresta) {
        return construirCarretera(idJugador, idAresta);
    }

    public boolean ConstruirPoblat(int idJugador, int idVertex) {
        return construirPoblat(idJugador, idVertex);
    }

    public boolean MillorarACiutat(int idJugador, int idVertex) {
        return millorarACiutat(idJugador, idVertex);
    }

    public TipusRecurs ComerciarBanca(int idJugador, TipusRecurs dona, TipusRecurs rep) {
        return comerciarBanca(idJugador, dona, rep);
    }

    public int CalcularPunts(int idJugador) {
        return calcularPunts(idJugador);
    }

    public void PassarTorn(int idJugador) {
        passarTorn(idJugador);
    }
}
