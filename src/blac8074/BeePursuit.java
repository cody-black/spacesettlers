package blac8074;

import javafx.scene.shape.Circle;
import spacesettlers.graphics.CircleGraphics;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/**
 * BeePursuit is our implementation of the Pure Pursuit Tracking Algorithm.
 * This algorithm was invented by the Robotics Institute at Carnegie Mellon
 * University and can be read about here: 
 * https://www.ri.cmu.edu/pub_files/pub3/coulter_r_craig_1992_1/coulter_r_craig_1992_1.pdf
 * 
 * Pure Pursuit is a simple, quick, and effective way to track a PID controlled 
 * agent onto a path specified by a series of waypoints. It works by emitting a
 * circle from the center of the agent and looks for where the circle
 * intersects with the path. The agent should then treat that intersection point
 * as an immediate goal (for example, using a PID-controller to locally navigate
 * towards that goal position).
 * 
 * We used Pure Pursuit in this project because it worked well with the existing
 * PI-controller from the MoveAction and our path finding algorithms returned
 * a path appropriate for Pure Pursuit.
 */
public class BeePursuit {

    private List<Position> path;

    BeePursuit() {
        path = new ArrayList<>();
    }

    /**
     * Set a new path for the BeePursuit to track onto
     *
     * @param newPath List of BeeNode's of the path.
     */
    public void setPath(List<BeeNode> newPath) {
        path.clear();
        for (BeeNode node : newPath) {
            path.add(node.getPosition());
        }
    }


    /**
     * @param space The Toroidal2DPhysics space to track on.
     * @param pos The Position of the agent
     * @param radius The radius of the circle the algorithm uses. See Pure
     *              Pursuit description above
     * @return A Position that the agent should immediately head towards.
     */

    public Position getDesiredPosition(Toroidal2DPhysics space, Position pos, double radius) {
        
        // If the path is empty, return agent's position.
        // We should only return null when this algorithm fails (no intersection
        // of circle and path).
        if (path.size() == 0) {
            return pos;
        }
        
        // Final desired position
        Position desiredPosition = null;
        
        // For future simplification
        double x = pos.getX();
        double y = pos.getY();

        // For each pair of points in the path
        for (int i = 0; i < path.size() - 1; i++) {
            Vector2D startVector = space.findShortestDistanceVector(pos, path.get(i));
            Vector2D endVector = space.findShortestDistanceVector(pos, path.get(i + 1));

            Position[] intersections = getCircleLineIntersections(startVector, endVector, radius);

            // Disaster case (should never happen)
            if (intersections == null) {
                continue;
            }

            // Select first if it's not null
            if (intersections[0] != null) {
                desiredPosition = new Position(intersections[0].getX() + x, intersections[0].getY() + y);
            }

            // Select the second intersection if the first one didn't work
            // or if it is closer to the end (so we always move towards the
            // end of the path)
            if (intersections[1] != null) {
                Position potentialPosition = new Position(intersections[1].getX() + x, intersections[1].getY() + y);
                if (desiredPosition == null || space.findShortestDistance(potentialPosition, path.get(i+1)) < space.findShortestDistance(desiredPosition, path.get(i+1))) {
                    desiredPosition = potentialPosition;
                }
            }
        }

        // If last point is closer than the radius, it's a good place to go
        // but would not be found via the intersections above.
        Position lastPoint = path.get(path.size() - 1);
        if (Math.sqrt((lastPoint.getX() - x) * (lastPoint.getX() - x) + (lastPoint.getY() - y) * (lastPoint.getY() - y)) <= radius) {
            return lastPoint;
        }

        // Do a final toroidalWrap of our desiredPosition to make sure
        // that nothing breaks.
        if (desiredPosition != null) {
            space.toroidalWrap(desiredPosition);
        }

        return desiredPosition;
    }

    /**
     * Calculates the intersection of a line and a circle.
     * Assumes the circle is at position (0,0).
     *
     * @param lineStart Start of the line segment
     * @param lineEnd End of the line segment
     * @param radius Radius of the circle
     * @return Intersection points. Values null if none.
     */
    private Position[] getCircleLineIntersections(Vector2D lineStart, Vector2D lineEnd, double radius) {

        // Cant intersect a 0-length segment
        if (lineStart.equals(lineEnd)) {
            return null;
        }

        Position[] intersections = {null, null};

        // More simplification variables
        double startX = lineStart.getXValue();
        double startY = lineStart.getYValue();
        double endX = lineEnd.getXValue();
        double endY = lineEnd.getYValue();
        double dx = endX - startX;
        double dy = endY - startY;

        // Calculations for distance and discriminant
        double distanceSqr = dx * dx + dy * dy;
        double determinant = startX * endY - endX * startY;

        // Calculate discriminant. A 0 or negative discriminant means no intersection.
        double discriminant = radius * radius * distanceSqr - determinant * determinant;
        if (discriminant <= 0) {
            return null;
        }

        // Calculate the intersections points (x1, y1) and (x2, y2) using fancy discriminant math
        double x1 = (determinant * dy + signum(dy) * dx * Math.sqrt(discriminant)) / distanceSqr;
        double x2 = (determinant * dy - signum(dy) * dx * Math.sqrt(discriminant)) / distanceSqr;
        double v = Math.abs(dy) * Math.sqrt(discriminant);
        double y1 = (-determinant * dx + v) / distanceSqr;
        double y2 = (-determinant * dx - v) / distanceSqr;

        // Are the intersections actually within the segment and not beyond the segment?
        boolean intersection1Valid = Math.min(startX, endX) < x1 && x1 < Math.max(startX, endX) || Math.min(startY, endY) < y1 && y1 < Math.max(startY, endY);
        boolean intersection2Valid = Math.min(startX, endX) < x2 && x2 < Math.max(startX, endX) || Math.min(startY, endY) < y2 && y2 < Math.max(startY, endY);

        if (intersection1Valid) {
            intersections[0] = new Position(x1, y1);
        }

        if (intersection2Valid) {
            intersections[1] = new Position(x2, y2);
        }

        return intersections;
    }


    /**
     * Slightly modified version of Math.signum() to change the handling of 0.
     * 
     * @param n A double
     * @return The sign of n with n = 0 returning 1.
     */
    private double signum(double n) {
        if (n == 0) return 1;
        else return Math.signum(n);
    }
}
