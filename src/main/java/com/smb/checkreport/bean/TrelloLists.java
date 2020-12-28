package com.smb.checkreport.bean;

import java.util.ArrayList;

public class TrelloLists {
    private String id;
    private String name;
    private boolean closed;
    private ArrayList<TrelloCards> cards;

    @Override
    public String toString() {
        return "TrelloLists{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", closed=" + closed +
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

    public ArrayList<TrelloCards> getCards() {
        return cards;
    }

    public void setCards(ArrayList<TrelloCards> cards) {
        this.cards = cards;
    }
}
