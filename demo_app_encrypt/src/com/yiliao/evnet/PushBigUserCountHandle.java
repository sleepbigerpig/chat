package com.yiliao.evnet;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.yiliao.service.version2_0.BigRoomService;

@Component
public class PushBigUserCountHandle implements ApplicationListener<PushBigUserCount> {

	@Autowired
	private BigRoomService bigRoomService;
	
	@Override
	public void onApplicationEvent(PushBigUserCount event) {
		Map<String, Object> map = (Map<String, Object>) event.getSource();
		
		this.bigRoomService.sendBigUserCount(map);
	}

}
