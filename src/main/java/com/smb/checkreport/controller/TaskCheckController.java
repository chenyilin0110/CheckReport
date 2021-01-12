package com.smb.checkreport.controller;

import com.smb.checkreport.bean.*;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.smb.checkreport.mapper.GetTrelloDataMapper;
import com.smb.checkreport.service.CheckReportService;
import com.smb.checkreport.service.TrelloExportService;
import com.smb.checkreport.utility.ConfigLoader;
import com.smb.checkreport.utility.Util;
import com.smb.checkreport.utility.Utility;
import com.smb.checkreport.utility.WebAPI;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFCreationHelper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequestMapping("/task")
public class TaskCheckController {

    private static Logger logger = LoggerFactory.getLogger(TaskCheckController.class);

    @Autowired
    private CheckReportService checkReportService;

    @Autowired
    private TrelloExportService trelloExportService;

    int failedCount = 0;
    boolean next_is_null_or_not = false;
    String[] ignore_element_code={"S1","S2","A01","A02","A03","A04","B01","B02","01","T1","T01","T5","T7","T80","T81"};

    @RequestMapping(value = "/manager/checkReport")
    @ResponseBody
    public ResponseEntity<String> checkReport(HttpServletRequest request, Model model, String order, String number, String machine_code, String type, String time){

        logger.info(">>> [" + request.getSession().getId() + "] Success get the id and type entered by the manager, order:" + order + " number: " + number + " machine_code: " + machine_code + " type: " + type);

        ApiReturn ar = new ApiReturn();
        try{
            if(type.equals("MA")){
                logger.debug(">>> [" + request.getSession().getId() + "] Go MA");
                return checkReportForMA(request, model, order, number, machine_code, type, time);
            }else if(type.equals("MB")){
                logger.debug(">>> [" + request.getSession().getId() + "] Go MB");
                return checkReportForMB(request, model, order, number, machine_code, type, time);
            }else if(type.equals("MC")){
                logger.debug(">>> [" + request.getSession().getId() + "] Go MC");
                return checkReportForMC(request, model, order, number, machine_code, type, time);
            }else if(type.equals("MD")){
                logger.debug(">>> [" + request.getSession().getId() + "] Go MD");
                return checkReportForMD(request, model, order, number, machine_code, type, time);
            }else{
                logger.debug(">>> [" + request.getSession().getId() + "] BUG");
                ar.setRetMessage("BUG?");
                ar.setRetStatus("Failed");
                return new ResponseEntity<String>(JSON.toJSONString(ar), HttpStatus.OK);
            }
        }catch (Exception e) {
            logger.debug(">>> [" + request.getSession().getId() + "] " + e.getMessage());
            e.printStackTrace();
            ar.setRetMessage(e.getMessage());
            ar.setRetStatus("Exception");
        }
        return new ResponseEntity<String>(JSON.toJSONString(ar), HttpStatus.OK);
    }

    public ResponseEntity<String> checkReportForMA(HttpServletRequest request, Model model, String order, String number, String machine_code, String type, String time){
        if(machine_code == null || machine_code.isEmpty()) {
            machine_code = null;
        }

        ApiReturn ar = new ApiReturn();
        try {
            // delete today.txt file
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date today = new Date();
            deleteFile(sdf.format(today).toString());

            // split the number
            String[] element_code = number.split(" ");
            for (int each_element_code_split = 0; each_element_code_split < element_code.length; each_element_code_split++) {
                failedCount = 0;
                // add \n to split the each number in outputForExcel.txt file
                if (each_element_code_split > 0) {
                    outputForExcelReport();
                }

                // check the order infomation first
                List<OrderInfo> get_order_status = checkReportService.getOrderStatusByOrderNum(order, request.getSession().getId());
                if (get_order_status.size() == 0) {
                    // can not find the order infomation
                    logger.error(">>> [" + request.getSession().getId() + "] Can not get the input order: " + order);
                    writeFile(null, order, null, -1);
                } else {
                    deleteFile(order + "_" + element_code[each_element_code_split]); // if the file is exist
                    next_is_null_or_not = false;
                    if(each_element_code_split == element_code.length - 1){
                        // the element code is last
                        next_is_null_or_not = true;
                    }
                    // success get element code
                    List<ElementLog> get_dispatch_detail_sns = checkReportService.getDispatchDetailSNS(element_code[each_element_code_split], machine_code, type, time, request.getSession().getId());
                    if(get_dispatch_detail_sns.size()==0){
                        // can not find the element code log, the finished number is zero or the finish date is not input time
                        writeFile(element_code[each_element_code_split], order, null, -2);
                    }else {
                        for (int j = 0; j < get_dispatch_detail_sns.size(); j++) {
                            // maybe dispatch_detail_sns have more than two number, so split the dispatch_detail_sns
                            String[] stringSplit = get_dispatch_detail_sns.get(j).getDispatchDetailSNS().split(",");
                            for (int k = 0; k < stringSplit.length; k++) {
                                if (!stringSplit[k].equals("")) {
                                    List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_dispath_detail_sns = checkReportService.getOrderSNByDispatchDetailSNS(stringSplit[k], request.getSession().getId());
                                    List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_order_num = checkReportService.getOrderSNByOrder(order, request.getSession().getId());
                                    if(get_order_sn_by_dispath_detail_sns.size() == 0){
                                        // -5 is mean maybe the dispatch has been changed, please check the dispatch
                                        writeFile(element_code[each_element_code_split], order, null, -5);
                                    }else if (get_order_sn_by_dispath_detail_sns.get(0).getOrderSN().equals(get_order_sn_by_order_num.get(0).getOrderSN())) {
                                        // the report accurated
                                        writeFile(element_code[each_element_code_split], stringSplit[k], order, null);
                                    } else {
                                        // the report have dispatch_details_sns but not accurated
                                        List<OrderInfo> get_order_num_by_order_sn = checkReportService.getOrderNumByOrderSN(get_order_sn_by_dispath_detail_sns.get(0).getOrderSN(), request.getSession().getId());
                                        writeFile(element_code[each_element_code_split], stringSplit[k], get_order_num_by_order_sn.get(0).getOrderNum(), order, null);
                                    }
                                } else {
                                    // the report have not dispatch_details_sns that is mean repeat
                                    if (get_order_status.get(0).getOrderStatus().equals("0")) {
                                        // 0 is no dispatch
                                        writeFile(element_code[each_element_code_split], order, null, 0);
                                    } else if (get_order_status.get(0).getOrderStatus().equals("1")) {
                                        // here have dispatch
                                        List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_order_num = checkReportService.getOrderSNByOrder(order, request.getSession().getId());
                                        List<RelManufactureElement> getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN = checkReportService.getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN(
                                                element_code[each_element_code_split], type, get_order_sn_by_order_num.get(0).getOrderSN(), request.getSession().getId());

                                        if (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.size() == 0) {
                                            // 10 is mean have not data in rel_manufacture_element
                                            writeFile(element_code[each_element_code_split], order, null, 10);
                                        } else if ((getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOnlineDate() == null) &&
                                                (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOfflineDate() == null)) {
                                            // 11 is mean online and offline is null
                                            writeFile(element_code[each_element_code_split], order, null, 11);
                                        } else if ( (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getIsFinished() == null) ||
                                                (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getIsFinished().equals("0")) ) {
                                            Date expect_online_date = sdf.parse(getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOnlineDate());
                                            int count = 0;
                                            String expect_online_calendar_before = FormatDate(expect_online_date, count);
                                            count++;
                                            String expect_online_calendar_after = FormatDate(expect_online_date, count);
                                            count++;
                                            // calendar to date
                                            Date expect_online_date_before = sdf.parse(expect_online_calendar_before);
                                            Date expect_online_date_after = sdf.parse(expect_online_calendar_after);

                                            // after offline date five day and before offline date five day
                                            Date expect_offline_date = sdf.parse(getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOfflineDate());
                                            String expect_offline_calendar_before = FormatDate(expect_offline_date, count);
                                            String expect_offline_calendar_after = FormatDate(expect_offline_date, count);
                                            // calendar to date
                                            Date expect_offline_date_before = sdf.parse(expect_offline_calendar_before);
                                            Date expect_offline_date_after = sdf.parse(expect_offline_calendar_after);

                                            Date finish_date = sdf.parse(get_dispatch_detail_sns.get(j).getFinishDatetime());

                                            if ((expect_online_date_before.before(finish_date)) || ((expect_online_date_after.after(finish_date)) && (expect_offline_date_before.before(finish_date))) || (expect_offline_date_after.after(finish_date))) {
                                                // 12 is mean the date is before the online date or after the offline date
                                                writeFile(element_code[each_element_code_split], order, null, 12);
                                            } else {
                                                // bug!!!
                                                writeFile(order, element_code[each_element_code_split]);
                                            }
                                        } else if (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getIsFinished().equals("1")) {
                                            // 13 is mean finish
                                            writeFile(element_code[each_element_code_split], order, null, 13);
                                            break;
                                        } else {
                                            // bug!!!
                                            writeFile(order, element_code[each_element_code_split]);
                                        }
                                    } else if (get_order_status.get(0).getOrderStatus().equals("2")) {
                                        writeFile(element_code[each_element_code_split], order, null, 2);
                                    }
                                }
                            }
                        }
                    }
                }
                if(failedCount == 0){
                    outputForExcelReport("OK");
                }
                ar.setRetMessage("Successfully check the report and write the file: " + element_code.length);
                ar.setRetStatus("Success");
            }
        } catch(Exception e){
            logger.error(">>> [" + request.getSession().getId() + "] " + e.getMessage());
            e.printStackTrace();
            ar.setRetMessage(e.getMessage());
            ar.setRetStatus("Exception");
        }
        return new ResponseEntity<String>(JSON.toJSONString(ar), HttpStatus.OK);
    }

