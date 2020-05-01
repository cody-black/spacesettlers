package blac8074;

import spacesettlers.objects.Flag;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;

import java.util.HashMap;
import java.util.Map;

/**
 * The BeePlanner assigns tasks to ships, keeps track of which ship is assigned to which task,
 * and keeps track of whether a ship is finished with its task.
 *
 */
public class BeePlanner {
    public enum BeeTask {
        FIND_ENEMY_FLAG, // Ship should move towards the enemy flag
        RETURN_TO_BASE, // Ship should go to nearest friendly base
        FIND_ALLY_FLAG, // Ship should return our flag
        GUARD, // Ship should guard our flag
        PROTECT, // Ship should protect flag carrier
        GET_ENERGY, // Ship should travel to closest beacon or base to get more energy
        GET_RESOURCES, // Ship should pick up nearest resource asteroid
        WANDER // Ship should chase nearest enemy and shoot it while collecting beacons and cores (Project 2 behavior)
    }

    Map<Ship, BeeTask> assignedTasks; // Stores the tasks that are assigned to each ship
    Map<Ship, Boolean> isFinished; // Tracks whether each ship is finished with its task

    boolean isCarryingEnemyFlag = false;
    boolean isFindingEnemyFlag = false;
    boolean isGuarding = false;
    boolean isProtecting = false;
    boolean isGettingResources = false;
    boolean isFindingAllyFlag = false;

    public BeePlanner() {
        assignedTasks = new HashMap<>();
        isFinished = new HashMap<>();
    }

    /**
     * Assigns the ship to a task
     */
    public void assignTask(Ship ship, Toroidal2DPhysics space) {
    	// Get our flag
    	Flag ourFlag = null;
		for (Flag flag : space.getFlags()) {
			if (flag.getTeamName().equals(ship.getTeamName())) {
				ourFlag = flag;
				break;
			}
		}
    	
		// Default task is WANDER
    	BeeTask assignedTask = BeeTask.WANDER;

        if (ship.isCarryingFlag()) {
        	// This ship is carrying the flag, it needs to return it to a base
        	assignedTask = BeeTask.RETURN_TO_BASE;
        	isFindingEnemyFlag = false;
        	isCarryingEnemyFlag = true;
        }
        else if (assignedTasks.containsKey(ship) && assignedTasks.get(ship) == BeeTask.GET_RESOURCES && ship.getResources().getTotal() > 0) {
        	// This ship just finished gathering resources and needs to bring them back to base
        	assignedTask = BeeTask.RETURN_TO_BASE;
        }
        else if (ship.getEnergy() < BeehaviorTeamClient.LOW_ENERGY_THRESH) {
        	// This ship is low on energy and needs to get more
        	assignedTask = BeeTask.GET_ENERGY;
        }
        else if (ourFlag.getPosition().getTotalTranslationalVelocity() > 0 && !isFindingAllyFlag && !ourFlag.isBeingCarried()) {
        	// Our flag is moving so it's probably left our base, return it
        	assignedTask = BeeTask.FIND_ALLY_FLAG;
        }
        else if (!isGuarding) {
            // No one is guarding, we should guard!
            assignedTask = BeeTask.GUARD;
        } else if (!isCarryingEnemyFlag && !isFindingEnemyFlag && ship.getEnergy() > BeehaviorTeamClient.LOW_ENERGY_THRESH) {
            // No one has the flag and we aren't looking for it, let's go get it
            assignedTask = BeeTask.FIND_ENEMY_FLAG;
        }
        else if (isCarryingEnemyFlag && !isProtecting) {
            // We have the flag and it's not this ship, go help him out!
            assignedTask = BeeTask.PROTECT;
        }
        else if (!isGettingResources) {
        	// No one is getting resources, so go gather resources
        	assignedTask = BeeTask.GET_RESOURCES;
        }

        assignedTasks.put(ship, assignedTask);
        isFinished.put(ship, false);

        switch (assignedTask) {
            case FIND_ENEMY_FLAG:
                isFindingEnemyFlag = true;
                break;
            case RETURN_TO_BASE:
            	break;
            case FIND_ALLY_FLAG:
            	isFindingAllyFlag = true;
                break;
            case GUARD:
                isGuarding = true;
                break;
            case PROTECT:
            	isProtecting = true;
                break;
            case GET_ENERGY:
            	break;
            case GET_RESOURCES:
            	isGettingResources = true;
            	break;
            case WANDER:
                break;
        }
    }

    /**
     * Marks each ship as having finished its task and sets the relevant boolean flags
     */
    public void finishTask(Ship ship) {
        BeeTask finishedTask = assignedTasks.get(ship);
        isFinished.put(ship, true);

        switch (finishedTask) {
            case FIND_ENEMY_FLAG:
                isFindingEnemyFlag = false;
                break;
            case RETURN_TO_BASE:
                break;
            case FIND_ALLY_FLAG:
            	isFindingAllyFlag = false;
                break;
            case GUARD:
                isGuarding = false; // Assumes only one guard
                break;
            case PROTECT:
            	isProtecting = false;
                break;
            case GET_ENERGY:
            	break;
            case GET_RESOURCES:
            	isGettingResources = false;
            	break;
            case WANDER:
                break;
        }
    }

    /**
     * Returns the task assigned to the given ship.
     * If the ship does not have a task or is finished with its current task, a new task is assigned.
     */
    public BeeTask getTask(Ship ship , Toroidal2DPhysics space) {
        if (!assignedTasks.containsKey(ship) || isFinished.get(ship) || ship.isCarryingFlag()) {
            assignTask(ship, space);
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
		boolean canShoot = assignedTasks.get(ship) != BeeTask.FIND_ENEMY_FLAG; // Ship is not going for enemy flag
		canShoot = canShoot && !ship.isCarryingFlag(); // Ship is not carrying enemy flag
		canShoot = canShoot && ship.getEnergy() > BeehaviorTeamClient.LOW_ENERGY_THRESH; // Ship has enough energy
		canShoot = canShoot && assignedTasks.get(ship) != BeeTask.GET_RESOURCES; // Ship is not gathering resources
		return canShoot;
	}
}
