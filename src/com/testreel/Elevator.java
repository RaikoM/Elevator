package com.testreel;
import java.util.TreeSet;

import static java.lang.Thread.interrupted;

public class Elevator implements Runnable {
    private final int MIN_FLOOR = 1;

    private int elevatorId;
    private int numFloors;
    private int currentFloor;
    private int numRiders;
    private boolean isGoingUp;
    private boolean isDoorOpen;
    private ElevatorControlSystem elevatorControlSystem;
    private TreeSet<Integer> upRequests;
    private TreeSet<Integer> downRequests;
    private EventBarrier[] floors;
    private Thread thread;


    public Elevator(int id, int numFloors, ElevatorControlSystem elevatorControlSystem){
        this.elevatorId = id;
        this.numFloors = numFloors;
        this.currentFloor = MIN_FLOOR;
        this.numRiders = 0;
        this.isGoingUp = true;
        this.isDoorOpen = false;
        this.elevatorControlSystem = elevatorControlSystem;
        this.upRequests = new TreeSet<Integer>();
        this.downRequests = new TreeSet<Integer>();

        this.floors = new EventBarrier[numFloors];
        for (int i = 0; i < this.numFloors; i++){
            this.floors[i] = new EventBarrier();
        }
        this.thread = new Thread(this, ""+this.elevatorId);
    }

    public Thread getThread(){
        return this.thread;
    }

    public boolean isGoingUp(){
        return this.isGoingUp;
    }

    public boolean isDoorOpen(){
        return this.isDoorOpen;
    }

    public int getElevatorId(){
        return this.elevatorId;
    }

    private void pauseThread(int floors){
        try {
            System.out.println("Pause time " + (floors * 2) + " seconds!");
            Thread.sleep(floors * 2000);
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    public void openDoors(){
        this.isDoorOpen = true;
        System.out.println("Elevator opens doors on floor " + this.currentFloor);
        int numWaiters = this.floors[this.currentFloor].waiters();
        if (numWaiters == 1){
            System.out.println("Floor " + this.currentFloor + " has " + numWaiters + " rider waiting on elevator " + this.elevatorId);
        } else {
            System.out.println("Floor " + this.currentFloor + " has " + numWaiters + " riders waiting on elevator " + this.elevatorId);
        }
        this.floors[this.currentFloor].raise(this.elevatorId, this.currentFloor);
    }


    public void closedDoors(){
        this.isDoorOpen = false;
        synchronized (this.elevatorControlSystem){
            this.elevatorControlSystem.notifyAll();
        }
        System.out.println("Elevator " + this.elevatorId +" doors closed on " + this.currentFloor);
    }

    public synchronized void visitFloor(int floor){
        if (this.isGoingUp){
            this.upRequests.remove(floor);
        } else {
            this.downRequests.remove(floor);
        }

        this.elevatorControlSystem.removeStop(floor, this.isGoingUp);
        if(this.isGoingUp){
            pauseThread(floor - currentFloor);
            System.out.println("Elevator " + this.elevatorId + " has moved to floor " + floor);
        } else {
            pauseThread(currentFloor - floor);
            System.out.println("Elevator " + this.elevatorId + " has moved to floor " + floor);
        }
        this.currentFloor = floor;

    }

    public synchronized boolean enter(int riderId){
        this.numRiders++;
        System.out.println("Rider " + riderId + " enters elevator on floor " + this.currentFloor);
        this.floors[this.currentFloor].complete(this.elevatorId, this.currentFloor);
        return true;
    }

    public synchronized void exit(int riderId){
        this.numRiders--;
        System.out.println("Rider " + riderId + " has exited elevator on floor " + this.currentFloor);
        this.floors[this.currentFloor].complete(this.elevatorId, this.currentFloor);
    }

    public void pass(){
        this.floors[this.currentFloor].complete(this.elevatorId, this.currentFloor);
    }

    public void requestFloor(int floorRequested, int riderId){
        boolean goingUp = floorRequested > this.currentFloor;
        requestFloor(floorRequested, goingUp, riderId);
    }

    public void requestFloor(int floorRequested, boolean goingUp, int riderId){
        synchronized (this){
            if(goingUp){
                this.upRequests.add(floorRequested);
                System.out.println("Elevator " + this.elevatorId + " receives request from rider " + riderId + " to go up to floor " + (floorRequested + 1));
            } else {
                this.downRequests.add(floorRequested);
                System.out.println("Elevator " + this.elevatorId + " receives request from rider " + riderId + " to go down to floor " + (floorRequested + 1));
            }
            this.elevatorControlSystem.addStop(floorRequested, goingUp, this);
            notifyAll();
        }
        this.floors[floorRequested].arrive(this.elevatorId, floorRequested, riderId);
    }

    private synchronized int getNextFloor(){
        if (isGoingUp()){
            Integer next = this.upRequests.higher(MIN_FLOOR);
            boolean hasNext = next != null;
            if (hasNext){
                System.out.println("Elevator " + this.elevatorId + " going up processes request from rider to go up to floor " + next + " from floor " + this.currentFloor);
                return next;
            } else {
                this.isGoingUp = false;
                this.currentFloor = this.floors.length;
                next = this.downRequests.lower(this.currentFloor);
                boolean hasNextRequestFromLowerFloor = next != null;
                if (hasNextRequestFromLowerFloor){
                    System.out.println("Elevator " + this.elevatorId + " going up processes request from rider to go down to floor " + next + " from floor " + this.currentFloor);
                    return next;
                } else {
                    return -1;
                }
            }
        } else {
            Integer next = this.downRequests.lower(this.currentFloor);
            boolean hasNext = next != null;
            if (hasNext && next >= MIN_FLOOR){
                System.out.println("Elevator " + this.elevatorId + " going down processes request from rider to go down to floor " + next + " from floor " + this.currentFloor);
                return next;
            } else {
                this.isGoingUp = true;
                this.currentFloor = MIN_FLOOR;
                next = this.upRequests.higher(currentFloor);
                boolean hasNextRequestFromHigherFloor = next != null;
                if (hasNextRequestFromHigherFloor){
                    System.out.println("Elevator " + this.elevatorId + "  going down processes request from rider to go up to floor " + next + " from floor " + this.currentFloor);
                    return next;
                } else {
                    return -1;
                }
            }
        }
    }

    public void run(){
        while(true){
            if(interrupted()) return;

            int nextFloor = getNextFloor();
            boolean noMoreRequests = this.upRequests.isEmpty() && this.downRequests.isEmpty();

            if (noMoreRequests){
                synchronized (this){
                    this.currentFloor = -1;
                    try {
                        System.out.println("Elevator " + this.elevatorId + " is waiting for rider requests");
                        wait();
                    } catch (InterruptedException e){
                        return;
                    }
                }
            } else if (nextFloor != -1){
                System.out.println("Elevator " + this.elevatorId + " will move up to floor " + nextFloor);
                visitFloor(nextFloor);
                openDoors();
                closedDoors();
            }
        }
    }
}