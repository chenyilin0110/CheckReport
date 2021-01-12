package com.smb.checkreport.bean;

public class ExportToCSV {
    private Integer id;
    private String cardName;
    private String checkListName;
    private String checkItemName;
    private String checkItemState;
    private String checkItemDue;
    private String idMember;
    private String remarks;
    private String cardId;
    private String checkItemsId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getCheckItemsId() {
        return checkItemsId;
    }

    public void setCheckItemsId(String checkItemsId) {
        this.checkItemsId = checkItemsId;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getCheckListName() {
        return checkListName;
    }

    public void setCheckListName(String checkListName) {
        this.checkListName = checkListName;
    }

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
