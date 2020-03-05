package com.slotmachine.ocr.mic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public class ToDoListData {

    private String location, machineId, description;
    private String user;
    //@Nullable private Integer numberOfProgressives;
    private String[] progressiveDescriptions;
    private ArrayList<String> progressiveDescriptionsList;
    //private boolean isCompleted, isSelected;
    private Map<String, Object> map;

    public ToDoListData(Map<String, Object> mapp) {
        this.map = mapp;

        /*this.location = mapp.get("location").toString();
        this.machineId = mapp.get("machine_id").toString();
        this.description = mapp.get("description").toString();
        if (mapp.containsKey("user")) { this.user = mapp.get("user").toString(); }
        if (mapp.containsKey("descriptionsArray")) {
            this.progressiveDescriptionsList = (ArrayList<String>)mapp.get("descriptionsArray");
        }*/

        this.location = mapp.get("l").toString();
        this.machineId = mapp.get("m").toString();
        this.description = mapp.get("d").toString();
        if (mapp.containsKey("u")) { this.user = mapp.get("u").toString(); }
        if (mapp.containsKey("da")) {
            this.progressiveDescriptionsList = (ArrayList<String>)mapp.get("da");
        }
    }

    public String getLocation() { return this.location; }
    public void setLocation(String location) { this.location = location; }

    public String getMachineId() { return this.machineId; }
    public void setMachineId(String machineId) { this.machineId = machineId; }

    public String getDescription() { return this.description; }
    public void setDescription(String description) { this.description = description; }

    @Nullable public String getUser() { return this.user; }
    public void setUser(String user) { this.user = user; }

    //@Nullable public Integer getNumberOfProgressives() { return this.numberOfProgressives; }
    //public void setNumberOfProgressives(int numberOfProgressives) { this.numberOfProgressives = numberOfProgressives; }

    public String[] getProgressiveDescriptions() { return this.progressiveDescriptions; }
    public void setProgressiveDescriptions(String[] progressiveDescriptions) { this.progressiveDescriptions = progressiveDescriptions; }

    public ArrayList<String> getProgressiveDescriptionsList() { return this.progressiveDescriptionsList; }

    /*public boolean isCompleted() { return this.isCompleted; }
    public void setCompleted(boolean isCompleted) { this.isCompleted = isCompleted; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean isSelected) { this.isSelected = isSelected; }*/

    public Map<String, Object> getMap() { return this.map; }

    public Integer getDescriptionsLength() { return this.progressiveDescriptionsList.size(); }
}
