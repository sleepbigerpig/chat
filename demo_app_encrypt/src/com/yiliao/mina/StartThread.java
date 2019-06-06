package com.yiliao.mina;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiliao.domain.Room;
import com.yiliao.service.WebSocketService;

public class StartThread extends Thread {
	
	Logger logger = LoggerFactory.getLogger(getClass());

	private int userId ;

	public StartThread(int userId) {
		super();
		this.userId = userId;
	}

	@Override
	public void run() {
		MinaClient mc = new MinaClient();
		mc.run(this.userId);
	}
	 
}
