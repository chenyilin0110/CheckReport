package com.smb.checkreport.bean;

public class ExportToCSV {
    private String cardName;
    private String checkItemName;
    private String checkItemState;
    private String checkItemDue;
    private String idMember;

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public String getCheckItemName() {
        return checkItemName;
    }

    public void setCheckItemName(String checkItemName) {
        this.checkItemName = checkItemName;
    }

    public String getCheckItemState() {
        return checkItemState;
    }

    public void setCheckItemState(String checkItemState) {
        this.checkItemState = checkItemState;
    }

    public String getCheckItemDue() {
        return checkItemDue;
    }

    public void setCheckItemDue(String checkItemDue) {
        this.checkItemDue = checkItemDue;
    }

    public String getIdMember() {
        return idMember;
    }

    public void setIdMember(String idMember) {
        this.idMember = idMember;
    }
}
