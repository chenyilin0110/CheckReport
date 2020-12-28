package com.smb.checkreport.page;

import java.util.List;


public class PageTemplate {

	public String getDraw() {
		return draw;
	}
	public void setDraw(String draw) {
		this.draw = draw;
	}
	public String getRecordsTotal() {
		return recordsTotal;
	}
	public void setRecordsTotal(String recordsTotal) {
		this.recordsTotal = recordsTotal;
	}
	public String getRecordsFiltered() {
		return recordsFiltered;
	}
	public void setRecordsFiltered(String recordsFiltered) {
		this.recordsFiltered = recordsFiltered;
	}

	private String draw;
	private String recordsTotal;
	private String recordsFiltered;
	private List<?> data;
	public List<?> getData() {
		return data;
	}
	public void setData(List<?> data) {
		this.data = data;
	}
}
