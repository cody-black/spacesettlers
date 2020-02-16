package blac8074;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;

public class BeeGraph {

	private BeeNode[] nodes;
	private int height;
	private int width;
	private double gridSize;
	static double mult = 1000000.0;
	
	public BeeGraph(int size, int height, int width, double gridSize) {
		nodes = new BeeNode[size];
		this.height = height;
		this.width = width;
		this.gridSize = gridSize;
	}
	
	public void addNode(int index, BeeNode node) {
		nodes[index] = node;
	}
	
	public BeeNode getNode(int index) {
		return nodes[index];
	}
	
	public int getSize() {
		return nodes.length;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	/*
	 *  When the node's grid square has an obstacle in it, it is obstructed
	 *  Returns true if a node has been obstructed
	 */
	public boolean obstructNode(int index) {
		BeeNode node = nodes[index];
		if (!node.getObstructed()) {
			node.setObstructed(true);
			// Change edge costs back to maximum
			for (Entry<BeeNode, Double> adjNode: node.getAdjacencyMap().entrySet()) {
				// Multiply edge distance by fixed amount so we can recover it later without recalculating cost
				adjNode.getKey().setEdgeCost(node, adjNode.getKey().getEdgeCost(node) * mult);
			}
			return true;
		}
		else {
			return false;
		}
	}
	
	public boolean unobstructNode(int index) {
		BeeNode node = nodes[index];
		if (node.getObstructed()) {
			node.setObstructed(false);
			// Change edge distance back to normal
			for (Entry<BeeNode, Double> adjNode: node.getAdjacencyMap().entrySet()) {
				adjNode.getKey().setEdgeCost(node, adjNode.getKey().getEdgeCost(node) / mult);
			}
			return true;
		}
		else {
			return false;
		}
	}
	
	/*
	 * Returns the distance between two nodes
	 */
	public double findDistance(int nodeIndex1, int nodeIndex2) {
		return findShortestDistance(nodes[nodeIndex1].getPosition(), nodes[nodeIndex2].getPosition());
	}
	
	public double findDistance(BeeNode node1, BeeNode node2) {
		return findShortestDistance(node1.getPosition(), node2.getPosition());
	}
	
	/**
	 * Public interface to find the shortest toroidal distance from one location to
	 * another. Returns a double (the distance). Use findShortestDistanceVector to
	 * get the vector telling you which way to move along this path. Useful if you
	 * just care about distance.
	 * 
	 * @param location1
	 * @param location2
	 * @return shortest distance length (magnitude of the vector pointing from
	 *         location1 to location2)
	 */
	public double findShortestDistance(Position location1, Position location2) {
		Vector2D shortDist = findShortestDistanceVector(location1, location2, width * (int)gridSize, height * (int)gridSize,
				(width * gridSize) / 2.0, (height * gridSize) / 2.0);
		return shortDist.getMagnitude();
	}

	/**
	 * Finds the shortest distance in toroidal space. Returns a vector pointing from
	 * the start to the target location and getMagnitude can be used to find the
	 * distance and the angle.
	 * 
	 * @param location1
	 * @param location2
	 * @param width
	 * @param height
	 * @param halfWidth
	 * @param halfHeight
	 * @return
	 */
	private Vector2D findShortestDistanceVector(Position location1, Position location2, double width, double height,
			double halfWidth, double halfHeight) {
		double x = location2.getX() - location1.getX();
		double y = location2.getY() - location1.getY();
		if (x > halfWidth) {
			if (y > halfHeight) {
				return new Vector2D(x - width, y - height);
			} else if (y < -halfHeight) {
				return new Vector2D(x - width, y + height);
			} else {
				return new Vector2D(x - width, y);
			}
		} else if (x < -halfWidth) {
			if (y > halfHeight) {
				return new Vector2D(x + width, y - height);
			} else if (y < -halfHeight) {
				return new Vector2D(x + width, y + height);
			} else {
				return new Vector2D(x + width, y);
			}
		} else if (y > halfHeight) {
			return new Vector2D(x, y - height);
		} else if (y < -halfHeight) {
			return new Vector2D(x, y + height);
		} else {
			return new Vector2D(x, y);
		}
	}
	
	// TODO: Almost certain this isn't actually A*
	public ArrayList<BeeNode> getAStarPath(int startIndex, int goalIndex) {
		int MAX_LOOPS = 1000;
		int timeout = 0;
		ArrayList<BeeNode> path = new ArrayList<BeeNode>();
		BeeNode startNode = nodes[startIndex];
		BeeNode goalNode = nodes[goalIndex];
		BeeNode currNode = startNode;
		double currCost = 0;
		double nextPriority;
		// TODO: is this part even needed?
		/*
		if (startNode.getAdjacencyMap().containsKey(goalNode)) {
			path.add(goalNode);
			return path;
		}
		*/
		/*
		while ((currNode != goalNode) && (timeout < MAX_LOOPS)) {
			for (Entry<BeeNode, Double> adjNode: currNode.getAdjacencyMap().entrySet()) {
				if (!frontier.contains(adjNode.getKey())) {
					adjNode.getKey().setTotalCost(adjNode.getValue() + this.findDistance(adjNode.getKey(), goalNode));
					frontier.add(adjNode.getKey());
				}
			}
			currNode = frontier.poll();
			path.add(currNode);
			++timeout;
		}
		*/
		if (startNode == goalNode) {
			path.add(startNode);
			return path;
		}
 		
		// A* search algorithm from notes on Canvas
		HashSet<BeeNode> closed = new HashSet<BeeNode>();
		PriorityQueue<BeeNode> frontier = new PriorityQueue<BeeNode>();
		for (Entry<BeeNode, Double> adjNode: currNode.getAdjacencyMap().entrySet()) {
			adjNode.getKey().setTotalCost(adjNode.getValue() + this.findDistance(adjNode.getKey(), goalNode));
			frontier.add(adjNode.getKey());
		}
		path.add(startNode); // Not from the notes
		while (true) {
			if (frontier.isEmpty()) {
				return path;
			}
			if (timeout == MAX_LOOPS) {
				return path;
			}
			currNode = frontier.poll();
			path.add(currNode);
			currCost = 0;
			for (int i = 0; i < path.size() - 1; i++) {
				//currCost += path.get(i).getEdgeCost(path.get(i + 1));
			}
			if (currNode == goalNode) {
				return path;
			}
			if (!closed.contains(currNode)) {
				closed.add(currNode);
				for (Entry<BeeNode, Double> adjNode: currNode.getAdjacencyMap().entrySet()) {
					nextPriority = currCost + adjNode.getValue() + this.findDistance(adjNode.getKey(), goalNode);
					if (frontier.contains(adjNode.getKey())) {
						// Node already in frontier, but with higher f(n)
						if (nextPriority < adjNode.getKey().getTotalCost()) {
							adjNode.getKey().setTotalCost(nextPriority);
							frontier.add(adjNode.getKey());
						}
					}
					// Node is not closed
					else if (!closed.contains(adjNode.getKey())) {
						adjNode.getKey().setTotalCost(nextPriority);
						frontier.add(adjNode.getKey());
					}
				}
			}
			++timeout;
		}
	}
	
	public int[] findAdjacentIndices(int index) {
		int[] indices = new int[8];
		int row = index / width;
		int column = index % width;
		// Adjacent indices that are to the right
		if (column != (width - 1)) {
			indices[0] = index + 1;
		}
		// Adjacent indices that are to the right and wrap around
		else {
			indices[0] = index - width + 1;
		}
		// Adjacent indices that are to the left
		if (column != 0) {
			indices[1] = index - 1;
		}
		// Adjacent indices that are to the left and wrap around
		else {
			indices[1] = index + width - 1;
		}
		// Adjacent indices that are up
		if (row > 0) {
			indices[2] = index - width;
		}
		// Adjacent indices that are up and wrap around
		else {
			indices[2] = index + width * (height - 1);
		}
		// Adjacent indices that are down
		if (row < (height - 1)) {
			indices[3] = index + width;
		}
		// Adjacent indices that are down and wrap around
		else {
			indices[3] = index - width * (height - 1);
		}
		// Adjacent indices that are up and to the right
		if (column != (width - 1)) {
			if (row > 0) {
				indices[4] = index + 1 - width;
			}
			else {
				indices[4] = index + 1 + width * (height - 1);
			}
		}
		else {
			if (row > 0) {
				indices[4] = index - 2 * width + 1;
			}
			else {
				indices[4] = index - width + 1 + width * (height - 1);
			}
		}
		// Adjacent indices that are up and to the left
		if (column != 0) {
			if (row > 0) {
				indices[5] = index - 1 - width;
			}
			else {
				indices[5] = index - 1 + width * (height - 1);;
			}
		}
		else {
			if (row > 0) {
				indices[5] = index + width - 1 - width;
			}
			else {
				indices[5] = index + width - 1 + width * (height - 1);
			}
		}
		// Adjacent indices that are down and to the right
		if (column != (width - 1)) {
			if (row < (height - 1)) {
				indices[6] = index + width + 1;
			}
			else {
				indices[6] = index - width * (height - 1) + 1;
			}
		}
		else {
			if (row < (height - 1)) {
				indices[6] = index + width - width + 1;
			}
			else {
				indices[6] = index - width * (height - 1) - width + 1;
			}
		}
		// Adjacent indices that are down and to the left
		if (column != 0) {
			if (row < (height - 1)) {
				indices[7] = index + width - 1;
			}
			else {
				indices[7] = index - width * (height - 1) - 1;
			}
		}
		else {
			if (row < (height - 1)) {
				indices[7] = index + width + width - 1;
			}
			else {
				indices[7] = index - width * (height - 1) + width - 1;
			}
		}
		return indices;
	}
}