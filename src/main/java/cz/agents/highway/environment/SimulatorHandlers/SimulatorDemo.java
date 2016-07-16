package cz.agents.highway.environment.SimulatorHandlers;

import cz.agents.alite.configurator.Configurator;
import cz.agents.highway.SimulatorLite.SimulatorCreator;
import cz.agents.highway.creator.AgentDrive;
import cz.agents.highway.storage.RadarData;
import cz.agents.highway.storage.RoadObject;
import cz.agents.highway.storage.plan.Action;
import cz.agents.highway.storage.plan.PlansOut;
import cz.agents.highway.storage.plan.WPAction;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.util.*;

/**
 * Created by david on 7/13/16.
 */

public class SimulatorDemo {
    private AgentDrive agentDrive;
    private RadarData simulatorState;
    private Boolean newPlan = false;
    private final Object mutex = new Object();
    public SimulatorDemo() {

    }
    private void run() throws InterruptedException {
        PlanCallback plc = new PlanCallbackImp();
        agentDrive = new AgentDrive("settings/groovy/local/david.groovy");
        agentDrive.registerPlanCallback(plc);
        Thread t = new Thread(agentDrive,"AgentDrive");
        t.start();
        while(true){
            synchronized (mutex)
            {
                while(!newPlan) {
                    mutex.wait();
                }
               // System.out.println("Excelent job");
                Thread.sleep(1000);
                agentDrive.update(simulatorState);
                newPlan = false;
            }
        }

    }
    public static void main(String[] args) throws InterruptedException {
        SimulatorDemo sim = new SimulatorDemo();
        sim.run();

    }
    class PlanCallbackImp implements PlanCallback {
    //final HashSet<Integer> plannedVehicles = new HashSet<Integer>();
    @Override
    public void execute(PlansOut plans) {
        System.out.println("Recieved plans " + plans);
        Map<Integer, RoadObject> currStates = plans.getCurrStates();
        RadarData radarData = new RadarData();
        float duration = 0;
        float lastDuration = 0;
        double timest = Configurator.getParamDouble("highway.SimulatorLocal.timestep", 1.0);
        float timestep = (float) timest;

        boolean removeCar = false;
        for (Integer carID : plans.getCarIds()) {
            Collection<Action> plan = plans.getPlan(carID);
            RoadObject state = currStates.get(carID);
            Point3f lastPosition = state.getPosition();
            Point3f myPosition = state.getPosition();
            for (Action action : plan) {
                if (action.getClass().equals(WPAction.class)) {
                    WPAction wpAction = (WPAction) action;
                    if (wpAction.getSpeed() == -1) {
                        myPosition = wpAction.getPosition();
                        removeCar = true;
                    }
                    if (wpAction.getSpeed() < 0.001) {
                        duration += 0.1f;
                    } else {
                        myPosition = wpAction.getPosition();
                        lastDuration = (float) (wpAction.getPosition().distance(lastPosition) / (wpAction.getSpeed()));
                        duration += wpAction.getPosition().distance(lastPosition) / (wpAction.getSpeed());
                    }
                    // creating point between the waipoints if my duration is greater than the defined timestep
                    if (duration >= timestep) {

                        float remainingDuration = timestep - (duration - lastDuration);
                        float ration = remainingDuration / lastDuration;
                        float x = myPosition.x - lastPosition.x;
                        float y = myPosition.y - lastPosition.y;
                        float z = myPosition.z - lastPosition.z;
                        Vector3f vec = new Vector3f(x, y, z);
                        vec.scale(ration);

                        myPosition = new Point3f(vec.x + lastPosition.x, vec.y + lastPosition.y, vec.z + lastPosition.z);
                        break;
                    }
                    lastPosition = wpAction.getPosition();
                }
            }
            if (!removeCar) {
                Vector3f vel = new Vector3f(state.getPosition());
                vel.negate();
                vel.add(myPosition);
                if (vel.length() < 0.0001) {
                    vel = state.getVelocity();
                    vel.normalize();
                    vel.scale(0.001f);
                }
                int lane = -1;
                state = new RoadObject(carID, System.currentTimeMillis(), lane, myPosition, vel);
                radarData.add(state);
                duration = 0;
            }
        }
        synchronized (mutex) {
            simulatorState = radarData;
            newPlan = true;
            mutex.notifyAll();
        }
    }
}
}

