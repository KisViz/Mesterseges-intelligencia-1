///Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.,h378775@stud.u-szeged.hu
import java.util.Random;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Comparator;
// import java.util.Arrays; // Komplexebb hash-hez szukseges lehet

import game.mario.Direction;
import game.mario.MarioGame;
import game.mario.MarioPlayer;
import game.mario.utils.MarioState;

/**
 * Ez az agens az A* (A-csillag) kereso algoritmust hasznalja a Mario palyan
 * valo navigalasra. A cel a maximalis pontszam elerese.
 * * Az algoritmus a getDirection() elso hivasakor megtervezi a teljes utvonalat
 * a palya vegeig, es ezt egy listaban tarolja. A kesobbi hivasok soran
 * mar csak a lista kovetkezo elemet adja vissza.
 * * A kereses maximalizalni probalja az f(n) = g(n) + h(n) erteket, ahol:
 * - g(n) a kezdettol az 'n' allapotig gyujtott valos pontszam.
 * - h(n) egy heurisztikus becsles az 'n' allapotbol a celig szerezheto pontszamra.
 * * A heurisztika (h(n)) egy elore kiszamolt tomb alapjan becsuli meg a hatralevo
 * maximalis pontszamot, figyelembe veve az ermeket es a dobozokat is.
 */
public class Agent extends MarioPlayer {

    /**
     * Az A* kereses egy csomopontjat (allapotat) reprezentalo belso osztaly.
     * Tarolja a jatekallapotot, a szulo csomopontot (utvonal rekonstrukciohoz),
     * a pontszamokat es az ide vezeto akciot.
     */
    private class AStarNode {
        MarioState state;
        AStarNode parent;
        Direction action;
        double gScore; // Pontszam a starttol idaig
        double hScore; // Heurisztikus becsles a celig
        double fScore; // f = g + h

        /**
         * Letrehoz egy uj A* csomopontot.
         * @param state A MarioState pillanatkepe.
         * @param parent Az elozo csomopont, ahonnan ide leptunk.
         * @param action Az akcio, amivel ide leptunk.
         * @param gScore Az eddig gyujtott pontszam.
         * @param hScore A heurisztikus becsles a jovobeli pontszamra.
         */
        public AStarNode(MarioState state, AStarNode parent, Direction action, double gScore, double hScore) {
            this.state = state;
            this.parent = parent;
            this.action = action;
            this.gScore = gScore;
            this.hScore = hScore;
            this.fScore = gScore + hScore;
        }
    }

    /**
     * A mar bejart allapotok gyors ellenorzesehez hasznalt hash generalasa.
     * Ez egy egyszerusitett hash: csak a poziciot es a levegoben letelt veszi figyelembe.
     * Nem kezeli, ha egy '?' blokk allapota megvaltozik!
     * @param s A MarioState, amibol a hash keszul.
     * @return Egy egyedi(nek szant) integer az allapothoz.
     */
    private int getSimpleStateHash(MarioState s) {
        // (X * PALYA_MAGASSAG + Y) * 2 + (levegoben_van ? 1 : 0)
        // A palya 13 magas (0-12)
        return ((int)s.mario.j * 13 + (int)s.mario.i) * 2 + (s.isInAir ? 1 : 0);
    }

    // --- UJ, OKOSABB HEURISZTIKA ---

    /**
     * Ez a tomb tarolja, hogy az 'x' oszlopbol a palya vegeig (99)
     * mennyi a maximalisan (optimista modon) elerheto pontszam.
     * A precomputeHeuristics() tolti fel.
     */
    private double[] futureScorePotential = null;

    /**
     * Kiszamolja es eltarolja a 'futureScorePotential' tombot.
     * Hatulrol elore haladva osszegzi a pontokat (targyak + haladas).
     * @param initialState A kiindulasi allapot, ebbol nyerjuk ki a terkepet.
     */
    private void precomputeHeuristics(MarioState initialState) {
        this.futureScorePotential = new double[100];
        int[][] map = initialState.map;
        double cumulativeScore = 0.0;

        // A palya vegerol (j=99) indulunk visszafele (j=0)
        for (int j = 99; j >= 0; j--) {
            // 1. Pontok az (i, j) oszlopon levo targyakert
            for (int i = 0; i < 13; i++) { // Palya magassaga 13
                if (map[i][j] == MarioGame.COIN) {
                    cumulativeScore += 100;
                } else if (map[i][j] == MarioGame.SURPRISE) {
                    cumulativeScore += 500;
                }
            }

            // 2. Pont a (j) -> (j+1) lepesert (kiveve a legvegen)
            if (j < 99) {
                cumulativeScore += 10;
            }

            // 3. Eltaroljuk az osszesitett potencialis pontszamot erre az 'x' koordinatara
            this.futureScorePotential[j] = cumulativeScore;
        }
    }

