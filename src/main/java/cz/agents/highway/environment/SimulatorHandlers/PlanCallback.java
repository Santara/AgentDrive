package cz.agents.highway.environment.SimulatorHandlers;

import cz.agents.highway.storage.plan.PlansOut;

/**
 * Created by david on 7/13/16.
 */
public interface PlanCallback {
    void execute(PlansOut plans);
}