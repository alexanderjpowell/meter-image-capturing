package com.slotmachine.ocr.mic;

import java.util.ArrayList;
import java.util.Map;
import javax.annotation.Nullable;
import java.util.Comparator;

public class ToDoListData {

    private int index;
    private String location, machineId, description;
    private String user;
    private ArrayList<String> progressiveDescriptionsList;
    private ArrayList<String> resetValuesList;
    private ArrayList<String> baseValuesList;
    private ArrayList<String> incrementValuesList;
    private Map<String, Object> map;

    public ToDoListData(int index, Map<String, Object> mapp) {
        this.map = mapp;

        this.index = index;
        this.location = mapp.get("l").toString();
        this.machineId = mapp.get("m").toString();
        this.description = mapp.get("d").toString();
        if (mapp.containsKey("u")) { this.user = mapp.get("u").toString(); }
        if (mapp.containsKey("da")) {
            this.progressiveDescriptionsList = (ArrayList<String>)mapp.get("da");
        }
        if (mapp.containsKey("ra")) {
            this.resetValuesList = (ArrayList<String>)mapp.get("ra");
        }
        if (mapp.containsKey("ba")) {
            this.baseValuesList = (ArrayList<String>)mapp.get("ba");
        }
        if (mapp.containsKey("ia")) {
            this.incrementValuesList = (ArrayList<String>)mapp.get("ia");
        }
    }

    public static Comparator<ToDoListData> indexComparator = new Comparator<ToDoListData>() {
        public int compare(ToDoListData t1, ToDoListData t2) {
            int index1 = t1.getIndex();
            int index2 = t2.getIndex();
            return index1 - index2; //ascending order
        }
    };

    public static Comparator<ToDoListData> machineIdComparator = new Comparator<ToDoListData>() {
        public int compare(ToDoListData t1, ToDoListData t2) {
            String machineId1 = t1.getMachineId();
            String machineId2 = t2.getMachineId();
            return machineId1.compareTo(machineId2); //ascending order
        }
    };

    public static Comparator<ToDoListData> locationComparator = new Comparator<ToDoListData>() {
        public int compare(ToDoListData t1, ToDoListData t2) {
            String location1 = t1.getLocation();
            String location2 = t2.getLocation();
            return location1.compareTo(location2); //ascending order
        }
    };

    public int getIndex() { return this.index; }
    public void setIndex(int index) { this.index = index; }

    public String getLocation() { return this.location; }
    public void setLocation(String location) { this.location = location; }

    public String getMachineId() { return this.machineId; }
    public void setMachineId(String machineId) { this.machineId = machineId; }

    public String getDescription() { return this.description; }
    public void setDescription(String description) { this.description = description; }

    @Nullable public String getUser() { return this.user; }
    public void setUser(String user) { this.user = user; }

    public ArrayList<String> getProgressiveDescriptionsList() { return this.progressiveDescriptionsList; }

    public ArrayList<String> getResetValuesList() { return this.resetValuesList; }

    public ArrayList<String> getBaseValuesList() { return this.baseValuesList; }

    public ArrayList<String> getIncrementValuesList() { return this.incrementValuesList; }

    public Map<String, Object> getMap() { return this.map; }

    public Integer getDescriptionsLength() { return this.progressiveDescriptionsList.size(); }
}