    public ResponseEntity<String> checkReportForMB(HttpServletRequest request, Model model, String order, String nest_program_no, String machine_code, String type, String time){
        ApiReturn ar = new ApiReturn();
        try {
            // delete today.txt file
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date today = new Date();
            deleteFile(sdf.format(today).toString());

            // split the nest program number
            String[] nest_program_no_split = nest_program_no.split(" ");
            for(int each_nest_program_no_split = 0; each_nest_program_no_split < nest_program_no_split.length; each_nest_program_no_split++) {
                failedCount = 0;
                // add \n to split the each nest program number in outputForExcel.txt file
                if(each_nest_program_no_split > 0){
                    outputForExcelReport();
                }

                // check the order infomation first
                List<OrderInfo> get_order_status = checkReportService.getOrderStatusByOrderNum(order, request.getSession().getId());
                if(get_order_status.size() == 0){
                    // can not find the order infomation
                    logger.error(">>> [" + request.getSession().getId() + "] Can not get the input order: " + order);
                    writeFile(null, order, nest_program_no_split[each_nest_program_no_split], -1);
                }else {
                    List<ApiLog> get_nest_program = checkReportService.getNestProgramByNestProgramNO(nest_program_no_split[each_nest_program_no_split], machine_code, type, request.getSession().getId());

                    if (get_nest_program.get(0) == null) {
                        // can not get the nest program number report
//                        failedCount++;
//                        not_find_nest_program_no.add(nest_program_no_split[each_nest_program_no_split]);

                        // check the nest program is exist or not in DB:qrcode_label  table:nest_info
                        String nest_program_id = checkReportService.getProgramIdInQrcodeLabel(nest_program_no_split[each_nest_program_no_split], request.getSession().getId());
                        if(nest_program_id == null){
                            // -3 is mean the nest program not upload
                            writeFile(null, order, nest_program_no_split[each_nest_program_no_split],-3);
                        }else{
                            // -4 is mean the smb web not receive the machine report
                            writeFile(null, order, nest_program_no_split[each_nest_program_no_split], -4);
                        }
                    } else {
                        deleteFile(order + "_" + nest_program_no_split[each_nest_program_no_split]); // if the file is exist
                        JSONArray joArray = JSONArray.parseArray(get_nest_program.get(0).getParameter());
                        String element_code[] = new String[999];
                        // get element code in nest program number
                        for (int i = 0; i < joArray.size(); i++) {
                            JSONObject jo = JSONObject.parseObject(joArray.getString(i));
                            element_code[i] = jo.getString("element_code");
                        }
                        for (int i = 0; i < element_code.length; i++) {
                            next_is_null_or_not = false;
                            if (element_code[i] != null) { // success get element code in nest program number
                                boolean for_check_element_code_whether_need_ignore = false;
                                for (int h = 0; h < ignore_element_code.length; h++){
                                    if(element_code[i].equals(ignore_element_code[h])){
                                        for_check_element_code_whether_need_ignore = true;
                                        break;
                                    }
                                }
                                int next = i;
                                next++;
                                if(element_code[next] == null){
                                    next_is_null_or_not = true;
                                }

                                if(!for_check_element_code_whether_need_ignore){
                                    List<ElementLog> get_dispatch_detail_sns = checkReportService.getDispatchDetailSNS(element_code[i], machine_code, type, time, request.getSession().getId());
                                    if(get_dispatch_detail_sns.size()==0){
                                        // can not find the element code log, the finished number is zero or the finish date is not input time
                                        writeFile(element_code[i], order, nest_program_no_split[each_nest_program_no_split], -2);
                                    }else {
                                        for (int j = 0; j < get_dispatch_detail_sns.size(); j++) {
                                            // maybe dispatch_detail_sns have more than two number, so split the dispatch_detail_sns
                                            String[] stringSplit = get_dispatch_detail_sns.get(j).getDispatchDetailSNS().split(",");
                                            for (int k = 0; k < stringSplit.length; k++) {
                                                if (!stringSplit[k].equals("")) {
                                                    List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_dispath_detail_sns = checkReportService.getOrderSNByDispatchDetailSNS(stringSplit[k], request.getSession().getId());
                                                    List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_order_num = checkReportService.getOrderSNByOrder(order, request.getSession().getId());
                                                    if(get_order_sn_by_dispath_detail_sns.size() == 0){
                                                        // -5 is mean maybe the dispatch has been changed, please check the dispatch
                                                        writeFile(element_code[i], order, nest_program_no_split[each_nest_program_no_split], -5);
                                                    }else if (get_order_sn_by_dispath_detail_sns.get(0).getOrderSN().equals(get_order_sn_by_order_num.get(0).getOrderSN())) {
                                                        // the report accurated
                                                        writeFile(element_code[i], stringSplit[k], order, nest_program_no_split[each_nest_program_no_split]);
                                                    } else {
                                                        // the report have dispatch_details_sns but not accurated
                                                        List<OrderInfo> get_order_num_by_order_sn = checkReportService.getOrderNumByOrderSN(get_order_sn_by_dispath_detail_sns.get(0).getOrderSN(), request.getSession().getId());
                                                        writeFile(element_code[i], stringSplit[k], get_order_num_by_order_sn.get(0).getOrderNum(), order, nest_program_no_split[each_nest_program_no_split]);
                                                    }
                                                } else {
                                                    // the report have not dispatch_details_sns that is mean repeat
                                                    if (get_order_status.get(0).getOrderStatus().equals("0")) {
                                                        // 0 is no dispatch
                                                        writeFile(element_code[i], order, nest_program_no_split[each_nest_program_no_split], 0);
                                                    } else if (get_order_status.get(0).getOrderStatus().equals("1")) {
                                                        // here have dispatch
                                                        List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_order_num = checkReportService.getOrderSNByOrder(order, request.getSession().getId());
                                                        List<RelManufactureElement> getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN = checkReportService.getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN(
                                                                element_code[i], type, get_order_sn_by_order_num.get(0).getOrderSN(), request.getSession().getId());

                                                        if (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.size() == 0) {
                                                            // 10 is mean have not data in rel_manufacture_element
                                                            writeFile(element_code[i], order, nest_program_no_split[each_nest_program_no_split], 10);
                                                        } else if ((getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOnlineDate() == null) &&
                                                                (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOfflineDate() == null)) {
                                                            List<DispatchDetail> get_expect_online_offline_in_dispatch_detail = checkReportService.getExpectOnlineAndOfflineInDispatchDetail(getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getSn(),
                                                                    request.getSession().getId());
                                                            if(get_expect_online_offline_in_dispatch_detail.get(0).getExpectOnlineDate() == null &&
                                                                    (get_expect_online_offline_in_dispatch_detail.get(0).getExpectOfflineDate() == null)){
                                                                // 11 is mean online and offline is null
                                                                writeFile(element_code[i], order, nest_program_no_split[each_nest_program_no_split], 11);
                                                            }else{
                                                                // bug!!!
                                                                writeFile(order, nest_program_no_split[each_nest_program_no_split]);
                                                            }
                                                        } else if ( (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getIsFinished() == null) ||
                                                                (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getIsFinished().equals("0")) ) {
                                                            // format the online and offline and finish date

                                                            // before online date five day and after online date five day
                                                            Date expect_online_date = sdf.parse(getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOnlineDate());
                                                            int count = 0;
                                                            String expect_online_calendar_before = FormatDate(expect_online_date, count);
                                                            count++;
                                                            String expect_online_calendar_after = FormatDate(expect_online_date, count);
                                                            count++;
                                                            // calendar to date
                                                            Date expect_online_date_before = sdf.parse(expect_online_calendar_before);
                                                            Date expect_online_date_after = sdf.parse(expect_online_calendar_after);

                                                            // after offline date five day and before offline date five day
                                                            Date expect_offline_date = sdf.parse(getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOfflineDate());
                                                            String expect_offline_calendar_before = FormatDate(expect_offline_date, count);
                                                            count++;
                                                            String expect_offline_calendar_after = FormatDate(expect_offline_date, count);
                                                            // calendar to date
                                                            Date expect_offline_date_before = sdf.parse(expect_offline_calendar_before);
                                                            Date expect_offline_date_after = sdf.parse(expect_offline_calendar_after);

                                                            Date finish_date = sdf.parse(get_dispatch_detail_sns.get(j).getFinishDatetime());

                                                            if ((expect_online_date_before.before(finish_date)) || ((expect_online_date_after.after(finish_date)) && (expect_offline_date_before.before(finish_date))) || (expect_offline_date_after.after(finish_date))) {
                                                                // 12 is mean the date is before the online date or after the offline date
                                                                writeFile(element_code[i], order, nest_program_no_split[each_nest_program_no_split], 12);
                                                            } else {
                                                                // bug!!!
                                                                writeFile(order, nest_program_no_split[each_nest_program_no_split]);
                                                            }
                                                        } else if (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getIsFinished().equals("1")) {
                                                            // 13 is mean finish
                                                            writeFile(element_code[i], order, nest_program_no_split[each_nest_program_no_split], 13);
                                                            break;
                                                        }  else {
                                                            // bug!!!
                                                            writeFile(order, nest_program_no_split[each_nest_program_no_split]);
                                                        }
                                                    } else if (get_order_status.get(0).getOrderStatus().equals("2")) {
                                                        writeFile(element_code[i], order, nest_program_no_split[each_nest_program_no_split], 2);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                break;
                            }
                        }
                    }
                }
                if(failedCount == 0){
                    outputForExcelReport("OK");
                }
            }

            ar.setRetMessage("Successfully check the report and write the file: " + nest_program_no_split.length);
            ar.setRetStatus("Success");
        } catch (Exception e) {
            logger.error(">>> [" + request.getSession().getId() + "] " + e.getMessage());
            e.printStackTrace();
            ar.setRetMessage(e.getMessage());
            ar.setRetStatus("Exception");
        }

        return new ResponseEntity<String>(JSON.toJSONString(ar), HttpStatus.OK);
    }

    public ResponseEntity<String> checkReportForMC(HttpServletRequest request, Model model, String order, String number, String machine_code, String type, String time) {
        if(machine_code == null || machine_code.isEmpty()) {
            machine_code = null;
        }

        ApiReturn ar = new ApiReturn();
        try {
            // delete today.txt file
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date today = new Date();
            deleteFile(sdf.format(today).toString());

            // split the number
            String[] element_code = number.split(" ");
            for (int each_element_code_split = 0; each_element_code_split < element_code.length; each_element_code_split++) {
                failedCount = 0;
                // add \n to split the each number in outputForExcel.txt file
                if (each_element_code_split > 0) {
                    outputForExcelReport();
                }

                // check the order infomation first
                List<OrderInfo> get_order_status = checkReportService.getOrderStatusByOrderNum(order, request.getSession().getId());
                if (get_order_status.size() == 0) {
                    // can not find the order infomation
                    logger.error(">>> [" + request.getSession().getId() + "] Can not get the input order: " + order);
                    writeFile(null, order, null, -1);
                } else {
                    deleteFile(order + "_" + element_code[each_element_code_split]); // if the file is exist
                    next_is_null_or_not = false;
                    if(each_element_code_split == element_code.length - 1){
                        // the element code is last
                        next_is_null_or_not = true;
                    }
                    // success get element code
                    List<ElementLog> get_dispatch_detail_sns = checkReportService.getDispatchDetailSNS(element_code[each_element_code_split], machine_code, type, time, request.getSession().getId());
                    if(get_dispatch_detail_sns.size()==0){
                        // can not find the element code log, the finished number is zero or the finish date is not input time
                        writeFile(element_code[each_element_code_split], order, null, -2);
                    }else {
                        for (int j = 0; j < get_dispatch_detail_sns.size(); j++) {
                            // maybe dispatch_detail_sns have more than two number, so split the dispatch_detail_sns
                            String[] stringSplit = get_dispatch_detail_sns.get(j).getDispatchDetailSNS().split(",");
                            for (int k = 0; k < stringSplit.length; k++) {
                                if (!stringSplit[k].equals("")) {
                                    List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_dispath_detail_sns = checkReportService.getOrderSNByDispatchDetailSNS(stringSplit[k], request.getSession().getId());
                                    List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_order_num = checkReportService.getOrderSNByOrder(order, request.getSession().getId());
                                    if(get_order_sn_by_dispath_detail_sns.size() == 0){
                                        // -5 is mean maybe the dispatch has been changed, please check the dispatch
                                        writeFile(element_code[each_element_code_split], order, null, -5);
                                    }else if (get_order_sn_by_dispath_detail_sns.get(0).getOrderSN().equals(get_order_sn_by_order_num.get(0).getOrderSN())) {
                                        // the report accurated
                                        writeFile(element_code[each_element_code_split], stringSplit[k], order, null);
                                    } else {
                                        // the report have dispatch_details_sns but not accurated
                                        List<OrderInfo> get_order_num_by_order_sn = checkReportService.getOrderNumByOrderSN(get_order_sn_by_dispath_detail_sns.get(0).getOrderSN(), request.getSession().getId());
                                        writeFile(element_code[each_element_code_split], stringSplit[k], get_order_num_by_order_sn.get(0).getOrderNum(), order, null);
                                    }
                                } else {
                                    // the report have not dispatch_details_sns that is mean repeat
                                    if (get_order_status.get(0).getOrderStatus().equals("0")) {
                                        // 0 is no dispatch
                                        writeFile(element_code[each_element_code_split], order, null, 0);
                                    } else if (get_order_status.get(0).getOrderStatus().equals("1")) {
                                        // here have dispatch
                                        List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_order_num = checkReportService.getOrderSNByOrder(order, request.getSession().getId());
                                        List<RelManufactureElement> getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN = checkReportService.getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN(
                                                element_code[each_element_code_split], type, get_order_sn_by_order_num.get(0).getOrderSN(), request.getSession().getId());

                                        if (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.size() == 0) {
                                            // 10 is mean have not data in rel_manufacture_element
                                            writeFile(element_code[each_element_code_split], order, null, 10);
                                        } else if ((getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOnlineDate() == null) &&
                                                (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOfflineDate() == null)) {
                                            // 11 is mean online and offline is null
                                            writeFile(element_code[each_element_code_split], order, null, 11);
                                        } else if ( (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getIsFinished() == null) ||
                                                (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getIsFinished().equals("0")) ) {
                                            Date expect_online_date = sdf.parse(getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOnlineDate());
                                            int count = 0;
                                            String expect_online_calendar_before = FormatDate(expect_online_date, count);
                                            count++;
                                            String expect_online_calendar_after = FormatDate(expect_online_date, count);
                                            count++;
                                            // calendar to date
                                            Date expect_online_date_before = sdf.parse(expect_online_calendar_before);
                                            Date expect_online_date_after = sdf.parse(expect_online_calendar_after);

                                            // after offline date five day and before offline date five day
                                            Date expect_offline_date = sdf.parse(getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOfflineDate());
                                            String expect_offline_calendar_before = FormatDate(expect_offline_date, count);
                                            String expect_offline_calendar_after = FormatDate(expect_offline_date, count);
                                            // calendar to date
                                            Date expect_offline_date_before = sdf.parse(expect_offline_calendar_before);
                                            Date expect_offline_date_after = sdf.parse(expect_offline_calendar_after);

                                            Date finish_date = sdf.parse(get_dispatch_detail_sns.get(j).getFinishDatetime());

                                            if ((expect_online_date_before.before(finish_date)) || ((expect_online_date_after.after(finish_date)) && (expect_offline_date_before.before(finish_date))) || (expect_offline_date_after.after(finish_date))) {
                                                // 12 is mean the date is before the online date or after the offline date
                                                writeFile(element_code[each_element_code_split], order, null, 12);
                                            } else {
                                                // bug!!!
                                                writeFile(order, element_code[each_element_code_split]);
                                            }
                                        } else if (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getIsFinished().equals("1")) {
                                            // 13 is mean finish
                                            writeFile(element_code[each_element_code_split], order, null, 13);
                                            break;
                                        } else {
                                            // bug!!!
                                            writeFile(order, element_code[each_element_code_split]);
                                        }
                                    } else if (get_order_status.get(0).getOrderStatus().equals("2")) {
                                        writeFile(element_code[each_element_code_split], order, null, 2);
                                    }
                                }
                            }
                        }
                    }
                }
                if(failedCount == 0){
                    outputForExcelReport("OK");
                }
                ar.setRetMessage("Successfully check the report and write the file: " + element_code.length);
                ar.setRetStatus("Success");
            }
        } catch(Exception e){
            logger.error(">>> [" + request.getSession().getId() + "] " + e.getMessage());
            e.printStackTrace();
            ar.setRetMessage(e.getMessage());
            ar.setRetStatus("Exception");
        }
        return new ResponseEntity<String>(JSON.toJSONString(ar), HttpStatus.OK);
    }

    public ResponseEntity<String> checkReportForMD(HttpServletRequest request, Model model, String order, String number, String machine_code, String type, String time){
        if(machine_code == null || machine_code.isEmpty()) {
            machine_code = null;
        }

        ApiReturn ar = new ApiReturn();
        try {
            // delete today.txt file
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date today = new Date();
            deleteFile(sdf.format(today).toString());

            // split the number
            String[] element_code = number.split(" ");
            for (int each_element_code_split = 0; each_element_code_split < element_code.length; each_element_code_split++) {
                failedCount = 0;
                // add \n to split the each number in outputForExcel.txt file
                if (each_element_code_split > 0) {
                    outputForExcelReport();
                }

                // check the order infomation first
                List<OrderInfo> get_order_status = checkReportService.getOrderStatusByOrderNum(order, request.getSession().getId());
                if (get_order_status.size() == 0) {
                    // can not find the order infomation
                    logger.error(">>> [" + request.getSession().getId() + "] Can not get the input order: " + order);
                    writeFile(null, order, null, -1);
                } else {
                    deleteFile(order + "_" + element_code[each_element_code_split]); // if the file is exist
                    next_is_null_or_not = false;
                    if(each_element_code_split == element_code.length - 1){
                        // the element code is last
                        next_is_null_or_not = true;
                    }
                    // success get element code
                    List<ElementLog> get_dispatch_detail_sns = checkReportService.getDispatchDetailSNS(element_code[each_element_code_split], machine_code, type, time, request.getSession().getId());
                    if(get_dispatch_detail_sns.size()==0){
                        // can not find the element code log, the finished number is zero or the finish date is not input time
                        writeFile(element_code[each_element_code_split], order, null, -2);
                    }else {
                        for (int j = 0; j < get_dispatch_detail_sns.size(); j++) {
                            // maybe dispatch_detail_sns have more than two number, so split the dispatch_detail_sns
                            String[] stringSplit = get_dispatch_detail_sns.get(j).getDispatchDetailSNS().split(",");
                            for (int k = 0; k < stringSplit.length; k++) {
                                if (!stringSplit[k].equals("")) {
                                    List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_dispath_detail_sns = checkReportService.getOrderSNByDispatchDetailSNS(stringSplit[k], request.getSession().getId());
                                    List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_order_num = checkReportService.getOrderSNByOrder(order, request.getSession().getId());
                                    if(get_order_sn_by_dispath_detail_sns.size() == 0){
                                        // -5 is mean maybe the dispatch has been changed, please check the dispatch
                                        writeFile(element_code[each_element_code_split], order, null, -5);
                                    }else if (get_order_sn_by_dispath_detail_sns.get(0).getOrderSN().equals(get_order_sn_by_order_num.get(0).getOrderSN())) {
                                        // the report accurated
                                        writeFile(element_code[each_element_code_split], stringSplit[k], order, null);
                                    } else {
                                        // the report have dispatch_details_sns but not accurated
                                        List<OrderInfo> get_order_num_by_order_sn = checkReportService.getOrderNumByOrderSN(get_order_sn_by_dispath_detail_sns.get(0).getOrderSN(), request.getSession().getId());
                                        writeFile(element_code[each_element_code_split], stringSplit[k], get_order_num_by_order_sn.get(0).getOrderNum(), order, null);
                                    }
                                } else {
                                    // the report have not dispatch_details_sns that is mean repeat
                                    if (get_order_status.get(0).getOrderStatus().equals("0")) {
                                        // 0 is no dispatch
                                        writeFile(element_code[each_element_code_split], order, null, 0);
                                    } else if (get_order_status.get(0).getOrderStatus().equals("1")) {
                                        // here have dispatch
                                        List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_order_num = checkReportService.getOrderSNByOrder(order, request.getSession().getId());
                                        List<RelManufactureElement> getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN = checkReportService.getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN(
                                                element_code[each_element_code_split], type, get_order_sn_by_order_num.get(0).getOrderSN(), request.getSession().getId());

                                        if (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.size() == 0) {
                                            // 10 is mean have not data in rel_manufacture_element
                                            writeFile(element_code[each_element_code_split], order, null, 10);
                                        } else if ((getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOnlineDate() == null) &&
                                                (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOfflineDate() == null)) {
                                            // 11 is mean online and offline is null
                                            writeFile(element_code[each_element_code_split], order, null, 11);
                                        } else if ( (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getIsFinished() == null) ||
                                                (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getIsFinished().equals("0")) ) {
                                            Date expect_online_date = sdf.parse(getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOnlineDate());
                                            int count = 0;
                                            String expect_online_calendar_before = FormatDate(expect_online_date, count);
                                            count++;
                                            String expect_online_calendar_after = FormatDate(expect_online_date, count);
                                            count++;
                                            // calendar to date
                                            Date expect_online_date_before = sdf.parse(expect_online_calendar_before);
                                            Date expect_online_date_after = sdf.parse(expect_online_calendar_after);

                                            // after offline date five day and before offline date five day
                                            Date expect_offline_date = sdf.parse(getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOfflineDate());
                                            String expect_offline_calendar_before = FormatDate(expect_offline_date, count);
                                            String expect_offline_calendar_after = FormatDate(expect_offline_date, count);
                                            // calendar to date
                                            Date expect_offline_date_before = sdf.parse(expect_offline_calendar_before);
                                            Date expect_offline_date_after = sdf.parse(expect_offline_calendar_after);

                                            Date finish_date = sdf.parse(get_dispatch_detail_sns.get(j).getFinishDatetime());

                                            if ((expect_online_date_before.before(finish_date)) || ((expect_online_date_after.after(finish_date)) && (expect_offline_date_before.before(finish_date))) || (expect_offline_date_after.after(finish_date))) {
                                                // 12 is mean the date is before the online date or after the offline date
                                                writeFile(element_code[each_element_code_split], order, null, 12);
                                            } else {
                                                // bug!!!
                                                writeFile(order, element_code[each_element_code_split]);
                                            }
                                        } else if (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getIsFinished().equals("1")) {
                                            // 13 is mean finish
                                            writeFile(element_code[each_element_code_split], order, null, 13);
                                            break;
                                        } else {
                                            // bug!!!
                                            writeFile(order, element_code[each_element_code_split]);
                                        }
                                    } else if (get_order_status.get(0).getOrderStatus().equals("2")) {
                                        writeFile(element_code[each_element_code_split], order, null, 2);
                                    }
                                }
                            }
                        }
                    }
                }
                if(failedCount == 0){
                    outputForExcelReport("OK");
                }
                ar.setRetMessage("Successfully check the report and write the file: " + element_code.length);
                ar.setRetStatus("Success");
            }
        } catch(Exception e){
            logger.error(">>> [" + request.getSession().getId() + "] " + e.getMessage());
            e.printStackTrace();
            ar.setRetMessage(e.getMessage());
            ar.setRetStatus("Exception");
        }
        return new ResponseEntity<String>(JSON.toJSONString(ar), HttpStatus.OK);
    }

    public String FormatDate(Date date, int count){
        SimpleDateFormat temp = new SimpleDateFormat("yyyy-MM-dd");
        Calendar expect_date = new GregorianCalendar();
        expect_date.setTime(date);
        if(count % 2 == 0){
            expect_date.add(expect_date.DATE, -5);
        }else{
            expect_date.add(expect_date.DATE, +5);
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return format.format(expect_date.getTime());
    }

    public void writeFile(String order, String nest_program_no) throws IOException{
        failedCount++;
        Path file_name = Paths.get(System.getProperty("user.dir"),"\\output\\" + order + "_" + nest_program_no + ".txt");
        File file = new File(file_name.toString());
        file.createNewFile();
        FileWriter writer = new FileWriter(file, true);// true is mean don't overwirte previous content
        writer.write("bug!!!");
        writer.flush();
        writer.close();
        outputForExcelReport("bug!!!");
    }

    public void writeFile(String element_code, String order, String nest_program_no, int status) throws IOException {
        failedCount++;
        Path file_name;
        if(nest_program_no == null){
            file_name = Paths.get(System.getProperty("user.dir"),"\\output\\" + order + "_" + element_code + ".txt");
        }else{
            file_name = Paths.get(System.getProperty("user.dir"),"\\output\\" + order + "_" + nest_program_no + ".txt");
        }
        File file = new File(file_name.toString());
        file.createNewFile();
        FileWriter writer = new FileWriter(file, true);// true is mean don't overwirte previous content
        if(status == -5){
            writer.write("工件:" + element_code + "請檢查派工單上下限時間是否被移除\n");
        }else if(status == -4){
            writer.write("報工主機未收到排版圖: " + nest_program_no + " 報工資訊(機台收數據有收到)!!\n");
            element_code = nest_program_no;
        }else if(status == -3){
            writer.write("排版圖: " + nest_program_no + "未上傳!!\n");
            element_code = nest_program_no;
        }else if(status == -2) {
            writer.write("請確認element_code: " + element_code + "製作完工時間!!\n");
        }else if(status == -1){
            writer.write("訂單: " + order + " 重工，找不到訂單!!\n");
            element_code = order;
        }else if(status == 0){
            writer.write("element_code: " + element_code + " 重工，此訂單未派工!!\n");
        }else if(status == 2){
            writer.write("element_code: " + element_code + " 重工，該訂單已經「結案」!!\n");
        }else if(status == 10){
            writer.write("element_code: " + element_code + " 重工，沒有資料在rel_manufacture_element中!!\n");
        }else if(status == 11){
            writer.write("element_code: " + element_code + " 重工，該派工單未填寫上下限時間!!\n");
        }else if(status == 12){
            writer.write("element_code: " + element_code + " 重工，超過上下限時間\n");
        }else if(status == 13){
            writer.write("element_code: " + element_code + " 重工，該訂單數量已足夠!!\n");
        }

        writer.flush();
        writer.close();
        outputForExcelReport(element_code, status);
    }

    public void writeFile(String element_code, String dispatch_detail_sns, String order, String nest_program_no) throws IOException {
        Path file_name;
        if(nest_program_no == null){
            file_name = Paths.get(System.getProperty("user.dir"),"\\output\\" + order + "_" + element_code + ".txt");
        }else{
            file_name = Paths.get(System.getProperty("user.dir"),"\\output\\" + order + "_" + nest_program_no + ".txt");
        }
        File file = new File(file_name.toString());
        file.createNewFile();
        FileWriter writer = new FileWriter(file, true);// true is mean don't overwirte previous content
        writer.write("element_code: " + element_code + ", dispatch_detail_sns: " + dispatch_detail_sns + " is accurate reported\n");
        writer.flush();
        writer.close();
    }

    public void writeFile(String element_code, String dispatch_detail_sns, String mappingToOtherOrder, String order, String nest_program_no) throws IOException {
        failedCount++;
        Path file_name;
        if(nest_program_no == null){
            file_name = Paths.get(System.getProperty("user.dir"),"\\output\\" + order + "_" + element_code + ".txt");
        }else{
            file_name = Paths.get(System.getProperty("user.dir"),"\\output\\" + order + "_" + nest_program_no + ".txt");
        }
        File file = new File(file_name.toString());
        file.createNewFile();
        FileWriter writer = new FileWriter(file, true);// true is mean don't overwirte previous content
        writer.write("element_code: " + element_code + ", dispatch_detail_sns: " + dispatch_detail_sns + " is not accurate reported, this report mapping to the order: " + mappingToOtherOrder + "\n");
        writer.flush();
        writer.close();
        // output element code and mapping order num
        outputForExcelReport(element_code, mappingToOtherOrder);
    }

    public void deleteFile(String deleteFileName) throws IOException {
        Path file_name = Paths.get(System.getProperty("user.dir"),"\\output\\" + deleteFileName + ".txt");
        File file = new File(file_name.toString());
        file.delete();
    }

    public void outputForExcelReport() throws IOException{
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date today = new Date();
        Path file_name = Paths.get(System.getProperty("user.dir"),"\\output\\" + sdf.format(today).toString() + ".txt");
        File file = new File(file_name.toString());
        file.createNewFile();
        FileWriter writer = new FileWriter(file, true);// true is mean don't overwirte previous content
        writer.write("\n");
        writer.flush();
        writer.close();
    }

    public void outputForExcelReport(String element_code) throws IOException{
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date today = new Date();
        Path file_name = Paths.get(System.getProperty("user.dir"),"\\output\\" + sdf.format(today).toString() + ".txt");
        File file = new File(file_name.toString());
        file.createNewFile();
        FileWriter writer = new FileWriter(file, true);// true is mean don't overwirte previous content
        writer.write(element_code);
        writer.flush();
        writer.close();
    }

    public void outputForExcelReport(String element_code, int status) throws IOException{
        String temp = "";
        if(!next_is_null_or_not){
            // the element code is not last
            temp += "、";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date today = new Date();
        Path file_name = Paths.get(System.getProperty("user.dir"),"\\output\\" + sdf.format(today).toString() + ".txt");
        File file = new File(file_name.toString());
        file.createNewFile();
        FileWriter writer = new FileWriter(file, true);// true is mean don't overwirte previous content
        if(status == -5){
            writer.write("工件:" + element_code + "請檢查派工單上下限時間是否被移除" + temp);
        }else if(status == -4){
            writer.write("報工主機未收到排版圖:" + element_code + " 報工資訊(機台收數據有收到)!!");
        }else if(status == -3){
            writer.write("排版圖:" + element_code + "未上傳!!");
        }else if(status == -2){
            writer.write("請確認工件:" + element_code + "製作完工時間" + temp);
        }else if(status == -1){
            writer.write("訂單:" + element_code + "重工，找不到訂單!!");
        }else if(status == 0){
            writer.write("工件:" + element_code + "沒有派工單" + temp);
        }else if(status == 2){
            writer.write("工件:" + element_code + "訂單已結案" + temp);
        }else if(status == 10){
            writer.write("工件:" + element_code + "沒有派工單" + temp);
        }else if(status == 11){
            writer.write("工件:" + element_code + "未填寫上下限時間" + temp);
        }else if(status == 12){
            writer.write("工件:" + element_code + "逾期" + temp);
        }else if(status == 13){
            writer.write("工件:" + element_code + "數量已足夠" + temp);
        }

        writer.flush();
        writer.close();
    }

    public void outputForExcelReport(String element_code, String otherOrder) throws IOException{
        String temp = "";
        if(!next_is_null_or_not){
            // the element code is not last
            temp += "、";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date today = new Date();
        Path file_name = Paths.get(System.getProperty("user.dir"),"\\output\\" + sdf.format(today).toString() + ".txt");
        File file = new File(file_name.toString());
        file.createNewFile();
        FileWriter writer = new FileWriter(file, true);// true is mean don't overwirte previous content
        writer.write("工件:" + element_code + "報工到訂單:" + otherOrder + temp);
        writer.flush();
        writer.close();
    }

//    @RequestMapping(value = "/download_other", method = RequestMethod.GET)
//    @ResponseBody
//    public ResponseEntity<InputStreamResource> downloadTrello(HttpServletRequest request, Model model, String member){
//
//        logger.info(">>> [" + request.getSession().getId() + "] Success get the member name: " + member);
//
//        ApiReturn ar = new ApiReturn();
//        String data = "";
//        try{
//            if(member.equals("PM")){
//                data = trelloExportService.all(request);
//            } else{
//                data = trelloExportService.eachMember(member, request);
//            }
//        }catch (Exception e) {
//            logger.debug(">>> [" + request.getSession().getId() + "] " + e.getMessage());
//            e.printStackTrace();
//            ar.setRetMessage(e.getMessage());
//            ar.setRetStatus("Exception");
//        }
//
//        // prepare write to csv
//        final byte[] bom = new byte[] { (byte) 239, (byte) 187, (byte) 191 };
//        SimpleDateFormat fileNameSDF = new SimpleDateFormat("MMdd");
//        SimpleDateFormat year = new SimpleDateFormat("yyyy");
//        Date today = new Date();
//
//        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(Util.concat(bom, data.getBytes())));
//
//        HttpHeaders header = new HttpHeaders();
//        if(member.equals("PM")){
//            header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + Util.getChinesePDFCode(year.format(today).toString() + "專案進度追蹤" + fileNameSDF.format(today).toString()) + ".csv");
//        } else{
//            header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + Util.getChinesePDFCode(member + ".csv"));
//        }
//        header.add("Cache-Control", "no-cache, no-store, must-revalidate");
//        header.add("Pragma", "no-cache");
//        header.add("Expires", "0");
//        return ResponseEntity.ok().headers(header).contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
//    }

    @RequestMapping(value = "/downloadByCalendar", method = RequestMethod.GET)
    @ResponseBody
    public void writeTodayTask(HttpServletRequest request, HttpServletResponse response, Model model, String start, String end) throws UnsupportedEncodingException, ParseException {

        logger.info(">>> [" + request.getSession().getId() + "] Success get the start date: " + start + " and end date: " + end);
        SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd");
        Date endDate = new Date();
        endDate = sdf.parse(end);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(endDate);
        calendar.add(calendar.DATE, +1);
        endDate = calendar.getTime();
        end = sdf.format(endDate);

        ApiReturn ar = new ApiReturn();

        // ------------------------------------------------
        List<TrelloLists> listTrelloLists = (List<TrelloLists>) request.getSession().getAttribute("listTrelloLists");
        Map<String, String> membersMap = (Map<String, String>) request.getSession().getAttribute("membersMap");

        XSSFWorkbook wb = new XSSFWorkbook();

        XSSFCellStyle listTitleStyle = Utility.createCellStyle(wb,(short)12,true,true, false, "PINK", false);
        XSSFCellStyle colNameStyle = Utility.createCellStyle(wb,(short)12,true,true, false, "YELLOW", false);
        XSSFCellStyle taskCenterStyle = Utility.createCellStyle(wb,(short)12,true,false, false, null, false);
        XSSFCellStyle taskDescStyle = Utility.createCellStyle(wb,(short)12,false,false, true, null, false);
        XSSFCellStyle dateStyle = Utility.createCellStyle(wb,(short)12,true,false, true, null, true);

        // 設定儲存格資料
        Sheet sheet = wb.createSheet("NEW");

        int rowIndex = 0;
        Row rowCol = sheet.createRow(rowIndex++);
        String[] colNames = {"Card","項目","說明","已完成","Due Day","Owner","備註"};

        for(int i = 0 ; i < colNames.length ; i++){
            Cell cellCol = rowCol.createCell(i);
            cellCol.setCellStyle(colNameStyle);
            cellCol.setCellValue(colNames[i]);
        }


        for(TrelloLists tl : listTrelloLists){
            logger.info("開始展開分類清單: " + tl.getName() + ", 卡片總數: " + tl.getCards().size());

            for(TrelloCards tc : tl.getCards()){
                String cardName = tc.getName();
                for(int eachCheckLists = 0; eachCheckLists < tc.getCheckLists().size(); eachCheckLists++){
                    for(int eachCheckItems = 0; eachCheckItems < tc.getCheckLists().get(eachCheckLists).getCheckItems().size(); eachCheckItems++){
                        String dueDay = tc.getCheckLists().get(eachCheckLists).getCheckItems().get(eachCheckItems).getDue();
                        if(dueDay != null){
                            // >0 dueDay > start; <0 dueDay < end
                            int resultStart = dueDay.compareTo(start);
                            int resultEnd = dueDay.compareTo(end);
                            if(!(resultStart > 0 && resultEnd < 0)){
                                tc.getCheckLists().get(eachCheckLists).getCheckItems().remove(eachCheckItems);
                                eachCheckItems--;
                                tc.getCheckLists().get(eachCheckLists).setCheckItemsCnt(tc.getCheckLists().get(eachCheckLists).getCheckItemsCnt() - 1);
                                tc.setCheckItemsCnt(tc.getCheckItemsCnt() - 1);
                            }
                        } else{
                            tc.getCheckLists().get(eachCheckLists).getCheckItems().remove(eachCheckItems);
                            eachCheckItems--;
                            tc.getCheckLists().get(eachCheckLists).setCheckItemsCnt(tc.getCheckLists().get(eachCheckLists).getCheckItemsCnt() - 1);
                            tc.setCheckItemsCnt(tc.getCheckItemsCnt() - 1);
                        }
                    }
                }
                if(tc.getCheckItemsCnt() > 1) {
                    CellRangeAddress craCard = new CellRangeAddress(rowIndex, rowIndex + tc.getCheckItemsCnt() - 1, 0, 0);
                    sheet.addMergedRegion(craCard);
                }

                for(TrelloCheckLists tcl : tc.getCheckLists()){
                    String checkListName = tcl.getName();

                    if(tcl.getCheckItemsCnt() > 1) {
                        CellRangeAddress craCheckList = new CellRangeAddress(rowIndex, rowIndex + tcl.getCheckItemsCnt() - 1, 1, 1);
                        sheet.addMergedRegion(craCheckList);
                    }

                    for(TrelloCheckItems tci : tcl.getCheckItems()){
                        String checkItemName = tci.getName();
                        String status = "";
                        if(tci.getState().equals("incomplete")){
                            status = "X";
                        } else if(tci.getState().equals("complete")){
                            status = "V";
                        }
                        String dueDay = "";
                        if(tci.getDue() != null && !tci.getDue().equals("null")){
                            dueDay = tci.getDue().split("T")[0];
                        }
                        Date due = sdf.parse(dueDay);
                        String owner = membersMap.get(tci.getIdMember());
                        Row taskRow = sheet.createRow(rowIndex++);
                        Cell cell1 = taskRow.createCell(0);
                        cell1.setCellStyle(taskDescStyle);
                        cell1.setCellValue(cardName);
                        Cell cell2 = taskRow.createCell(1);
                        cell2.setCellStyle(taskDescStyle);
                        cell2.setCellValue(checkListName);
                        Cell cell3 = taskRow.createCell(2);
                        cell3.setCellStyle(taskDescStyle);
                        cell3.setCellValue(checkItemName);
                        Cell cell4 = taskRow.createCell(3);
                        cell4.setCellStyle(taskCenterStyle);
                        cell4.setCellValue(status);
                        Cell cell5 = taskRow.createCell(4);
                        cell5.setCellStyle(dateStyle);
                        cell5.setCellValue(due);
                        Cell cell6 = taskRow.createCell(5);
                        cell6.setCellStyle(taskCenterStyle);
                        cell6.setCellValue(owner);
                        Cell cell7 = taskRow.createCell(6);
                        cell7.setCellStyle(taskDescStyle);
                        cell7.setCellValue("");
                    }
                }

                // auto filter
                CellRangeAddress c = CellRangeAddress.valueOf("D1:F1");
                sheet.setAutoFilter(c);
            }
        }

        for (int i = 0 ; i < 7 ; i++) {
            sheet.autoSizeColumn(i);
            if(i == 6){
                sheet.setColumnWidth(i, sheet.getColumnWidth(i)*3);
            }
        }

        SimpleDateFormat fileNameSDF = new SimpleDateFormat("MMdd");
        SimpleDateFormat year = new SimpleDateFormat("yyyy");
        Date today = new Date();

        String filename = fileNameSDF.format(today).toString() + "當日專案進度追蹤.xlsx";
        String headerFileName = new String(filename.getBytes(), "ISO8859-1");
        response.setHeader("Content-Disposition", "attachment; filename="+headerFileName);
        OutputStream out = null;
        try{
            out = new BufferedOutputStream(response.getOutputStream());
            wb.write(out);
        }catch (IOException e){
            System.out.println("excel會出錯誤");
        }finally {
            try{
                out.close();
                wb.close();
            } catch (IOException e){
                System.out.println("excel會出錯誤");
            }
        }
    }

    @RequestMapping(value = "/download", method = RequestMethod.GET)
    @ResponseBody
    public void write(HttpServletRequest request, HttpServletResponse response, Model model, String member) throws UnsupportedEncodingException, ParseException {

        logger.info(">>> [" + request.getSession().getId() + "] Success get the member name: " + member);

        ApiReturn ar = new ApiReturn();

        // ------------------------------------------------
        List<TrelloLists> listTrelloLists = (List<TrelloLists>) request.getSession().getAttribute("listTrelloLists");
        Map<String, String> membersMap = (Map<String, String>) request.getSession().getAttribute("membersMap");

        XSSFWorkbook wb = new XSSFWorkbook();

        XSSFCellStyle listTitleStyle = Utility.createCellStyle(wb,(short)12,true,true, false, "PINK", false);
        XSSFCellStyle colNameStyle = Utility.createCellStyle(wb,(short)12,true,true, false, "YELLOW", false);
        XSSFCellStyle taskCenterStyle = Utility.createCellStyle(wb,(short)12,true,false, false, null, false);
        XSSFCellStyle taskDescStyle = Utility.createCellStyle(wb,(short)12,false,false, true, null, false);
        XSSFCellStyle dateStyle = Utility.createCellStyle(wb,(short)12,true,false, true, null, true);


        // 設定儲存格資料
        Sheet sheet = wb.createSheet("NEW");

        int rowIndex = 0;
        for(TrelloLists tl : listTrelloLists){
            logger.info("開始展開分類清單: " + tl.getName() + ", 卡片總數: " + tl.getCards().size());
            CellRangeAddress craList = new CellRangeAddress(rowIndex, rowIndex, 0, 5);
            sheet.addMergedRegion(craList);
            Row rowList = sheet.createRow(rowIndex++);
            Cell cellList = rowList.createCell(0);
            cellList.setCellStyle(listTitleStyle);
            cellList.setCellValue(tl.getName());
            Row rowCol = sheet.createRow(rowIndex++);
            String[] colNames = {"Card","項目","說明","已完成","Due Day","Owner","備註"};

            for(int i = 0 ; i < colNames.length ; i++){
                Cell cellCol = rowCol.createCell(i);
                cellCol.setCellStyle(colNameStyle);
                cellCol.setCellValue(colNames[i]);
            }

            for(TrelloCards tc : tl.getCards()){
                String cardName = tc.getName();

                if(tc.getCheckItemsCnt() > 1) {
                    CellRangeAddress craCard = new CellRangeAddress(rowIndex, rowIndex + tc.getCheckItemsCnt() - 1, 0, 0);
                    sheet.addMergedRegion(craCard);
                }

                for(TrelloCheckLists tcl : tc.getCheckLists()){
                    String checkListName = tcl.getName();

                    if(tcl.getCheckItemsCnt() > 1) {
                        CellRangeAddress craCheckList = new CellRangeAddress(rowIndex, rowIndex + tcl.getCheckItemsCnt() - 1, 1, 1);
                        sheet.addMergedRegion(craCheckList);
                    }

                    for(TrelloCheckItems tci : tcl.getCheckItems()){
                        String checkItemName = tci.getName();
                        String status = "";
                        if(tci.getState().equals("incomplete")){
                            status = "X";
                        } else if(tci.getState().equals("complete")){
                            status = "V";
                        }
                        String dueDay = "";
                        if(tci.getDue() != null && !tci.getDue().equals("null")){
                            dueDay = tci.getDue().split("T")[0];
                        }
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        Date due = new Date();
                        if(dueDay.length() > 0){
                            due = sdf.parse(dueDay);
                        }
                        String owner = membersMap.get(tci.getIdMember());
                        Row taskRow = sheet.createRow(rowIndex++);
                        Cell cell1 = taskRow.createCell(0);
                        cell1.setCellStyle(taskDescStyle);
                        cell1.setCellValue(cardName);
                        Cell cell2 = taskRow.createCell(1);
                        cell2.setCellStyle(taskDescStyle);
                        cell2.setCellValue(checkListName);
                        Cell cell3 = taskRow.createCell(2);
                        cell3.setCellStyle(taskDescStyle);
                        cell3.setCellValue(checkItemName);
                        Cell cell4 = taskRow.createCell(3);
                        cell4.setCellStyle(taskCenterStyle);
                        cell4.setCellValue(status);
                        Cell cell5 = taskRow.createCell(4);
                        cell5.setCellStyle(dateStyle);
                        if(dueDay.length() > 0){
                            cell5.setCellValue(due);
                        } else{
                            cell5.setCellValue(dueDay);
                        }
                        Cell cell6 = taskRow.createCell(5);
                        cell6.setCellStyle(taskCenterStyle);
                        cell6.setCellValue(owner);
                        Cell cell7 = taskRow.createCell(6);
                        cell7.setCellStyle(taskDescStyle);
                        cell7.setCellValue("");
                    }
                }

                // auto filter
                CellRangeAddress c = CellRangeAddress.valueOf("D2:F2");
                sheet.setAutoFilter(c);
            }
        }

        for (int i = 0 ; i < 7 ; i++) {
            sheet.autoSizeColumn(i);
            if(i == 6){
                sheet.setColumnWidth(i, sheet.getColumnWidth(i)*3);
            }
        }

        SimpleDateFormat fileNameSDF = new SimpleDateFormat("MMdd");
        SimpleDateFormat year = new SimpleDateFormat("yyyy");
        Date today = new Date();

        String filename = year.format(today).toString() + "專案進度追蹤" + fileNameSDF.format(today).toString() + ".xlsx";
        String headerFileName = new String(filename.getBytes(), "ISO8859-1");
        response.setHeader("Content-Disposition", "attachment; filename="+headerFileName);
        OutputStream out = null;
        try{
            out = new BufferedOutputStream(response.getOutputStream());
            wb.write(out);
        }catch (IOException e){
            System.out.println("excel會出錯誤");
        }finally {
            try{
                out.close();
                wb.close();
            } catch (IOException e){
                System.out.println("excel會出錯誤");
            }
        }
    }

//    @RequestMapping(value = "/search")
//    @ResponseBody
//    public ResponseEntity<String> search(HttpServletRequest request, Model model, String member){
//
//        logger.info(">>> [" + request.getSession().getId() + "] Success and go to search");
//
//        ApiReturn ar = new ApiReturn();
//        String data = "";
//        try{
//            trelloExportService.getTrello(request);
//            ar.setRetMessage("Successfully get all trello data");
//            ar.setRetStatus("Success");
//        }catch (Exception e) {
//            logger.debug(">>> [" + request.getSession().getId() + "] " + e.getMessage());
//            e.printStackTrace();
//            ar.setRetMessage(e.getMessage());
//            ar.setRetStatus("Exception");
//        }
//        return new ResponseEntity<String>(JSON.toJSONString(ar), HttpStatus.OK);
//    }

    @RequestMapping(value = "/searchDT")
    @ResponseBody
    public JSONObject searchDT(HttpServletRequest request, Model model, String member) throws IOException, ParseException {

        logger.info(">>> [" + request.getSession().getId() + "] Success and go to search");

        JSONObject returnJson = new JSONObject();
        List<ExportToCSV> data = trelloExportService.getTrelloDT(request, returnJson, 0, null, null);

        returnJson.put("data", data);
        return returnJson;
    }

    @RequestMapping(value = "/updateComplete")
    @ResponseBody
    public ResponseEntity<String> updateComplete(HttpServletRequest request, Model model, String cardId, String checkItemsId, String status){
        if(status.equals("in")){
            // incomplete
            logger.info(">>> [" + request.getSession().getId() + "] update the checkItemsId: " + checkItemsId + "from complete to incomplete");
        } else{
            logger.info(">>> [" + request.getSession().getId() + "] update the checkItemsId: " + checkItemsId + "from incomplete to complete");
        }

        ApiReturn ar = new ApiReturn();

        String returnStr = new String();
        Properties prop = ConfigLoader.loadConfig("trello.properties");

        //權限相關
        String trelloKey = prop.getProperty("trello.key");
        String trelloToken = prop.getProperty("trello.token");
        String urlParam = "&key=" + trelloKey + "&token=" + trelloToken;


        try{
            returnStr = WebAPI.sendAPI_trello_put("cards/"+cardId+"/checkItem/"+checkItemsId+"?state=" + status + urlParam);
            List<ExportToCSV> data = trelloExportService.getTrelloDT(request, null, 1, checkItemsId, status);
            ar.setRetMessage("");
            ar.setRetStatus("Success");
        } catch (Exception e){
            e.printStackTrace();
            ar.setRetMessage(e.getMessage());
            ar.setRetStatus("Exception");
        }
        return new ResponseEntity<String>(JSON.toJSONString(ar), HttpStatus.OK);
    }
}