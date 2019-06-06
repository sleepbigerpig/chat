package com.yiliao.domain;

public class OnLineRes extends Mid {
 
	private static final long serialVersionUID = 1L;
	/** 房间号 */
	private int roomId;
	/** 链接人用户编号 */
	private int connectUserId;
	/** 是否住够进行视频聊天 -1:不能进行聊天  1:可以进行聊天 */
	private int satisfy;
	
	public OnLineRes() {
		// TODO Auto-generated constructor stub
	}
	

	public OnLineRes(int roomId, int connectUserId) {
		super();
		this.roomId = roomId;
		this.connectUserId = connectUserId;
	}



	public int getRoomId() {
		return roomId;
	}

	public void setRoomId(int roomId) {
		this.roomId = roomId;
	}

	public int getConnectUserId() {
		return connectUserId;
	}

	public void setConnectUserId(int connectUserId) {
		this.connectUserId = connectUserId;
	}


	public int getSatisfy() {
		return satisfy;
	}


	public void setSatisfy(int satisfy) {
		this.satisfy = satisfy;
	}

}
