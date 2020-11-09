package com.smb.checkreport.mapper;
import com.smb.checkreport.bean.*;
import com.smb.checkreport.bean.ElementLog;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface SelectDataForCheckReportMapper {
    @Select("<script>"
            + "select sn, max(create_date) as create_date, nampspace, action, user_sn, user_name, max(parameter) as parameter, info, memo, api_step, api_step_updatetime, api_step_info from smb.api_log where action = 'newElementForList'"
            + "and create_date >= #{strDate}"
            + "and parameter like CONCAT('%',#{type},'%')"
            + "and parameter like CONCAT('%',#{machine_code},'%')"
            + "and parameter like CONCAT('%',#{nest_program_no},'%')"
            + "order by create_date desc"
            + "</script>")
    List<ApiLog> getNestProgramByNestProgramNO(@Param("nest_program_no") String nest_program_no, @Param("machine_code") String machine_code, @Param("type") String type, @Param("strDate") String strDate);

    @Select("<script>"
            + "select sn, machine_code, element_code, finish_number, finish_datetime, cdate, dispatch_detail_sns from smb.element_log where finish_datetime >= #{strDate}"
            + "<if test='machine_code != null'> and machine_code = #{machine_code} </if>"
            + "and step_code = #{type}"
            + "and element_code = #{element_code}"
            + "and finish_number > '0'"
            + "group by finish_datetime, dispatch_detail_sns order by finish_datetime desc;"
            + "</script>")
    List<ElementLog> getDispatchDetailSNS(@Param("element_code") String element_code, @Param("machine_code") String machine_code, @Param("type") String type, @Param("strDate") String strDate);

    @Select("<script>"
            + "select order_sn from smb.dispatch_detail where sn = #{dispatch_detail_sns}"
            + "</script>")
    List<GetOrderSNByDispatchDetailSNS> getOrderSNByDispatchDetailSNS(@Param("dispatch_detail_sns") String dispatch_detail_sns);

    @Select("<script>"
            + "select sn as order_sn from smb.order_info where order_num = #{order}"
            + "</script>")
    List<GetOrderSNByDispatchDetailSNS> getOrderSNByOrder(@Param("order") String order);

    @Select("<script>"
            + "select sn, order_num from smb.order_info where sn = #{sn}"
            + "</script>")
    List<OrderInfo> getOrderNumByOrderSN(@Param("sn") String sn);

    @Select("<script>"
            + "select * from smb.order_info where order_num = #{order_num}"
            + "</script>")
    List<OrderInfo> getOrderStatusByOrderNum(@Param("order_num") String order_num);

    @Select("<script>"
            + "select * from smb.rel_manufacture_element where element_code = #{element_code} and step_code = #{type} and order_sn = #{order_sn}"
            + "</script>")
    List<RelManufactureElement> getIsFinishedInRelManufactureElementByElementCodeAndStepCodeAndOrderSN(@Param("element_code") String element_code, @Param("type") String type, @Param("order_sn") String order_sn);

    @Select("<script>"
            + "select program_id from qrcode_label.nest_info where program_id = #{nest_program}"
            + "</script>")
    String getProgramIdInQrcodeLabel(@Param("nest_program") String nest_program);

}
