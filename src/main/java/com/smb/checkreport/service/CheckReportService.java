package com.smb.checkreport.service;

import com.smb.checkreport.mapper.SelectDataForCheckReportMapper;
import com.smb.checkreport.bean.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

@Service
public class CheckReportService {

    private static Logger logger = LoggerFactory.getLogger(CheckReportService.class);

    @Autowired
    private SelectDataForCheckReportMapper selectDataForCheckReportMapper;

    public List<ApiLog> getElementCodeByNestProgramNO(String nest_program_no, String machine_code, String type, String sessionID){
        logger.info(">>> [" + sessionID + "] " + "get element code by nest program no: " + nest_program_no);
        // get today and format YYYY--MM-DD
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -3);// check the reports on Monday maybe the reports on Saturday and Sunday so minus three days
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = sdfDate.format(cal.getTime());
        return selectDataForCheckReportMapper.getElementCodeByNestProgramNO(nest_program_no, machine_code, type, strDate);
    }

    public List<ElementLog> getDispatchDetailSNS(String element_code, String machine_code, String type, String sessionID){
        logger.info(">>> [" + sessionID + "] " + "get dispatch ditail sns by element code: " + element_code);
        // get today and format YYYY--MM-DD
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -3); // check the reports on Monday maybe the reports on Saturday and Sunday so minus three days
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = sdfDate.format(cal.getTime());
        return selectDataForCheckReportMapper.getDispatchDetailSNS(element_code, machine_code, type, strDate);
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
}
