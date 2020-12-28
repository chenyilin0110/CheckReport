package com.smb.checkreport.bean;

import java.util.ArrayList;
import java.util.List;

public class TrelloCards {
    private String id;
    private String name;
    private boolean closed;
    private List<String> idCheckLists;
    private ArrayList<TrelloCheckLists> checkLists;
    private int checkItemsCnt;

    @Override
    public String toString() {
        return "TrelloCards{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", closed=" + closed +
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

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public List<String> getIdCheckLists() {
        return idCheckLists;
    }

    public void setIdCheckLists(List<String> idCheckLists) {
        this.idCheckLists = idCheckLists;
    }

    public ArrayList<TrelloCheckLists> getCheckLists() {
        return checkLists;
    }

    public void setCheckLists(ArrayList<TrelloCheckLists> checkLists) {
        this.checkLists = checkLists;
    }

    public int getCheckItemsCnt() {
        return checkItemsCnt;
    }

    public void setCheckItemsCnt(int checkItemsCnt) {
        this.checkItemsCnt = checkItemsCnt;
    }
}
