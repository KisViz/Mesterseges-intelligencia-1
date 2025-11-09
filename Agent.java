///,h378775@stud.u-szeged.hu
// A kod nem tartalmaz ekezetes karaktereket a forditasi hibak elkerulese vegett.

import java.util.Random;

import game.mario.Direction;
import game.mario.MarioGame;
import game.mario.MarioPlayer;
import game.mario.utils.MarioState;

/**
 * Az Agent osztaly egy egyszeru reflex ugynokot valosit meg.
 * Donteseit kizarolag a Mario koruli kozvetlen cellak alapjan hozza meg.
 * A dontesek prioritasi sorrendben tortennek:
 * 1. Bonuszok megszerzese (meglepetesblokk)
 * 2. Akadalyok es arkok atugrasa a tulelesert
 * 3. Ermek gyujtese
 * 4. Alapertelmezett haladas jobbra
 *
 * Az ugynok a jatek allapotat (state) hasznalja a kornyezo elemek
 * (fal, cso, erme, stb.) erzekelesere es Mario foldon letenek ellenorzesere.
 */
public class Agent extends MarioPlayer {

    /**
     * Az ugynok konstruktora.
     * @param color A jatekos szine (grafikus feluleten)
     * @param random Az orokolt veletlenszam-generator
     * @param state A jatek jelenlegi allapota
     */
    public Agent(int color, Random random, MarioState state) {
        super(color, random, state);
    }

    /**
     * Ez a fuggveny hatarozza meg Mario kovetkezo lepeseit.
     * A dontes egy egyszeru, szabaly-alapu reflex logika menten tortenik.
     *
     * @param remainingTime A gondolkodasra maradt ido nanoszekundumban (ebben
     * az egyszeru ugynokben nincs hasznalva)
     * @return A valasztott irany (Direction)
     */
    @Override
    public Direction getDirection(long remainingTime) {

        // --- 1. Allapotinformaciok begyujtese ---
        // Mario pozicioja: i = sor (Y), j = oszlop (X)
        // A pozicio double, ezert int-re kasztoljuk a palya indexeleshez.
        int y = (int) state.mario.i;
        int x = (int) state.mario.j;

        // A palya int[][] tipusu, az elemeket a MarioGame konstansaival hasonlitjuk ossze.
        int[][] map = state.map;
        boolean onGround = !state.isInAir; // Foldon van, ha nincs a levegoben

        // Palya meretei (a README alapjan)
        int mapHeight = 13;
        int mapWidth = 100;

        // --- 2. Hatar-ellenorzesek (IndexOutOfBounds elkerulese) ---

        // Ha elertuk a palya jobb szelet, ne csinaljunk semmit
        if (x + 1 >= mapWidth) {
            state.apply(null); // Allapot frissitese a keretrendszernek
            return null;
        }

        // Kozvetlen kornyezo cellak beolvasasa, hatarokat figyelembe veve
        int cellRight = map[y][x + 1];

        int cellAbove = MarioGame.WALL; // Alapertelmezetten fal, ha a palya tetejen vagyunk
        if (y > 0) {
            cellAbove = map[y - 1][x];
        }

        int cellDiagDownRight = MarioGame.EMPTY; // Alapertelmezetten ures (arok), ha a palya aljan vagyunk
        if (y + 1 < mapHeight) {
            cellDiagDownRight = map[y + 1][x + 1];
        }


        // --- 3. Priorizalt Reflex Szabalyok ---

        // 1. PRIORITAS: Meglepetes blokk megszerzese (500 pont)
        // Ha a foldon allunk es egy '?' van felettunk, ugorjunk.
        if (onGround && cellAbove == MarioGame.SURPRISE) {
            Direction action = new Direction(MarioGame.UP);
            state.apply(action);
            return action;
        }

        // 2. PRIORITAS: Tuleles (Akadalyok es Arkok)
        if (onGround) {
            // Ha jobbra fal vagy cso van, ugorjunk.
            if (cellRight == MarioGame.WALL || cellRight == MarioGame.PIPE) {
                Direction action = new Direction(MarioGame.UP);
                state.apply(action);
                return action;
            }

            // Ha jobbra elottunk a fold hianyzik (arok), ugorjunk.
            // (Csak akkor, ha a jobbra levo cella is ures, kulonben falnak ugrunk)
            if (cellDiagDownRight == MarioGame.EMPTY && cellRight == MarioGame.EMPTY) {
                Direction action = new Direction(MarioGame.UP);
                state.apply(action);
                return action;
            }
        }

        // 3. PRIORITAS: Ermek gyujtese (100 pont)
        // Ha jobbra erme van, menjunk erte.
        if (cellRight == MarioGame.COIN) {
            Direction action = new Direction(MarioGame.RIGHT);
            state.apply(action);
            return action;
        }

        // Ha felettunk van erme es a foldon vagyunk, ugorjunk erte.
        if (onGround && cellAbove == MarioGame.COIN) {
            Direction action = new Direction(MarioGame.UP);
            state.apply(action);
            return action;
        }

        // 4. PRIORITAS: Alapertelmezett akcio (Haladas)
        // Ha semmi mas nem indokolt, haladjunk jobbra.
        Direction action = new Direction(MarioGame.RIGHT);
        state.apply(action);
        return action;
    }
}