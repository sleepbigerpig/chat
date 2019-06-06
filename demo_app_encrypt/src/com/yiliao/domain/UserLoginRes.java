package com.yiliao.domain;

import java.io.Serializable;

public class UserLoginRes implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2157975205087278767L;
	
	
	private int mid;
	/** 提示消息 */
	private String message;
	/** 登陆状态 */
	private int state;

	public UserLoginRes() {
		// TODO Auto-generated constructor stub
	}

	public int getMid() {
		return mid;
	}

	public void setMid(int mid) {
		this.mid = mid;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

}