    /**
     * Heurisztika: A lehetseges jovobeli pontszam becslese.
     * Ez egy optimista becsles (admissible heurisztika maximalizalashoz),
     * mivel feltetelezi, hogy minden elottunk levo targyat fel tudunk venni.
     * @param s A jelenlegi MarioState.
     * @return A becsult jovobeli pontszam.
     */
    private double calculateHeuristic(MarioState s) {
        int marioX = (int)s.mario.j;

        // Biztonsagi ellenorzes, ha a tomb meg nincs inicializalva vagy Mario kiesett
        if (this.futureScorePotential == null || marioX < 0 || marioX >= 100) {
            return (99.0 - marioX) * 10.0; // Alap heurisztika
        }

        // Az X poziciobol hatralevo elore kiszamitott pontok (targyak + tovabbhaladas)
        return this.futureScorePotential[marioX];
    }

    // --- EDDIG TARTOTT A HEURISZTIKA FINOMITASA ---

    /**
     * Kiszamolja, hogy egy adott lepes mennyi pontot eredmenyezett.
     * @param oldState A lepes elotti allapot.
     * @param newState A lepes utani allapot.
     * @param action A vegrehajtott akcio.
     * @return A lepessel szerzett pontszam (vagy negativ vegtelen, ha halalos).
     */
    private double calculateScoreDelta(MarioState oldState, MarioState newState, Direction action) {
        double score = 0;
        int oldX = (int)oldState.mario.j;
        int oldY = (int)oldState.mario.i;
        int newX = (int)newState.mario.j;
        int newY = (int)newState.mario.i;

        // Eloszor ellenorizzuk, hogy kileptunk-e a palyarol (index 13+)
        if (newY >= 13) {
            return Double.NEGATIVE_INFINITY; // Palya alatti halal
        }

        // Ezutan ellenorizzuk a verembe esest (az utolso sor, 12-es index, URES)
        // Ezt mar biztonsagosan megtehetjuk, mert tudjuk, hogy newY nem 13+
        if (newY == 12 && newState.map[newY][newX] == MarioGame.EMPTY) {
            return Double.NEGATIVE_INFINITY; // Hatalmas buntetes
        }


        // Jobbra haladasert jaro pont
        if (newX > oldX) {
            score += 10;
        }

        // Erme felvetele (az uj pozicion az *regi* terkepe erme volt)
        // Feltetelezzuk, hogy az apply() eltavolitja az ermet a newState.map-rol.
        if (oldState.map[newY][newX] == MarioGame.COIN) {
            score += 100;
        }

        // Meglepetes blokk kiutese
        if (action != null && action.direction == MarioGame.UP && !oldState.isInAir && oldY > 0) {
            // A blokk Mario *felett* volt
            if (oldState.map[oldY - 1][oldX] == MarioGame.SURPRISE) {
                // Csak akkor kap pontot, ha tenyleg kiutotte (newState.map mar modosult)
                if (newState.map[oldY-1][oldX] != MarioGame.SURPRISE) {
                    score += 500;
                }
            }
        }

        // Buntetes, ha falnak utkozik (nem mozog)
        if (newX == oldX && action != null && action.direction == MarioGame.RIGHT) {
            score -= 5; // Enyhe buntetes, hogy ne toporogjon
        }

        // Buntetes a balra haladasert (altalaban nem optimalis)
        if (newX < oldX) {
            score -= 20; // Eros buntetes
        }

        return score;
    }

    /**
     * Visszafejti a megtalalt utvonalat a celtol a startig es eltarolja.
     * @param goalNode A celallapotot tartalmazo csomopont.
     */
    private void reconstructPath(AStarNode goalNode) {
        this.plannedPath = new LinkedList<>();
        AStarNode current = goalNode;
        // Visszalepkedunk a szulokon a startig
        while (current.parent != null && current.action != null) {
            this.plannedPath.addFirst(current.action); // Elejere szurjuk be
            current = current.parent;
        }
    }


