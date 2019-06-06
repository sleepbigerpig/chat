package com.yiliao.timer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiliao.domain.NewRedPacketRes;
import com.yiliao.domain.Room;
import com.yiliao.domain.UserIoSession;
import com.yiliao.service.VideoChatService;
import com.yiliao.util.Mid;
import com.yiliao.util.SpringConfig;

import net.sf.json.JSONObject;

/**
 * 
 * @author Administrator
 * 
 */
public class VideoTiming{

	// 需要计时的用户
	/**
	 * map 中 需要存储的数据 用户ID为Key value 存储 (k: gold v:当前用户的金币，k:deplete
	 *  v:主播每分钟聊天消耗的金币 ，k: timing v:当前用户已聊天的分钟数)
	 */
	public static Map<Integer, Map<String, Integer>> timingUser = new ConcurrentHashMap<Integer, Map<String, Integer>>();
	/**
	 * 需要清理的用户Map
	 */
	public static Map<Integer, Map<String, Integer>> clearUser = new ConcurrentHashMap<Integer, Map<String, Integer>>();
	
	Logger logger = LoggerFactory.getLogger(getClass());
	
	/** 已发送提醒的用户 */
	public static  List<Integer> arr = new ArrayList<>();
	
	//获取 videoChatService
	private static VideoChatService videoChatService=null;
	
	static{
		videoChatService = (VideoChatService) SpringConfig.getInstance().getBean("videoChatService");
	}
	
	/**
	 * 开始计时
	 */
	public void TimerTiming()  {
		try {
			timingUser.forEach((k,v) ->{
				logger.info("---计费定时器正在执行---");
				    // 得到当前聊天的时间(单位:秒) 换算为分钟数
					int timing = v.get("timing") % 60 == 0 ? v.get("timing") % 60 +1 : v.get("timing") / 60 + 1;
					// 判断用户的金额是否住够继续和主播进入下一分钟的聊天
					// 如果足够 继续聊天
					// 否则断开链接 并扣除用户的金币
					
					logger.info("用户{}和{}聊天,金币{},聊天时长{},房间号{}", k,v.get("anthorId"), v.get("gold"),v.get("timing"),v.get("roomId"));
					
					if (v.get("timing") < 60 ) {
						// 聊天时间增加1秒
						v.put("timing", v.get("timing") + 1);
					/**
					 *  时间刚好60秒钟 且用户金币只住够聊天1分钟  加入到需要清理的集合中
					 */
					}else  if(v.get("timing") == 60 && v.get("gold") == v.get("deplete")){
						// 不足已聊天 加入到需要清理的用户Map
						clearUser.put(k, v);
					//用户金币大于1分钟的聊天时间
					}else if(v.get("gold") >= (v.get("deplete") * (timing==0?1:timing))){
						// 聊天时间增加1秒
						v.put("timing", v.get("timing") + 1);
						//计算出当前当前用户是否住够下一分钟的金币
						//如果不住够 那么提醒用户充值
						if(v.get("gold") < v.get("deplete")*((timing==0?1:timing)+1) && !arr.contains(k)) {
							logger.info("开始给{}推送金币不足",k);
						    IoSession session = UserIoSession.getInstance().getMapIoSession(k);
						    NewRedPacketRes np = new NewRedPacketRes();
						    np.setMid(Mid.notSufficientFunds);
						    logger.info("session"+ session.toString());
						    if(null != session)
						      session.write(JSONObject.fromObject(np).toString());
						    arr.add(k);
						}
					} else {
						// 不足已聊天 加入到需要清理的用户Map
						clearUser.put(k, v);
					}
			});
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("定时器计时异常!", e);
		}
	}

	/**
	 * 清理时间不足的用户
	 */
	public void clearUser() {
	
		try {
			for (Map.Entry<Integer, Map<String, Integer>> map : clearUser.entrySet()) {
				
				logger.info("map->{}", map);
				
				Room room = RoomTimer.useRooms.get(map.getValue().get("roomId"));
				
				if (null == room) {
					logger.info("房间不存在,房间号{}", map.getValue().get("roomId"));
					room = new Room(map.getValue().get("roomId"));
					room.setLaunchUserId(map.getKey());
					room.setCoverLinkUserId(map.getValue().get("anthorId"));
					
					RoomTimer.useRooms.put(map.getValue().get("roomId"), room);
				}
				videoChatService.breakLink(room.getRoomId(),5);
				
				// 清除当前已计算了结果了的
				clearUser.remove(map.getKey());
				timingUser.remove(map.getKey());
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}

	/**
	 * 根据房间好得到房间
	 * 
	 * @param roomId
	 * @return
	 */
	public static Room getRoom(int roomId) {

		Room room = null;
		for (Map.Entry<Integer, Map<String, Integer>> map : timingUser.entrySet()) {

			LoggerFactory.getLogger(VideoTiming.class).info("map中的数据-->{}",map.toString());
			
			if (null != map.getValue() && map.getValue().get("roomId") == roomId) {
				room = new Room(map.getValue().get("roomId"));
				room.setLaunchUserId(map.getKey());
				room.setCoverLinkUserId(map.getValue().get("anthorId"));
			}
		}
		return room;
	}
	
	
	/**
	 * 判断用户是否在聊天
	 * 返回 true 标示忙碌
	 * @param userId
	 */
	public static boolean getUserExist(int userId) {
		
		for (Map.Entry<Integer, Map<String, Integer>> map : timingUser.entrySet()) {
			LoggerFactory.getLogger(VideoTiming.class).info("map中的数据-->{}",map.toString());
			if (null != map.getValue() && map.getValue().get("anthorId") == userId || map.getKey() == userId) {
				return true;
			}
		}
		return false;
	}
  
	/**
	 * 根据用户编号返回房间号
	 * @param userId
	 * @return
	 */
	public static List<Integer> getByUserResRoom(int userId){
		
		List<Integer> arr = new ArrayList<>();
		
		for (Map.Entry<Integer, Map<String, Integer>> map : timingUser.entrySet()) {
            //根据用户编号返回房间号集合
			if (map.getValue().get("anthorId") == userId || map.getKey() == userId) {
				Room room = new Room(map.getValue().get("roomId"));
				room.setLaunchUserId(map.getKey());
				room.setCoverLinkUserId(map.getValue().get("anthorId"));
				RoomTimer.useRooms.put(map.getValue().get("roomId"), room);
				arr.add(map.getValue().get("roomId"));
			}
		}
		return arr;
	}

}
