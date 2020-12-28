package com.smb.checkreport.bean;

import java.util.ArrayList;

public class TrelloCheckLists {
    private String id;
    private String name;
    private ArrayList<TrelloCheckItems> checkItems;
    private int checkItemsCnt;

    @Override
    public String toString() {
        return "TrelloCheckLists{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", checkItemsCnt=" + checkItemsCnt +
                '}';
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<TrelloCheckItems> getCheckItems() {
        return checkItems;
    }

    public void setCheckItems(ArrayList<TrelloCheckItems> checkItems) {
        this.checkItems = checkItems;
    }

    public int getCheckItemsCnt() {
        return checkItemsCnt;
    }

    public void setCheckItemsCnt(int checkItemsCnt) {
        this.checkItemsCnt = checkItemsCnt;
    }
}
