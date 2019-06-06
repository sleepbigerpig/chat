package com.yiliao.domain;
/**
 * 涉嫌违规推送
 * @author Administrator
 *
 */
public class BannedRes extends Mid {
	 
	private static final long serialVersionUID = 1L;
	
	private int userId;
	
	private String content;
	
	public BannedRes() {
		// TODO Auto-generated constructor stub
	}

	public BannedRes(int userId, String content) {
		super();
		this.userId = userId;
		this.content = content;
	}




	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
	
	

}
