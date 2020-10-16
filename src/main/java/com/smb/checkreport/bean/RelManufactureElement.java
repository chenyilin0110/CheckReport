package com.smb.checkreport.bean;

import java.io.Serializable;

public class RelManufactureElement implements Serializable {
    private String sn;
    private String moSN;
    private String elementSN;
    private String elementCode;
    private String elementDesc;
    private String orderSN;
    private String productSN;
    private String doNumber;
    private String stepCode;
    private String expectOnlineDate;
    private String expectOfflineDate;
    private String actualOnlineDate;
    private String actualOfflineDate;
    private String isFinished;
    private String finishNumber;
    private String lastFinishDate;
    private String orderStatus;

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getMoSN() {
        return moSN;
    }

    public void setMoSN(String moSN) {
        this.moSN = moSN;
    }

    public String getElementSN() {
        return elementSN;
    }

    public void setElementSN(String elementSN) {
        this.elementSN = elementSN;
    }

    public String getElementCode() {
        return elementCode;
    }

    public void setElementCode(String elementCode) {
        this.elementCode = elementCode;
    }

    public String getElementDesc() {
        return elementDesc;
    }

    public void setElementDesc(String elementDesc) {
        this.elementDesc = elementDesc;
    }

    public String getOrderSN() {
        return orderSN;
    }

    public void setOrderSN(String orderSN) {
        this.orderSN = orderSN;
    }

    public String getProductSN() {
        return productSN;
    }

    public void setProductSN(String productSN) {
        this.productSN = productSN;
    }

    public String getDoNumber() {
        return doNumber;
    }

    public void setDoNumber(String doNumber) {
        this.doNumber = doNumber;
    }

    public String getStepCode() {
        return stepCode;
    }

    public void setStepCode(String stepCode) {
        this.stepCode = stepCode;
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

    public String getIsFinished() {
        return isFinished;
    }

    public void setIsFinished(String isFinished) {
        this.isFinished = isFinished;
    }

    public String getFinishNumber() {
        return finishNumber;
    }

    public void setFinishNumber(String finishNumber) {
        this.finishNumber = finishNumber;
    }

    public String getLastFinishDate() {
        return lastFinishDate;
    }

    public void setLastFinishDate(String lastFinishDate) {
        this.lastFinishDate = lastFinishDate;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
}
