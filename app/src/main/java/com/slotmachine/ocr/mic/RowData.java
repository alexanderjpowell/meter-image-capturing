package com.slotmachine.ocr.mic;

public class RowData {

    private String machineId, date, numberOfProgressives, numberOfProgressives1, numberOfProgressives2, numberOfProgressives3;

    public RowData() { }

    public RowData(String machineId, String date, String numberOfProgressives, String numberOfProgressives1, String numberOfProgressives2, String numberOfProgressives3) {
        this.machineId = machineId;
        this.date = date;
        this.numberOfProgressives = numberOfProgressives;
        this.numberOfProgressives1 = numberOfProgressives1;
        this.numberOfProgressives2 = numberOfProgressives2;
        this.numberOfProgressives3 = numberOfProgressives3;
    }

    public String getMachineId() {
        return this.machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getDate() {
        return this.date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getNumberOfProgressives() {
        return this.numberOfProgressives;
    }

    public void setNumberOfProgressives(String numberOfProgressives) {
        this.numberOfProgressives = numberOfProgressives;
    }

    public String getNumberOfProgressives1() {
        return this.numberOfProgressives1;
    }

    public void setNumberOfProgressives1(String numberOfProgressives1) {
        this.numberOfProgressives1 = numberOfProgressives1;
    }

    public String getNumberOfProgressives2() {
        return this.numberOfProgressives2;
    }

    public void setNumberOfProgressives2(String numberOfProgressives2) {
        this.numberOfProgressives2 = numberOfProgressives2;
    }

    public String getNumberOfProgressives3() {
        return this.numberOfProgressives3;
    }

    public void setNumberOfProgressives3(String numberOfProgressives3) {
        this.numberOfProgressives3 = numberOfProgressives3;
    }
}
