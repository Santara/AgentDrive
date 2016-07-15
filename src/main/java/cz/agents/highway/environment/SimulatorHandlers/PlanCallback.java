package cz.agents.highway.environment.SimulatorHandlers;


import cz.agents.highway.storage.plan.PlansOut;

import java.util.Map;

/**
 * Created by david on 7/13/16.
 */
public interface PlanCallback {
    public void execute(PlansOut plans);
}
