package com.slotmachine.ocr.mic;

// POJO for to-do list data displayed to the user
public class ToDoListData {

    private String location, machineId, description, user, date;
    private boolean isCompleted, isSelected;

    public ToDoListData(String location,
                        String machineId,
                        String description,
                        String user,
                        String date,
                        boolean isCompleted,
                        boolean isSelected) {
        this.location = location;
        this.machineId = machineId;
        this.description = description;
        this.user = user;
        this.date = date;
        this.isCompleted = isCompleted;
        this.isSelected = isSelected;
    }

    public String getLocation() { return this.location; }
    public void setLocation(String location) { this.location = location; }

    public String getMachineId() { return this.machineId; }
    public void setMachineId(String machineId) { this.machineId = machineId; }

    public String getDescription() { return this.description; }
    public void setDescription(String description) { this.description = description; }

    public String getUser() { return this.user; }
    public void setUser(String user) { this.user = user; }

    public String getDate() { return this.date; }
    public void setDate(String date) { this.date = date; }

    public boolean isCompleted() { return this.isCompleted; }
    public void setCompleted(boolean isCompleted) { this.isCompleted = isCompleted; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }
}
