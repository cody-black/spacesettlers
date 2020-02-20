package blac8074;

import java.util.HashMap;

import spacesettlers.utilities.*;
/**
 * A BeeNode represents a node on a BeeGraph
 *
 * Each node keeps track of its adjacent nodes and the edge costs between itself and its adjacent nodes
 * Each node also has a Position that corresponds to its location in space
 */

public class BeeNode implements Comparable<BeeNode>{
	private Position position;
	private boolean obstructed;
	private double priority;
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
	
	public void setEdgeCost(BeeNode node, Double cost) {
		adjacencyMap.replace(node, cost);
	}
	
	public double getEdgeCost(BeeNode node) {
		return adjacencyMap.get(node);
	}
	
	public void setPriority(double priority) {
		this.priority = priority;
	}
	
	public double getPriority() {
		return priority;
	}
	
	public HashMap<BeeNode, Double> getAdjacencyMap() {
		return adjacencyMap;
	}
	
	/*
	 * Returns the distance between this node and another node
	 * TODO: make this work with wrap around
	public double findDistance(BeeNode node) {
		return this.getVector2D().subtract(node.getVector2D()).getMagnitude();
	}
	*/
	
	/*
	 * Mainly for telling a PriorityQueue how to prioritize nodes (I think)
	 */
	@Override
	public int compareTo(BeeNode node) {
		if ((this.priority - node.getPriority()) < 0) {
			return -1;
		}
		else if ((this.priority - node.getPriority()) > 0) {
			return 1;
		}
		else {
			return 0;
		}
		// This wouldn't return the right number if 
		// this.priority - node.getPriority() was between -1 and 1 and not 0
		//return (int)(this.priority - node.getPriority());
	}
}
