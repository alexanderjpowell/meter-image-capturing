package com.slotmachine.ocr.mic;

import javax.annotation.Nullable;

// POJO for to-do list data displayed to the user
public class ToDoListData {

    private String location, machineId, description, user;
    @Nullable private Integer numberOfProgressives;
    private boolean isCompleted, isSelected;

    public ToDoListData(String location,
                        String machineId,
                        String description,
                        @Nullable String user,
                        @Nullable Integer numberOfProgressives,
                        boolean isCompleted,
                        boolean isSelected) {
        this.location = location;
        this.machineId = machineId;
        this.description = description;
        this.user = user;
        this.numberOfProgressives = numberOfProgressives;
        this.isCompleted = isCompleted;
        this.isSelected = isSelected;
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

    public boolean isCompleted() { return this.isCompleted; }
    public void setCompleted(boolean isCompleted) { this.isCompleted = isCompleted; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean isSelected) { this.isSelected = isSelected; }
}
