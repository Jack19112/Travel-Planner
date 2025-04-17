package com.travel.planner.gui;

import com.travel.planner.model.Edge;
import com.travel.planner.model.PathResult;
import com.travel.planner.service.Graph;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TravelPlannerGUI extends Application {
    private Graph graph = new Graph();
    private ListView<String> resultList = new ListView<>();
    private TextField sourceField = new TextField();
    private TextField destField = new TextField();

    @Override
    public void start(Stage primaryStage) {
        graph.loadFromDatabase();

        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(20));

        GridPane inputGrid = createInputGrid();
        HBox actionButtons = createActionButtons();
        VBox resultBox = new VBox(10, new Label("Optimal Paths:"), resultList);

        mainLayout.getChildren().addAll(inputGrid, actionButtons, resultBox);

        Scene scene = new Scene(mainLayout, 1000, 700);
        primaryStage.setTitle("Travel Route Planner");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private GridPane createInputGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.addRow(0, new Label("From:"), sourceField);
        grid.addRow(1, new Label("To:"), destField);

        sourceField.setPrefWidth(300);
        destField.setPrefWidth(300);

        return grid;
    }

    private HBox createActionButtons() {
        Button searchButton = new Button("Find Routes");
        Button addRoadBtn = new Button("Add Road Route");
        Button addTrainBtn = new Button("Add Train Route");
        Button addAirplaneBtn = new Button("Add Airplane Route");
        Button showGraphBtn = new Button("Show Graph");

        String buttonStyle = "-fx-padding: 10 20; -fx-font-size: 14;";
        searchButton.setStyle(buttonStyle + "-fx-background-color: #4CAF50; -fx-text-fill: white;");
        addRoadBtn.setStyle(buttonStyle);
        addTrainBtn.setStyle(buttonStyle);
        addAirplaneBtn.setStyle(buttonStyle);
        showGraphBtn.setStyle(buttonStyle + "-fx-background-color: #9C27B0; -fx-text-fill: white;");

        searchButton.setOnAction(e -> showAllPaths());
        addRoadBtn.setOnAction(e -> showAddRouteDialog("road"));
        addTrainBtn.setOnAction(e -> showAddRouteDialog("train"));
        addAirplaneBtn.setOnAction(e -> showAddRouteDialog("airplane"));
        showGraphBtn.setOnAction(e -> showGraph());

        HBox buttonBox = new HBox(15, searchButton, addRoadBtn, addTrainBtn, addAirplaneBtn, showGraphBtn);
        buttonBox.setPadding(new Insets(15, 0, 15, 0));

        return buttonBox;
    }

    private void showGraph() {
        Stage graphStage = new Stage();
        graphStage.setTitle("Transportation Network Graph");

        Pane graphPane = new Pane();
        graphPane.setPrefSize(1200, 800);

        Set<String> nodes = graph.getNodes();
        List<Edge> edges = graph.getEdges();

        double centerX = 600;
        double centerY = 400;
        double radius = 300;
        double angleStep = 2 * Math.PI / nodes.size();

        Map<String, Point2D> nodePositions = new HashMap<>();
        int index = 0;
        for (String node : nodes) {
            double angle = index * angleStep;
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            nodePositions.put(node, new Point2D(x, y));
            index++;
        }

        for (Edge edge : edges) {
            Point2D sourcePos = nodePositions.get(edge.source);
            Point2D destPos = nodePositions.get(edge.destination);

            if (sourcePos != null && destPos != null) {
                Line line = new Line(
                        sourcePos.getX(), sourcePos.getY(),
                        destPos.getX(), destPos.getY());

                switch (edge.mode) {
                    case "road" -> line.setStroke(Color.FORESTGREEN);
                    case "train" -> line.setStroke(Color.ORANGE);
                    case "airplane" -> line.setStroke(Color.DODGERBLUE);
                    default -> line.setStroke(Color.GRAY);
                }

                line.setStrokeWidth(2);
                graphPane.getChildren().add(line);

                String edgeInfo = String.format("%s (₹%.0f, %.0fkm, %.1fh)",
                        edge.mode, edge.cost, edge.distance, edge.time);

                Text label = new Text(
                        (sourcePos.getX() + destPos.getX()) / 2,
                        (sourcePos.getY() + destPos.getY()) / 2,
                        edgeInfo);
                label.setFont(Font.font(10));
                label.setFill(Color.DARKSLATEGRAY);
                graphPane.getChildren().add(label);
            }
        }

        for (String node : nodes) {
            Point2D pos = nodePositions.get(node);
            if (pos != null) {
                Circle circle = new Circle(pos.getX(), pos.getY(), 20);
                circle.setFill(Color.LIGHTBLUE);
                circle.setStroke(Color.DARKBLUE);
                circle.setStrokeWidth(2);

                Text label = new Text(pos.getX() - 15, pos.getY() + 30, node);
                label.setFont(Font.font(12));
                label.setFill(Color.DARKBLUE);

                graphPane.getChildren().addAll(circle, label);
            }
        }

        VBox legend = new VBox(5);
        legend.setPadding(new Insets(10));
        legend.setStyle("-fx-background-color: rgba(255,255,255,0.8); -fx-border-color: #ccc;");
        legend.setLayoutX(20);
        legend.setLayoutY(20);

        legend.getChildren().add(new Label("Transport Modes:"));
        legend.getChildren().add(createLegendItem("Road", Color.FORESTGREEN));
        legend.getChildren().add(createLegendItem("Train", Color.ORANGE));
        legend.getChildren().add(createLegendItem("Airplane", Color.DODGERBLUE));

        graphPane.getChildren().add(legend);

        ScrollPane scrollPane = new ScrollPane(graphPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        Scene scene = new Scene(scrollPane, 1000, 700);
        graphStage.setScene(scene);
        graphStage.show();
    }

    private HBox createLegendItem(String text, Color color) {
        HBox item = new HBox(5);
        Rectangle colorBox = new Rectangle(15, 15, color);
        item.getChildren().addAll(colorBox, new Label(text));
        return item;
    }

    private void showAddRouteDialog(String mode) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add " + capitalize(mode) + " Route");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField sourceField = new TextField();
        TextField destField = new TextField();
        TextField fareField = new TextField();
        TextField distanceField = new TextField();
        TextField timeField = new TextField();

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        Node addButton = dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);

        Runnable validateFields = () -> {
            boolean isValid = !sourceField.getText().trim().isEmpty()
                    && !destField.getText().trim().isEmpty()
                    && !timeField.getText().trim().isEmpty();

            try {
                if (!mode.equals("road")) {
                    Double.parseDouble(fareField.getText());
                }
                if (!mode.equals("airplane")) {
                    Double.parseDouble(distanceField.getText());
                }
                Double.parseDouble(timeField.getText());
            } catch (NumberFormatException e) {
                isValid = false;
            }

            addButton.setDisable(!isValid);
        };

        sourceField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        destField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        fareField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        distanceField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        timeField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());

        if (mode.equals("airplane")) {
            grid.addRow(0, new Label("From:"), sourceField);
            grid.addRow(1, new Label("To:"), destField);
            grid.addRow(2, new Label("Fare:"), fareField);
            grid.addRow(3, new Label("Time (hours):"), timeField);
        } else {
            grid.addRow(0, new Label("From:"), sourceField);
            grid.addRow(1, new Label("To:"), destField);
            grid.addRow(2, new Label(mode.equals("road") ? "Distance (km):" : "Fare:"),
                    mode.equals("road") ? distanceField : fareField);
            grid.addRow(3, new Label("Time (hours):"), timeField);
            if (mode.equals("train")) {
                grid.addRow(4, new Label("Distance (km):"), distanceField);
            }
        }

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == addButtonType) {
                try {
                    String source = sourceField.getText().trim();
                    String dest = destField.getText().trim();
                    double fare = mode.equals("road") ? 0 : Double.parseDouble(fareField.getText());
                    double distance = mode.equals("airplane") ? 0 : Double.parseDouble(distanceField.getText());
                    double time = Double.parseDouble(timeField.getText());

                    boolean success = false;
                    switch (mode) {
                        case "road" -> success = graph.addRoadRoute(source, dest, distance);
                        case "train" -> success = graph.addTrainRoute(source, dest, fare, distance, time);
                        case "airplane" -> success = graph.addAirplaneRoute(source, dest, fare, time);
                    }

                    if (success) {
                        showAlert("Success", "Route successfully added to database!", Alert.AlertType.INFORMATION);
                    } else {
                        showAlert("Error", "Failed to save route to database", Alert.AlertType.ERROR);
                    }
                } catch (NumberFormatException ex) {
                    showAlert("Input Error", "Please enter valid numeric values", Alert.AlertType.ERROR);
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showAllPaths() {
        resultList.getItems().clear();
        String start = sourceField.getText().trim();
        String end = destField.getText().trim();

        if (start.isEmpty() || end.isEmpty()) {
            showAlert("Input Error", "Please enter both source and destination", Alert.AlertType.ERROR);
            return;
        }

        try {
            displayResult("Cheapest Route", graph.findShortestPath(start, end, "cost"));
            displayResult("Fastest Route", graph.findShortestPath(start, end, "time"));
            displayResult("Shortest Distance", graph.findShortestPath(start, end, "distance"));
        } catch (Exception e) {
            showAlert("Error", "No path found between locations", Alert.AlertType.ERROR);
        }
    }

    private void displayResult(String title, PathResult result) {
        resultList.getItems().add("=== " + title + " ===");
        result.path.forEach(edge -> resultList.getItems().add(String.format("%s → %s by %s (₹%.2f, %.2fkm, %.2fh)",
                edge.source, edge.destination, edge.mode, edge.cost, edge.distance, edge.time)));
        resultList.getItems().add(String.format("Total: ₹%.2f | %.2fkm | %.2fh%n", result.totalCost,
                result.totalDistance, result.totalTime));
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
