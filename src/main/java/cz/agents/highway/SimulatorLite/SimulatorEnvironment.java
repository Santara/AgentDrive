package cz.agents.highway.SimulatorLite;
import cz.agents.highway.environment.SimulatorHandlers.PlanCallback;
import cz.agents.highway.SimulatorLite.storage.SimulatorEvent;
import cz.agents.highway.SimulatorLite.storage.VehicleStorage;
import cz.agents.highway.SimulatorLite.storage.vehicle.Car;
import cz.agents.highway.SimulatorLite.storage.vehicle.Ghost;
import cz.agents.highway.SimulatorLite.storage.vehicle.Vehicle;
import cz.agents.alite.common.event.Event;
import cz.agents.alite.common.event.EventHandler;
import cz.agents.alite.common.event.EventProcessor;
import cz.agents.alite.configurator.Configurator;
import cz.agents.alite.environment.eventbased.EventBasedEnvironment;
import cz.agents.alite.protobuf.communicator.ClientCommunicator;
import cz.agents.alite.protobuf.communicator.Communicator;
import cz.agents.alite.protobuf.communicator.callback.ConnectCallback;
import cz.agents.alite.protobuf.factory.FactoryInterface;
import cz.agents.alite.protobuf.factory.ProtobufFactory;
import cz.agents.alite.simulation.SimulationEventType;
import cz.agents.alite.transport.SocketTransportLayer;
import cz.agents.highway.creator.AgentDrive;
import cz.agents.highway.environment.SimulatorHandlers.SimulatorDemo;
import cz.agents.highway.protobuf.factory.dlr.DLR_PlansFactory;
import cz.agents.highway.protobuf.factory.dlr.DLR_UpdateFactory;
import cz.agents.highway.protobuf.factory.simplan.PlansFactory;
import cz.agents.highway.protobuf.factory.simplan.UpdateFactory;
import cz.agents.highway.protobuf.generated.dlr.DLR_MessageContainer;
import cz.agents.highway.protobuf.generated.simplan.MessageContainer;
import cz.agents.highway.storage.RadarData;
import cz.agents.highway.storage.RoadObject;
import cz.agents.highway.storage.plan.Action;
import cz.agents.highway.storage.plan.PlansOut;
import cz.agents.highway.storage.plan.WPAction;
import org.apache.log4j.Logger;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * Class representing the environment of the simulation
 * <p/>
 * Created by wmatex on 3.7.14.
 */
public class SimulatorEnvironment extends EventBasedEnvironment {
    private static final Logger logger = Logger.getLogger(SimulatorEnvironment.class);
    /// All simulated vehicles
    private VehicleStorage vehicleStorage;
    private Communicator communicator;
    private RadarData simulatorState;
    private Boolean newPlan = false;
    private final Object mutex = new Object();
    private AgentDrive agentDrive;

    /// Time between updates
    static public final long UPDATE_STEP = 20;

    /// Time between communication updates
    static public final long COMM_STEP = 200;

    /**
     * Create the environment, the storage and register event handlers
     */
    public SimulatorEnvironment(final EventProcessor eventProcessor) {
        super(eventProcessor);
        vehicleStorage = new VehicleStorage(this);
        getEventProcessor().addEventHandler(new EventHandler() {
            @Override
            public EventProcessor getEventProcessor() {
                return eventProcessor;
            }

            @Override
            public void handleEvent(Event event) {
                // Start updating vehicles when the simulation starts
                if (event.isType(SimulationEventType.SIMULATION_STARTED)) {
                    logger.info("SIMULATION STARTED received");
                    // Connect to server
                    try {
                        logger.info("Connecting to server ...");
                        initCommunication();
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        logger.info("Connected");
                        getEventProcessor().addEvent(SimulatorEvent.UPDATE, null, null, null);
                        getEventProcessor().addEvent(SimulatorEvent.COMMUNICATION_UPDATE, null, null, null);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (event.isType(SimulatorEvent.COMMUNICATION_UPDATE)) {
                        // if(vehicleStorage.generateRadarData().getCars().size() != 0)
                        // {
                        agentDrive.update(vehicleStorage.generateRadarData());
                        // }

                    getEventProcessor().addEvent(SimulatorEvent.COMMUNICATION_UPDATE, null, null, null, COMM_STEP);
                }
            }
        });


    }

    private void initCommunication() throws IOException {
        PlanCallback plc = new PlanCallbackImp();
        agentDrive = new AgentDrive("settings/groovy/local/david.groovy");
        agentDrive.registerPlanCallback(plc);
        Thread t = new Thread(agentDrive,"AgentDrive");
        t.start();
    }


    /**
     * Initialize the environment and add some vehicles
     */
    public void init() {
        getEventProcessor().addEventHandler(vehicleStorage);
    }


    public VehicleStorage getStorage() {
        return vehicleStorage;
    }


    class PlanCallbackImp implements PlanCallback {
        //final HashSet<Integer> plannedVehicles = new HashSet<Integer>();
        @Override
        public void execute(PlansOut plans) {
            logger.debug("Received plans: " + plans.getCarIds());
            for (int carID : plans.getCarIds()) {
                logger.debug("Plan for car " + carID + ": " + plans.getPlan(carID));

                // This is the init plan
                if (((WPAction) plans.getPlan(carID).iterator().next()).getSpeed() == -1) {
                    LinkedList<Vehicle> allVehicles = vehicleStorage.getAllVehicles();
                    vehicleStorage.removeVehicle(carID);
                } else {
                    Vehicle vehicle = vehicleStorage.getVehicle(carID);
                    if (vehicle == null) {
                        // Get the first action containing car info
                        WPAction action = (WPAction) plans.getPlan(carID).iterator().next();
                        if (plans.getPlan(carID).size() > 1) {
                            Iterator<Action> iter = plans.getPlan(carID).iterator();
                            Point3f first = ((WPAction) iter.next()).getPosition();
                            Point3f second = ((WPAction) iter.next()).getPosition();
                            Vector3f heading = new Vector3f(second.x - first.x, second.y - first.y, second.z - first.z);
                            heading.normalize();
                            vehicleStorage.addVehicle(new Car(carID, 0, action.getPosition(), heading, (float) action.getSpeed() /*30.0f*/));
                        } else
                            vehicleStorage.addVehicle(new Car(carID, 0, action.getPosition(), new Vector3f(0, -1, 0), (float) action.getSpeed()));
                    } else {
                        vehicle.getVelocityController().updatePlan(plans.getPlan(carID));
                        vehicle.setWayPoints(plans.getPlan(carID));
                    }
                }
            }
        }
    }
}
