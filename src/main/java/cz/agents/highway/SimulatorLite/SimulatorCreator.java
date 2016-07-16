package cz.agents.highway.SimulatorLite;

import cz.agents.highway.SimulatorLite.visualization.NetVisLayer;
import cz.agents.highway.SimulatorLite.visualization.TrafficVisLayer;
import cz.agents.highway.SimulatorLite.visualization.ZoomVehicleLayer;
import cz.agents.alite.configreader.ConfigReader;
import cz.agents.alite.configurator.Configurator;
import cz.agents.alite.creator.Creator;
import cz.agents.alite.creator.CreatorFactory;
import cz.agents.alite.simulation.Simulation;
import cz.agents.alite.vis.VisManager;
import cz.agents.alite.vis.layer.common.*;
import cz.agents.highway.storage.plan.PlansOut;
import cz.agents.highway.util.Utils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import javax.vecmath.Point2d;
import java.awt.*;
import java.io.FileNotFoundException;

/**
 * Creates and initializes the simulation environment and the Alite event processor
 *
 * Created by wmatex on 3.7.14.
 */
public class SimulatorCreator implements Creator {
    private Simulation simulation;
    private SimulatorEnvironment environment;
    private static final String CONFIG_FILE = "settings/groovy/config.groovy";
    private static String HIGHWAY_CONFIG_FILE;
    private static final Logger logger = Logger.getLogger(SimulatorCreator.class);

    @Override
    public void init(String[] strings) {
        ConfigReader configReader = new ConfigReader();
        if(strings.length>1){
            HIGHWAY_CONFIG_FILE = strings[1];
            configReader.loadAndMerge(HIGHWAY_CONFIG_FILE);
        }
        configReader.loadAndMerge(CONFIG_FILE);
        Configurator.init(configReader);

        String logfile = Configurator.getParamString("simulator.lite.configurationFile", "settings/log4j/log4j.properties");
        PropertyConfigurator.configure(logfile);

        logger.info("log4j logger properties loaded from: " + logfile);
        logger.setLevel(Level.INFO);
    }

    @Override
    public void create() {
        simulation = new Simulation();
        double simulationSpeed = Configurator.getParamDouble("simulator.lite.simulationSpeed", 1.0);
        simulation.setSimulationSpeed(simulationSpeed);
        environment = new SimulatorEnvironment(simulation);
        environment.init();

       // createVisualization();
    }

    private void createVisualization() {
        VisManager.setInitParam(Configurator.getParamString("simulator.lite.name","Simulator-Lite"), 1024, 768);
        VisManager.setSceneParam(new VisManager.SceneParams() {

            @Override
            public Point2d getDefaultLookAt() {
                return new Point2d(0, 0);
            }

            @Override
            public double getDefaultZoomFactor() {
                return 3;
            }
        });

        VisManager.init();
        

        // Overlay
        //VisManager.registerLayer(ColorLayer.create(Color.LIGHT_GRAY));
        VisManager.registerLayer(ColorLayer.create(new Color(210, 255, 165)));
        VisManager.registerLayer(FpsLayer.create());
        try {
            VisManager.registerLayer(LogoLayer.create(Utils.getResourceUrl("img/atg_blue.png")));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        VisManager.registerLayer(VisInfoLayer.create());

        VisManager.registerLayer(HelpLayer.create());

        // Traffic visualization layer
        VisManager.registerLayer(NetVisLayer.create());
        VisManager.registerLayer(TrafficVisLayer.create(environment.getStorage()));
        VisManager.registerLayer(ZoomVehicleLayer.create(environment.getStorage()));         
        

    }

    public void runSimulation() {
        simulation.run();
    }

    public static void main(String[] args) {
        SimulatorCreator creator = (SimulatorCreator) CreatorFactory.createCreator(args);
        creator.init(args);
        creator.create();
        creator.runSimulation();
    }

}
