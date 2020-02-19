package blac8074;

import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PurePursuit {

    private List<Position> path;

    PurePursuit() {
        path = new ArrayList<>();
    }

    public void setPath(ArrayList<BeeNode> newPath) {
        path.clear();
        for (BeeNode node : newPath) {
            path.add(node.getPosition());
        }
    }

    public Position getLookaheadPoint(Toroidal2DPhysics space, Position pos, double radius) {
        Position lookahead = null;
        double x = pos.getX();
        double y = pos.getY();

        // iterate through all pairs of points
        for (int i = 0; i < path.size() - 1; i++) {
            // form a segment from each two adjacent points
            Position segmentStart = path.get(i);
            Position segmentEnd = path.get(i + 1);

            Vector2D segmentStartVector = space.findShortestDistanceVector(pos, segmentStart);
            Vector2D segmentEndVector = space.findShortestDistanceVector(pos, segmentEnd);

            // translate the segment to the origin
            double[] p1 = new double[]{segmentStartVector.getXValue(), segmentStartVector.getYValue()};
            double[] p2 = new double[]{segmentEndVector.getXValue(), segmentEndVector.getYValue()};

            // calculate an intersection of a segment and a circle with radius r (lookahead) and origin (0, 0)
            double dx = p2[0] - p1[0];
            double dy = p2[1] - p1[1];
            double d = Math.sqrt(dx * dx + dy * dy);
            double D = p1[0] * p2[1] - p2[0] * p1[1];

            // if the discriminant is zero or the points are equal, there is no intersection
            double discriminant = radius * radius * d * d - D * D;
            if (discriminant < 0 || Arrays.equals(p1, p2)) continue;

            // the x components of the intersecting points
            double x1 = (D * dy + signum(dy) * dx * Math.sqrt(discriminant)) / (d * d);
            double x2 = (D * dy - signum(dy) * dx * Math.sqrt(discriminant)) / (d * d);

            // the y components of the intersecting points
            double y1 = (-D * dx + Math.abs(dy) * Math.sqrt(discriminant)) / (d * d);
            double y2 = (-D * dx - Math.abs(dy) * Math.sqrt(discriminant)) / (d * d);

            // whether each of the intersections are within the segment (and not the entire line)
            boolean validIntersection1 = Math.min(p1[0], p2[0]) < x1 && x1 < Math.max(p1[0], p2[0])
                    || Math.min(p1[1], p2[1]) < y1 && y1 < Math.max(p1[1], p2[1]);
            boolean validIntersection2 = Math.min(p1[0], p2[0]) < x2 && x2 < Math.max(p1[0], p2[0])
                    || Math.min(p1[1], p2[1]) < y2 && y2 < Math.max(p1[1], p2[1]);

            // remove the old lookahead if either of the points will be selected as the lookahead
            if (validIntersection1 || validIntersection2) lookahead = null;

            // select the first one if it's valid
            if (validIntersection1) {
                lookahead = new Position(x1 + x, y1 + y);
            }

            // select the second one if it's valid and either lookahead is none,
            // or it's closer to the end of the segment than the first intersection
            if (validIntersection2) {
                if (lookahead == null || Math.abs(x1 - p2[0]) > Math.abs(x2 - p2[0]) || Math.abs(y1 - p2[1]) > Math.abs(y2 - p2[1])) {
                    lookahead = new Position(x2 + x, y2 + y);
                }
            }
        }

        // special case for the very last point on the path
        if (path.size() > 0) {
            Position lastPoint = path.get(path.size() - 1);

            double endX = lastPoint.getX();
            double endY = lastPoint.getY();

            // if we are closer than lookahead distance to the end, set it as the lookahead
            if (Math.sqrt((endX - x) * (endX - x) + (endY - y) * (endY - y)) <= radius) {
                return new Position(endX, endY);
            }
        }

        space.toroidalWrap(lookahead); // just in case

        return lookahead;
    }

    private double signum(double n) {
        if (n == 0) return 1;
        else return Math.signum(n);
    }
}