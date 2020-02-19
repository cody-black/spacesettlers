package blac8074;

import java.awt.Color;
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

public class BeehaviorTeamClient extends TeamClient {
	// Size of each square in the grid (40 is the max int that works, 10 kinda works but lags, higher/lower values untested)
	static final double GRID_SIZE = 20;
	// If true, pathfinding will use A*, if false, pathfinding will use other method (hill climbing)
	static final boolean A_STAR = true;
	// The ship's path updates every this many timesteps
	static final int PATH_UPDATE_INTERVAL = 20;
	// Whether or not to draw graphics for debugging pathfinding
	static final boolean DEBUG_GRAPHICS = true;
	
	BeeGraph graph;
	
	HashSet<SpacewarGraphics> pathGraphics;
	HashSet<SpacewarGraphics> gridGraphics;
	HashMap<Ship, ArrayList<BeeNode>> paths;
	PurePursuit pp;
	
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
		// Store grid graphics so we don't have to re-draw it repeatedly
		gridGraphics = drawGrid(new Position(0, 0), GRID_SIZE, 1080, 1600, Color.GRAY);
		pp = new PurePursuit();
		paths = new HashMap<Ship, ArrayList<BeeNode>>();
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, AbstractAction> actions = new HashMap<UUID, AbstractAction>();		
		// TODO: is there some other way to get our team name other than from a ship?
		Ship ship = null;
		for (AbstractObject actionable :  actionableObjects) {
			if (actionable instanceof Ship) {
				ship = (Ship) actionable;
				break;
			}
		}
		// Update obstructions of first half of graph on even timesteps
		if ((space.getCurrentTimestep() & 1) == 0) {
			findObstructions(ship.getRadius() / 2, space, ship.getTeamName(), 0, graph.getSize() / 2 - 1);
		}
		// Update obstructions of last half of graph on odd timesteps
		else {
			findObstructions(ship.getRadius() / 2, space, ship.getTeamName(), graph.getSize() / 2, graph.getSize() - 1);
		}
		
		for (AbstractObject actionable :  actionableObjects) {
			// Assign actions to living ships
			if ((actionable instanceof Ship) && actionable.isAlive()) {
				ship = (Ship) actionable;
				Position currentPosition = ship.getPosition();
				// Find new path every so many timesteps
				if ((space.getCurrentTimestep() % PATH_UPDATE_INTERVAL) == 0) {
					ArrayList<BeeNode> path;
					Position targetPos = findTarget(ship, space).getPosition();
					// Generate path using selected algorithm
					if (A_STAR) {
						path = graph.getAStarPath(positionToNodeIndex(currentPosition), positionToNodeIndex(targetPos));
					}
					else {
						path = graph.getHillClimbingPath(positionToNodeIndex(currentPosition), positionToNodeIndex(targetPos));
					}
					// Create new path graphics
					if (DEBUG_GRAPHICS) {
						pathGraphics.clear();
						// Add green circles representing nodes on the path
						for (BeeNode node : path) {
							pathGraphics.add(new CircleGraphics((int)GRID_SIZE / 8, Color.GREEN, node.getPosition()));
						}
					}
					pp.setPath(path);
					paths.put(ship, path);				
				}

				// Always move based on last path
				// TODO: Handle multiple agents in the future (multiple PurePursuits)
				Position goalPos = pp.getLookaheadPoint(space, ship.getPosition(), 1.5 * GRID_SIZE);

				MoveAction action = new MoveAction(space, ship.getPosition(), goalPos);

				action.setKpRotational(25.0);
				action.setKvRotational(2.0 * Math.sqrt(25.0));
				action.setKpTranslational(9.0);
				action.setKvTranslational(2.2 * Math.sqrt(9.0));

				actions.put(actionable.getId(), action);
			}
			else {
				actions.put(actionable.getId(), new DoNothingAction());
			}
		}
		return actions;
	}

	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {

	}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		HashSet<SpacewarGraphics> graphics = new HashSet<SpacewarGraphics>();
		if (DEBUG_GRAPHICS) {
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
		}
		return graphics;
	}


	@Override
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects, 
			ResourcePile resourcesAvailable, 
			PurchaseCosts purchaseCosts) {
		// TODO Auto-generated method stub
		return new HashMap<UUID,PurchaseTypes>();
	}

	@Override
	public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public AbstractObject findTarget(Ship ship, Toroidal2DPhysics space) {
		Set<Beacon> beacons = space.getBeacons();

		Beacon closestBeacon = null;
		double bestDistance = Double.POSITIVE_INFINITY;

		// Find the closest beacon
		for (Beacon beacon : beacons) {
			double dist = space.findShortestDistance(ship.getPosition(), beacon.getPosition());
			if (dist < bestDistance) {
				bestDistance = dist;
				closestBeacon = beacon;
			}
		}
		return closestBeacon;
	}
	
	/*
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
	
	// Converts a position to the index of the grid square that contains that position
	private int positionToNodeIndex(Position position) {
		int x = (int)(position.getX() / GRID_SIZE);
		int y = (int)(position.getY() / GRID_SIZE);
		return x + y * graph.getWidth();
	}

	public void findObstructions(int radius, Toroidal2DPhysics space, String teamName, int startIndex, int stopIndex) {
		// Check the selected nodes for obstructions
		for (int nodeIndex = startIndex; nodeIndex <= stopIndex; nodeIndex++) {
			boolean obstructionFound = false;
			for (Asteroid asteroid : space.getAsteroids()) {
				// Check for non-mineable asteroids
				if (!asteroid.isMineable()) {
					if (space.findShortestDistanceVector(asteroid.getPosition(), graph.getNode(nodeIndex).getPosition())
							.getMagnitude() <= (radius + (2 * asteroid.getRadius()))) {
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
								.getMagnitude() <= (radius + (2 * asteroid.getRadius()))) {
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
					if (space.findShortestDistanceVector(base.getPosition(), graph.getNode(nodeIndex).getPosition())
							.getMagnitude() <= (radius + (2 * base.getRadius()))) {
						graph.obstructNode(nodeIndex);
						obstructionFound = true;
						break;
					}
				}
			}
			if (!obstructionFound && (teamName != null)) {
				// Check for enemy ships
				for (Ship ship : space.getShips()) {
					if (!ship.getTeamName().equals(teamName)) {
						if (space.findShortestDistanceVector(ship.getPosition(), graph.getNode(nodeIndex).getPosition())
								.getMagnitude() <= (radius + (2 * ship.getRadius()))) {
							graph.obstructNode(nodeIndex);
							obstructionFound = true;
							break;
						}
					}
				}
			}
			// Check for weapon projectiles
			if (!obstructionFound) {
				for (AbstractWeapon weapon : space.getWeapons()) {
					if (space.findShortestDistanceVector(weapon.getPosition(), graph.getNode(nodeIndex).getPosition())
							.getMagnitude() <= (radius + (2 * weapon.getRadius()))) {
						graph.obstructNode(nodeIndex);
						obstructionFound = true;
						break;
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
