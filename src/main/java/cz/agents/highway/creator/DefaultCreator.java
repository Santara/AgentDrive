package cz.agents.highway.creator;

import java.awt.Color;

import javax.vecmath.Point2d;

import cz.agents.highway.vis.NetLayer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import cz.agents.alite.configreader.ConfigReader;
import cz.agents.alite.configurator.Configurator;
import cz.agents.alite.creator.Creator;
import cz.agents.alite.simulation.Simulation;
import cz.agents.alite.vis.VisManager;
import cz.agents.alite.vis.VisManager.SceneParams;
import cz.agents.alite.vis.layer.common.ColorLayer;
import cz.agents.alite.vis.layer.common.FpsLayer;
import cz.agents.alite.vis.layer.common.HelpLayer;
import cz.agents.alite.vis.layer.common.LogoLayer;
import cz.agents.alite.vis.layer.common.VisInfoLayer;
import cz.agents.highway.environment.HighwayEnvironment;
import cz.agents.highway.util.Utils;
import cz.agents.highway.vis.ProtobufVisLayer;
import cz.agents.highway.vis.SimulationControlLayer;

public class DefaultCreator implements Creator {
    protected Simulation simulation = null;
    protected DashBoardController dashBoard = null;
    protected HighwayEnvironment highwayEnvironment = null;

    private final Logger logger = Logger.getLogger(DefaultCreator.class);

    public void init(String[] args) {

       
        String configfile = "settings/groovy/highway.groovy";
        if(args.length>1){
            configfile = args[1];
        }        

        // Configuration loading using alite's Configurator and ConfigReader
        ConfigReader configReader = new ConfigReader();
        configReader.loadAndMerge(configfile);
        Configurator.init(configReader);
      

        String logfile = Configurator.getParamString("cz.highway.configurationFile", "settings/log4j/log4j.properties");
        PropertyConfigurator.configure(logfile);
        
        logger.info("Configuration loaded from: " + configfile);
        logger.info("log4j logger properties loaded from: " + logfile);
        logger.setLevel(Level.INFO);

    }

    public void create() {
        int seed = Configurator.getParamInt("highway.seed", 0);
        logger.info("Seed set to: " + seed);
        double simulationSpeed = Configurator.getParamDouble("highway.simulationSpeed", 1.0);
        logger.info("Simulation speed: " + simulationSpeed);
        long simulationDuration = Configurator.getParamInt("highway.simulationDuration", 100000);
        logger.info("Simulation duration: " + simulationDuration);

        logger.info("\n>>> SIMULATION CREATION\n");
        simulation = new Simulation(simulationDuration);

        logger.info("\n>>> ENVIRONMENT CREATION\n");
        dashBoard = new DashBoardController(simulation);
        highwayEnvironment = new HighwayEnvironment(simulation, dashBoard.getCommunicator());

        if (Configurator.getParamBool("highway.vis.isOn", false)) {
            logger.info("\n>>> VISUALISATION CREATION\n");
            createVisualization();
            VisManager.registerLayer(new NetLayer(highwayEnvironment.getRoadNetwork()));
            VisManager.registerLayer(ProtobufVisLayer.create(highwayEnvironment.getStorage()));
            VisManager.registerLayer(SimulationControlLayer.create(simulation));
        }
        simulation.setSimulationSpeed(simulationSpeed);
 
    }
    void runSimulation(){
        dashBoard.startSimulation();
         //simulation.run();

    }

    private void createVisualization() {
        logger.info(">>> VISUALIZATION CREATED");

        VisManager.setInitParam("Highway Protobuf Operator", 1024, 768);
        VisManager.setSceneParam(new SceneParams() {

            @Override
            public Point2d getDefaultLookAt() {
                return new Point2d(0, 0);
            }

            @Override
            public double getDefaultZoomFactor() {
                return 0.75;
            }
        });

        VisManager.init();

        // Overlay

        VisManager.registerLayer(ColorLayer.create(Color.LIGHT_GRAY));
        VisManager.registerLayer(VisInfoLayer.create());
        VisManager.registerLayer(FpsLayer.create());
        VisManager.registerLayer(LogoLayer.create(Utils.getResourceUrl("img/atg_blue.png")));
        VisManager.registerLayer(HelpLayer.create());

    }

    @Deprecated
    public static void main(String[] args) {
        System.out.print("RUNNING Highway DefaultCreator.java");
        for (int i = 0; i < args.length; i++) {
            System.out.print(" " + args[0]);
        }
        System.out.println(".");
        DefaultCreator creator = new DefaultCreator();
        creator.init(args);
        creator.create();
        creator.runSimulation();

    }

  

  

   

   
    
}
