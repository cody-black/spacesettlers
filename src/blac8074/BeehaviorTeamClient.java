package blac8074;

import java.awt.*;
import java.util.*;

import spacesettlers.actions.*;
import spacesettlers.clients.TeamClient;
import spacesettlers.graphics.*;
import spacesettlers.objects.*;
import spacesettlers.objects.powerups.*;
import spacesettlers.objects.resources.*;
import spacesettlers.objects.weapons.*;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;

/**
 * A multi-ship capture the flag team that use a BeePlanner to coordinate and assign various tasks.
 */
public class BeehaviorTeamClient extends TeamClient {
	// Size of each square in the grid (40 is the max int that works, 10 kinda works but lags, higher/lower values untested)
	static final double GRID_SIZE = 20;
	// If true, pathfinding will use A*, if false, pathfinding will use other method (hill climbing)
	static final boolean A_STAR = true;
	// The ship's path updates every this many timesteps
	static final int PATH_UPDATE_INTERVAL = 10;
	// Whether or not to draw graphics for debugging
	static final boolean DEBUG_PATH = false;
	static final boolean DEBUG_PLANNER = true;

	// When a ship picks up this many resources, it will return them to base
	static final int RETURN_RESOURCES_AMT = 500;
	
	// Values learned from project 2
	static final double TRANSLATIONAL_KP = 19.0;
	static final double ROTATIONAL_KP = 28.0;
	static final double LOW_ENERGY_THRESH = 2000;
	static final double SHOOT_ENEMY_DIST = 500;
	
	// Graph to store nodes that represent the environment
	BeeGraph graph;
	
	//For storing path graphics so we only have to create them when a new path is generated
	HashSet<SpacewarGraphics> pathGraphics;
	// Store grid graphics so we don't have to re-draw it repeatedly
	HashSet<SpacewarGraphics> gridGraphics;
	// Stores bee pursuit graphics
	HashSet<SpacewarGraphics> bpGraphics;

	// Stores planner graphics
	HashSet<SpacewarGraphics> plannerGraphics;

	// Keeps track of targets that each ship is moving to
	HashMap<Ship, AbstractObject> targets;
	// Stores the path that a ship will take to move to its target
	HashMap<Ship, ArrayList<BeeNode>> paths;
	// Ship that is being shot at by each ship
	HashMap<Ship, Ship> targetShips;
	// Bee Pursuit - used to move along paths
	HashMap<Ship, BeePursuit> beePursuits;

	// Bee Planner - keeps track of and assigns tasks to ships
	BeePlanner planner;
	
	
	@Override
	public void initialize(Toroidal2DPhysics space) {
		// Number of grid squares in x dimension
		int numSquaresX = space.getWidth() / (int)GRID_SIZE;
		// Number of grid squares in y dimension
		int numSquaresY = space.getHeight() / (int)GRID_SIZE;
		graph = new BeeGraph(numSquaresX * numSquaresY, numSquaresY, numSquaresX, GRID_SIZE);
		BeeNode node;
		for (int i = 0; i < graph.getSize(); i++) {
			int x = i % numSquaresX;
			int y = i / numSquaresX;
			Position nodePos = new Position(x * GRID_SIZE + GRID_SIZE / 2, y * GRID_SIZE + GRID_SIZE / 2);
			node = new BeeNode(nodePos);
			graph.addNode(i, node);
			//System.out.println("Created node " + i +  " at: " + node.getPosition());
		}

		// Add adjacent nodes to each node
		for (int i = 0; i < graph.getSize(); i++) {
			double distance;
			int[] adjacent = graph.findAdjacentIndices(i);
			node = graph.getNode(i);
			// Add edge costs for each adjacent node
			for (int j = 0; j < adjacent.length; j++) {
				if (j < 4) {
					distance = GRID_SIZE;
				}
				else {
					// a == b => c = sqrt(a^2 + b^2) = sqrt(2 * a^2) = sqrt(2 * a * a)
					distance = Math.sqrt(2 * GRID_SIZE * GRID_SIZE); 
				}
				node.addAdjacent(graph.getNode(adjacent[j]), distance);
				//System.out.println("Add adjacent node " + adjacent[j] + " to node " + i + " with distance " + distance);
			}
		}
		pathGraphics = new HashSet<SpacewarGraphics>();
		gridGraphics = drawGrid(new Position(0, 0), GRID_SIZE, 1080, 1600, Color.GRAY);
		beePursuits = new HashMap<Ship, BeePursuit>();
		paths = new HashMap<Ship, ArrayList<BeeNode>>();
		bpGraphics = new HashSet<>();
		targets = new HashMap<Ship, AbstractObject>();
		targetShips = new HashMap<Ship, Ship>();
		planner = new BeePlanner();
		plannerGraphics = new HashSet<>();
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		
	}

	@Override
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, AbstractAction> actions = new HashMap<UUID, AbstractAction>();		
		Ship exampleShip = null;
		for (AbstractActionableObject actionable :  actionableObjects) {
			if (actionable instanceof Ship) {
				exampleShip = (Ship) actionable;
				break;
			}
		}

