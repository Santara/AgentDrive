package cz.agents.highway.creator;

import cz.agents.alite.common.event.Event;
import cz.agents.alite.common.event.EventHandler;
import cz.agents.alite.common.event.EventProcessor;
import cz.agents.alite.configreader.ConfigReader;
import cz.agents.alite.configurator.Configurator;
import cz.agents.alite.simulation.SimulationEventType;
import cz.agents.highway.agent.Agent;
import cz.agents.highway.environment.SimulatorHandlers.LocalSimulatorHandler;
import cz.agents.highway.environment.SimulatorHandlers.ModuleSimulatorHandler;
import cz.agents.highway.environment.SimulatorHandlers.PlanCallback;
import cz.agents.highway.environment.roadnet.XMLReader;
import cz.agents.highway.storage.HighwayEventType;
import cz.agents.highway.storage.HighwayStorage;
import cz.agents.highway.storage.RadarData;
import cz.agents.highway.storage.plan.PlansOut;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by david on 7/13/16.
 */
public class AgentDrive extends DefaultCreator implements EventHandler,Runnable {

    private final Logger logger = Logger.getLogger(DashBoardController.class);
    protected long timestep;
    private PlanCallback plancallback;
    public AgentDrive(String configFileLocation)
    {
        CONFIG_FILE = configFileLocation;
    }
    @Override
    public void run() {
        init();
        create();
    }
    public void init()
    {
        // Configuration loading using alite's Configurator and ConfigReader
        ConfigReader configReader = new ConfigReader();
        configReader.loadAndMerge(DEFAULT_CONFIG_FILE);
        configReader.loadAndMerge(CONFIG_FILE);

        Configurator.init(configReader);


        String logfile = Configurator.getParamString("cz.highway.configurationFile", "settings/log4j/log4j.properties");
        PropertyConfigurator.configure(logfile);

        logger.setLevel(Level.INFO);
        logger.info("Configuration loaded from: " + CONFIG_FILE);
        if(logger.isDebugEnabled()){
            logger.debug("Printing complete configuration on the System.out >>");
            configReader.writeTo(new PrintWriter(System.out));
        }
        logger.info("log4j logger properties loaded from: " + logfile);
    }
    public void update(RadarData radarData)
    {
        //highwayEnvironment.getEventProcessor().addEvent(HighwayEventType.RADAR_DATA, highwayEnvironment.getStorage(), null, radarData, Math.max(1, (long) (timestep * 100)));
        highwayEnvironment.getStorage().updateCars(radarData);
    }


    public void registerPlanCallback(PlanCallback plancallback)
    {
        this.plancallback = plancallback;
    }

    @Override
    public void create() {

        timestep = Configurator.getParamInt("highway.timestep", 100);
        super.create();
        simulation.addEventHandler(this);
        runSimulation();
    }

    @Override
    public void runSimulation() {
        initTraffic();
        simulation.run();

    }

    private void initTraffic() {
        final XMLReader reader = new XMLReader(Configurator.getParamString("highway.net.folder", "notDefined"));
        // All vehicle id's
        final Collection<Integer> vehicles = reader.getRoutes().keySet();
        final Map<Integer, Float> departures = reader.getDepartures();
        // final int size = vehicles.size();
        final int size;
        if (!Configurator.getParamBool("highway.dashboard.sumoSimulation", true)) {
            size = Configurator.getParamInt("highway.dashboard.numberOfCarsInSimulation", vehicles.size());
        } else {
            size = vehicles.size();
        }
        final int simulatorCount = Configurator.getParamList("highway.dashboard.simulatorsToRun", String.class).size();
        final HighwayStorage storage = highwayEnvironment.getStorage();
        // Divide vehicles evenly to the simulators
        //
        Iterator<Integer> vehicleIt = vehicles.iterator();
        PlansOut plans = new PlansOut();
        //   RadarData update = ;
        Map<Integer, Agent> agents = storage.getAgents();
        Set<Integer> plannedVehiclesLocal = new HashSet<Integer>();
        int sizeL = size;
        if (size > vehicles.size()) sizeL = vehicles.size();
        // Iterate over all configured vehicles

        for (int i = 0; i < sizeL; i++) {
            int vehicleID = vehicleIt.next();
            if (Configurator.getParamBool("highway.dashboard.sumoSimulation", true)) {
                storage.addForInsert(vehicleID, departures.get(vehicleID));
            } else {
                storage.addForInsert(vehicleID);
            }
            plannedVehiclesLocal.add(vehicleID);
        }
        final Set<Integer> plannedVehicles = plannedVehiclesLocal;
        highwayEnvironment.addSimulatorHandler(new ModuleSimulatorHandler(highwayEnvironment, new HashSet<Integer>(plannedVehicles),plancallback));

    }

    @Override
    public EventProcessor getEventProcessor() {
        return simulation;
    }

    @Override
    public void handleEvent(Event event) {
        if (event.isType(SimulationEventType.SIMULATION_STARTED)) {
            System.out.println("Caught SIMULATION_STARTED from DashBoard");
            getEventProcessor().addEvent(HighwayEventType.TIMESTEP, null, null, null, timestep);
        }
        else if (event.isType(HighwayEventType.TIMESTEP)) {
            getEventProcessor().addEvent(HighwayEventType.TIMESTEP, null, null, null, timestep);
        }
    }


}
