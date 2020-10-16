package com.smb.checkreport.bean;

public class ElementLog {
    private String sn;
    private String machineCode;
    private String elementCode;
    private String finishNumber;
    private String finishDatetime;
    private String cdate;
    private String dispatchDetailSNS;

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getMachineCode() {
        return machineCode;
    }

    public void setMachineCode(String machineCode) {
        this.machineCode = machineCode;
    }

    public String getElementCode() {
        return elementCode;
    }

    public void setElementCode(String elementCode) {
        this.elementCode = elementCode;
    }

    public String getFinishNumber() {
        return finishNumber;
    }

    public void setFinishNumber(String finishNumber) {
        this.finishNumber = finishNumber;
    }

    public String getFinishDatetime() {
        return finishDatetime;
    }

    public void setFinishDatetime(String finishDatetime) {
        this.finishDatetime = finishDatetime;
    }

    public String getCdate() {
        return cdate;
    }

    public void setCdate(String cdate) {
        this.cdate = cdate;
    }

    public String getDispatchDetailSNS() {
        return dispatchDetailSNS;
    }

    public void setDispatchDetailSNS(String dispatchDetailSNS) {
        this.dispatchDetailSNS = dispatchDetailSNS;
    }
}
