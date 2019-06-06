package com.yiliao.util;

import com.yiliao.service.WebSocketService;

public class StopThread extends Thread {

	private int roomId;

	public StopThread(int roomId) {
		super();
		this.roomId = roomId;
	}

	@Override
	public void run() {

		WebSocketService webSocketService = (WebSocketService) SpringConfig
				.getInstance().getBean("webSocketService");
		synchronized (webSocketService) {
			webSocketService.stopRoomSend(roomId);
		}
	}
}
