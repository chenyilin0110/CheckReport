package com.smb.checkreport.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.smb.checkreport.bean.*;
import com.smb.checkreport.controller.TaskCheckController;
import com.smb.checkreport.mapper.GetTrelloDataMapper;
import com.smb.checkreport.mapper.SelectDataForCheckReportMapper;
import com.smb.checkreport.utility.ConfigLoader;
import com.smb.checkreport.utility.Constant;
import com.smb.checkreport.utility.Utility;
import com.smb.checkreport.utility.WebAPI;
import io.joshworks.restclient.http.HttpResponse;
import io.joshworks.restclient.http.JsonNode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class TrelloExportService {
    @Autowired
    private GetTrelloDataMapper getTrelloDataMapper;

    private static Logger logger = LoggerFactory.getLogger(TrelloExportService.class);

    public void getTrello(HttpServletRequest request) throws IOException, ParseException {
        String returnStr = new String();
        Properties prop = ConfigLoader.loadConfig("trello.properties");

        //權限相關
        String trelloKey = prop.getProperty("trello.key");
        String trelloToken = prop.getProperty("trello.token");
        String urlParam = "?key=" + trelloKey + "&token=" + trelloToken;

        //源頭board
        String boardID = prop.getProperty("trello.board");

        //取得人員對照表
        Map<String, String> membersMap = new HashMap<String, String>();
        returnStr = WebAPI.sendAPI_trello("boards/"+boardID+"/members"+urlParam);
        List<TrelloMembers> listTrelloMembers = JSONArray.parseArray(returnStr, TrelloMembers.class);
        for(TrelloMembers tm : listTrelloMembers){
            if(tm.getFullName().toUpperCase().contains("JASON")){
                membersMap.put(tm.getId(), "Jason");
            } else if(tm.getFullName().toUpperCase().contains("YILIN")){
                membersMap.put(tm.getId(), "羿霖");
            } else if(tm.getFullName().toUpperCase().contains("YOUNG")){
                membersMap.put(tm.getId(), "孟揚");
            } else if(tm.getFullName().toUpperCase().contains("XERIOU")){
                membersMap.put(tm.getId(), "喜仙");
            } else if(tm.getFullName().toUpperCase().contains("LUNCHI")){
                membersMap.put(tm.getId(), "倫奇");
            } else if(tm.getFullName().contains("曉真")){
                membersMap.put(tm.getId(), "曉真");
            }
        }

        //取得分類列表
        returnStr = WebAPI.sendAPI_trello("boards/"+boardID+"/lists"+urlParam);
        List<TrelloLists> listTrelloLists = JSONArray.parseArray(returnStr, TrelloLists.class);

        //取得列表底下 Card
        for(TrelloLists tl : listTrelloLists){
            logger.debug(tl.toString());
            returnStr = WebAPI.sendAPI_trello("lists/"+tl.getId()+"/cards"+urlParam);
            List<TrelloCards> listTrelloCards = JSONArray.parseArray(returnStr, TrelloCards.class);
            for(TrelloCards tc : listTrelloCards){
                logger.debug(tc.toString());
                List<TrelloCheckLists> listTrelloCheckLists = new ArrayList<TrelloCheckLists>();
                int accCheckItemsCnt = 0;
                for(String icl : tc.getIdCheckLists()){
                    returnStr = WebAPI.sendAPI_trello("checklists/"+icl+urlParam);
                    TrelloCheckLists tcl = JSONObject.parseObject(returnStr, TrelloCheckLists.class);
                    tcl.setCheckItemsCnt(tcl.getCheckItems().size());
                    accCheckItemsCnt += tcl.getCheckItemsCnt();
                    listTrelloCheckLists.add(tcl);
                }
                tc.setCheckLists((ArrayList<TrelloCheckLists>) listTrelloCheckLists);
                tc.setCheckItemsCnt(accCheckItemsCnt);
            }
            tl.setCards((ArrayList<TrelloCards>) listTrelloCards);
        }
        request.getSession().setAttribute("listTrelloLists", listTrelloLists);
        request.getSession().setAttribute("membersMap", membersMap);
    }

    public String all(HttpServletRequest request) throws IOException, ParseException {

        // trello need key and token
        String key = ConfigLoader.loadConfig("trello.properties").getProperty("trello.key");
        String token = ConfigLoader.loadConfig("trello.properties").getProperty("trello.token");
        String boardId = ConfigLoader.loadConfig("trello.properties").getProperty("trello.boardId");

        // user properties
        String getUserShowList = ConfigLoader.loadConfig("trello.properties").getProperty("user.showLists");
        String[] showList = getUserShowList.split(",");

        // members on this board
        String getMember = ConfigLoader.loadConfig("trello.properties").getProperty("user.members");
        String[] member = getMember.split(",");

        // step 1. get lists on this board
        logger.info(">>> [" + request.getSession().getId() + "] Get all list on the board");
        HttpResponse<String> getLists = GetTrelloDataMapper.getLists(boardId, key, token);
        List<Lists> lists = JSONArray.parseArray(getLists.getBody(), Lists.class);
        // save the show lists id
        ArrayList<String> showListId = new ArrayList<String>();
        for(Lists each : lists){
            for(int eachShowList = 0; eachShowList < showList.length; eachShowList++){
                if(each.getName().equals(showList[eachShowList])){
                    showListId.add(each.getId());
                }
            }
        }

        // step 2. get cards on this board
        logger.info(">>> [" + request.getSession().getId() + "] Get all card on the board");
        HttpResponse<String> getCards = GetTrelloDataMapper.getCards(boardId, key, token);
        List<Cards> cards = JSONArray.parseArray(getCards.getBody(), Cards.class);
        // remove some cards that not in show lists
        for(int eachCards = 0; eachCards < cards.size(); eachCards++){
            boolean bl = false;
            for(int eachShowListId = 0; eachShowListId < showListId.size(); eachShowListId++){
                if(cards.get(eachCards).getIdList().equals(showListId.get(eachShowListId))){
                    bl = true;
                    break;
                }

                // bl = false
                if(eachShowListId == showListId.size() - 1 && !bl){
                    // remove the index in cards
                    cards.remove(eachCards);
                    eachCards--;
                }
            }
        }

        // step 3. get all member fullName and id on this board
        logger.info(">>> [" + request.getSession().getId() + "] Get all member id in this board");
        ArrayList<String> fullName = new ArrayList<String>();
        ArrayList<String> fullNameId = new ArrayList<String>();
        for(int eachMember = 0; eachMember < member.length; eachMember++){
            HttpResponse<JsonNode> memberName = GetTrelloDataMapper.getMemberName(member[eachMember], key, token);
            // get json object and get
            org.json.JSONObject memberNameJsonObject = memberName.getBody().getObject();
            fullName.add(memberNameJsonObject.getString("fullName"));
            fullNameId.add(memberNameJsonObject.getString("id"));
        }

        // prepare write to csv
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String data = "Card,項目,說明,已完成,dueDay,Owner,備註\r\n";

        // step 4. get checkItems on a checklist
        for(int eachCards = 0; eachCards < cards.size(); eachCards++){
            logger.info(">>> [" + request.getSession().getId() + "] Get check list");
            String[] getIdCheckLists = cards.get(eachCards).getIdChecklists().replaceAll("[\\]\\[\"]","").split(",");

            for(int eachIdCheckLists = 0; eachIdCheckLists < getIdCheckLists.length; eachIdCheckLists++){
                if(getIdCheckLists[eachIdCheckLists].equals("")) {
                    // no check items
                    data += cards.get(eachCards).getName()+""
                            + "\r\n";
                } else {
                    // have check items

                    // get check list name
                    logger.info(">>> [" + request.getSession().getId() + "] Get check list name");
                    HttpResponse<String> getCheckListName = GetTrelloDataMapper.getCheckListName(getIdCheckLists[eachIdCheckLists], key, token);
                    Object checkListName = JSONObject.parse(getCheckListName.getBody());

                    // get check items
                    logger.info(">>> [" + request.getSession().getId() + "] Get check items on a checklist");
                    HttpResponse<String> getCheckItems = GetTrelloDataMapper.getCheckItems(getIdCheckLists[eachIdCheckLists], key, token);
                    List<CheckItems> checkItemsLists = JSONArray.parseArray(getCheckItems.getBody(), CheckItems.class);
                    if(checkItemsLists.size() == 0){
                        // check items is empty
                        data += cards.get(eachCards).getName()+","
                                + ((JSONObject) checkListName).getString("name")+""
                                + "\r\n";
                    } else{
                        for (CheckItems ci : checkItemsLists) {
                            // transfer the , to ， in csv the ',' is mean change column
                            if(ci.getName().indexOf(",") >= 0){
                                String replace = ci.getName().replace(',', '，');
                                ci.setName(replace);
                            }

                            // transfer idMember to full name
                            for(int eachFullNameId = 0; eachFullNameId < fullNameId.size(); eachFullNameId++){
                                if(ci.getIdMember() != null && ci.getIdMember().equals(fullNameId.get(eachFullNameId))){
                                    ci.setIdMember(Constant.CHINESE_NAME_MAP.get(fullName.get(eachFullNameId)));
                                }
                            }
                            if(ci.getIdMember() == null){
                                ci.setIdMember("");
                            }

                            // parse the check items due day
                            String temp = ci.getDue();
                            if(temp != null){
                                temp = temp.split("T")[0];
                                temp += " 23:59:59";
                                SimpleDateFormat sdfToDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                Date parseDate = sdfToDate.parse(temp);
                                ci.setDue(sdf.format(parseDate));
                            } else{
                                ci.setDue("");
                            }

                            // transfer the state
                            if(ci.getState().equals("complete")){
                                ci.setState("✔");
                            } else{
                                ci.setState("✘");
                            }

                            // assign data
                            data += cards.get(eachCards).getName()+","
                                    + ((JSONObject) checkListName).getString("name")+","
                                    +ci.getName()+","
                                    +ci.getState()+","
                                    +ci.getDue()+","
                                    +ci.getIdMember()+""

                                    + "\r\n";
                        }
                    }
                }
            }
        }
        return data;
    }

    public String eachMember(String inputMemberName, HttpServletRequest request) throws IOException, ParseException {

        // trello need key and token
        String key = ConfigLoader.loadConfig("trello.properties").getProperty("trello.key");
        String token = ConfigLoader.loadConfig("trello.properties").getProperty("trello.token");
        String boardId = ConfigLoader.loadConfig("trello.properties").getProperty("trello.boardId");

        // user properties
        String getUserShowList = ConfigLoader.loadConfig("trello.properties").getProperty("user.showLists");
        String[] showList = getUserShowList.split(",");


        // members on this board
        ArrayList<String> tempArray = new ArrayList<String>();
        tempArray.add(Constant.NAME_MAP.get(inputMemberName));
        Object[] member = tempArray.toArray();

        // step 1. get lists on this board
        logger.info(">>> [" + request.getSession().getId() + "] Get all list on the board");
        HttpResponse<String> getLists = GetTrelloDataMapper.getLists(boardId, key, token);
        List<Lists> lists = JSONArray.parseArray(getLists.getBody(), Lists.class);
        // save the show lists id
        ArrayList<String> showListId = new ArrayList<String>();
        for(Lists each : lists){
            for(int eachShowList = 0; eachShowList < showList.length; eachShowList++){
                if(each.getName().equals(showList[eachShowList])){
                    showListId.add(each.getId());
                }
            }
        }

        // step 2. get cards on this board
        logger.info(">>> [" + request.getSession().getId() + "] Get all card on the board");
        HttpResponse<String> getCards = GetTrelloDataMapper.getCards(boardId, key, token);
        List<Cards> cards = JSONArray.parseArray(getCards.getBody(), Cards.class);
        // remove some cards that not in show lists
        for(int eachCards = 0; eachCards < cards.size(); eachCards++){
            boolean bl = false;
            for(int eachShowListId = 0; eachShowListId < showListId.size(); eachShowListId++){
                if(cards.get(eachCards).getIdList().equals(showListId.get(eachShowListId))){
                    bl = true;
                    break;
                }

                // bl = false
                if(eachShowListId == showListId.size() - 1 && !bl){
                    // remove the index in cards
                    cards.remove(eachCards);
                    eachCards--;
                }
            }
        }

        // step 3. get all member fullName and id on this board
        logger.info(">>> [" + request.getSession().getId() + "] Get member id");
        String fullName = "";
        String fullNameId = "";
        for(Object ob : member){
            HttpResponse<JsonNode> memberName = GetTrelloDataMapper.getMemberName(ob.toString(), key, token);
            // get json object and get
            org.json.JSONObject memberNameJsonObject = memberName.getBody().getObject();
            fullName = memberNameJsonObject.getString("fullName");
            fullNameId = memberNameJsonObject.getString("id");
        }

        // prepare write to csv
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String data = "Card,項目,說明,已完成,dueDay,Owner,被註\r\n";

        // step 4. get checkItems on a checklist
        for(int eachCards = 0; eachCards < cards.size(); eachCards++){
            String[] getIdCheckLists = cards.get(eachCards).getIdChecklists().replaceAll("[\\]\\[\"]","").split(",");
            logger.info(">>> [" + request.getSession().getId() + "] Get check list");

            for(int eachIdCheckLists = 0; eachIdCheckLists < getIdCheckLists.length; eachIdCheckLists++){
                if(getIdCheckLists[eachIdCheckLists].equals("")) {
                    // no check items and do nothing
                } else {
                    // have check items

                    // get check list name
                    logger.info(">>> [" + request.getSession().getId() + "] Get check list name");
                    HttpResponse<String> getCheckListName = GetTrelloDataMapper.getCheckListName(getIdCheckLists[eachIdCheckLists], key, token);
                    Object checkListName = JSONObject.parse(getCheckListName.getBody());

                    // get check items
                    logger.info(">>> [" + request.getSession().getId() + "] Get check items on a checklist");
                    HttpResponse<String> getCheckItems = GetTrelloDataMapper.getCheckItems(getIdCheckLists[eachIdCheckLists], key, token);
                    List<CheckItems> checkItemsLists = JSONArray.parseArray(getCheckItems.getBody(), CheckItems.class);
                    if(checkItemsLists.size() == 0){
                        // check items is empty and do nothing
                    } else{
                        for (CheckItems ci : checkItemsLists) {
                            // find input member card
                            if(ci.getIdMember() == null){
                                // do nothing
                            } else if(ci.getIdMember().toString().equals(fullNameId)) {
                                // transfer the , to ， in csv the ',' is mean change column
                                if(ci.getName().indexOf(",") >= 0){
                                    String replace = ci.getName().replace(',', '，');
                                    ci.setName(replace);
                                }

                                // transfer idMember to full name
                                ci.setIdMember(Constant.CHINESE_NAME_MAP.get(fullName));

                                // parse the check items due day
                                String temp = ci.getDue();
                                if(temp != null){
                                    temp = temp.split("T")[0];
                                    temp += " 23:59:59";
                                    SimpleDateFormat sdfToDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                    Date parseDate = sdfToDate.parse(temp);
                                    ci.setDue(sdf.format(parseDate));
                                } else{
                                    ci.setDue("");
                                }

                                // transfer the state
                                if(ci.getState().equals("complete")){
                                    ci.setState("✔");
                                } else{
                                    ci.setState("✘");
                                }

                                // assign data
                                data += cards.get(eachCards).getName()+","
                                        + ((JSONObject) checkListName).getString("name")+","
                                        +ci.getName()+","
                                        +ci.getState()+","
                                        +ci.getDue()+","
                                        +ci.getIdMember()+""

                                        + "\r\n";
                            } else{
                                // do nothing
                            }

                        }
                    }
                }
            }
        }
        return data;
    }

}
