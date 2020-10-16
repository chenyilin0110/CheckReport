package com.smb.checkreport.bean;

import java.io.Serializable;

public class ApiReturn implements Serializable {

	private String retMessage;
	private String retStatus;

	public String getRetMessage() {
		return retMessage;
	}

	public void setRetMessage(String retMessage) {
		this.retMessage = retMessage;
	}

	public String getRetStatus() {
		return retStatus;
	}

	public void setRetStatus(String retStatus) {
		this.retStatus = retStatus;
	}
}
