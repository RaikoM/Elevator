package com.testreel;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {

    public static void main(String[] args) throws InterruptedException, IOException {
        System.out.println("Launching Elevator");
        System.out.println();

        int numFloors = 13;
        int numElevators = 3;
        int numRiders = 0;
        List<Rider> riderList = new ArrayList<Rider>();


        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Enter integer for number of riders in building: ");
        try {
            numRiders = Integer.parseInt(bufferedReader.readLine());
        } catch (NumberFormatException e){
            System.err.println("Invalid number format!");
        }

        bufferedReader.close();

        System.out.println();
        System.out.println("-----------");
        System.out.println("Number of floors in building: "  + numFloors);
        System.out.println("Number of elevators: " + numElevators);
        System.out.println("Number of riders: " + numRiders);

        File log = new File("elevator.log");
        System.setOut(new PrintStream(new FileOutputStream(log)));

        ElevatorControlSystem building = new ElevatorControlSystem(numFloors, numElevators);

        Random startFloor = new Random();
        Random stopFloor = new Random();
        for (int i = 0; i < numRiders; i++){
            Rider rider = new Rider(i, building, startFloor.nextInt(numFloors), stopFloor.nextInt(numFloors));
            riderList.add(rider);
        }

        //Start rider threads
        for (Rider rider : riderList){
            rider.getThread().start();
        }

        //Start elevators
        for (Elevator elevator : building.getElevators()){
            elevator.getThread().start();
        }

        //Wait for rider threads to terminate
        for (Rider rider : riderList){
            rider.getThread().join();
        }

        //Stop elevators
        for (Elevator elevator : building.getElevators()){
            elevator.getThread().interrupt();
        }

    }
}