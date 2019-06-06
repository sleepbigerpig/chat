package com.yiliao.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiliao.domain.Room;
import com.yiliao.service.WebSocketService;

public class StartThread extends Thread {
	
	Logger logger = LoggerFactory.getLogger(getClass());

	private Room room;

	public StartThread(Room room) {
		super();
		this.room = room;
	}

	@Override
	public void run() {
		
		WebSocketService webSocketService = (WebSocketService) SpringConfig
				.getInstance().getBean("webSocketService");
		synchronized (webSocketService) {
			webSocketService.singleRoomSend(room);
		}
	}
	 
}
