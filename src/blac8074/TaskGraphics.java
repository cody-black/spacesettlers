package blac8074;

import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.objects.Ship;
import spacesettlers.utilities.Position;

import java.awt.*;

public class TaskGraphics extends SpacewarGraphics {
    Ship ship;
    BeePlanner.BeeTask task;

    public TaskGraphics(Ship ship, BeePlanner.BeeTask task) {
        super(20, 40);
        this.ship = ship;
        this.task = task;
    }

    @Override
    public Position getActualLocation() {
        return ship.getPosition();
    }

    @Override
    public void draw(Graphics2D graphics) {
        graphics.setColor(Color.white);
        graphics.drawString(task.toString(), (int) ship.getPosition().getX() + 10, (int) ship.getPosition().getY() - 10);
    }

    @Override
    public boolean isDrawable() {
        return true;
    }
}
