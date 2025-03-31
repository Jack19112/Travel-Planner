package com.travel.planner.gui;

import com.travel.planner.model.PathResult;
import com.travel.planner.service.Graph;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

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

        String buttonStyle = "-fx-padding: 10 20; -fx-font-size: 14;";
        searchButton.setStyle(buttonStyle + "-fx-background-color: #4CAF50; -fx-text-fill: white;");
        addRoadBtn.setStyle(buttonStyle);
        addTrainBtn.setStyle(buttonStyle);
        addAirplaneBtn.setStyle(buttonStyle);

        searchButton.setOnAction(e -> showAllPaths());
        addRoadBtn.setOnAction(e -> showAddRouteDialog("road"));
        addTrainBtn.setOnAction(e -> showAddRouteDialog("train"));
        addAirplaneBtn.setOnAction(e -> showAddRouteDialog("airplane"));

        HBox buttonBox = new HBox(15, searchButton, addRoadBtn, addTrainBtn, addAirplaneBtn);
        buttonBox.setPadding(new Insets(15, 0, 15, 0));

        return buttonBox;
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