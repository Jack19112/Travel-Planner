package com.travel.planner.model;

import java.util.List;

public class PathResult{
    public List<Edge> path;
    public double totalCost;
    public double totalDistance;
    public double totalTime;

    public PathResult(List<Edge> path, double totalCost, double totalDistance, double totalTime){
        this.path = path;
        this.totalCost = totalCost;
        this.totalDistance = totalDistance;
        this.totalTime = totalTime;
    }
}