		// Update obstructions frequently to make the debug graphics more responsive
		if (DEBUG_PATH) {
			bpGraphics.clear();
			pathGraphics.clear();
			// Update obstructions of first half of graph on even timesteps
			if ((space.getCurrentTimestep() & 1) == 0) {
				findObstructions(exampleShip.getRadius(), space, exampleShip.getTeamName(), 0, graph.getSize() / 2 - 1);
			}
			// Update obstructions of last half of graph on odd timesteps
			else {
				findObstructions(exampleShip.getRadius(), space, exampleShip.getTeamName(), graph.getSize() / 2, graph.getSize() - 1);
			}
		}
		// Update obstructions only once before calculating new path
		else {
			// Update obstructions of first half of graph 1 timestep before the path is calculated
			if ((space.getCurrentTimestep() % PATH_UPDATE_INTERVAL) == (PATH_UPDATE_INTERVAL - 1)) {
				findObstructions(exampleShip.getRadius(), space, exampleShip.getTeamName(), 0, graph.getSize() / 2 - 1);
			}
			// Update obstructions of last half two timesteps before the path is calculated
			else if ((space.getCurrentTimestep() % PATH_UPDATE_INTERVAL) == (PATH_UPDATE_INTERVAL - 2)) {
				findObstructions(exampleShip.getRadius(), space, exampleShip.getTeamName(), graph.getSize() / 2, graph.getSize() - 1);
			}
		}

		if (DEBUG_PLANNER) {
			plannerGraphics.clear();
		}

		boolean isSomeoneCarryingTheFlag = false;

		Map<Ship, BeePlanner.BeeTask> shipTasks = new HashMap<>();
		
		for (AbstractObject actionable :  actionableObjects) {
			// Assign actions to living ships
			if ((actionable instanceof Ship) && actionable.isAlive()) {
				Ship ship = (Ship) actionable;

				if (!beePursuits.containsKey(ship)) {
					beePursuits.put(ship, new BeePursuit());
				}

				isSomeoneCarryingTheFlag = isSomeoneCarryingTheFlag || ship.isCarryingFlag();

				BeePlanner.BeeTask task = planner.getTask(ship, space);

				if (DEBUG_PLANNER) {
					plannerGraphics.add(new TaskGraphics(ship, task));
				}

				shipTasks.put(ship, task);
			}
			else {
				actions.put(actionable.getId(), new DoNothingAction());
			}
		}

		// Apply actions after we assign tasks in the previous loop
		for (Ship ship : shipTasks.keySet()) {
			BeePlanner.BeeTask task = shipTasks.get(ship);
			switch (task) {
				case FIND_ENEMY_FLAG:
					actions.put(ship.getId(), findEnemyFlagAction(space, ship));
					break;
				case RETURN_TO_BASE:
					actions.put(ship.getId(), returnToBaseAction(space, ship));
					break;
				case FIND_ALLY_FLAG:
					actions.put(ship.getId(), findAllyFlagAction(space, ship));
					break;
				case GUARD:
					actions.put(ship.getId(), guardAction(space, ship));
					break;
				case PROTECT:
					actions.put(ship.getId(), protectAction(space, ship));
					break;
				case GET_ENERGY:
					actions.put(ship.getId(), getEnergyAction(space, ship));
					break;
				case GET_RESOURCES:
					actions.put(ship.getId(), getResourcesAction(space,ship));
					break;
				case WANDER:
					actions.put(ship.getId(), wanderAction(space, ship));
					break;
				default:
					actions.put(ship.getId(), new DoNothingAction());
			}
		}

		// Sometimes the "carrying enemy flag" flag isn't set right (usually because of death)
		// So set it correct here
		if (!isSomeoneCarryingTheFlag) {
			planner.setCarryingEnemyFlag(false);
		}
		
