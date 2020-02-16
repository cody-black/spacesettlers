package blac8074;

import java.util.HashMap;

import spacesettlers.utilities.*;

public class BeeNode {
	private Position position;
	private boolean obstructed;
	private HashMap<BeeNode, Double> adjacencyMap;
	
	public BeeNode() {
		adjacencyMap = new HashMap<BeeNode, Double>();
	}
	
	public BeeNode(Position position) {
		adjacencyMap = new HashMap<BeeNode, Double>();
		this.position = position;
		this.obstructed = false;
	}
	
	public void addAdjacent(BeeNode node, Double cost) {
		adjacencyMap.put(node, cost);
	}
	
	public void removeAdjacent(BeeNode node) {
		adjacencyMap.remove(node);
	}
	
	public Position getPosition() {
		return position;
	}
	
	public Vector2D getVector2D() {
		return new Vector2D(position);
	}
	
	public void setObstructed(boolean obstructed) {
		this.obstructed = obstructed;
	}
	
	public boolean getObstructed() {
		return obstructed;
	}
	
	public void setCost(BeeNode node, Double cost) {
		adjacencyMap.replace(node, cost);
	}
	
	public double getCost(BeeNode node) {
		return adjacencyMap.get(node);
	}
	
	public HashMap<BeeNode, Double> getAdjacencyMap() {
		return adjacencyMap;
	}
}
