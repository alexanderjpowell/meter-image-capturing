package com.slotmachine.ocr.mic;

// POJO for scan data displayed to the user
public class RowData {

    private String documentId, machineId, date, user, progressive1, progressive2, progressive3, progressive4, progressive5, progressive6, progressive7, progressive8, progressive9, progressive10, location, notes;
    private boolean isSelected;

    public RowData(String documentId,
                   String machineId,
                   String date,
                   String user,
                   String progressive1,
                   String progressive2,
                   String progressive3,
                   String progressive4,
                   String progressive5,
                   String progressive6,
                   String progressive7,
                   String progressive8,
                   String progressive9,
                   String progressive10,
                   String location,
                   String notes,
                   boolean isSelected) {
        this.documentId = documentId;
        this.machineId = machineId;
        this.date = date;
        this.user = user;
        this.progressive1 = progressive1;
        this.progressive2 = progressive2;
        this.progressive3 = progressive3;
        this.progressive4 = progressive4;
        this.progressive5 = progressive5;
        this.progressive6 = progressive6;
        this.progressive7 = progressive7;
        this.progressive8 = progressive8;
        this.progressive9 = progressive9;
        this.progressive10 = progressive10;
        this.location = location;
        this.notes = notes;
        this.isSelected = isSelected;
    }

    public String getDocumentId() { return this.documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

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

    public String getUser() {
        return this.user;
    }
    public void setUser(String user) {
        this.user = user;
    }

    public String getProgressive1() {
        return this.progressive1;
    }
    public void setProgressive1(String progressive1) {
        this.progressive1 = progressive1;
    }

    public String getProgressive2() {
        return this.progressive2;
    }
    public void setProgressive2(String progressive2) {
        this.progressive2 = progressive2;
    }

    public String getProgressive3() {
        return this.progressive3;
    }
    public void setProgressive3(String progressive3) {
        this.progressive3 = progressive3;
    }

    public String getProgressive4() {
        return this.progressive4;
    }
    public void setProgressive4(String progressive4) {
        this.progressive4 = progressive4;
    }

    public String getProgressive5() {
        return this.progressive5;
    }
    public void setProgressive5(String progressive5) {
        this.progressive5 = progressive5;
    }

    public String getProgressive6() {
        return this.progressive6;
    }
    public void setProgressive6(String progressive6) {
        this.progressive6 = progressive6;
    }

    public String getProgressive7() {
        return this.progressive7;
    }
    public void setProgressive7(String progressive7) {
        this.progressive7 = progressive7;
    }

    public String getProgressive8() {
        return this.progressive8;
    }
    public void setProgressive8(String progressive8) {
        this.progressive8 = progressive8;
    }

    public String getProgressive9() {
        return this.progressive9;
    }
    public void setProgressive9(String progressive9) {
        this.progressive9 = progressive9;
    }

    public String getProgressive10() {
        return this.progressive10;
    }
    public void setProgressive10(String progressive10) {
        this.progressive10 = progressive10;
    }

    public String getLocation() {
        return this.location;
    }
    public void setLocation(String location) {
        this.location = location;
    }

    public String getNotes() { return this.notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }
}
