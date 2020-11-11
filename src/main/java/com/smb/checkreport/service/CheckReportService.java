package com.smb.checkreport.service;

import com.smb.checkreport.mapper.SelectDataForCheckReportMapper;
import com.smb.checkreport.bean.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@Service
public class CheckReportService {

    private static Logger logger = LoggerFactory.getLogger(CheckReportService.class);

    @Autowired
    private SelectDataForCheckReportMapper selectDataForCheckReportMapper;

    public List<ApiLog> getNestProgramByNestProgramNO(String nest_program_no, String machine_code, String type, String sessionID){
        logger.info(">>> [" + sessionID + "] " + "get element code by nest program no: " + nest_program_no);
        // get today and format YYYY--MM-DD
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -15);// check the reports on Monday maybe the reports on Saturday and Sunday so minus three days
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = sdfDate.format(cal.getTime());
        return selectDataForCheckReportMapper.getNestProgramByNestProgramNO(nest_program_no, machine_code, type, strDate);
    }

    public List<ElementLog> getDispatchDetailSNS(String element_code, String machine_code, String type, String time, String sessionID) throws ParseException {
        logger.info(">>> [" + sessionID + "] " + "get dispatch ditail sns by element code: " + element_code);
        int max = 1;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(true);
        Date format_date = sdf.parse(time);
        Calendar before_date = new GregorianCalendar();
        Calendar after_date = new GregorianCalendar();
        before_date.setTime(format_date);
        after_date.setTime(format_date);
        before_date.add(before_date.DATE, -2);
        after_date.add(after_date.DATE, 2);

        String startTime = sdf.format(before_date.getTime());
        String endTime = sdf.format(after_date.getTime());
        startTime = startTime + " 00:00:00";
        endTime = endTime + " 23:59:59";
        List<ElementLog> get_dispatch_detail_sns = selectDataForCheckReportMapper.getDispatchDetailSNS(element_code, machine_code, type, startTime, endTime);
        while( (get_dispatch_detail_sns.size() == 0) && (max <= 5) ){
            // if can not get the data that i will add date range step by one until add & minus five day
            before_date.add(before_date.DATE, -1);
            after_date.add(after_date.DATE, 1);
            startTime = sdf.format(before_date.getTime());
            endTime = sdf.format(after_date.getTime());
            startTime = startTime + " 00:00:00";
            endTime = endTime + " 23:59:59";
            get_dispatch_detail_sns = selectDataForCheckReportMapper.getDispatchDetailSNS(element_code, machine_code, type, startTime, endTime);
            max++;
        }
        return get_dispatch_detail_sns;
    }

    public List<GetOrderSNByDispatchDetailSNS> getOrderSNByDispatchDetailSNS(String dispatch_detail_sns, String sessionID){
        logger.info(">>> [" + sessionID + "] " + "get order sn by dispatch_detail_sns: " + dispatch_detail_sns);
        return selectDataForCheckReportMapper.getOrderSNByDispatchDetailSNS(dispatch_detail_sns);
    }

    public List<GetOrderSNByDispatchDetailSNS> getOrderSNByOrder(String order, String sessionID){
        logger.info(">>> [" + sessionID + "] " + "get order sn by order_num: " + order);
        return selectDataForCheckReportMapper.getOrderSNByOrder(order);
    }

    public List<OrderInfo> getOrderNumByOrderSN(String sn, String sessionID){
        logger.info(">>> [" + sessionID + "] " + "get order num by order sn: " + sn);
        return selectDataForCheckReportMapper.getOrderNumByOrderSN(sn);
    }

    public List<OrderInfo> getOrderStatusByOrderNum(String order_num, String sessionID){
        logger.info(">>> [" + sessionID + "] " + "get order status by order num: " + order_num);
        return selectDataForCheckReportMapper.getOrderStatusByOrderNum(order_num);
    }

    public List<RelManufactureElement> getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN(String element_code, String type, String order_sn, String sessionID){
        logger.info(">>> [" + sessionID + "] " + "get the element code is or not is_finished by element code: " + element_code + " , step_code: " + type + " ,order_sn: " + order_sn);
        return selectDataForCheckReportMapper.getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN(element_code, type, order_sn);
    }

    public String getProgramIdInQrcodeLabel(String nest_program, String sessionID){
        logger.info(">>> [" + sessionID + "] " + "get the nest program: " + nest_program + " and ready to find the " + nest_program + " in sql:qrcode_label table:nest_info");
        return selectDataForCheckReportMapper.getProgramIdInQrcodeLabel(nest_program);
    }

    public List<DispatchDetail> getExpectOnlineAndOfflineInDispatchDetail(String rel_manufacture_element_sn, String sessionID){
        logger.info(">>> [" + sessionID + "] " + "get expect online and offline by rel_manufacture_element_sn: " + rel_manufacture_element_sn);
        return selectDataForCheckReportMapper.getExpectOnlineAndOfflineInDispatchDetail(rel_manufacture_element_sn);
    }

}
