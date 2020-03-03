package com.slotmachine.ocr.mic;

import java.util.HashMap;
import javax.annotation.Nullable;

// POJO for to-do list data displayed to the user
public class ToDoListData {

    private String location, machineId, description, user;
    @Nullable private Integer numberOfProgressives;
    private String[] progressiveDescriptions;
    private boolean isCompleted, isSelected;
    private HashMap<String, Object> map;

    public ToDoListData(String location,
                        String machineId,
                        String description,
                        @Nullable String user,
                        @Nullable Integer numberOfProgressives,
                        String[] progressiveDescriptions,
                        boolean isCompleted,
                        boolean isSelected) {
        this.location = location;
        this.machineId = machineId;
        this.description = description;
        this.user = user;
        this.numberOfProgressives = numberOfProgressives;
        this.progressiveDescriptions = progressiveDescriptions;
        this.isCompleted = isCompleted;
        this.isSelected = isSelected;

        this.map = new HashMap<>();
        this.map.put("completed", this.isCompleted);
        this.map.put("description", this.description);
        this.map.put("location", this.location);
        this.map.put("machine_id", this.machineId);
        this.map.put("progressive_count", this.numberOfProgressives.toString());
        this.map.put("user", this.user);
        this.map.put("p_1", this.progressiveDescriptions[0]);
        this.map.put("p_2", this.progressiveDescriptions[1]);
        this.map.put("p_3", this.progressiveDescriptions[2]);
        this.map.put("p_4", this.progressiveDescriptions[3]);
        this.map.put("p_5", this.progressiveDescriptions[4]);
        this.map.put("p_6", this.progressiveDescriptions[5]);
        this.map.put("p_7", this.progressiveDescriptions[6]);
        this.map.put("p_8", this.progressiveDescriptions[7]);
        this.map.put("p_9", this.progressiveDescriptions[8]);
        this.map.put("p_10", this.progressiveDescriptions[9]);
    }

    public String getLocation() { return this.location; }
    public void setLocation(String location) { this.location = location; }

    public String getMachineId() { return this.machineId; }
    public void setMachineId(String machineId) { this.machineId = machineId; }

    public String getDescription() { return this.description; }
    public void setDescription(String description) { this.description = description; }

    @Nullable public String getUser() { return this.user; }
    public void setUser(String user) { this.user = user; }

    @Nullable public Integer getNumberOfProgressives() { return this.numberOfProgressives; }
    public void setNumberOfProgressives(int numberOfProgressives) { this.numberOfProgressives = numberOfProgressives; }

    public String[] getProgressiveDescriptions() { return this.progressiveDescriptions; }
    public void setProgressiveDescriptions(String[] progressiveDescriptions) { this.progressiveDescriptions = progressiveDescriptions; }

    public boolean isCompleted() { return this.isCompleted; }
    public void setCompleted(boolean isCompleted) { this.isCompleted = isCompleted; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean isSelected) { this.isSelected = isSelected; }

    public HashMap<String, Object> getMap() { return this.map; }
}
