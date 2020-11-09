package com.smb.checkreport.controller;

import com.smb.checkreport.bean.*;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.smb.checkreport.service.CheckReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/task")
public class TaskCheckController {

    private static Logger logger = LoggerFactory.getLogger(TaskCheckController.class);

    @Autowired
    private CheckReportService checkReportService;

    @RequestMapping(value = "/manager/checkReport")
    @ResponseBody
    public ResponseEntity<String> checkReport(HttpServletRequest request, Model model, String order, String number, String machine_code, String type){

        logger.info(">>> [" + request.getSession().getId() + "] Success get the id and type entered by the manager, order:" + order + " number: " + number + " machine_code: " + machine_code + " type: " + type);

        ApiReturn ar = new ApiReturn();
        try{
            if(type.equals("MA")){
                logger.debug(">>> [" + request.getSession().getId() + "] Go MA");
                return checkReportForMA(request, model, order, number, machine_code, type);
            }else if(type.equals("MB")){
                logger.debug(">>> [" + request.getSession().getId() + "] Go MB");
                return checkReportForMB(request, model, order, number, machine_code, type);
            }else if(type.equals("MC")){
                logger.debug(">>> [" + request.getSession().getId() + "] Go MC");
                return checkReportForMC(request, model, order, number, machine_code, type);
            }else if(type.equals("MD")){
                logger.debug(">>> [" + request.getSession().getId() + "] Go MD");
                return checkReportForMD(request, model, order, number, machine_code, type);
            }else{
                logger.error(">>> [" + request.getSession().getId() + "] BUG");
                ar.setRetMessage("BUG?");
                ar.setRetStatus("Failed");
                return new ResponseEntity<String>(JSON.toJSONString(ar), HttpStatus.OK);
            }
        }catch (Exception e) {
            logger.error(">>> [" + request.getSession().getId() + "] " + e.getMessage());
            e.printStackTrace();
            ar.setRetMessage(e.getMessage());
            ar.setRetStatus("Exception");
        }
        return new ResponseEntity<String>(JSON.toJSONString(ar), HttpStatus.OK);
    }