    // --- Az Agent fo logikaja ---

    /**
     * A megtervezett utvonal (lepesek sorozata).
     * Ha null, meg nincs terv.
     */
    private LinkedList<Direction> plannedPath = null;

    /**
     * Jelzi, ha a tervezes idokorlat miatt meghiusult, hogy ne probaljuk ujra.
     */
    private boolean planningFailed = false;

    /**
     * Konstruktor.
     */
    public Agent(int color, Random random, MarioState state) {
        super(color, random, state);
    }

    /**
     * Ez a metodus hivodik meg minden jatek-iteracioban.
     * @param remainingTime A teljes jatekra hatralevo gondolkodasi ido (ms).
     * @return A valasztott lepes (Direction).
     */
    @Override
    public Direction getDirection(long remainingTime) {

        // 1. Tervezes (csak az elso hivasor, ha van eleg ido)
        if (plannedPath == null && !planningFailed) {
            if (remainingTime < 200) { // Tul keves ido, inkabb reflex
                planningFailed = true;
            } else {
                // Eloszor kiszamitjuk a heurisztikat
                precomputeHeuristics(state);
                // Aztan futtatjuk az A* keresest
                planPath(state, remainingTime);
            }
        }

        // 2. Vegrehajtas (ha van ervenyes tervunk)
        if (plannedPath != null && !plannedPath.isEmpty()) {
            return plannedPath.pollFirst(); // Terv kovetkezo lepesenek vegrehajtasa
        }

        // 3. Tartalek terv (ha az A* meghiusult, lejart az ideje, vagy nincs tobb lepes)
        // Az eredeti reflex agent logikaja:
        return getReflexDirection(state);
    }

    /**
     * Elinditja az A* keresest a teljes utvonal megtervezesehez.
     * (JAVITVA: Kivedi a game_engine.jar apply() metodusanak hibajat)
     * @param initialState A jatek kezdoallapota.
     * @param remainingTime A hatralevo ido.
     */
    private void planPath(MarioState initialState, long remainingTime) {
        // Idokorlat beallitasa
        long timeBudgetMs = Math.min(800, remainingTime - 200);
        long startTime = System.currentTimeMillis();

        PriorityQueue<AStarNode> openList = new PriorityQueue<>(Comparator.comparingDouble(n -> -n.fScore));
        Set<Integer> closedList = new HashSet<>();

        AStarNode startNode = new AStarNode(
                new MarioState(initialState),
                null,
                null,
                0.0,
                calculateHeuristic(initialState)
        );
        openList.add(startNode);
        AStarNode bestNodeSoFar = startNode;

        try {
            while (!openList.isEmpty() && (System.currentTimeMillis() - startTime) < timeBudgetMs) {

                AStarNode currentNode = openList.poll();

                int currentY = (int)currentNode.state.mario.i;
                int currentX = (int)currentNode.state.mario.j;

                // --- JAVITAS KEZDODIK: ROBUSZTUS HALAL-ELLENORZES ---
                // Kiszurjuk az osszes allapotot, ami a game_engine.jar hibajat eloidezheti.

                // 1. Ellenorizzuk, hogy az (X, Y) koordinatak a palyan belul vannak-e.
                if (currentY < 0 || currentY >= 13 || currentX < 0 || currentX >= 100) {
                    continue; // Ez az allapot mar a palyan kivul van, ne terjesszuk ki.
                }

                // 2. Mostmar biztonsagosan indexelhetjuk a terkepet [currentY][currentX]
                // Ellenorizzuk a "halal szelen all" esetet (Y=12, ures mezo felett)
                if (currentY == 12 && currentNode.state.map[currentY][currentX] == MarioGame.EMPTY) {
                    continue; // Ez az allapot a halal szelen all (verem felett).
                    // Barmelyik 'apply()' hivas erre crashelne. Ne terjesszuk ki.
                }
                // --- JAVITAS VEGE ---

                // Cel ellenorzes
                if (currentX == 99) { // A >= 99 felesleges, a 100-at mar kiszurtuk
                    reconstructPath(currentNode);
                    return; // Tervezes sikeres
                }

                if (currentX > bestNodeSoFar.state.mario.j) {
                    bestNodeSoFar = currentNode;
                }

                int stateHash = getSimpleStateHash(currentNode.state);
                if (closedList.contains(stateHash)) {
                    continue;
                }
                closedList.add(stateHash);

                Direction[] actions = {
                        new Direction(MarioGame.RIGHT),
                        new Direction(MarioGame.UP),
                        new Direction(MarioGame.LEFT),
                        null
                };

                for (Direction action : actions) {
                    MarioState newState = new MarioState(currentNode.state);

                    // Ez a hivas mostmar biztonsagos, mert a fenti ellenorzes
                    // kiszurte azokat a 'currentNode'-okat, amik crasht okoznanak.
                    newState.apply(action);

                    double deltaScore = calculateScoreDelta(currentNode.state, newState, action);

                    if (deltaScore == Double.NEGATIVE_INFINITY) {
                        continue;
                    }

                    double gScore = currentNode.gScore + deltaScore;
                    double hScore = calculateHeuristic(newState);

                    AStarNode neighborNode = new AStarNode(newState, currentNode, action, gScore, hScore);

                    if (!closedList.contains(getSimpleStateHash(newState))) {
                        openList.add(neighborNode);
                    }
                }
            }
        } catch (Exception e) {
            // Ha barki mas hiba tortenik (pl. a hash-sel), atallunk reflex modra
            planningFailed = true;
            return;
        }

        // Ha lejart az ido, es nem ertuk el a celt
        if (plannedPath == null && bestNodeSoFar.parent != null) {
            reconstructPath(bestNodeSoFar);
        } else {
            planningFailed = true;
        }
    }

