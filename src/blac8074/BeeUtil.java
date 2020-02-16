package blac8074;

public class BeeUtil {
	
	public static int[] findAdjacentIndices(int index, int height, int width) {
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
