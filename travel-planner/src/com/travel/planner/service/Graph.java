package com.travel.planner.service;

import com.travel.planner.model.Edge;
import com.travel.planner.model.PathResult;
import com.travel.planner.util.DBManager;

import java.sql.*;
import java.util.*;

public class Graph {
    private final Map<String, List<Edge>> adjacencyList = new HashMap<>();
    private static final double ROAD_COST_PER_KM = 2;
    private static final double ROAD_SPEED = 60;

    public void loadFromDatabase() {
        System.out.println("Loading routes from database...");
        String query = "SELECT * FROM routes";
        try (Connection conn = DBManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            int count = 0;
            while (rs.next()) {
                count++;
                String mode = rs.getString("mode");
                String source = rs.getString("source");
                String dest = rs.getString("destination");
                double cost = rs.getDouble("cost");
                double distance = rs.getDouble("distance");
                double time = rs.getDouble("time");

                System.out.printf("Loading route %d: %s to %s by %s%n", count, source, dest, mode);

                addEdge(source, dest, mode, cost, distance, time, false);
            }

            System.out.println("Successfully loaded " + count + " routes");

        } catch (SQLException e) {
            System.err.println("Error loading from database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean addEdge(String source, String destination, String mode, double cost, double distance, double time,
            boolean saveToDb) {
        try {
            adjacencyList.putIfAbsent(source, new ArrayList<>());
            adjacencyList.get(source).add(new Edge(source, destination, mode, cost, distance, time));

            adjacencyList.putIfAbsent(destination, new ArrayList<>());
            adjacencyList.get(destination).add(new Edge(destination, source, mode, cost, distance, time));

            if (saveToDb) {
                return saveRouteToDatabase(source, destination, mode, cost, distance, time);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean saveRouteToDatabase(String source, String dest, String mode, double cost, double distance,
            double time) {

        String sql = "INSERT INTO routes (source, destination, mode, cost, distance, time) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, source);
            pstmt.setString(2, dest);
            pstmt.setString(3, mode);
            pstmt.setDouble(4, cost);
            pstmt.setDouble(5, distance);
            pstmt.setDouble(6, time);

            int rowsAffected = pstmt.executeUpdate();
            conn.commit();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            if (e.getMessage().contains("Duplicate entry")) {
                System.out.println("Route already exists: " + source + "-" + dest + "-" + mode);
            }
            return false;
        }
    }

    public boolean addRoadRoute(String source, String destination, double distance) {
        try {
            double cost = distance * ROAD_COST_PER_KM;
            double time = distance / ROAD_SPEED;

            boolean forward = addEdge(source, destination, "road", cost, distance, time, true);
            boolean reverse = addEdge(destination, source, "road", cost, distance, time, true);

            return forward && reverse;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean addTrainRoute(String source, String destination, double fare, double distance, double time) {
        try {

            boolean forward = addEdge(source, destination, "train", fare, distance, time, true);
            boolean reverse = addEdge(destination, source, "train", fare, distance, time, true);

            return forward && reverse;

        } catch (Exception e) {
            return false;
        }
    }

    public boolean addAirplaneRoute(String source, String destination, double fare, double time) {
        try {
            return addEdge(source, destination, "airplane", fare, 0, time, true);
        } catch (Exception e) {
            return false;
        }
    }

    public Set<String> getNodes() {
        return adjacencyList.keySet();
    }

    public List<Edge> getEdges() {
        List<Edge> allEdges = new ArrayList<>();
        for (List<Edge> edges : adjacencyList.values()) {
            allEdges.addAll(edges);
        }
        return allEdges;
    }

    public PathResult findShortestPath(String start, String end, String criterion) {

        if (!adjacencyList.containsKey(start) || !adjacencyList.containsKey(end)) {
            return new PathResult(Collections.emptyList(), 0, 0, 0);
        }

        Map<String, Double> distances = new HashMap<>();
        Map<String, Edge> previousEdges = new HashMap<>();
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>();

        for (String node : adjacencyList.keySet()) {
            distances.put(node, Double.POSITIVE_INFINITY);
        }

        distances.put(start, 0.0);
        queue.add(new NodeDistance(start, 0.0));

        // Dijkstra's Algorithm

        while (!queue.isEmpty()) {

            NodeDistance current = queue.poll();
            String currentNode = current.node;

            if (currentNode.equals(end)) {
                break;
            }

            if (current.distance > distances.get(currentNode)) {
                continue;
            }

            for (Edge edge : adjacencyList.getOrDefault(currentNode, Collections.emptyList())) {

                String neighbor = edge.destination;

                double weight = switch (criterion.toLowerCase()) {
                    case "cost" -> edge.cost;
                    case "distance" -> edge.distance;
                    case "time" -> edge.time;
                    default -> throw new IllegalArgumentException("Invalid criterion: " + criterion);
                };

                double newDist = distances.get(currentNode) + weight;

                if (newDist < distances.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    distances.put(neighbor, newDist);
                    previousEdges.put(neighbor, edge);
                    queue.add(new NodeDistance(neighbor, newDist));
                }
            }
        }

        List<Edge> path = new ArrayList<>();
        String current = end;

        while (previousEdges.containsKey(current)) {
            Edge edge = previousEdges.get(current);
            path.add(edge);
            current = edge.source;
        }
        Collections.reverse(path);

        double totalCost = path.stream().mapToDouble(e -> e.cost).sum();
        double totalDistance = path.stream().mapToDouble(e -> e.distance).sum();
        double totalTime = path.stream().mapToDouble(e -> e.time).sum();

        return new PathResult(path, totalCost, totalDistance, totalTime);
    }

    private static class NodeDistance implements Comparable<NodeDistance> {

        String node;
        double distance;

        NodeDistance(String node, double distance) {
            this.node = node;
            this.distance = distance;
        }

        @Override
        public int compareTo(NodeDistance other) {
            return Double.compare(this.distance, other.distance);
        }
    }
}
