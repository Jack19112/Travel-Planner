package com.travel.planner;

import com.travel.planner.gui.TravelPlannerGUI;
import com.travel.planner.util.DBManager;

public class Main {
    public static void main(String[] args) {

        DBManager.initializeDB();

        TravelPlannerGUI.launch(TravelPlannerGUI.class, args);

    }
}