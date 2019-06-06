package com.yiliao.service;

import com.yiliao.domain.Room;

public interface WebSocketService {
	
	/**
	 * 获取已使用的房间列表
	 */
	void getUseRoomList();
	

	void singleRoomSend(Room room);
	
	
	void stopRoomSend(int roomId);
	
}
