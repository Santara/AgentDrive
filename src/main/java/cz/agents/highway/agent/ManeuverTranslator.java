package cz.agents.highway.agent;

import cz.agents.highway.maneuver.*;
import cz.agents.highway.storage.RoadObject;
import cz.agents.highway.storage.VehicleSensor;
import cz.agents.highway.storage.plan.ManeuverAction;
import cz.agents.highway.storage.plan.Action;
import cz.agents.highway.storage.plan.WPAction;

import javax.vecmath.Point2f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import java.util.List;

/**
 * This class translates maneuvers generated by SDAgent to waypoints
 * Created by wmatex on 15.7.14.
 */
public class ManeuverTranslator {
    private static final double RADIUS = 1f;

    private static final double MAX_ANGLE = Math.PI/2;

    private static final int TRY_COUNT = 10;

    private static final float EPSILON = 0.01f;

    private VehicleSensor sensor;
    private final RouteNavigator navigator;
    private final int id;

    public ManeuverTranslator(int id, RouteNavigator navigator) {
        this.id = id;
        this.navigator = navigator;
    }

    public void setSensor(VehicleSensor sensor) {
        this.sensor = sensor;
    }

    public Action translate(CarManeuver maneuver) {
        if (maneuver == null) {
            Point2f initial = navigator.getInitialPosition();
            return new WPAction(id, 0d, new Point3f(initial.x, initial.y, 0), 0);
        }
        RoadObject me = sensor.senseCurrentState();
        // Check the type of maneuver
        if ((maneuver instanceof StraightManeuver) || (maneuver instanceof AccelerationManeuver)
                || (maneuver instanceof DeaccelerationManeuver)) {
            Point2f innerPoint = generateWaypointInLane(me.getLane(), maneuver);
            return point2Waypoint(innerPoint, maneuver);
        } else if (maneuver instanceof LaneLeftManeuver) {
            Point2f innerPoint = generateWaypointInLane(me.getLane() + 1, maneuver);
            return point2Waypoint(innerPoint, maneuver);
        } else if (maneuver instanceof LaneRightManeuver) {
            Point2f innerPoint = generateWaypointInLane(me.getLane() - 1, maneuver);
            return point2Waypoint(innerPoint, maneuver);
        } else {
            return new ManeuverAction(sensor.getId(), maneuver.getStartTime() / 1000.0,
                    maneuver.getVelocityOut(), maneuver.getLaneOut(), maneuver.getDuration());
        }
    }

    private Point2f generateWaypointInLane(int relativeLane, CarManeuver maneuver) {
        RoadObject me = sensor.senseCurrentState();

        Point3f p = me.getPosition();
        Point2f pos2D = new Point2f(p.x, p.y);
        Vector3f v = me.getVelocity();
        Vector2f vel2D = new Vector2f(v.x, v.y);

        // Translate the position according to the maneuver duration and vehicle speed
        Vector2f v2 = new Vector2f(vel2D);
        //v2.scale((float) maneuver.getDuration());
        pos2D.add(v2);

        // Get the closest route point the translated position
        Point2f innerPoint = null;
        // Change to right
        if (relativeLane < 0) {
            navigator.changeLaneRight();
        } else if (relativeLane > 0) {
            navigator.changeLaneLeft();
        }
        int i = 0;
        do {
            innerPoint = navigator.getRoutePoint();
            if (!pointCloseEnough(innerPoint, pos2D, vel2D)) {
                navigator.advanceInRoute();
            }
            i++;
        } while (i < TRY_COUNT);

        if (innerPoint == null) {
            return navigator.getInitialPosition();
        } else {
            return innerPoint;
        }
    }

    private WPAction point2Waypoint(Point2f point, CarManeuver maneuver) {
        return new WPAction(sensor.getId(), maneuver.getStartTime() / 1000,
                new Point3f(point.x, point.y, sensor.senseCurrentState().getPosition().z),
                maneuver.getVelocityOut());
    }

    /**
     * This method determines whether the waypoint candidate is close enough (in radius) to the position
     * in the direction given by velocity vector
     *
     * @param innerPoint Waypoint candidate
     * @param position   Position
     * @param velocity   Velocity vector
     * @return
     */
    private boolean pointCloseEnough(Point2f innerPoint, Point2f position, Vector2f velocity) {
        // Direction vector of waypoint candidate relative to position
        Vector2f direction = new Vector2f();
        direction.sub(innerPoint, position);

        return  velocity.angle(direction) < MAX_ANGLE &&
                distance(innerPoint, position, direction, velocity) < RADIUS;
    }

    /**
     * This method computes the distance of the waypoint. It is the Euklidian distance of the waypoint
     * multiplied by absolute value of the sin of the angle between the direction of the waypoint
     * and the vector of velocity. This ensures, that waypoints that are less deviating from
     * the direction of the vehicle's movement and are close enough are picked.
     */
    private float distance(Point2f innerPoint, Point2f position, Vector2f direction, Vector2f velocity) {
        float d = innerPoint.distance(position);
        return d*d*Math.abs((float)Math.sin(direction.angle(velocity))+EPSILON);
    }
}
