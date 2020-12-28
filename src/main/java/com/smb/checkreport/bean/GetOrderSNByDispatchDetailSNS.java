package com.smb.checkreport.bean;

import java.io.Serializable;

public class GetOrderSNByDispatchDetailSNS implements Serializable {
    private String orderSN;

    public String getOrderSN() {
        return orderSN;
    }

    public void setOrderSN(String orderSN) {
        this.orderSN = orderSN;
    }
}
