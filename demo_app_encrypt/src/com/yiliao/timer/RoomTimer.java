package com.yiliao.timer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiliao.domain.Room;

import net.sf.json.JSONObject;

public class RoomTimer {
	
	

	/** 所有的空闲房间 */
	public static List<Room> freeRooms = Collections.synchronizedList(new ArrayList<Room>());

	/** 所有已使用的房间 */
	public static Map<Integer, Room> useRooms = new ConcurrentHashMap<Integer, Room>();
	
	
	static final Logger logger = LoggerFactory.getLogger(RoomTimer.class);

 

	public void productionFreeRoom() {
		System.out.println("开始生产空闲房间");
		// 生产房间
		for (int i = 1; i <= 100000; i++) {
			freeRooms.add(new Room(i));
		}
		System.out.println("空闲房间已生产完成.");
	}

	
	/**
	 * 获取被链接人 是否在连线中
	 * @return
	 */
	public static boolean getUserIsBusy(int userId) {
		
		for (Map.Entry<Integer, Room> m : useRooms.entrySet()) {
			
			//如果当前用户是被链接人 且链接人不为0 表示忙碌
			if (m.getValue().getCoverLinkUserId() == userId && m.getValue().getLaunchUserId() != 0) {
				System.out.println("当前房间号--->"+m.getKey());
			    return true;
			//如果 当前用是发起人 且被连接人不为0 表示忙碌
			}else if (m.getValue().getLaunchUserId() == userId && m.getValue().getCoverLinkUserId() != 0){
				 System.out.println("当前房间号--->"+m.getKey());
				 return true;
			}
		}
		return false;
	}
	
	/**
	 * 根据用户链接人用户编号返回房间好
	 * @param userId
	 * @return
	 */
	public static List<Integer> getUserIdReturnRoomId(int userId){
		
		List<Integer> rooms = new ArrayList<Integer>();
		
		for (Map.Entry<Integer, Room> m : useRooms.entrySet()) {
			if (m.getValue().getLaunchUserId() == userId || m.getValue().getCoverLinkUserId() == userId) {
				rooms.add(m.getKey());
			}
		}
		return rooms;
	}
	
	
	/**
	 * 根据频道ID返回房间号
	 * @param channelId
	 * @return Map 结果  key为房间号  value 为违规用户
	 */
	public static Map<String, Object> getChannelIdReturnRoomId(String channelId){
		
		Map<String, Object> map = new HashMap<String, Object>();
		logger.info("channelId->{}",channelId);
		for (Map.Entry<Integer, Room> m : useRooms.entrySet()) {
//			logger.info("内存中的房间->{}",JSONObject.fromObject(m).toString());
			if (null !=m.getValue().getLaunchUserLiveCode() && m.getValue().getLaunchUserLiveCode().equals(channelId)) {
				map.put("roomId", m.getKey());
				map.put("userId", m.getValue().getLaunchUserId());
				map.put("room", JSONObject.fromObject(m.getValue()).toString());
			}else if(null != m.getValue().getCoverLinkUserLiveCode() && m.getValue().getCoverLinkUserLiveCode().equals(channelId) ){
				map.put("roomId", m.getKey());
				map.put("userId", m.getValue().getCoverLinkUserId());
				map.put("room", JSONObject.fromObject(m.getValue()).toString());
			}
		}
		return map;
	}

}
