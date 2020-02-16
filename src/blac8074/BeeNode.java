package blac8074;

import java.util.HashMap;

import spacesettlers.utilities.*;

public class BeeNode {
	private Position position;
	private HashMap<BeeNode, Double> adjacencyMap;
	
	public BeeNode() {
		adjacencyMap = new HashMap<BeeNode, Double>();
	}
	
	public BeeNode(Position position) {
		adjacencyMap = new HashMap<BeeNode, Double>();
		this.position = position;
	}
	
	public void addAdjacent(BeeNode node, Double distance) {
		adjacencyMap.put(node, distance);
	}
	
	public void removeAdjacent(BeeNode node) {
		adjacencyMap.remove(node);
	}
	
	public Position getPosition() {
		return position;
	}
}
