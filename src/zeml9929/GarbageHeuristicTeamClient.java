package zeml9929;

import spacesettlers.actions.*;
import spacesettlers.clients.TeamClient;
import spacesettlers.graphics.LineGraphics;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.graphics.TargetGraphics;
import spacesettlers.objects.*;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;
import java.awt.Color;

import java.util.*;

/**
 * A team of agents who fly towards the nearest asteroids and then brings the resources back to base.
 *
 * @author Noah Zemlin
 */
public class GarbageHeuristicTeamClient extends TeamClient {
	HashSet<SpacewarGraphics> graphics;
	HashMap<UUID, Position> targetsPos;
	HashMap<UUID, UUID> targetsUUID;
	HashMap<AiCore, Ship> coreToShipMap;
	HashMap<Ship, AiCore> shipToCoreMap;
	HashMap<Beacon, Ship> beaconToShipMap;

	@Override
	public void initialize(Toroidal2DPhysics space) {
		graphics = new HashSet<>();
		targetsPos = new HashMap<>();
		targetsUUID = new HashMap<>();
		coreToShipMap = new HashMap<AiCore, Ship>();
		shipToCoreMap = new HashMap<Ship, AiCore>();
		beaconToShipMap = new HashMap<Beacon, Ship>();
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		// TODO Auto-generated method stub

	}


	@Override
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, AbstractAction> randomActions = new HashMap<UUID, AbstractAction>();
		
		
		for (AbstractObject actionable :  actionableObjects) {
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;
				Position currentPosition = ship.getPosition();
				UUID shipId = ship.getId();
				AbstractObject currentTarget = space.getObjectById(targetsUUID.get(shipId));

				// If we don't have a target, find one
				// If we are targeting a base and have no resources, retarget.
				if (currentTarget == null || (currentTarget instanceof Base && ship.getResources().getTotal() == 0)) {
					currentTarget = findTarget(ship, space);
					if (currentTarget != null) {
						targetsUUID.put(shipId, currentTarget.getId());
					}
				}

				// Move towards target
				// Eventually move ahead of the target based on it's velocity?
				if (currentTarget != null) {
					Position currentTargetPos = currentTarget.getPosition();
					Vector2D currentTargetVelocity = currentTarget.getPosition().getTranslationalVelocity();
					if (currentTarget.isMoveable()) {
						currentTargetPos = new Position(currentTargetPos.getX()+1.2*currentTargetVelocity.getXValue(),
								currentTargetPos.getY()+1.2*currentTargetVelocity.getYValue());
					}
					MoveAction newAction = null;
					newAction = new MoveAction(space, currentPosition, currentTargetPos);

					// lets go hella fast
					newAction.setKpRotational(7.0);
					newAction.setKvRotational(2.0 * Math.sqrt(7.0));
					newAction.setKpTranslational(1.5);
					newAction.setKvTranslational(2.0 * Math.sqrt(1.5));

					randomActions.put(ship.getId(), newAction);

					// Update graphics
					targetsPos.put(shipId, currentTargetPos);
				}
				else {
					randomActions.put(shipId, new DoNothingAction());
				}
			} 
			else {
				// it is a base and random doesn't do anything to bases
				randomActions.put(actionable.getId(), new DoNothingAction());
			}
		}
	
		return randomActions;
	}


	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
		// once a core has been picked up, remove it from the list 
		// of core being pursued (so it can be picked up at its
		// new location)
		for (AiCore core : space.getCores()) {
			if (!core.isAlive()) {
				shipToCoreMap.remove(coreToShipMap.get(core));
				coreToShipMap.remove(core);
			}
		}
		for (Beacon beacon : space.getBeacons()) {
			if (!beacon.isAlive()) {
				beaconToShipMap.remove(beacon);
			}
		}
	}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		boolean DEBUG_GRAPHICS = true;
		
		HashSet<SpacewarGraphics> newGraphics = new HashSet<SpacewarGraphics>();  
		for (Position targetPos : targetsPos.values()) {
			SpacewarGraphics graphic = new TargetGraphics(20, getTeamColor(), targetPos);
			newGraphics.add(graphic);
		}
		if (DEBUG_GRAPHICS) {
			newGraphics.addAll(drawGrid(new Position(0, 0), 40, 1080, 1600, Color.GRAY));
		}
		return newGraphics;
	}
	
	/*
	 * Draws a grid of squares of the given size, given the width and height of the grid,
	 * the position of the upper left corner of the grid, and the color of the grid lines
	 */
	private Set<SpacewarGraphics> drawGrid(Position startPos, int gridSize, int height, int width, Color color) {
		HashSet<SpacewarGraphics> grid = new HashSet<SpacewarGraphics>();
		LineGraphics line;
		// Add vertical lines
		for (int i = (int)startPos.getX(); i <= width + startPos.getX(); i += gridSize) {
			line = new LineGraphics(new Position(i, startPos.getY()), new Position(i, height + startPos.getY()), new Vector2D(0, height));
			line.setLineColor(color);
			grid.add(line);
		}
		// Add horizontal lines
		for (int i = (int)startPos.getY(); i <= height + startPos.getY(); i += gridSize) {
			line = new LineGraphics(new Position(startPos.getX(), i), new Position(width + startPos.getX(), i), new Vector2D(width, 0));
			line.setLineColor(color);
			grid.add(line);
		}
		return grid;
	}

	@Override
	/**
	 * Random never purchases 
	 */
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects, 
			ResourcePile resourcesAvailable, 
			PurchaseCosts purchaseCosts) {

		HashMap<UUID, PurchaseTypes> purchases = new HashMap<UUID, PurchaseTypes>();
		double BASE_BUYING_DISTANCE = 400;
		boolean bought_base = false;

		
		if (purchaseCosts.canAfford(PurchaseTypes.BASE, resourcesAvailable)) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Ship) {
					Ship ship = (Ship) actionableObject;
					Set<Base> bases = space.getBases();

					// how far away is this ship to a base of my team?
					double minDistance = Double.MAX_VALUE;
					for (Base base : bases) {
						if (base.getTeamName().equalsIgnoreCase(getTeamName())) {
							double distance = space.findShortestDistance(ship.getPosition(), base.getPosition());
							if (distance < minDistance) {
								minDistance = distance;
							}
						}
					}

					if (minDistance > BASE_BUYING_DISTANCE) {
						purchases.put(ship.getId(), PurchaseTypes.BASE);
						bought_base = true;
						//System.out.println("Buying a base!!");
						break;
					}
				}
			}		
		}
		return purchases;
	}

	/**
	 * This is the new way to shoot (and use any other power up once they exist)
	 */
	public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		
		HashMap<UUID, SpaceSettlersPowerupEnum> powerupMap = new HashMap<UUID, SpaceSettlersPowerupEnum>();
		
