package com.yiliao.evnet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.yiliao.domain.Room;
import com.yiliao.service.WebSocketService;

@Component
public class PushLinkUserHandle implements ApplicationListener<PushLinkUser> {

	@Autowired
	private WebSocketService webSocketService;
	
	@Override
	public void onApplicationEvent(PushLinkUser event) {
	   
		Room room = (Room) event.getSource();
		if(null != room) {
			webSocketService.singleRoomSend(room);
		}
	}

}
