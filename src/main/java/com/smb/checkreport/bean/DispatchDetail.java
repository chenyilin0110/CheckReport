package com.smb.checkreport.bean;

import java.io.Serializable;

public class DispatchDetail implements Serializable {
    private String sn;
    private String dispatchSN;
    private String dispatchCode;
    private String relMESN;
    private String dispatchNumber;
    private String expectOnlineDate;
    private String expectOfflineDate;
    private String actualOnlineDate;
    private String actualOfflineDate;
    private String memo;
    private String finishNumber;
    private String elementLogData;
    private String orderSN;
    private String orderExpectOnlineDate;
    private String orderExpectOfflineDate;
    private String orderStatus;

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getDispatchSN() {
        return dispatchSN;
    }

    public void setDispatchSN(String dispatchSN) {
        this.dispatchSN = dispatchSN;
    }

    public String getDispatchCode() {
        return dispatchCode;
    }

    public void setDispatchCode(String dispatchCode) {
        this.dispatchCode = dispatchCode;
    }

    public String getRelMESN() {
        return relMESN;
    }

    public void setRelMESN(String relMESN) {
        this.relMESN = relMESN;
    }

    public String getDispatchNumber() {
        return dispatchNumber;
    }

    public void setDispatchNumber(String dispatchNumber) {
        this.dispatchNumber = dispatchNumber;
    }

    public String getExpectOnlineDate() {
        return expectOnlineDate;
    }

    public void setExpectOnlineDate(String expectOnlineDate) {
        this.expectOnlineDate = expectOnlineDate;
    }

    public String getExpectOfflineDate() {
        return expectOfflineDate;
    }

    public void setExpectOfflineDate(String expectOfflineDate) {
        this.expectOfflineDate = expectOfflineDate;
    }

    public String getActualOnlineDate() {
        return actualOnlineDate;
    }

    public void setActualOnlineDate(String actualOnlineDate) {
        this.actualOnlineDate = actualOnlineDate;
    }

    public String getActualOfflineDate() {
        return actualOfflineDate;
    }

    public void setActualOfflineDate(String actualOfflineDate) {
        this.actualOfflineDate = actualOfflineDate;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getFinishNumber() {
        return finishNumber;
    }

    public void setFinishNumber(String finishNumber) {
        this.finishNumber = finishNumber;
    }

    public String getElementLogData() {
        return elementLogData;
    }

    public void setElementLogData(String elementLogData) {
        this.elementLogData = elementLogData;
    }

    public String getOrderSN() {
        return orderSN;
    }

    public void setOrderSN(String orderSN) {
        this.orderSN = orderSN;
    }

    public String getOrderExpectOnlineDate() {
        return orderExpectOnlineDate;
    }

    public void setOrderExpectOnlineDate(String orderExpectOnlineDate) {
        this.orderExpectOnlineDate = orderExpectOnlineDate;
    }

    public String getOrderExpectOfflineDate() {
        return orderExpectOfflineDate;
    }

    public void setOrderExpectOfflineDate(String orderExpectOfflineDate) {
        this.orderExpectOfflineDate = orderExpectOfflineDate;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
}
