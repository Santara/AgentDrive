package cz.agents.highway.agent;

import cz.agents.alite.common.entity.Entity;
import cz.agents.alite.configurator.Configurator;
import cz.agents.highway.environment.roadnet.Edge;
import cz.agents.highway.environment.roadnet.RoadNetworkRouter;
import cz.agents.highway.environment.roadnet.network.RoadNetwork;
import cz.agents.highway.storage.VehicleActuator;
import cz.agents.highway.storage.VehicleSensor;
import org.apache.log4j.Logger;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.util.List;

public class Agent extends Entity {

    int id;
    private static final Logger logger = Logger.getLogger(Agent.class);

    protected VehicleSensor sensor;
    protected VehicleActuator actuator;
    protected RouteNavigator navigator;


    public Agent(int id) {
        super("" + id);
        this.id = id;
        List<Edge> route = RoadNetworkRouter.generateRoute(id);
        navigator = new RouteNavigator(route);
        logger.info("Agent " + id + " created");
    }


    public void addSensor(final VehicleSensor sensor) {
        this.sensor = sensor;
        logger.info("Sensor added: " + sensor);
    }

    public void addActuator(VehicleActuator actuator) {
        this.actuator = actuator;
        logger.info("Actuator added: " + actuator);
    }

    public RouteNavigator getNavigator() {
        return navigator;
    }

    public Point3f getInitialPosition() {
        return new Point3f(0, 0, 0);
    }

    public Vector3f getInitialVelocity() {
        return navigator.getInitialVelocity();
    }

    public void setNavigator(RouteNavigator navigator) {
        this.navigator = navigator;
    }
}
