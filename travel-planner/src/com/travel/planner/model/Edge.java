package com.travel.planner.model;

public class Edge{
    public String source;
    public String destination;
    public String mode;
    public double cost;
    public double distance;
    public double time;

    public Edge(String source, String destination, String mode, double cost, double distance, double time){
        this.source = source;
        this.destination = destination;
        this.mode = mode;
        this.cost = cost;
        this.distance = distance;
        this.time = time;
    }
}