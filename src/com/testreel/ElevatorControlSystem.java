package com.testreel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ElevatorControlSystem {
    private int numFloors;

    private List<Elevator> elevators;
    private Elevator[] goingUpStops; //array to keep track of elevators going up
    private Elevator[] goingDownStops; //array to keep track of elevators going down


    public ElevatorControlSystem(int numFloors, int numElevators){
        this.numFloors = numFloors;
        this.elevators = new ArrayList<Elevator>();

        for (int i = 0; i < numElevators; i++){
            elevators.add(new Elevator(i, this.numFloors, this));
        }

        this.goingUpStops = new Elevator[this.numFloors];
        this.goingDownStops = new Elevator[this.numFloors];
    }

    public List<Elevator> getElevators(){
        return this.elevators;
    }


    public synchronized void addStop(int requestedFloor, boolean isGoingUp, Elevator elevator){
        if(isGoingUp){
            this.goingUpStops[requestedFloor] = elevator;
        } else {
            this.goingDownStops[requestedFloor] = elevator;
        }
    }


    public synchronized void removeStop(int arrivedFloor, boolean isGoingUp){
        if(isGoingUp){
            this.goingUpStops[arrivedFloor] = null;
        } else {
            this.goingDownStops[arrivedFloor] = null;
        }
    }


    public synchronized Elevator find(int floor, boolean isGoingUp){
        Elevator elevator;
        boolean hasElevatorGoingUpToFloor = this.goingUpStops[floor] != null;
        boolean hasElevatorGoingDownToFloor = this.goingDownStops[floor] != null;
        if (isGoingUp && hasElevatorGoingUpToFloor){
            elevator = this.goingUpStops[floor];
            return elevator;
        } else if (!isGoingUp && hasElevatorGoingDownToFloor){
            elevator = this.goingDownStops[floor];
            return elevator;
        } else {
            Random random = new Random();
            int randomElevatorIndex = random.nextInt(this.elevators.size());
            elevator = elevators.get(randomElevatorIndex);
            this.addStop(floor, isGoingUp, elevator);
            return elevator;
        }
    }

    private Elevator call(int fromFloor, boolean isGoingUp, int riderId){
        Elevator elevator = find(fromFloor, isGoingUp);
        if (isGoingUp){
            if (fromFloor < 13){
                System.out.println("Elevator " + elevator.getElevatorId() +  " called by rider " + riderId + " on floor " + fromFloor +" to go up");
            } else {
                System.out.println("Can't go up from floor " + fromFloor);
            }

        } else {
            if (fromFloor > 1){
                System.out.println("Elevator " + elevator.getElevatorId() +  " called by rider " + riderId + " on floor " + fromFloor +" to go down");
            } else {
                System.out.println("Can't go down from floor " + fromFloor);
            }

        }
        elevator.requestFloor(fromFloor, isGoingUp, riderId);
        while(elevator.isGoingUp() != isGoingUp){
            elevator.pass();
            synchronized (this){
                while(elevator.isDoorOpen()){
                    try {
                        System.out.println("Elevator" + elevator.getElevatorId() + " is not going in the requested direction.");
                        wait();
                    } catch (InterruptedException e){

                    }
                }
            }
            elevator.requestFloor(fromFloor, isGoingUp, riderId);
        }
        return elevator;
    }


    public Elevator callUp(int fromFloor, int riderId){
        return call(fromFloor, true, riderId);
    }


    public Elevator callDown(int fromFloor, int riderId){
        return call(fromFloor, false, riderId);
    }
}