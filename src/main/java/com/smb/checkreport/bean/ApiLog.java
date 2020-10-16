package com.smb.checkreport.bean;

import java.io.Serializable;

public class ApiLog implements  Serializable{
    private String sn;
    private String createDate;
    private String nampspace;
    private String action;
    private String userSn;
    private String userName;
    private String parameter;
    private String info;
    private String memo;
    private String apiStep;
    private String apiStepUpdatetime;
    private String apiStepInfo;

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getNampspace() {
        return nampspace;
    }

    public void setNampspace(String nampspace) {
        this.nampspace = nampspace;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getUserSn() {
        return userSn;
    }

    public void setUserSn(String userSn) {
        this.userSn = userSn;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getApiStep() {
        return apiStep;
    }

    public void setApiStep(String apiStep) {
        this.apiStep = apiStep;
    }

    public String getApiStepUpdatetime() {
        return apiStepUpdatetime;
    }

    public void setApiStepUpdatetime(String apiStepUpdatetime) {
        this.apiStepUpdatetime = apiStepUpdatetime;
    }

    public String getApiStepInfo() {
        return apiStepInfo;
    }

    public void setApiStepInfo(String apiStepInfo) {
        this.apiStepInfo = apiStepInfo;
    }
}
