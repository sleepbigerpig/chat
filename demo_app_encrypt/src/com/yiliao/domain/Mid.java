package com.yiliao.domain;

import java.io.Serializable;

public class Mid implements Serializable{
	 
	private static final long serialVersionUID = 1L;
	
	private Integer  mid ;
	
	public Mid() {
		
	}
	
	public Mid(int mid) {
		this.mid = mid;
	}

	public Integer getMid() {
		return mid;
	}

	public void setMid(Integer mid) {
		this.mid = mid;
	}
	
	

}
