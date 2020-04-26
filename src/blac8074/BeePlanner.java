package blac8074;

import spacesettlers.objects.Ship;

import java.util.HashMap;
import java.util.Map;

public class BeePlanner {
    public enum BeeTask {
        FIND_ENEMY_FLAG, // Ship should move towards the enemy flag
        RETURN_ENEMY_FLAG, // Ship with flag should head to base
        FIND_ALLY_FLAG, // If our flag is gone, Ship should return it (?)
        GUARD, // Ship should protect our flag
        PROTECT, // Ship should protect flag carrier
        WANDER // Project 2 behaviour
    }

    Map<Ship, BeeTask> assignedTasks;

    boolean isCarryingEnemyFlag = false;
    boolean isFindingEnemyFlag = false;
    boolean isGuarding = false;

    public BeePlanner() {
        assignedTasks = new HashMap<>();
    }

    public void assignTask(Ship ship) {
        BeeTask assignedTask = BeeTask.WANDER;

        if (!isGuarding) {
            // No one is guarding, we should guard!
            assignedTask = BeeTask.GUARD;
        } else if (!isCarryingEnemyFlag && !isFindingEnemyFlag) {
            // No one has the flag and we aren't looking for it, let's go get it
            assignedTask = BeeTask.FIND_ENEMY_FLAG;
        } else if (isCarryingEnemyFlag && ship.isCarryingFlag()) {
            // We have the flag and it's this ship that has it, return it
            assignedTask = BeeTask.RETURN_ENEMY_FLAG;
        } else if (isCarryingEnemyFlag) {
            // We have the flag and it's not this ship, go help him out!
            assignedTask = BeeTask.PROTECT;
        }

        assignedTasks.put(ship, assignedTask);

        switch (assignedTask) {
            case FIND_ENEMY_FLAG:
                isFindingEnemyFlag = true;
                break;
            case RETURN_ENEMY_FLAG:
                break;
            case FIND_ALLY_FLAG:
                break;
            case GUARD:
                isGuarding = true;
                break;
            case PROTECT:
                break;
            case WANDER:
                break;
        }
    }

    public void finishTask(Ship ship) {
        BeeTask removedTask = assignedTasks.remove(ship);

        switch (removedTask) {
            case FIND_ENEMY_FLAG:
                isFindingEnemyFlag = false;
                break;
            case RETURN_ENEMY_FLAG:
                isCarryingEnemyFlag = false;
                break;
            case FIND_ALLY_FLAG:
                break;
            case GUARD:
                isGuarding = false; // Assumes only one guard
                break;
            case PROTECT:
                break;
            case WANDER:
                break;
        }
    }

    public BeeTask getTask(Ship ship) {
        if (!assignedTasks.containsKey(ship)) {
            assignTask(ship);
        }

        return assignedTasks.get(ship);
    }

    public void setCarryingEnemyFlag(boolean carryingEnemyFlag) {
        isCarryingEnemyFlag = carryingEnemyFlag;
    }
}
