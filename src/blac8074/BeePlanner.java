package blac8074;

import spacesettlers.objects.Ship;

import java.util.HashMap;
import java.util.Map;

public class BeePlanner {
    public enum BeeTask {
        FIND_ENEMY_FLAG, // Ship should move towards the enemy flag
        RETURN_TO_BASE, // Ship with flag should head to base
        FIND_ALLY_FLAG, // If our flag is gone, Ship should return it (?)
        GUARD, // Ship should protect our flag
        PROTECT, // Ship should protect flag carrier
        GET_ENERGY, // Ship should travel to closest beacon or base to get more energy
        GET_RESOURCES, // Ship should pick up nearest resource asteroid
        WANDER // Project 2 behaviour
    }

    Map<Ship, BeeTask> assignedTasks;

    boolean isCarryingEnemyFlag = false;
    boolean isFindingEnemyFlag = false;
    boolean isGuarding = false;
    double lowEnergyThresh;

    public BeePlanner(double lowEnergyThresh) {
        assignedTasks = new HashMap<>();
        this.lowEnergyThresh = lowEnergyThresh;
    }

    public void assignTask(Ship ship) {
        BeeTask assignedTask = BeeTask.WANDER;

        if (ship.isCarryingFlag()) {
        	// This ship is carrying the flag, it needs to return it to a base
        	assignedTask = BeeTask.RETURN_TO_BASE;
        	isFindingEnemyFlag = false;
        	isCarryingEnemyFlag = true;
        }
        else if (ship.getEnergy() < lowEnergyThresh) {
        	assignedTask = BeeTask.GET_ENERGY;
        }
        else if (!isGuarding) {
            // No one is guarding, we should guard!
            assignedTask = BeeTask.GUARD;
        } else if (!isCarryingEnemyFlag && !isFindingEnemyFlag) {
            // No one has the flag and we aren't looking for it, let's go get it
            assignedTask = BeeTask.FIND_ENEMY_FLAG;
        }	
        else if (isCarryingEnemyFlag) {
            // We have the flag and it's not this ship, go help him out!
            assignedTask = BeeTask.PROTECT;
        }

        assignedTasks.put(ship, assignedTask);

        switch (assignedTask) {
            case FIND_ENEMY_FLAG:
                isFindingEnemyFlag = true;
                break;
            case RETURN_TO_BASE:
            	break;
            case FIND_ALLY_FLAG:
                break;
            case GUARD:
                isGuarding = true;
                break;
            case PROTECT:
                break;
            case GET_ENERGY:
            	break;
            case GET_RESOURCES:
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
            case RETURN_TO_BASE:
                break;
            case FIND_ALLY_FLAG:
                break;
            case GUARD:
                isGuarding = false; // Assumes only one guard
                break;
            case PROTECT:
                break;
            case GET_ENERGY:
            	break;
            case GET_RESOURCES:
            	break;
            case WANDER:
                break;
        }
    }

    public BeeTask getTask(Ship ship) {
        if (!assignedTasks.containsKey(ship) || ship.isCarryingFlag()) {
            assignTask(ship);
        }

        return assignedTasks.get(ship);
    }

    public void setCarryingEnemyFlag(boolean carryingEnemyFlag) {
        isCarryingEnemyFlag = carryingEnemyFlag;
    }
    
    /**
     * Returns true if the given ship is allowed to shoot, false if it isn't
     */
	public boolean shipCanShoot(Ship ship) {
		boolean canShoot = getTask(ship) != BeeTask.FIND_ENEMY_FLAG; // Ship is not going for enemy flag
		canShoot = canShoot && !ship.isCarryingFlag(); // Ship is not carrying enemy flag
		canShoot = canShoot && getTask(ship) != BeeTask.GET_ENERGY; // Ship is not going to get more energy
		return canShoot;
	}
}
