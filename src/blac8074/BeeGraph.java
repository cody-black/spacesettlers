package blac8074;

public class BeeGraph {

	public BeeNode[] nodes;
	
	public BeeGraph(int size) {
		nodes = new BeeNode[size];
	}
	
	public void addNode(int index, BeeNode node) {
		nodes[index] = node;;
	}
	
	public BeeNode getNode(int index) {
		return nodes[index];
	}
	
	public int getSize() {
		return nodes.length;
	}
}