//		for (AbstractObject actionable :  actionableObjects) {
//			if (actionable instanceof Ship) {
//				Ship ship = (Ship) actionable;
//				if (random.nextDouble() < SHOOT_PROBABILITY) {
//					AbstractWeapon newBullet = ship.getNewWeapon(SpaceSettlersPowerupEnum.FIRE_MISSILE);
//					if (newBullet != null) {
//						powerupMap.put(ship.getId(), SpaceSettlersPowerupEnum.FIRE_MISSILE);
//						//System.out.println("Firing!");
//					}
//				}
//			}
//		}
		return powerupMap;
	}

	public AbstractObject findTarget(Ship ship, Toroidal2DPhysics space) {
		Set<Asteroid> asteroids = space.getAsteroids();
		AbstractObject targetObj = null;
		if (ship.getEnergy() < 2000) {
			Beacon beacon = pickNearestBeacon(space, ship);
			if (beacon != null) {
				beaconToShipMap.put(beacon, ship);
				return beacon;
			}
		}
		// Return core to base
		if (ship.getNumCores() > 0) {
			for (Base base : space.getBases()) {
				if (base.getTeam().getTeamName().equals(getTeamName())) {
					targetObj = base;
				}
			}
		} 
		// Get core
		else {
			// go find a core (or keep aiming for one if you already are)
			AbstractAction current = ship.getCurrentAction();
			if (current == null || !shipToCoreMap.containsKey(ship)) {
				AiCore core = pickNearestFreeCore(space, ship);

				if (core != null) {
					coreToShipMap.put(core, ship);
					shipToCoreMap.put(ship, core);
					return core;
				}
				else {
					Beacon beacon = pickNearestBeacon(space, ship);
					if (beacon != null) {
						beaconToShipMap.put(beacon, ship);
					}
					return beacon;
				}
			} else {
				// update the goal location since cores move
				UUID myCoreId = shipToCoreMap.get(ship).getId();
				AiCore myCore = (AiCore) space.getObjectById(myCoreId);
				return myCore;
			}
		}
		return targetObj;
	}

	/**
	 * Find the nearest beacon to this ship
	 * @param space
	 * @param ship
	 * @return
	 */
	private Beacon pickNearestBeacon(Toroidal2DPhysics space, Ship ship) {
		// get the current beacons
		Set<Beacon> beacons = space.getBeacons();

		Beacon closestBeacon = null;
		double bestDistance = Double.POSITIVE_INFINITY;

		for (Beacon beacon : beacons) {
			double dist = space.findShortestDistance(ship.getPosition(), beacon.getPosition());
			if ((dist < bestDistance) && (!beaconToShipMap.containsKey(beacon))) {
				bestDistance = dist;
				closestBeacon = beacon;
			}
		}

		return closestBeacon;
	}
	
	/**
	 * Find the nearest free core to this ship
	 * @param space
	 * @param ship
	 * @return
	 */
	private AiCore pickNearestFreeCore(Toroidal2DPhysics space, Ship ship) {
		Set<AiCore> cores = space.getCores();

		AiCore closestCore = null;
		double bestDistance = Double.POSITIVE_INFINITY;

		for (AiCore core : cores) {
			if (coreToShipMap.containsKey(core)) {
				continue;
			}

			double dist = space.findShortestDistance(ship.getPosition(), core.getPosition());
			if (dist < bestDistance) {
				bestDistance = dist;
				closestCore = core;
			}
		}

		return closestCore;
	}
}