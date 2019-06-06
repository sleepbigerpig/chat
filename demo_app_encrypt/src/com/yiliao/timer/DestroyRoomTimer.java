package com.yiliao.timer;

import java.util.Iterator;
import java.util.Map;

import com.yiliao.domain.Room;

/**
 * 销毁空闲房间定时器
 * @author Administrator
 *
 */
public class DestroyRoomTimer{
	/**
	 * 处理空闲房间
	 */
	public void destroyRoom() {
		
		for (Iterator<Map.Entry<Integer, Room>> it = RoomTimer.useRooms.entrySet().iterator(); it.hasNext();){
		    Map.Entry<Integer, Room> item = it.next();
		   if((System.currentTimeMillis() - item.getValue().getCreateTime() > 1000*60)
				   && item.getValue().getCoverLinkUserId() == 0 
				   && item.getValue().getLaunchUserId() == 0 ) {
			   it.remove();
		   }
		}
	}

	
	

}
