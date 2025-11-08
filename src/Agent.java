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
        Direction action = new Direction(MarioGame.DIRECTIONS[random.nextInt(MarioGame.DIRECTIONS.length)]);
        state.apply(action);
        return action;
    }
}