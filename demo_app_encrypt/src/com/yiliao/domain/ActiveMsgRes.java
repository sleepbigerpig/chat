package com.yiliao.domain;

public class ActiveMsgRes extends Mid {
 
	private static final long serialVersionUID = 1L;
	/**模拟给谁发消息*/
	private int activeUserId;
	/**模拟类型  0.给用户发送真实消息(当前收到消息的用户为主播) 1.模拟收到招呼消息(当前用户为普通用户)*/
	private int activeType;
	/**消息类容*/
	private String msgContent;

	public ActiveMsgRes() {
		// TODO Auto-generated constructor stub
	}

	public ActiveMsgRes(int activeUserId, int activeType, String msgContent) {
		super();
		this.activeUserId = activeUserId;
		this.activeType = activeType;
		this.msgContent = msgContent;
	}

	public int getActiveUserId() {
		return activeUserId;
	}

	public void setActiveUserId(int activeUserId) {
		this.activeUserId = activeUserId;
	}

	public int getActiveType() {
		return activeType;
	}

	public void setActiveType(int activeType) {
		this.activeType = activeType;
	}

	public String getMsgContent() {
		return msgContent;
	}

	public void setMsgContent(String msgContent) {
		this.msgContent = msgContent;
	}
	
	
	
}
