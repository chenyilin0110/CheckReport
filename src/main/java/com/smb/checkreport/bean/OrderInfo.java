package com.smb.checkreport.bean;

import java.io.Serializable;

public class OrderInfo implements Serializable {
    private String sn;
    private String orderNum;
    private String customerSN;
    private String customerName;
    private String projectName;
    private String projectNumber;
    private String itemName;
    private String orderEndDate;
    private String orderFisishDate;
    private String orderType;
    private String orderStatus;

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(String orderNum) {
        this.orderNum = orderNum;
    }

    public String getCustomerSN() {
        return customerSN;
    }

    public void setCustomerSN(String customerSN) {
        this.customerSN = customerSN;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectNumber() {
        return projectNumber;
    }

    public void setProjectNumber(String projectNumber) {
        this.projectNumber = projectNumber;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getOrderEndDate() {
        return orderEndDate;
    }

    public void setOrderEndDate(String orderEndDate) {
        this.orderEndDate = orderEndDate;
    }

    public String getOrderFisishDate() {
        return orderFisishDate;
    }

    public void setOrderFisishDate(String orderFisishDate) {
        this.orderFisishDate = orderFisishDate;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
}