    /**
     * Az eredeti reflex-alapu agens logikaja, tartaleknak.
     * (JAVITVA: A vegtelelen ciklusok elkerulese)
     */
    private Direction getReflexDirection(MarioState state) {
        int[][] map = state.map;
        int marioY = (int)state.mario.i;
        int marioX = (int)state.mario.j;
        boolean onGround = !state.isInAir;

        // 1. Fuggoleges (Y) hatarok ellenorzese
        if (marioY < 0 || marioY >= 13) {
            return new Direction(MarioGame.RIGHT);
        }
        if (marioY - 1 < 0) {
            return new Direction(MarioGame.RIGHT);
        }
        if (marioY + 1 >= 13) {
            return new Direction(MarioGame.RIGHT);
        }

        // 2. Vizszintes (X) hatarok ellenorzese
        if (marioX < 0 || marioX >= 100) {
            return new Direction(MarioGame.RIGHT);
        }
        if (marioX + 1 >= 100) {
            return new Direction(MarioGame.RIGHT);
        }


        // --- JAVITAS: Ciklus elkerulese ---
        // Ha szakadek vagy akadaly van elottunk,
        // a sima 'UP' vegtelelen ciklust okoz.
        // Helyette ugorjunk, de hagyjuk, hogy a kovetkezo
        // iteracioban az agens (akar a reflex, akar az A*)
        // dontson a tovabbhaladasrol.

        int tileAheadBelow = map[marioY + 1][marioX + 1];
        if (tileAheadBelow == MarioGame.EMPTY && onGround) {
            return new Direction(MarioGame.UP);
            // Ez meg mindig okozhat ciklust, ha a lyuk tul szeles,
            // de az A* tervnek kellene ezt megoldania.
        }

        int tileAhead = map[marioY][marioX + 1];
        if ((tileAhead == MarioGame.WALL || tileAhead == MarioGame.PIPE) && onGround) {
            return new Direction(MarioGame.UP);
            // Ez is ciklust okozhat.
        }
        // -----

        // A reflex agens alapvetoen rossz donteseket hoz.
        // A fo cel, hogy az A* sikeresen lefusson.
        // Ha az A* elbukik, a legjobb, amit tehetunk, hogy
        // megprobalunk jobbra menni, es bizunk.

        // Ez a resz jo:
        int tileAbove = map[marioY - 1][marioX];
        if (tileAbove == MarioGame.SURPRISE && onGround) {
            return new Direction(MarioGame.UP);
        }

        if (marioX + 2 < 100) {
            int tileAheadAir = map[marioY - 1][marioX + 2];
            if (tileAheadAir == MarioGame.COIN && onGround) {
                return new Direction(MarioGame.UP);
            }
        }

        // Alapertelmezett lepes
        return new Direction(MarioGame.RIGHT);
    }
}