///,h378775@stud.u-szeged.hu
// A kod nem tartalmaz ekezetes karaktereket a forditasi hibak elkerulese vegett.

import java.util.Random;
import java.util.PriorityQueue;

import game.mario.Direction;
import game.mario.MarioGame;
import game.mario.MarioPlayer;
import game.mario.utils.MarioState;

/**
 * Az Agent osztaly egy korlatozott "lookahead" ugynokot valosit meg,
 * amely A* elveket hasznal a legjobb celpont kivalasztasara.
 *
 * 1. Eloszor futtat egy magas prioritasu tulelesi reflex-ellenorzest (pl. arok/fal ugasa).
 * 2. Ha nincs kozvetlen veszely, A* alapu celpontkeresest vegez.
 * 3. Egy PriorityQueue-t hasznal a kozeli ermek es meglepetesek rangsorolasara
 * a (koltseg / ertek) arany alapjan (f = g / v).
 * 4. A legjobb (legalacsonyabb f erteku) celpont fele tesz egy lepest.
 * 5. Ha nincs celpont a latotavon belul, alapertelmezetten jobbra halad.
 */
public class Agent extends MarioPlayer {

    // A* alapu celpont-tarolo osztaly
    // Implementalja a Comparable-t, hogy a PriorityQueue hasznalhassa.
    private class TargetNode implements Comparable<TargetNode> {
        int x, y;
        double score; // Az 'f' ertek (koltseg / ertek)

        public TargetNode(int x, int y, double score) {
            this.x = x;
            this.y = y;
            this.score = score;
        }

        /**
         * Osszehasonlitas a PriorityQueue szamara.
         * A kisebb pontszam (jobb arany) elorebb kerul.
         */
        @Override
        public int compareTo(TargetNode other) {
            if (this.score < other.score) {
                return -1;
            }
            if (this.score > other.score) {
                return 1;
            }
            return 0;
        }
    }

    // Milyen tavolsagra nezzen elore az ugynok (oszlopokban)
    private static final int LOOKAHEAD_DISTANCE = 20;

    /**
     * Az ugynok konstruktora.
     */
    public Agent(int color, Random random, MarioState state) {
        super(color, random, state);
    }

    /**
     * A fuggveny, ami kivalasztja a kovetkezo lepest.
     */
    @Override
    public Direction getDirection(long remainingTime) {

        // --- 1. Allapotinformaciok ---
        int y = (int) state.mario.i;
        int x = (int) state.mario.j;
        int[][] map = state.map;
        boolean onGround = !state.isInAir;

        int mapHeight = 13;
        int mapWidth = 100;

        // --- 2. Magas Prioritasu Tulelesi Reflexek ---
        // Ha kozvetlen veszely van (arok, fal), azonnal reagalunk
        // es nem keresunk celpontot.

        // Hatar-ellenorzes
        if (x + 1 >= mapWidth) {
            state.apply(null);
            return null;
        }

        int cellRight = map[y][x + 1];
        int cellDiagDownRight = (y + 1 < mapHeight) ? map[y + 1][x + 1] : MarioGame.EMPTY;

        if (onGround) {
            // Fal vagy cso elottunk
            if (cellRight == MarioGame.WALL || cellRight == MarioGame.PIPE) {
                Direction action = new Direction(MarioGame.UP);
                state.apply(action);
                return action;
            }
            // Arok elottunk
            if (cellDiagDownRight == MarioGame.EMPTY && cellRight == MarioGame.EMPTY) {
                Direction action = new Direction(MarioGame.UP);
                state.apply(action);
                return action;
            }
        }

        // --- 3. A* Celpont Kereses (Lookahead) ---
        // Ha nincs kozvetlen veszely, keressuk a legjobb celpontot.

        PriorityQueue<TargetNode> targets = findBestTargets(x, y, map, mapHeight, mapWidth);

        Direction action;

        if (targets.isEmpty()) {
            // Nincs celpont a kozelben, haladjunk jobbra
            action = new Direction(MarioGame.RIGHT);
        } else {
            // Kivalasztjuk a legjobb celpontot (legalacsonyabb "koltseg/pont" arany)
            TargetNode bestTarget = targets.poll();

            // Eldontjuk, merre lepjunk a celpont fele
            action = getMoveTowardsTarget(bestTarget, x, y, onGround);
        }

        // A lepest vegul alkalmazzuk es visszaadjak
        state.apply(action);
        return action;
    }

    /**
     * Megkeresi a kozeli celpontokat es egy "f" ertek alapjan
     * (koltseg/ertek) PriorityQueue-be rendezi oket.
     *
     * @param x Mario jelenlegi X pozicioja
     * @param y Mario jelenlegi Y pozicioja
     * @param map A palya
     * @param mapHeight Palya magassaga
     * @param mapWidth Palya szelessege
     * @return Egy prioritasi sor, ami a legjobb celpontokat tartalmazza elol.
     */
    private PriorityQueue<TargetNode> findBestTargets(int x, int y, int[][] map, int mapHeight, int mapWidth) {
        PriorityQueue<TargetNode> pq = new PriorityQueue<TargetNode>();

        // Csak egy limitalt "dobozt" vizsgalunk Mario elott
        int scanUntilX = Math.min(x + LOOKAHEAD_DISTANCE, mapWidth);

        for (int i = 0; i < mapHeight; i++) {
            for (int j = x; j < scanUntilX; j++) {

                int item = map[i][j];
                double value = 0;

                if (item == MarioGame.COIN) {
                    value = 100.0; //
                } else if (item == MarioGame.SURPRISE) {
                    value = 500.0; //
                }

                if (value > 0) {
                    // 'g' (koltseg) = Manhattan-tavolsag.
                    // +1, hogy elkeruljuk a 0-val valo osztast, ha rajta allunk.
                    int g = Math.abs(x - j) + Math.abs(y - i) + 1;

                    // 'f' = g / v (koltseg / ertek arany)
                    // Azert negaljuk, mert a SURPRISE-t (ami felettunk van)
                    // alacsonyabb Y-on erjuk el (pl. y-1), de az ugras koltseges.
                    // Ez egy egyszerusitett heurisztika.
                    // A fenti SURPRISE blokkokat preferaljuk.
                    if (item == MarioGame.SURPRISE && i < y) {
                        // Ha felettunk van a surprise, csokkentjuk a "koltseget"
                        // (noveljuk a prioritasat)
                        g = g / 2;
                    }

                    double f_score = (double) g / value;
                    pq.add(new TargetNode(j, i, f_score));
                }
            }
        }
        return pq;
    }

    /**
     * Egy egyszeru irany-valaszto, ami a celpont fele probal lepni.
     */
    private Direction getMoveTowardsTarget(TargetNode target, int x, int y, boolean onGround) {

        // 1. Ha a celpont felettunk van (pl. erme, surprise) es foldon vagyunk, ugorjunk.
        if (target.y < y && onGround) {
            return new Direction(MarioGame.UP);
        }

        // 2. Ha a celpont jobbra van, menjunk jobbra.
        if (target.x > x) {
            return new Direction(MarioGame.RIGHT);
        }

        // 3. Ha a celpont balra van, menjunk balra.
        if (target.x < x) {
            return new Direction(MarioGame.LEFT);
        }

        // 4. Ha a celpont velunk egy oszlopban van (de nem felettunk),
        // vagy barmi mas eset, haladjunk jobbra.
        return new Direction(MarioGame.RIGHT);
    }
}