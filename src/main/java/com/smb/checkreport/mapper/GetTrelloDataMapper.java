package com.smb.checkreport.mapper;

import io.joshworks.restclient.http.HttpResponse;
import io.joshworks.restclient.http.JsonNode;
import io.joshworks.restclient.http.Unirest;

public interface GetTrelloDataMapper {

    static HttpResponse<String> getLists(String board, String key, String token){
        HttpResponse<String> lists = Unirest.get("https://api.trello.com/1/boards/" + board + "/lists")
                .queryString("key", key)
                .queryString("token", token)
                .asString();
        return lists;
    }

    static HttpResponse<String> getCards(String board, String key, String token){
        HttpResponse<String> cards = Unirest.get("https://api.trello.com/1/boards/" + board + "/cards")
                .queryString("key", key)
                .queryString("token", token)
                .asString();
        return cards;
    }

    static HttpResponse<JsonNode> getMemberName(String member, String key, String token){
        HttpResponse<JsonNode> memberName = Unirest.put("https://api.trello.com/1/members/" + member)
                .header("Accept", "application/json")
                .queryString("key", key)
                .queryString("token", token)
                .asJson();
        return memberName;
    }

    static HttpResponse<String> getCheckItems(String checkListsId, String key, String token){
        HttpResponse<String> checkItems = Unirest.get("https://api.trello.com/1/checklists/" + checkListsId + "/checkItems")
                .queryString("key", key)
                .queryString("token", token)
                .asString();
        return checkItems;
    }

    static HttpResponse<String> getCheckListName(String checkList, String key, String token){
        HttpResponse<String> checkListName = Unirest.put("https://api.trello.com/1/checklists/" + checkList)
                .queryString("key", key)
                .queryString("token", token)
                .asString();
        return checkListName;
    }
}
