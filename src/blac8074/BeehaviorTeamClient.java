package blac8074;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.clients.TeamClient;
import spacesettlers.graphics.*;
import spacesettlers.objects.AbstractActionableObject;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;

import blac8074.BeeGraph;


public class BeehaviorTeamClient extends TeamClient {
	BeeGraph graph;
	static double GRID_SIZE = 40;
	static int MAP_HEIGHT = 1080;
	static int MAP_WIDTH = 1600;
	
	@Override
	public void initialize(Toroidal2DPhysics space) {
		int numSquaresX = MAP_WIDTH / (int)GRID_SIZE;
		int numSquaresY = MAP_HEIGHT / (int)GRID_SIZE;
		graph = new BeeGraph(numSquaresX * numSquaresY, numSquaresY, numSquaresX);
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
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, AbstractAction> actions = new HashMap<UUID, AbstractAction>();
		for (AbstractObject actionable : actionableObjects) {
				actions.put(actionable.getId(), new DoNothingAction());
		}
		return actions;
	}

	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		boolean DEBUG_GRAPHICS = true;
		HashSet<SpacewarGraphics> newGraphics = new HashSet<SpacewarGraphics>();
		if (DEBUG_GRAPHICS) {
			// Draw grid on screen
			newGraphics.addAll(drawGrid(new Position(0, 0), GRID_SIZE, 1080, 1600, Color.GRAY));
			// Draw circles representing each node
			for (int i = 0; i < graph.getSize(); i++) {
				if (graph.getNode(i).getObstructed()) {
					newGraphics.add(new CircleGraphics((int)GRID_SIZE / 8, Color.RED, graph.getNode(i).getPosition()));
				}
				else {
					newGraphics.add(new CircleGraphics((int)GRID_SIZE / 8, Color.GREEN, graph.getNode(i).getPosition()));
				}
			}
		}
		return newGraphics;
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
}
