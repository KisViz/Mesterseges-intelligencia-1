///Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.,h378775@stud.u-szeged.hu
package src;

import java.util.Random;

import game.mario.Direction;
import game.mario.MarioGame;
import game.mario.MarioPlayer;
import game.mario.utils.MarioState;

public class Agent extends MarioPlayer {
    public Agent(int color, Random random, MarioState state) {
        super(color, random, state);
    }

    @Override
    public Direction getDirection(long remainingTime) {

        // allapot lekerdezese
        int[][] map = state.map; //palya terkep
        int marioY = (int)state.mario.i; //mario pozicioja (int kent, ert a map ugy tarolja)
        int marioX = (int)state.mario.j;
        boolean onGround = !state.isInAir; //a foldon vagyunk e

        // a palya 13x100
        if (marioX + 1 >= 100 || marioY + 1 >= 13 || marioY - 1 < 0) {
            return new Direction(MarioGame.RIGHT);
        }

        // szakadek elkerulese
        // ha elotte alatta nincs kocka ugrik
        int tileAheadBelow = map[marioY + 1][marioX + 1];
        if (tileAheadBelow == MarioGame.EMPTY && onGround) {
            return new Direction(MarioGame.UP);
        }

        // akadaly
        // ha elotte akadaly van ugrik
        int tileAhead = map[marioY + 1][marioX + 1];
        if ((tileAhead == MarioGame.WALL || tileAhead == MarioGame.PIPE) && onGround) {
            return new Direction(MarioGame.UP);
        }

        // ha folotte meglepetes van es foldon van ugrik
        int tileAbove = map[marioY - 1][marioX];
        if (tileAbove == MarioGame.SURPRISE && onGround) {
            return new Direction(MarioGame.UP);
        }

        // egyebkent megyunk jobbra
        return new Direction(MarioGame.RIGHT);
    }
}