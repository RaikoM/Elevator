package com.testreel;

public class Rider implements Runnable{

    private int riderId;
    private ElevatorControlSystem elevatorControlSystem;
    private int startFloor;
    private int stopFloor;
    private Thread thread;

    public Rider(int riderId, ElevatorControlSystem elevatorControlSystem, int startFloor, int stopFloor) {
        this.riderId = riderId;
        this.elevatorControlSystem = elevatorControlSystem;
        this.startFloor = startFloor;
        this.stopFloor = stopFloor;
        this.thread = new Thread(this, ""+this.riderId);
    }

    public Thread getThread() {
        return this.thread;
    }

    @Override
    public void run() {
        Elevator elevator;
        while (true){
            if (startFloor < stopFloor){
                System.out.println("Rider " + this.riderId + " wants to go up from floor " + (startFloor + 1));
                elevator = elevatorControlSystem.callUp(startFloor, riderId);
            } else {
                System.out.println("Rider " + this.riderId + " wants to go down from floor " + (startFloor + 1));
                elevator = elevatorControlSystem.callDown(startFloor, riderId);
            }

            System.out.println("Rider " + this.riderId + " is about to enter elevator " + elevator.getElevatorId());

            if (elevator.enter(this.riderId)){
                System.out.println("Rider " + this.riderId + " has entered elevator " + elevator.getElevatorId());
                break;
            }
        }

        System.out.println("Rider " + this.riderId + " on elevator " + elevator.getElevatorId() + " wants to go to floor " + (stopFloor + 1));
        elevator.requestFloor(stopFloor, this.riderId);

        System.out.println("Rider" + this.riderId + " exits elevator " + elevator.getElevatorId() + " on floor " + (stopFloor + 1));
        elevator.exit(this.riderId);
    }
}