package cz.agents.highway.SimulatorLite.controller;

import cz.agents.highway.SimulatorLite.storage.vehicle.Vehicle;

/**
 * Interface that updates vehicle's velocity based on the steering angle and pedal state
 *
 * Created by wmatex on 8.7.14.
 */
public interface ControllerInterface {
    public void updateVehicleVelocity(Vehicle vehicle, long deltaTime, float steeringAngle, float pedalState);
}