		// Create new path graphics
		if (DEBUG_PATH) {
			// Add green circles representing nodes on the path
			for (ArrayList<BeeNode> path : paths.values()) {
				for (BeeNode node : path) {
					pathGraphics.add(new CircleGraphics((int)GRID_SIZE / 8, Color.GREEN, node.getPosition()));
				}
			}
		}
		return actions;
	}

	/**
	 * The ship flies to the enemy flag to pick it up
	 */
	private AbstractAction findEnemyFlagAction(Toroidal2DPhysics space, Ship ship) {
		Position currentPosition = ship.getPosition();
		// Find new path every so many timesteps
		if ((space.getCurrentTimestep() % PATH_UPDATE_INTERVAL) == 0) {
			ArrayList<BeeNode> path;
			AbstractObject target = null;

			for (Flag flag : space.getFlags()) {
				if (!flag.getTeamName().equals(ship.getTeamName())) {
					target = flag;
					break;
				}
			}

			// If we have a valid target
			if (target != null) {
				targets.put(ship, target);
				Position targetPos = target.getPosition();

				// Generate path using A* algorithm
				if (A_STAR) {
					path = graph.getAStarPath(positionToNodeIndex(currentPosition), positionToNodeIndex(targetPos));
				}
				// Generate path using other algorithm (hill climbing)
				else {
					path = graph.getHillClimbingPath(positionToNodeIndex(currentPosition), positionToNodeIndex(targetPos));
				}
				beePursuits.get(ship).setPath(path);
				paths.put(ship, path);
			}
		}

		if (ship.isCarryingFlag() || planner.isCarryingEnemyFlag == true) {
			planner.setCarryingEnemyFlag(true);
			planner.finishTask(ship);
		}

		return getMoveFromBeePursuit(space, ship);
	}

	/**
	 * The ship flies to the nearest friendly base
	 */
	private AbstractAction returnToBaseAction(Toroidal2DPhysics space, Ship ship) {
		Position currentPosition = ship.getPosition();
		// Find new path every so many timesteps
		if ((space.getCurrentTimestep() % PATH_UPDATE_INTERVAL) == 0) {
			ArrayList<BeeNode> path;
			AbstractObject target = null;

			Base closestBase = pickNearestFriendlyBase(space, ship);

			if (closestBase == null) {
				// probably should never happen
				//System.out.println("No friendly bases found");
				return new DoNothingAction();
			}

			target = closestBase;

			if (space.findShortestDistance(closestBase.getPosition(), ship.getPosition()) < 50) {
				planner.finishTask(ship);
			}

			// If we have a valid target
			if (target != null) {
				targets.put(ship, target);
				Position targetPos = target.getPosition();

				// Generate path using A* algorithm
				if (A_STAR) {
					path = graph.getAStarPath(positionToNodeIndex(currentPosition), positionToNodeIndex(targetPos));
				}
				// Generate path using other algorithm (hill climbing)
				else {
					path = graph.getHillClimbingPath(positionToNodeIndex(currentPosition), positionToNodeIndex(targetPos));
				}
				beePursuits.get(ship).setPath(path);
				paths.put(ship, path);
			}
		}

		return getMoveFromBeePursuit(space, ship);
	}

	/**
	 * The ship flies to its team's flag to return it
	 */
	private AbstractAction findAllyFlagAction(Toroidal2DPhysics space, Ship ship) {
		Flag ourFlag = null;
		
		for (Flag flag : space.getFlags()) {
			if (flag.getTeamName().equals(ship.getTeamName())) {
				ourFlag = flag;
				break;
			}
		}
		
		targets.put(ship, ourFlag);
		Position currentPosition = ship.getPosition();
		Position targetPos = ourFlag.getPosition();
		ArrayList<BeeNode> path;

		// Generate path using A* algorithm
		if (A_STAR) {
			path = graph.getAStarPath(positionToNodeIndex(currentPosition), positionToNodeIndex(targetPos));
		}
		// Generate path using other algorithm (hill climbing)
		else {
			path = graph.getHillClimbingPath(positionToNodeIndex(currentPosition), positionToNodeIndex(targetPos));
		}
		beePursuits.get(ship).setPath(path);
		paths.put(ship, path);
		
		// If our flag is being carried or has been returned
		if (ourFlag.isBeingCarried() || ourFlag.getPosition().getTotalTranslationalVelocity() == 0) {
			planner.finishTask(ship);
		}
		
		return getMoveFromBeePursuit(space, ship);
	}

	/**
	 * The ship attacks the enemy ship closest to the ship's team's flag
	 */
	private AbstractAction guardAction(Toroidal2DPhysics space, Ship ship) {
		Position currentPosition = ship.getPosition();
		// Find new path every so many timesteps
		if ((space.getCurrentTimestep() % PATH_UPDATE_INTERVAL) == 0) {
			ArrayList<BeeNode> path;

			Flag ourFlag = null;

			AbstractObject target = null;

			for (Flag flag : space.getFlags()) {
				if (flag.getTeamName().equals(ship.getTeamName())) {
					ourFlag = flag;
					break;
				}
			}

			if (!ourFlag.isBeingCarried()) {
				// Target nearest enemy to our flag
				target = pickNearestEnemyToPosition(space, ship, ourFlag.getPosition());
			}
			// If our flag is being carried
			else {
				// Target flag carrier
				for (Ship otherShip : space.getShips()) {
					if (!ship.getTeamName().equals(otherShip.getTeamName())) {
						if (otherShip.isCarryingFlag())
						target = otherShip;
					}
				}
			}

			// If we have a valid target
			if (target != null) {
				targets.put(ship, target);
				Position targetPos = target.getPosition();

				if (space.findShortestDistance(ship.getPosition(), target.getPosition()) < (3 * ship.getRadius())) {
					targetPos = ship.getPosition();
				}

				// Generate path using A* algorithm
				if (A_STAR) {
					path = graph.getAStarPath(positionToNodeIndex(currentPosition), positionToNodeIndex(targetPos));
				}
				// Generate path using other algorithm (hill climbing)
				else {
					path = graph.getHillClimbingPath(positionToNodeIndex(currentPosition), positionToNodeIndex(targetPos));
				}
				beePursuits.get(ship).setPath(path);
				paths.put(ship, path);
			}
			else {
				return new DoNothingAction();
			}
		}

		// The Guard task is only valid for 1 tick
		planner.finishTask(ship);

		return getMoveFromBeePursuit(space, ship);
	}

	/**
	 * The ship attacks the ship closest to the ship on its team that is carrying the flag
	 */
	private AbstractAction protectAction(Toroidal2DPhysics space, Ship ship) {
		Position currentPosition = ship.getPosition();
		// Find new path every so many timesteps
		if ((space.getCurrentTimestep() % PATH_UPDATE_INTERVAL) == 0) {
			ArrayList<BeeNode> path;

			AbstractObject target = null;

			for (Ship otherShip : space.getShips()) {
				if (ship.getTeamName().equals(otherShip.getTeamName())) {
					if (otherShip.isCarryingFlag()) {
						// Target enemy nearest to flag carrier
						target = pickNearestEnemyToPosition(space, ship, otherShip.getPosition());
					}
				}
			}
			
			// If we have a valid target
			if (target != null) {
				targets.put(ship, target);
				Position targetPos = target.getPosition();

				if (space.findShortestDistance(ship.getPosition(), target.getPosition()) < (3 * ship.getRadius())) {
					targetPos = ship.getPosition();
				}

				// Generate path using A* algorithm
				if (A_STAR) {
					path = graph.getAStarPath(positionToNodeIndex(currentPosition), positionToNodeIndex(targetPos));
				}
				// Generate path using other algorithm (hill climbing)
				else {
					path = graph.getHillClimbingPath(positionToNodeIndex(currentPosition), positionToNodeIndex(targetPos));
				}
				beePursuits.get(ship).setPath(path);
				paths.put(ship, path);
			}
			else {
				return new DoNothingAction();
			}
		}

		// The Protect task is only valid for 1 tick
		planner.finishTask(ship);

		return getMoveFromBeePursuit(space, ship);
	}
	
	/**
	 * The ship flies to the closest beacon or base with enough energy
	 */
	private AbstractAction getEnergyAction(Toroidal2DPhysics space, Ship ship) {
		// Find new path every so many timesteps
		if ((space.getCurrentTimestep() % PATH_UPDATE_INTERVAL) == 0) {
			ArrayList<BeeNode> path;

			targets.remove(ship);
			
			AbstractObject closestEnergy = pickNearestUnreservedEnergy(space, ship);

			if (closestEnergy == null) {
				// No beacons or bases with enough energy available
				return new DoNothingAction();
			}
			else {
				targets.put(ship, closestEnergy);
				Position currentPos = ship.getPosition();
				Position targetPos = closestEnergy.getPosition();

				// Generate path using A* algorithm
				if (A_STAR) {
					path = graph.getAStarPath(positionToNodeIndex(currentPos), positionToNodeIndex(targetPos));
				}
				// Generate path using other algorithm (hill climbing)
				else {
					path = graph.getHillClimbingPath(positionToNodeIndex(currentPos), positionToNodeIndex(targetPos));
				}
				beePursuits.get(ship).setPath(path);
				paths.put(ship, path);
			}
		}
		
		// Finish task when ship has enough energy
		if (ship.getEnergy() > LOW_ENERGY_THRESH) {
			planner.finishTask(ship);
		}
		
		return getMoveFromBeePursuit(space, ship);
	}
	
	/**
	 * The ship flies to the closest resource asteroid
	 */
	private AbstractAction getResourcesAction(Toroidal2DPhysics space, Ship ship) {
		// Find new path every so many timesteps
		if ((space.getCurrentTimestep() % PATH_UPDATE_INTERVAL) == 0) {
			ArrayList<BeeNode> path;

			Asteroid closestResources = pickNearestMineableAsteroid(space, ship);

			if (closestResources != null) {
				targets.put(ship, closestResources);
				Position currentPos = ship.getPosition();
				Position targetPos = closestResources.getPosition();

				// Generate path using A* algorithm
				if (A_STAR) {
					path = graph.getAStarPath(positionToNodeIndex(currentPos), positionToNodeIndex(targetPos));
				}
				// Generate path using other algorithm (hill climbing)
				else {
					path = graph.getHillClimbingPath(positionToNodeIndex(currentPos), positionToNodeIndex(targetPos));
				}
				beePursuits.get(ship).setPath(path);
				paths.put(ship, path);
			}
		}
		
		// Finish task when ship has picked up "enough" resources or is low on energy
		if (ship.getResources().getTotal() > RETURN_RESOURCES_AMT || ship.getEnergy() < LOW_ENERGY_THRESH) {
			planner.finishTask(ship);
		}
		
		return getMoveFromBeePursuit(space, ship);
	}

	private AbstractAction wanderAction(Toroidal2DPhysics space, Ship ship) {
		Position currentPosition = ship.getPosition();
		// Find new path every so many timesteps
		if ((space.getCurrentTimestep() % PATH_UPDATE_INTERVAL) == 0) {
			ArrayList<BeeNode> path;
			AbstractObject target = findTarget(ship, space);
			// If we have a valid target
			if (target != null) {
				targets.put(ship, target);
				Position targetPos = target.getPosition();
				// Avoid crashing into the target ship
				if (target instanceof Ship) {
					if (space.findShortestDistance(ship.getPosition(), target.getPosition()) < (3 * ship.getRadius())) {
						targetPos = ship.getPosition();
					}
				}
				else {
					// Aim ahead of a moving target
					if (target.isMoveable()) {
						targetPos = new Position(targetPos.getX() + targetPos.getxVelocity(), targetPos.getY() + targetPos.getyVelocity());
						space.toroidalWrap(targetPos);
					}
				}

				// Generate path using A* algorithm
				if (A_STAR) {
					path = graph.getAStarPath(positionToNodeIndex(currentPosition), positionToNodeIndex(targetPos));
				}
				// Generate path using other algorithm (hill climbing)
				else {
					path = graph.getHillClimbingPath(positionToNodeIndex(currentPosition), positionToNodeIndex(targetPos));
				}
				beePursuits.get(ship).setPath(path);
				paths.put(ship, path);
			}
			else {
				return new DoNothingAction();
			}
		}

		// The wandering task is only valid for 1 tick
		planner.finishTask(ship);

		return getMoveFromBeePursuit(space, ship);
	}

	public AbstractAction getMoveFromBeePursuit(Toroidal2DPhysics space, Ship ship) {
		// Always make a move based on last path
		Position currentPosition = ship.getPosition();

		double radius = 2.0 * GRID_SIZE;
		Position goalPos = beePursuits.get(ship).getDesiredPosition(space, ship.getPosition(), radius);

		// Expand radius if we dont find anything
		int iters = 0;
		while (goalPos == null && iters < 20) {
			iters++;
			radius *= 1.25;
			goalPos = beePursuits.get(ship).getDesiredPosition(space, ship.getPosition(), radius);
		}

		// If we still couldn't find path
		if (iters >= 20) {
			return new DoNothingAction();
		}

		// Add a target graphic at our goal position
		bpGraphics.add(new TargetGraphics(16, Color.PINK, goalPos));

		// If the ship is allowed to shoot, find an enemy ship to shoot at
		targetShips.put(ship, null);
		if (planner.shipCanShoot(ship)) {
			// Find the closest enemy ship within a certain distance
			double shipDistance = SHOOT_ENEMY_DIST;
			for (Ship otherShip : space.getShips()) {
				// If the other ship is an enemy ship and is alive
				if (!otherShip.getTeamName().equals(ship.getTeamName()) && otherShip.isAlive()) {
					// Get distance without toroidal wrap because toroidal shooting is hard
					double distance = new Vector2D(currentPosition).subtract(new Vector2D(otherShip.getPosition())).getMagnitude();
					if (distance < shipDistance) {
						targetShips.put(ship, otherShip);
						shipDistance = distance;
					}
				}
			}
		}
		
		MoveActionWithOrientation action;
		// If there's no ship to target, don't worry about orientation
		if (targetShips.get(ship) == null) {
			action = new MoveActionWithOrientation(space, ship.getPosition(), goalPos);
			action.setKpRotational(0.0);
			action.setKvRotational(0.0);
		}
		// If there is an enemy ship to target, point at the enemy ship
		else {
			Position targetShipPos = targetShips.get(ship).getPosition();
			goalPos.setOrientation(new Vector2D(targetShipPos.getX() - currentPosition.getX(),
					targetShipPos.getY() - currentPosition.getY()).getAngle());
			action = new MoveActionWithOrientation(space, ship.getPosition(), goalPos);
			action.setKpRotational(ROTATIONAL_KP);
			action.setKvRotational(2 * Math.sqrt(ROTATIONAL_KP));
		}

		action.setKpTranslational(TRANSLATIONAL_KP);
		action.setKvTranslational(2 * Math.sqrt(TRANSLATIONAL_KP));

		return action;
	}

	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
	}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		HashSet<SpacewarGraphics> graphics = new HashSet<SpacewarGraphics>();
		if (DEBUG_PATH) {
			// Draw grid on screen
			graphics.addAll(gridGraphics);
			// Draw red circles representing each obstructed node
			for (int i = 0; i < graph.getSize(); i++) {
				if (graph.getNode(i).getObstructed()) {
					graphics.add(new CircleGraphics((int)GRID_SIZE / 8, Color.RED, graph.getNode(i).getPosition()));
				}
			}
			// Add the graphics representing the generated path
			graphics.addAll(pathGraphics);
			// Add graphics showing where on the path we are aiming
			graphics.addAll(bpGraphics);
		}

		if (DEBUG_PLANNER) {
			// Draw current tasks
			graphics.addAll(plannerGraphics);
		}
		
		return graphics;
	}


	@Override
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects, 
			ResourcePile resourcesAvailable, 
			PurchaseCosts purchaseCosts) {

		HashMap<UUID, PurchaseTypes> purchases = new HashMap<UUID, PurchaseTypes>();
		double BASE_BUYING_DISTANCE = 400;
		
		// Buy a new ship if we can afford it
		if (purchaseCosts.canAfford(PurchaseTypes.SHIP, resourcesAvailable)) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Base) {
					Base base = (Base) actionableObject;
					purchases.put(base.getId(), PurchaseTypes.SHIP);
					break;
				}
			}
		}

		// Buy a double max energy powerup if we can afford it
		if (purchaseCosts.canAfford(PurchaseTypes.POWERUP_DOUBLE_MAX_ENERGY, resourcesAvailable)) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Ship) {
					Ship ship = (Ship) actionableObject;
					purchases.put(ship.getId(), PurchaseTypes.POWERUP_DOUBLE_MAX_ENERGY);
				}
			}
		}
		
		// Buy a new base if we can afford it
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
					// Only buy a base if it's far enough from existing bases
					if (minDistance > BASE_BUYING_DISTANCE) {
						purchases.put(ship.getId(), PurchaseTypes.BASE);
						//System.out.println("Buying a base!!");
						break;
					}
				}
			}		
		}
		return purchases;
	}

	@Override
	public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		
		HashMap<UUID, SpaceSettlersPowerupEnum> powerUps = new HashMap<UUID, SpaceSettlersPowerupEnum>();
		
		// If we have a power up for doubling max energy, use it
		for (AbstractActionableObject actionableObject : actionableObjects) {
			if (actionableObject instanceof Ship) {
				Ship ship = (Ship) actionableObject;
				if (ship.isValidPowerup(SpaceSettlersPowerupEnum.DOUBLE_MAX_ENERGY)) {
					powerUps.put(ship.getId(), SpaceSettlersPowerupEnum.DOUBLE_MAX_ENERGY);
				}
			}
		}
		
		// Potentially fire every 3 timesteps
		if ((space.getCurrentTimestep() % 3) == 0) {
			for (AbstractActionableObject actionableObject : actionableObjects){
				if (actionableObject instanceof Ship) {
					Ship ship = (Ship) actionableObject;
					if (targetShips.get(ship) != null) {
						Position shipPos = ship.getPosition();
						Position targetShipPos = targetShips.get(ship).getPosition();
						
						// Add things that we don't want to shoot to shootObstructions
						Set<AbstractObject> shootObstructions = new HashSet<AbstractObject>();
						// Add asteroids to obstructions
						shootObstructions.addAll(space.getAsteroids());
						for (Ship otherShip : space.getShips()) {
							// Add other friendly ships to obstructions
							if (ship.getTeamName().equals(otherShip.getTeamName()) && ship.getId() != otherShip.getId()) {
								shootObstructions.add(otherShip);
							}
						}
						for (Base base : space.getBases()) {
							// Add friendly bases to obstructions
							if (base.getTeamName().equals(ship.getTeamName())) {
								shootObstructions.add(base);
							}
						}

						// If there's nothing to block our missiles between us and the target
						if (space.isPathClearOfObstructions(shipPos, targetShipPos, shootObstructions, ship.getRadius())) {
							// Calculate the difference between the ship's current orientation and the orientation it needs to face the targeted ship
							double angleDiff = actionableObject.getPosition().getOrientation() - new Vector2D(new Position(targetShipPos.getX() - shipPos.getX(),  
									targetShipPos.getY() - shipPos.getY())).getAngle();
							angleDiff = (angleDiff + Math.PI) % (2 * Math.PI) - Math.PI;
							// If the ship is basically pointing at the targeted ship, fire missiles
							if (Math.abs(angleDiff) < 0.2) {
								SpaceSettlersPowerupEnum powerup = SpaceSettlersPowerupEnum.FIRE_MISSILE;
								powerUps.put(actionableObject.getId(), powerup);
							}
						}
					}
				}
			}
		}
		
		return powerUps;
	}
	
	/*
	 * Find what object the ship should aim for
	 */
	public AbstractObject findTarget(Ship ship, Toroidal2DPhysics space) {
		AbstractObject targetObj = null;
		// We are low on energy -> go get energy
		if (ship.getEnergy() < LOW_ENERGY_THRESH) {
			AbstractObject energy = pickNearestUnreservedEnergy(space, ship);
			if (energy != null) {
				return energy;
			}
		}
		// We don't have a core -> check for cores
		if (ship.getNumCores() == 0) {
			AiCore core = pickNearestCore(space, ship);
			// There is a core -> go get it
			if (core != null) {
				return core;
			}
			// There isn't a core -> 
			else {
				// The ship is a little heavy (lots of resources) -> return to base
				if (ship.getMass() > 350) {
					Base base = pickNearestFriendlyBase(space, ship);
					if (base != null) {
						return base;
					}
				}
				// Chase an enemy ship
				Ship enemy = pickNearestEnemy(space, ship);
				if (enemy != null) {
					return enemy;
				}
			}
		} 
		// Return core to base
		else {
			AiCore core = pickNearestCore(space, ship);
			// There isn't another core or there is another core but we are low on energy -> go to base
			if ((core == null) || (ship.getEnergy() < LOW_ENERGY_THRESH)) {
				Base base = pickNearestFriendlyBase(space, ship);
				if (base != null) {
					return base;
				}
			}
			// There are multiple cores and we have enough energy -> go get another core
			else {
				return core;
			}
		}
		return targetObj;
	}
	
	/**
	 * Find the nearest beacon or base with 2000 or more energy and is not being targeted by another ship
	 */
	private AbstractObject pickNearestUnreservedEnergy(Toroidal2DPhysics space, Ship ship) {
		// get the current beacons
		Set<Beacon> beacons = space.getBeacons();

		AbstractObject closestEnergy = null;
		double bestDistance = Double.POSITIVE_INFINITY;
		double dist;
		for (Beacon beacon : beacons) {
			// Ignore beacons that are targeted by another ship
			if (!targets.containsValue(beacon)) {
				dist = getPathLength(space, graph.getAStarPath(positionToNodeIndex(ship.getPosition()), positionToNodeIndex(beacon.getPosition())));
				if (dist < bestDistance) {
					bestDistance = dist;
					closestEnergy = beacon;
				}
			}
		}
		if (targets.get(ship) instanceof Beacon && targets.get(ship).isAlive()) {
			dist = getPathLength(space, graph.getAStarPath(positionToNodeIndex(ship.getPosition()), positionToNodeIndex(targets.get(ship).getPosition())));
			if (dist < bestDistance) {
				bestDistance = dist;
				closestEnergy = targets.get(ship);
			}
		}
		for (Base base : space.getBases()) {
			if (base.getTeamName().equalsIgnoreCase(ship.getTeamName())) {
				if (base.getEnergy() > 2000) {
					// Ignore bases that are being targeted by another ship
					if (!targets.containsValue(base)) {
						dist = getPathLength(space, graph.getAStarPath(positionToNodeIndex(ship.getPosition()), positionToNodeIndex(base.getPosition())));
						if (dist < bestDistance) {
							bestDistance = dist;
							closestEnergy = base;
						}
					}
				}
			}
		}
		if (targets.get(ship) instanceof Base && ((Base)targets.get(ship)).getEnergy() > 2000 && targets.get(ship).isAlive()) {
			dist = getPathLength(space, graph.getAStarPath(positionToNodeIndex(ship.getPosition()), positionToNodeIndex(targets.get(ship).getPosition())));
			if (dist < bestDistance) {
				bestDistance = dist;
				closestEnergy = targets.get(ship);
			}
		}
		
		return closestEnergy;
	}
	
	/**
	 * Find the nearest friendly base
	 */
	private Base pickNearestFriendlyBase(Toroidal2DPhysics space, Ship ship) {
		Set<Base> bases = space.getBases();

		Base closestBase = null;
		double bestDistance = Double.POSITIVE_INFINITY;
		
		for (Base base : bases) {
			// Check if the base belongs to our team
			if (base.getTeamName().equalsIgnoreCase(ship.getTeamName())) {
				double dist = getPathLength(space, graph.getAStarPath(positionToNodeIndex(ship.getPosition()), positionToNodeIndex(base.getPosition())));
				if (dist < bestDistance) {
					bestDistance = dist;
					closestBase = base;
				}
			}
		}
		return closestBase;
	}
	
	/**
	 * Find the nearest AiCore
	 */
	private AiCore pickNearestCore(Toroidal2DPhysics space, Ship ship) {
		Set<AiCore> cores = space.getCores();

		AiCore closestCore = null;
		double bestDistance = Double.POSITIVE_INFINITY;

		for (AiCore core : cores) {
			double dist = space.findShortestDistance(ship.getPosition(), core.getPosition());
			if (dist < bestDistance) {
				bestDistance = dist;
				closestCore = core;
			}
		}

		return closestCore;
	}
	
	/**
	 * Find the nearest enemy ship
	 */
	private Ship pickNearestEnemy(Toroidal2DPhysics space, Ship ship) {
		return pickNearestEnemyToPosition(space, ship, ship.getPosition());
	}

	/**
	 * Find the nearest enemy ship to a position
	 */
	private Ship pickNearestEnemyToPosition(Toroidal2DPhysics space, Ship ship, Position position) {
		double bestDistance = Double.POSITIVE_INFINITY;
		Ship closestEnemy = null;
		for (Ship otherShip : space.getShips()) {
			// If the other ship is a living enemy ship
			if (!otherShip.getTeamName().equalsIgnoreCase(ship.getTeamName()) && otherShip.isAlive()) {
				// Get distance without toroidal wrap because toroidal shooting is hard
				double distance = space.findShortestDistance(position, otherShip.getPosition());
				if (distance < bestDistance) {
					bestDistance = distance;
					closestEnemy = otherShip;
				}
			}
		}
		return closestEnemy;
	}
	
	/**
	 * Find the nearest mineable asteroid
	 */
	private Asteroid pickNearestMineableAsteroid(Toroidal2DPhysics space, Ship ship) {
		Asteroid closestMineableAsteroid = null;
		double bestDistance = Double.POSITIVE_INFINITY;
		for (Asteroid asteroid : space.getAsteroids()) {
			if (asteroid.isMineable()) {
				double dist = space.findShortestDistance(ship.getPosition(), asteroid.getPosition());
				if (dist < bestDistance) {
					bestDistance = dist;
					closestMineableAsteroid = asteroid;
				}
			}
		}
		return closestMineableAsteroid;
	}
	
	/**
	 * Draws a grid (using lines) of squares of the given size, given the width and height of the grid,
	 * the position of the upper left corner of the grid, and the color of the grid lines
	 */
	private HashSet<SpacewarGraphics> drawGrid(Position startPos, double gridSize, int height, int width, Color color) {
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
	
	private double getPathLength(Toroidal2DPhysics space, ArrayList<BeeNode> path) {
		double length = 0;
		for (int i = 0; i < path.size() - 1; i++) {
			length += space.findShortestDistance(path.get(i).getPosition(), path.get(i + 1).getPosition());
		}
		return length;
	}

	/**
	 * Converts a position to the index of the grid square that contains that position
	 */
	private int positionToNodeIndex(Position position) {
		int x = (int)(position.getX() / GRID_SIZE);
		int y = (int)(position.getY() / GRID_SIZE);
		return x + y * graph.getWidth();
	}

	/**
	 * Check each grid square to see if it contains an obstacle or is near enough to an obstacle
	 * If it is, obstruct the corresponding node
	 * If not, unobstruct the corresponding node
	 *
	 * Obstacles currently include: non-mineable asteroids, bases, enemy ships, weapon projectiles
	 */
	public void findObstructions(int radius, Toroidal2DPhysics space, String teamName, int startIndex, int stopIndex) {
		// Check the selected nodes for obstructions
		for (int nodeIndex = startIndex; nodeIndex <= stopIndex; nodeIndex++) {
			boolean obstructionFound = false;
			for (Asteroid asteroid : space.getAsteroids()) {
				// Check for non-mineable asteroids
				if (!asteroid.isMineable()) {
					if (space.findShortestDistanceVector(asteroid.getPosition(), graph.getNode(nodeIndex).getPosition())
							.getMagnitude() <= (2 * radius + asteroid.getRadius())) {
						graph.obstructNode(nodeIndex);
						obstructionFound = true;
						break;
					}
					// Check if asteroid is moving towards the current node
					if (asteroid.isMoveable()) {
						// Note: this may not completely work if the asteroid is moving very fast
						Position futurePos = new Position(asteroid.getPosition().getX() + asteroid.getPosition().getxVelocity(),
								asteroid.getPosition().getY() + asteroid.getPosition().getyVelocity());
						if (space.findShortestDistanceVector(futurePos, graph.getNode(nodeIndex).getPosition())
								.getMagnitude() <= (2 * radius + asteroid.getRadius())) {
							graph.obstructNode(nodeIndex);
							obstructionFound = true;
							break;
						}
					}
				}
			}
			if (!obstructionFound) {
				// Check for bases
				for (Base base : space.getBases()) {
					// Don't obstruct around the base our ship is heading to
					if (!targets.containsValue(base)) {
						if (space.findShortestDistanceVector(base.getPosition(), graph.getNode(nodeIndex).getPosition())
								.getMagnitude() <= (radius + (2 * base.getRadius()))) {
							graph.obstructNode(nodeIndex);
							obstructionFound = true;
							break;
						}
					}
				}
			}
			if (!obstructionFound && (teamName != null)) {
				// Check for living enemy ships
				for (Ship ship : space.getShips()) {
					if (!ship.getTeamName().equals(teamName) && ship.isAlive()) {
						// If the enemy ship isn't being chased by our ship
						if (!targets.containsValue(ship)) {
							if (space.findShortestDistanceVector(ship.getPosition(), graph.getNode(nodeIndex).getPosition())
									.getMagnitude() <= (radius + (2 * ship.getRadius()))) {
								graph.obstructNode(nodeIndex);
								obstructionFound = true;
								break;
							}
						}
					}
				}
			}
			// Check for weapon projectiles
			if (!obstructionFound) {
				for (AbstractWeapon weapon : space.getWeapons()) {
					// Don't obstruct nodes based on our own bullets because otherwise ships get confused when they shoot in front of themselves
					if (!weapon.getFiringShip().getTeamName().equalsIgnoreCase(teamName)) {
						if (space.findShortestDistanceVector(weapon.getPosition(), graph.getNode(nodeIndex).getPosition())
								.getMagnitude() <= (radius + (2 * weapon.getRadius()))) {
							graph.obstructNode(nodeIndex);
							obstructionFound = true;
							break;
						}
					}
				}
			}
			// No obstructions were found for the current node
			if (!obstructionFound) {
				graph.unobstructNode(nodeIndex);
			}
		}
	}
}