    public ResponseEntity<String> checkReportForMA(HttpServletRequest request, Model model, String order, String number, String machine_code, String type){
        if(machine_code == null || machine_code.isEmpty()) {
            machine_code = null;
        }
        int failedCount = 0;
        List<String> not_find_number = new ArrayList<>();

        ApiReturn ar = new ApiReturn();
        try {
            // delete today.txt file
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date today = new Date();
            deleteFile(sdf.format(today).toString());

            // split the number
            String[] element_code = number.split(" ");
            for (int each_element_code_split = 0; each_element_code_split < element_code.length; each_element_code_split++) {
                // add \n to split the each number in outputForExcel.txt file
                if (each_element_code_split > 0) {
                    outputForExcelReport();
                }

                // check the order infomation first
                List<OrderInfo> get_order_status = checkReportService.getOrderStatusByOrderNum(order, request.getSession().getId());
                if (get_order_status.size() == 0) {
                    // can not find the order infomation
                    logger.error(">>> [" + request.getSession().getId() + "] Can not get the input order: " + order);
                    writeFile(null, order, element_code[each_element_code_split], -1);
                } else {
                    deleteFile(order + "_" + element_code[each_element_code_split]); // if the file is exist

                    // success get element code
                    List<ElementLog> get_dispatch_detail_sns = checkReportService.getDispatchDetailSNS(element_code[each_element_code_split], machine_code, type, request.getSession().getId());
                    if(get_dispatch_detail_sns.size()==0){
                        // can not find the element code log, the finished number is zero
                        writeFile(element_code[each_element_code_split], order, null, -2);
                    }else {
                        for (int j = 0; j < get_dispatch_detail_sns.size(); j++) {
                            // maybe dispatch_detail_sns have more than two number, so split the dispatch_detail_sns
                            String[] stringSplit = get_dispatch_detail_sns.get(j).getDispatchDetailSNS().split(",");
                            for (int k = 0; k < stringSplit.length; k++) {
                                if (!stringSplit[k].equals("")) {
                                    List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_dispath_detail_sns = checkReportService.getOrderSNByDispatchDetailSNS(stringSplit[k], request.getSession().getId());
                                    List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_order_num = checkReportService.getOrderSNByOrder(order, request.getSession().getId());
                                    if (get_order_sn_by_dispath_detail_sns.get(0).getOrderSN().equals(get_order_sn_by_order_num.get(0).getOrderSN())) {
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
                                        writeFile(element_code[each_element_code_split], order, element_code[each_element_code_split], 0);
                                    } else if (get_order_status.get(0).getOrderStatus().equals("1")) {
                                        // 1 is have dispatch
                                        List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_order_num = checkReportService.getOrderSNByOrder(order, request.getSession().getId());
                                        List<RelManufactureElement> getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN = checkReportService.getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN(
                                                element_code[each_element_code_split], type, get_order_sn_by_order_num.get(0).getOrderSN(), request.getSession().getId());

                                        if (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.size() == 0) {
                                            // 10 is mean have not data in rel_manufacture_element
                                            writeFile(element_code[each_element_code_split], order, null, 10);
                                        } else if (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getIsFinished().equals("1")) {
                                            // 11 is mean finish
                                            writeFile(element_code[each_element_code_split], order, null, 11);
                                            break;
                                        } else if (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getIsFinished() == null) {
                                            // Isfinished null is mean what the fuck???? status = 999
                                            writeFile(element_code[each_element_code_split], order, null, 999);
                                            break;
                                        } else if ((getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOnlineDate() == null) &&
                                                (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOfflineDate() == null)) {
                                            // 12 is mean online and offline is null
                                            writeFile(element_code[each_element_code_split], order, null, 12);
                                        } else {
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
                                                // 13 is mean the date is before the online date or after the offline date
                                                writeFile(element_code[each_element_code_split], order, null, 13);
                                            } else {
                                                // bug!!!
                                                writeFile(order, element_code[each_element_code_split]);
                                            }
                                        }
                                    } else if (get_order_status.get(0).getOrderStatus().equals("2")) {
                                        writeFile(element_code[each_element_code_split], order, null, 2);
                                    }
                                }
                            }
                        }
                    }
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

    public ResponseEntity<String> checkReportForMB(HttpServletRequest request, Model model, String order, String nest_program_no, String machine_code, String type){
        int failedCount = 0;
        List<String> not_find_nest_program_no = new ArrayList<>();

        ApiReturn ar = new ApiReturn();
        try {
            // delete today.txt file
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date today = new Date();
            deleteFile(sdf.format(today).toString());

            // split the nest program number
            String[] nest_program_no_split = nest_program_no.split(" ");
            for(int each_nest_program_no_split = 0; each_nest_program_no_split < nest_program_no_split.length; each_nest_program_no_split++) {
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
                    List<ApiLog> get_elementCode = checkReportService.getElementCodeByNestProgramNO(nest_program_no_split[each_nest_program_no_split], machine_code, type, request.getSession().getId());

                    if (get_elementCode.get(0) == null) {
                        // can not get the report
                        failedCount++;
                        not_find_nest_program_no.add(nest_program_no_split[each_nest_program_no_split]);

                        // check the nest program is exist or not in sql:qrcode_label  table:nest_info
                        String nest_program_id = checkReportService.getProgramIdInQrcodeLabel(nest_program_no_split[each_nest_program_no_split], request.getSession().getId());
                        if(nest_program_id == null){
                            writeFile(null, order, nest_program_no_split[each_nest_program_no_split],-3);
                        }else{
                            writeFile(order, nest_program_no_split[each_nest_program_no_split]);
                        }
                    } else {
                        deleteFile(order + "_" + nest_program_no_split[each_nest_program_no_split]); // if the file is exist
                        JSONArray joArray = JSONArray.parseArray(get_elementCode.get(0).getParameter());
                        String element_code[] = new String[999];
                        // get element code in nest program number
                        for (int i = 0; i < joArray.size(); i++) {
                            JSONObject jo = JSONObject.parseObject(joArray.getString(i));
                            element_code[i] = jo.getString("element_code");
                        }
                        for (int i = 0; i < element_code.length; i++) {
                            if (element_code[i] != null) { // success get element code in nest program number
                                List<ElementLog> get_dispatch_detail_sns = checkReportService.getDispatchDetailSNS(element_code[i], machine_code, type, request.getSession().getId());
                                if(get_dispatch_detail_sns.size()==0){
                                    // can not find the element code log, the finished number is zero
                                    writeFile(element_code[i], order, nest_program_no_split[each_nest_program_no_split], -2);
                                }else {
                                    for (int j = 0; j < get_dispatch_detail_sns.size(); j++) {
                                        // maybe dispatch_detail_sns have more than two number, so split the dispatch_detail_sns
                                        String[] stringSplit = get_dispatch_detail_sns.get(j).getDispatchDetailSNS().split(",");
                                        for (int k = 0; k < stringSplit.length; k++) {
                                            if (!stringSplit[k].equals("")) {
                                                List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_dispath_detail_sns = checkReportService.getOrderSNByDispatchDetailSNS(stringSplit[k], request.getSession().getId());
                                                List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_order_num = checkReportService.getOrderSNByOrder(order, request.getSession().getId());
                                                if (get_order_sn_by_dispath_detail_sns.get(0).getOrderSN().equals(get_order_sn_by_order_num.get(0).getOrderSN())) {
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
                                                    // 1 is have dispatch
                                                    List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_order_num = checkReportService.getOrderSNByOrder(order, request.getSession().getId());
                                                    List<RelManufactureElement> getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN = checkReportService.getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN(
                                                            element_code[i], type, get_order_sn_by_order_num.get(0).getOrderSN(), request.getSession().getId());

                                                    if (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.size() == 0) {
                                                        // 10 is mean have not data in rel_manufacture_element
                                                        writeFile(element_code[i], order, nest_program_no_split[each_nest_program_no_split], 10);
                                                    } else if (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getIsFinished().equals("1")) {
                                                        // 11 is mean finish
                                                        writeFile(element_code[i], order, nest_program_no_split[each_nest_program_no_split], 11);
                                                        break;
                                                    } else if ((getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOnlineDate() == null) &&
                                                            (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOfflineDate() == null)) {
                                                        // 12 is mean online and offline is null
                                                        writeFile(element_code[i], order, nest_program_no_split[each_nest_program_no_split], 12);
                                                    } else {
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
                                                            // 13 is mean the date is before the online date or after the offline date
                                                            writeFile(element_code[i], order, nest_program_no_split[each_nest_program_no_split], 13);
                                                        } else {
                                                            writeFile(order, nest_program_no_split[each_nest_program_no_split]);
                                                        }
                                                    }
                                                } else if (get_order_status.get(0).getOrderStatus().equals("2")) {
                                                    writeFile(element_code[i], order, nest_program_no_split[each_nest_program_no_split], 2);
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
            }

            if( (failedCount == 0) && (nest_program_no_split.length > 0) ){
                // mean the nest program is not null and no failed
                ar.setRetMessage("Successfully check the report and write the file: " + nest_program_no_split.length);
                ar.setRetStatus("Success");
            }else if(failedCount > 0){
                // mean have failed
                String failed_name = "";
                for(int each_not_find_nest_program_no = 0; each_not_find_nest_program_no < not_find_nest_program_no.size(); each_not_find_nest_program_no++){
                    if(each_not_find_nest_program_no == not_find_nest_program_no.size()-1){
                        failed_name = failed_name + not_find_nest_program_no.get(each_not_find_nest_program_no);
                    }else{
                        failed_name = failed_name + not_find_nest_program_no.get(each_not_find_nest_program_no) + ", ";
                    }
                }
                ar.setRetMessage("Successfully check the report and write the file: " + (nest_program_no_split.length - failedCount) + ", cannot get the report: " + failed_name + " ,and failed number: " + failedCount);
                ar.setRetStatus("Failed");
            }else{
                ar.setRetMessage("Can not get the input order infomation");
                ar.setRetStatus("Failed");
            }
        } catch (Exception e) {
            logger.error(">>> [" + request.getSession().getId() + "] " + e.getMessage());
            e.printStackTrace();
            ar.setRetMessage(e.getMessage());
            ar.setRetStatus("Exception");
        }

        return new ResponseEntity<String>(JSON.toJSONString(ar), HttpStatus.OK);
    }

    public ResponseEntity<String> checkReportForMC(HttpServletRequest request, Model model, String order, String number, String machine_code, String type) {
        if(machine_code == null || machine_code.isEmpty()) {
            machine_code = null;
        }
        int failedCount = 0;
        List<String> not_find_number = new ArrayList<>();

        ApiReturn ar = new ApiReturn();
        try {
            // delete today.txt file
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date today = new Date();
            deleteFile(sdf.format(today).toString());

            // split the number
            String[] element_code = number.split(" ");
            for (int each_element_code_split = 0; each_element_code_split < element_code.length; each_element_code_split++) {
                // add \n to split the each number in outputForExcel.txt file
                if (each_element_code_split > 0) {
                    outputForExcelReport();
                }

                // check the order infomation first
                List<OrderInfo> get_order_status = checkReportService.getOrderStatusByOrderNum(order, request.getSession().getId());
                if (get_order_status.size() == 0) {
                    // can not find the order infomation
                    logger.error(">>> [" + request.getSession().getId() + "] Can not get the input order: " + order);
                    writeFile(null, order, element_code[each_element_code_split], -1);
                } else {
                    deleteFile(order + "_" + element_code[each_element_code_split]); // if the file is exist

                    // success get element code
                    List<ElementLog> get_dispatch_detail_sns = checkReportService.getDispatchDetailSNS(element_code[each_element_code_split], machine_code, type, request.getSession().getId());
                    if(get_dispatch_detail_sns.size()==0){
                        // can not find the element code log, the finished number is zero
                        writeFile(element_code[each_element_code_split], order, null, -2);
                    }else {
                        for (int j = 0; j < get_dispatch_detail_sns.size(); j++) {
                            // maybe dispatch_detail_sns have more than two number, so split the dispatch_detail_sns
                            String[] stringSplit = get_dispatch_detail_sns.get(j).getDispatchDetailSNS().split(",");
                            for (int k = 0; k < stringSplit.length; k++) {
                                if (!stringSplit[k].equals("")) {
                                    List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_dispath_detail_sns = checkReportService.getOrderSNByDispatchDetailSNS(stringSplit[k], request.getSession().getId());
                                    List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_order_num = checkReportService.getOrderSNByOrder(order, request.getSession().getId());
                                    if (get_order_sn_by_dispath_detail_sns.get(0).getOrderSN().equals(get_order_sn_by_order_num.get(0).getOrderSN())) {
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
                                        writeFile(element_code[each_element_code_split], order, element_code[each_element_code_split], 0);
                                    } else if (get_order_status.get(0).getOrderStatus().equals("1")) {
                                        // 1 is have dispatch
                                        List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_order_num = checkReportService.getOrderSNByOrder(order, request.getSession().getId());
                                        List<RelManufactureElement> getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN = checkReportService.getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN(
                                                element_code[each_element_code_split], type, get_order_sn_by_order_num.get(0).getOrderSN(), request.getSession().getId());

                                        if (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.size() == 0) {
                                            // 10 is mean have not data in rel_manufacture_element
                                            writeFile(element_code[each_element_code_split], order, null, 10);
                                        } else if (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getIsFinished().equals("1")) {
                                            // 11 is mean finish
                                            writeFile(element_code[each_element_code_split], order, null, 11);
                                            break;
                                        } else if (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getIsFinished() == null) {
                                            // Isfinished null is mean what the fuck???? status = 999
                                            writeFile(element_code[each_element_code_split], order, null, 999);
                                            break;
                                        } else if ((getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOnlineDate() == null) &&
                                                (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOfflineDate() == null)) {
                                            // 12 is mean online and offline is null
                                            writeFile(element_code[each_element_code_split], order, null, 12);
                                        } else {
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
                                                // 13 is mean the date is before the online date or after the offline date
                                                writeFile(element_code[each_element_code_split], order, null, 13);
                                            } else {
                                                // bug!!!
                                                writeFile(order, element_code[each_element_code_split]);
                                            }
                                        }
                                    } else if (get_order_status.get(0).getOrderStatus().equals("2")) {
                                        writeFile(element_code[each_element_code_split], order, null, 2);
                                    }
                                }
                            }
                        }
                    }
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

    public ResponseEntity<String> checkReportForMD(HttpServletRequest request, Model model, String order, String number, String machine_code, String type){
        if(machine_code == null || machine_code.isEmpty()) {
            machine_code = null;
        }
        int failedCount = 0;
        List<String> not_find_number = new ArrayList<>();

        ApiReturn ar = new ApiReturn();
        try {
            // delete today.txt file
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date today = new Date();
            deleteFile(sdf.format(today).toString());

            // split the number
            String[] element_code = number.split(" ");
            for (int each_element_code_split = 0; each_element_code_split < element_code.length; each_element_code_split++) {
                // add \n to split the each number in outputForExcel.txt file
                if (each_element_code_split > 0) {
                    outputForExcelReport();
                }

                // check the order infomation first
                List<OrderInfo> get_order_status = checkReportService.getOrderStatusByOrderNum(order, request.getSession().getId());
                if (get_order_status.size() == 0) {
                    // can not find the order infomation
                    logger.error(">>> [" + request.getSession().getId() + "] Can not get the input order: " + order);
                    writeFile(null, order, element_code[each_element_code_split], -1);
                } else {
                    deleteFile(order + "_" + element_code[each_element_code_split]); // if the file is exist

                    // success get element code
                    List<ElementLog> get_dispatch_detail_sns = checkReportService.getDispatchDetailSNS(element_code[each_element_code_split], machine_code, type, request.getSession().getId());
                    if(get_dispatch_detail_sns.size()==0){
                        // can not find the element code log, the finished number is zero
                        writeFile(element_code[each_element_code_split], order, null, -2);
                    }else {
                        for (int j = 0; j < get_dispatch_detail_sns.size(); j++) {
                            // maybe dispatch_detail_sns have more than two number, so split the dispatch_detail_sns
                            String[] stringSplit = get_dispatch_detail_sns.get(j).getDispatchDetailSNS().split(",");
                            for (int k = 0; k < stringSplit.length; k++) {
                                if (!stringSplit[k].equals("")) {
                                    List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_dispath_detail_sns = checkReportService.getOrderSNByDispatchDetailSNS(stringSplit[k], request.getSession().getId());
                                    List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_order_num = checkReportService.getOrderSNByOrder(order, request.getSession().getId());
                                    if (get_order_sn_by_dispath_detail_sns.get(0).getOrderSN().equals(get_order_sn_by_order_num.get(0).getOrderSN())) {
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
                                        writeFile(element_code[each_element_code_split], order, element_code[each_element_code_split], 0);
                                    } else if (get_order_status.get(0).getOrderStatus().equals("1")) {
                                        // 1 is have dispatch
                                        List<GetOrderSNByDispatchDetailSNS> get_order_sn_by_order_num = checkReportService.getOrderSNByOrder(order, request.getSession().getId());
                                        List<RelManufactureElement> getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN = checkReportService.getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN(
                                                element_code[each_element_code_split], type, get_order_sn_by_order_num.get(0).getOrderSN(), request.getSession().getId());

                                        if (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.size() == 0) {
                                            // 10 is mean have not data in rel_manufacture_element
                                            writeFile(element_code[each_element_code_split], order, null, 10);
                                        } else if (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getIsFinished().equals("1")) {
                                            // 11 is mean finish
                                            writeFile(element_code[each_element_code_split], order, null, 11);
                                            break;
                                        } else if (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getIsFinished() == null) {
                                            // Isfinished null is mean what the fuck???? status = 999
                                            writeFile(element_code[each_element_code_split], order, null, 999);
                                            break;
                                        } else if ((getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOnlineDate() == null) &&
                                                (getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOfflineDate() == null)) {
                                            // 12 is mean online and offline is null
                                            writeFile(element_code[each_element_code_split], order, null, 12);
                                        } else {
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
                                                // 13 is mean the date is before the online date or after the offline date
                                                writeFile(element_code[each_element_code_split], order, null, 13);
                                            } else {
                                                // bug!!!
                                                writeFile(order, element_code[each_element_code_split]);
                                            }
                                        }
                                    } else if (get_order_status.get(0).getOrderStatus().equals("2")) {
                                        writeFile(element_code[each_element_code_split], order, null, 2);
                                    }
                                }
                            }
                        }
                    }
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
        Path file_name = Paths.get(System.getProperty("user.dir"),"\\output\\" + order + "_" + nest_program_no + ".txt");
        File file = new File(file_name.toString());
        file.createNewFile();
        FileWriter writer = new FileWriter(file, true);// true is mean don't overwirte previous content
        writer.write("BUG!!!");
        writer.flush();
        writer.close();
        outputForExcelReport("BUG here!!!");
    }

    public void writeFile(String element_code, String order, String nest_program_no, int status) throws IOException {
        Path file_name;
        if(nest_program_no == null){
            file_name = Paths.get(System.getProperty("user.dir"),"\\output\\" + order + "_" + element_code + ".txt");
        }else{
            file_name = Paths.get(System.getProperty("user.dir"),"\\output\\" + order + "_" + nest_program_no + ".txt");
        }
        File file = new File(file_name.toString());
        file.createNewFile();
        FileWriter writer = new FileWriter(file, true);// true is mean don't overwirte previous content
        if(status == -3){
            writer.write("排版圖: " + nest_program_no + "未上傳!!\n");
            element_code = nest_program_no;
        }else if(status == -2) {
            writer.write("element_code: " + element_code + " 此工件製作失敗!!\n");
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
            writer.write("element_code: " + element_code + " 重工，該訂單數量已足夠!!\n");
        }else if(status == 12){
            writer.write("element_code: " + element_code + " 重工，該派工單未填寫上下限時間!!\n");
        }else if(status == 13){
            writer.write("element_code: " + element_code + " 重工，超過上下限時間\n");
        }else if(status == 999){
            writer.write("element_code: " + element_code + " Isfinished欄位是null\n");
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
        writer.write(element_code + "、");
        writer.flush();
        writer.close();
    }

    public void outputForExcelReport(String element_code, int status) throws IOException{
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date today = new Date();
        Path file_name = Paths.get(System.getProperty("user.dir"),"\\output\\" + sdf.format(today).toString() + ".txt");
        File file = new File(file_name.toString());
        file.createNewFile();
        FileWriter writer = new FileWriter(file, true);// true is mean don't overwirte previous content
        if(status == -3){
            writer.write("排版圖: " + element_code + "未上傳!!\n");
        }else if(status == -2){
            writer.write("工件:" + element_code + "製作失敗、");
        }else if(status == -1){
            writer.write("訂單: " + element_code + "重工，找不到訂單!!");
        }else if(status == 0){
            writer.write("工件:" + element_code + "沒有派工單、");
        }else if(status == 2){
            writer.write("工件:" + element_code + "訂單已結案、");
        }else if(status == 10){
            writer.write("工件:" + element_code + "沒有派工單、");
        }else if(status == 11){
            writer.write("工件:" + element_code + "數量已足夠、");
        }else if(status == 12){
            writer.write("工件:" + element_code + "未填寫上下限時間、");
        }else if(status == 13){
            writer.write("工件:" + element_code + "逾期、");
        }else if(status == 999){
            writer.write("工件:" + element_code + "Isfinished欄位是null、");
        }
        writer.flush();
        writer.close();
    }

    public void outputForExcelReport(String element_code, String otherOrder) throws IOException{
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date today = new Date();
        Path file_name = Paths.get(System.getProperty("user.dir"),"\\output\\" + sdf.format(today).toString() + ".txt");
        File file = new File(file_name.toString());
        file.createNewFile();
        FileWriter writer = new FileWriter(file, true);// true is mean don't overwirte previous content
        writer.write(element_code + "報工到訂單:" + otherOrder + "、");
        writer.flush();
        writer.close();
    }
}