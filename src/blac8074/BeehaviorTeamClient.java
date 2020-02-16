package blac8074;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.clients.TeamClient;
import spacesettlers.graphics.*;
import spacesettlers.objects.AbstractActionableObject;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.objects.weapons.AbstractWeapon;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;

import blac8074.BeeGraph;


public class BeehaviorTeamClient extends TeamClient {
	BeeGraph graph;
	static double GRID_SIZE = 40;
	HashMap<AbstractObject, Integer> obstacleMap;
	HashSet<SpacewarGraphics> newGraphics;
	
	@Override
	public void initialize(Toroidal2DPhysics space) {
		int numSquaresX = space.getWidth() / (int)GRID_SIZE;
		int numSquaresY = space.getHeight() / (int)GRID_SIZE;
		graph = new BeeGraph(numSquaresX * numSquaresY, numSquaresY, numSquaresX, GRID_SIZE);
		BeeNode node;
		Position position;
		for (int i = 0; i < graph.getSize(); i++) {
			int x = i % numSquaresX;
			int y = i / numSquaresX;
			position = new Position(x * GRID_SIZE + GRID_SIZE / 2, y * GRID_SIZE + GRID_SIZE / 2);
			node = new BeeNode(position);
			graph.addNode(i, node);
			//System.out.println("Created node " + i +  " at: " + node.getPosition());
		}

		int[] adjacent;
		double distance;
		for (int i = 0; i < graph.getSize(); i++) {
			adjacent = graph.findAdjacentIndices(i);
			node = graph.getNode(i);
			for (int j = 0; j < 8; j++) {
				if (j < 4) {
					distance = GRID_SIZE;
				}
				else {
					distance = Math.sqrt(2 * GRID_SIZE * GRID_SIZE); 
				}
				node.addAdjacent(graph.getNode(adjacent[j]), distance);
				//System.out.println("Add adjacent node " + adjacent[j] + " to node " + i + " with distance " + distance);
			}
		}
		newGraphics = new HashSet<SpacewarGraphics>();
		obstacleMap = new HashMap<AbstractObject, Integer>();
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, AbstractAction> actions = new HashMap<UUID, AbstractAction>();		
		int nodeIndex;
		// TODO: allow obstacles to take up multiple grid squares...somehow
		if ((space.getCurrentTimestep() % 10) == 0) {
			for (Asteroid asteroid : space.getAsteroids()) {
				if (!asteroid.isMineable()) {
					nodeIndex = positionToNodeIndex(asteroid.getPosition());
					if (!obstacleMap.containsKey(asteroid)) {
						obstacleMap.put(asteroid, nodeIndex);
						graph.obstructNode(nodeIndex);
					}
					else {
						// The asteroid has moved into a new grid square
						if (obstacleMap.get(asteroid) != nodeIndex) {
							graph.unobstructNode(obstacleMap.get(asteroid));
							graph.obstructNode(nodeIndex);
							obstacleMap.put(asteroid, nodeIndex);
						}
						// The asteroid hasn't moved, make sure the node is still obstructed
						else {
							graph.obstructNode(nodeIndex);
						}	
					}
				}
			}
		}
		for (AbstractWeapon weapon : space.getWeapons()) {
			nodeIndex = positionToNodeIndex(weapon.getPosition());
			if (!obstacleMap.containsKey(weapon)) {
				obstacleMap.put(weapon, nodeIndex);
				graph.obstructNode(nodeIndex);
			}
			// The weapon has moved into a new grid square
			else if (obstacleMap.get(weapon) != nodeIndex) {
				graph.unobstructNode(obstacleMap.get(weapon));
				graph.obstructNode(nodeIndex);
				obstacleMap.put(weapon, nodeIndex);
			}
		}
		
		for (AbstractObject actionable :  actionableObjects) {
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;
				Position currentPosition = ship.getPosition();
				nodeIndex = positionToNodeIndex(currentPosition);
				newGraphics.add(new CircleGraphics((int)GRID_SIZE / 8, Color.GREEN, graph.getNode(nodeIndex).getPosition()));
				ArrayList<BeeNode> path = graph.getAStarPath(nodeIndex, positionToNodeIndex(findTarget(ship, space).getPosition()));
				for (BeeNode node : path) {
					newGraphics.add(new CircleGraphics((int)GRID_SIZE / 8, Color.GREEN, node.getPosition()));
				}
			}
		}
		
		for (AbstractObject actionable : actionableObjects) {
				actions.put(actionable.getId(), new DoNothingAction());
		}
		return actions;
	}

	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
		for (Entry<AbstractObject, Integer> obstacle: obstacleMap.entrySet()) {
			// If obstruction is dead, unobstruct the node
			// TODO: this doesn't actually work
			if (!obstacle.getKey().isAlive()) {
				graph.unobstructNode(obstacle.getValue());
				obstacleMap.remove(obstacle.getKey());
				System.out.println("Removing dead object");
			}
		}
		/*
		// TODO: but this one does?
		for (Asteroid asteroid : space.getAsteroids()) {
			if (!asteroid.isAlive()) {
				graph.unobstructNode(obstacleMap.get(asteroid));
				obstacleMap.remove(asteroid);
			}
		}
		*/
		
	}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		boolean DEBUG_GRAPHICS = true;
		
		HashSet<SpacewarGraphics> graphics = new HashSet<SpacewarGraphics>();
		if (DEBUG_GRAPHICS) {
			// TODO: reduce lag when drawing lots of objects
			// Draw grid on screen
			graphics.addAll(drawGrid(new Position(0, 0), GRID_SIZE, 1080, 1600, Color.GRAY));
			// Draw circles representing each node
			for (int i = 0; i < graph.getSize(); i++) {
				if (graph.getNode(i).getObstructed()) {
					graphics.add(new CircleGraphics((int)GRID_SIZE / 8, Color.RED, graph.getNode(i).getPosition()));
				}
				else {
					//newGraphics.add(new CircleGraphics((int)GRID_SIZE / 8, Color.GREEN, graph.getNode(i).getPosition()));
				}
			}
		}
		graphics.addAll(newGraphics);
		newGraphics.clear();
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
	private Set<SpacewarGraphics> drawGrid(Position startPos, double gridSize, int height, int width, Color color) {
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
	
}
