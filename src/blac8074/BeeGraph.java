package blac8074;

import java.util.*;

import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;

/**
 * The BeeGraph stores and performs operations on BeeNodes
 * 
 *
 */

public class BeeGraph {

	private BeeNode[] nodes;
	private int height;
	private int width;
	private double gridSize;
	static final double MULT = 1000000.0;
	
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
			for (BeeNode adjNode: node.getAdjacencyMap().keySet()) {
				// Multiply edge distance by fixed amount so we can recover it later without recalculating cost
				adjNode.setEdgeCost(node, adjNode.getEdgeCost(node) * MULT);
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
			for (BeeNode adjNode: node.getAdjacencyMap().keySet()) {
				adjNode.setEdgeCost(node, adjNode.getEdgeCost(node) / MULT);
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

	/*
	 * Uses A* to find a path from the start node to the goal node
	 * TODO: clean this up
	 */
	public ArrayList<BeeNode> getAStarPath(int startIndex, int goalIndex) {
		unobstructNode(startIndex); // This won't return a path if the start node is obstructed
		// Yes, the start and goal nodes are switched
		// Otherwise the path ArrayList would have to be reversed
		BeeNode startNode = nodes[goalIndex];
		BeeNode goalNode = nodes[startIndex];
		
		HashSet<BeeNode> closed = new HashSet<BeeNode>();
		PriorityQueue<BeeNode> frontier = new PriorityQueue<BeeNode>();
		ArrayList<BeeNode> path = new ArrayList<BeeNode>();
		
		// Stores the current minimum cost at the specified node
		HashMap<BeeNode, Double> costAtNode = new HashMap<BeeNode, Double>();
		// Stores the parent of each node in the form childNode, parentNode
		HashMap<BeeNode, BeeNode> parentMap = new HashMap<BeeNode, BeeNode>();
		
		parentMap.put(startNode, null);
		costAtNode.put(startNode, 0.0);
		
		// Put all children of the start node into the frontier
		for (BeeNode adjNode : startNode.getAdjacencyMap().keySet()) {
			double cost = startNode.getEdgeCost(adjNode);
			costAtNode.put(adjNode, cost);
			adjNode.setPriority(cost + this.findDistance(adjNode, goalNode));
			frontier.add(adjNode);
			parentMap.put(adjNode, startNode);
		}
		
		int loopCount = 0;
		int MAX_LOOPS = 1000;
		
		while (true) {
			if (frontier.isEmpty()) {
				break;
			}
			if (loopCount == MAX_LOOPS) {
				// System.out.println("A* pathfinding timed out");
				return path;
			}
			BeeNode nextNode = frontier.poll();
			if (nextNode == goalNode) {
				break;
			}
			if (!closed.contains(nextNode)) {
				closed.add(nextNode);
				for (BeeNode adjNode : nextNode.getAdjacencyMap().keySet()) {
					double cost = costAtNode.get(nextNode) + nextNode.getEdgeCost(adjNode);
					if ((!costAtNode.containsKey(adjNode)) || cost < costAtNode.get(adjNode)) {
						if (!closed.contains(adjNode)) {
							costAtNode.put(adjNode, cost);
							adjNode.setPriority(cost + this.findDistance(adjNode, goalNode));
							frontier.add(adjNode);
							parentMap.put(adjNode, nextNode);
						}
					}
				}
			}
			++loopCount;
		}
		
		// Find path from goal to root of tree
		BeeNode parentNode = goalNode;
		loopCount = 0;
		while (parentNode != startNode) {
			if (loopCount == MAX_LOOPS) {
				// System.out.println("A* pathfinding timed out");
				return path;
			}
			path.add(parentNode);
			parentNode = parentMap.get(parentNode);
			++loopCount;
		}
		path.add(parentNode);
		return path;
	}
	
	/*
	 * Uses hill climbing to find a path from the start node to the goal node
	 */
	public ArrayList<BeeNode> getHillClimbingPath(int startIndex, int goalIndex) {
		unobstructNode(startIndex); // This won't return a path if the start node is obstructed

		BeeNode currentNode = nodes[startIndex];
		BeeNode goalNode = nodes[goalIndex];

		ArrayList<BeeNode> path = new ArrayList<BeeNode>();
		double lastHeuristic = Double.POSITIVE_INFINITY;

		while (true) {
			// Get adjacent nodes to our current node
			Set<BeeNode> adjNodes = currentNode.getAdjacencyMap().keySet();

			double bestHeuristic = Double.POSITIVE_INFINITY;
			BeeNode bestNode = null;

			for (BeeNode node : adjNodes) {
				// Is the goal adjacent? Add it to path and stop pathfinding
				if (node == goalNode) {
					path.add(goalNode);
					bestHeuristic = Double.POSITIVE_INFINITY; // to break the while loop
					break;
				}

				// Ignore obstructed nodes
				if (node.getObstructed()) {
					continue;
				}

				// Our heuristic function is simple distance
				double heuristic = this.findDistance(node, goalNode);

				// Is this adjacent node better than the others?
				if (heuristic < bestHeuristic) {
					bestHeuristic = heuristic;
					bestNode = node;
				}
			}

			// No longer improving
			if (bestHeuristic >= lastHeuristic) {
				break;
			}

			// We found a good node. Add it to the path and set it as next node to explore.
			path.add(bestNode);
			currentNode = bestNode;
		}

		return path;
	}
	
	/**
	 * Toroidal wrap based on the height/width of the environment
	 * 
	 * @param position
	 */
	public Position toroidalWrap(Position position) {
		Position newPos = position.deepCopy();
		while (newPos.getX() < 0) {
			newPos.setX(newPos.getX() + (width * gridSize));
		}

		while (newPos.getY() < 0) {
			newPos.setY(newPos.getY() + (height * gridSize));
		}

		newPos.setX(newPos.getX() % (width * gridSize));
		newPos.setY(newPos.getY() % (height * gridSize));
		return newPos;
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