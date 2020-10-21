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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/task")
public class TaskConfirmController {

    private static Logger logger = LoggerFactory.getLogger(TaskConfirmController.class);

    @Autowired
    private CheckReportService checkReportService;

    @RequestMapping(value = "/manager/checkReport")
    @ResponseBody
    public ResponseEntity<String> checkReport(HttpServletRequest request, Model model, String order, String nest_program_no, String machine_code, String type){

        logger.info(">>> [" + request.getSession().getId() + "] Success get the id and type entered by the manager, order:" + order + " nest_program_no: " + nest_program_no + " machine_code: " + machine_code + " type: " + type);
        int failedCount = 0;
        ApiReturn ar = new ApiReturn();
        try {
            String[] nest_program_no_split = nest_program_no.split(",");
            for(int each_nest_program_no_split = 0; each_nest_program_no_split < nest_program_no_split.length; each_nest_program_no_split++) {
                List<ApiLog> get_elementCode = checkReportService.getElementCodeByNestProgramNO(nest_program_no_split[each_nest_program_no_split], machine_code, type, request.getSession().getId());
                if (get_elementCode.get(0) == null) {
                    failedCount++;
                    ar.setRetMessage("Can not find the report!!");
                    ar.setRetStatus("Failed");
                } else {
                    deleteFile(order + "_" + nest_program_no_split[each_nest_program_no_split]);//delete checkreport.txt in log folder
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
                                        List<OrderInfo> get_order_status = checkReportService.getOrderStatusByOrderNum(order, request.getSession().getId());
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
//                                                String expect_online_calendar_before = getCalendar(getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOnlineDate());

                                                // format the online and offline and finish date
                                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                                                // before online date five day and after online date five day
                                                Date expect_online_date = sdf.parse(getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOnlineDate());
                                                Calendar online_calendar_before = new GregorianCalendar();
                                                Calendar online_calendar_after = new GregorianCalendar();
                                                online_calendar_before.setTime(expect_online_date);
                                                online_calendar_after.setTime(expect_online_date);
                                                online_calendar_before.add(online_calendar_before.DATE, -5);
                                                online_calendar_after.add(online_calendar_after.DATE, +5);
                                                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                                                String expect_online_calendar_before = format.format(online_calendar_before.getTime());
                                                String expect_online_calendar_after = format.format(online_calendar_after.getTime());
                                                // calendar to date
                                                Date expect_online_date_before = sdf.parse(expect_online_calendar_before);
                                                Date expect_online_date_after = sdf.parse(expect_online_calendar_after);

                                                // after offline date five day and before offline date five day
                                                Date expect_offline_date = sdf.parse(getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN.get(0).getExpectOfflineDate());
                                                Calendar offline_calendar_before = new GregorianCalendar();
                                                Calendar offline_calendar_after = new GregorianCalendar();
                                                offline_calendar_before.setTime(expect_offline_date);
                                                offline_calendar_after.setTime(expect_offline_date);
                                                offline_calendar_before.add(offline_calendar_before.DATE, -5);
                                                offline_calendar_after.add(offline_calendar_before.DATE, +5);
                                                String expect_offline_calendar_before = format.format(offline_calendar_before.getTime());
                                                String expect_offline_calendar_after = format.format(offline_calendar_after.getTime());
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
                        } else {
                            break;
                        }
                    }
                    ar.setRetMessage("Sucess check the report and write the file: " + (nest_program_no_split.length - failedCount) + ", cannot get the report: " + failedCount);
                    ar.setRetStatus("Success");
                }
            }
        } catch (Exception e) {
            logger.error(">>> [" + request.getSession().getId() + "] " + e.getMessage());
            e.printStackTrace();
            ar.setRetMessage(e.getMessage());
            ar.setRetStatus("Exception");
        }

        return new ResponseEntity<String>(JSON.toJSONString(ar), HttpStatus.OK);
    }

//    public String getCalendar(String getDate){
//        // format the online and offline and finish date
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//        // before online date five day and after online date five day
//        Date expect_online_date = sdf.parse(getDate);
//        Calendar online_calendar_before = new GregorianCalendar();
//        Calendar online_calendar_after = new GregorianCalendar();
//        online_calendar_before.setTime(expect_online_date);
//        online_calendar_after.setTime(expect_online_date);
//        online_calendar_before.add(online_calendar_before.DATE, -5);
//        online_calendar_after.add(online_calendar_after.DATE, +5);
//        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
//        String expect_online_calendar_before = format.format(online_calendar_before.getTime());
//        String expect_online_calendar_after = format.format(online_calendar_after.getTime());
//        return "hhh";
//    }
    public void writeFile(String order, String nest_program_no) throws IOException{
        Path file_name = Paths.get(System.getProperty("user.dir"),"\\log\\" + order + "_" + nest_program_no + ".txt");
        File file = new File(file_name.toString());
        file.createNewFile();
        FileWriter writer = new FileWriter(file, true);// true is mean don't overwirte previous content
        writer.write("BUG!!!");
        writer.flush();
        writer.close();
    }

    public void writeFile(String element_code, String dispatch_detail_sns, String order, String nest_program_no) throws IOException {
        Path file_name = Paths.get(System.getProperty("user.dir"),"\\log\\" + order + "_" + nest_program_no + ".txt");
        File file = new File(file_name.toString());
        file.createNewFile();
        FileWriter writer = new FileWriter(file, true);// true is mean don't overwirte previous content
        writer.write("element_code: " + element_code + ", dispatch_detail_sns: " + dispatch_detail_sns + " is accurate reported\n");
        writer.flush();
        writer.close();
    }

    public void writeFile(String element_code, String dispatch_detail_sns, String mappingToOtherOrder, String order, String nest_program_no) throws IOException {
        Path file_name = Paths.get(System.getProperty("user.dir"),"\\log\\" + order + "_" + nest_program_no + ".txt");
        File file = new File(file_name.toString());
        file.createNewFile();
        FileWriter writer = new FileWriter(file, true);// true is mean don't overwirte previous content
        writer.write("element_code: " + element_code + ", dispatch_detail_sns: " + dispatch_detail_sns + " is not accurate reported, this report mapping to the order: " + mappingToOtherOrder + "\n");
        writer.flush();
        writer.close();
    }

    public void writeFile(String element_code, String order, String nest_program_no, int status) throws IOException {
        Path file_name = Paths.get(System.getProperty("user.dir"),"\\log\\" + order + "_" + nest_program_no + ".txt");
        File file = new File(file_name.toString());
        file.createNewFile();
        FileWriter writer = new FileWriter(file, true);// true is mean don't overwirte previous content
        if(status == 0){
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
        }
        writer.flush();
        writer.close();
    }

    public void deleteFile(String deleteFileName) throws IOException {
        Path file_name = Paths.get(System.getProperty("user.dir"),"\\log\\" + deleteFileName + ".txt");
        File file = new File(file_name.toString());
        file.delete();
    }
}