package zeml9929;

import spacesettlers.actions.*;
import spacesettlers.clients.TeamClient;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.graphics.TargetGraphics;
import spacesettlers.objects.*;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

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

	@Override
	public void initialize(Toroidal2DPhysics space) {
		graphics = new HashSet<>();
		targetsPos = new HashMap<>();
		targetsUUID = new HashMap<>();
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
					targetsUUID.put(shipId, currentTarget.getId());
				}

				// Move towards target
				// Eventually move ahead of the target based on it's velocity?
				if (currentTarget != null) {
					Position currentTargetPos = currentTarget.getPosition();
					MoveAction newAction = null;
					newAction = new MoveAction(space, currentPosition, currentTargetPos);

					// lets go hella fast
					newAction.setKpRotational(10.0);
					newAction.setKvRotational(2.0 * Math.sqrt(10.0));
					newAction.setKpTranslational(2.0);
					newAction.setKvTranslational(2.0 * Math.sqrt(2.0));

					randomActions.put(ship.getId(), newAction);

					// Update graphics
					targetsPos.put(shipId, currentTargetPos);
				}
			} else {
				// it is a base and random doesn't do anything to bases
				randomActions.put(actionable.getId(), new DoNothingAction());
		}
			

		}
	
		return randomActions;
	
	}


	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
	}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		HashSet<SpacewarGraphics> newGraphics = new HashSet<SpacewarGraphics>();  
		for (Position targetPos : targetsPos.values()) {
			SpacewarGraphics graphic = new TargetGraphics(20, getTeamColor(), targetPos);
			newGraphics.add(graphic);
		}
		return newGraphics;
	}


	@Override
	/**
	 * Random never purchases 
	 */
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects, 
			ResourcePile resourcesAvailable, 
			PurchaseCosts purchaseCosts) {
		return new HashMap<UUID,PurchaseTypes>();

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
		Set<Asteroid> astroids = space.getAsteroids();
		AbstractObject targetObj = null;
		if (ship.getResources().getTotal() == 0) {
			int bestDistance = Integer.MAX_VALUE;
			for (Asteroid ast : astroids) {
				int distance = (int) space.findShortestDistance(ship.getPosition(), ast.getPosition());
				if (ast.isMineable() && distance < bestDistance) {
					bestDistance = distance;
					targetObj = ast;
				}
			}
		} else {
			for (Base base : space.getBases()) {
				if (base.getTeam().getTeamName().equals(getTeamName())) {
					targetObj = base;
				}
			}
		}
		return targetObj;
	}

}