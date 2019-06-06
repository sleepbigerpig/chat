package com.yiliao.service.impl.impl1_7;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.yiliao.domain.Room;
import com.yiliao.service.impl.ICommServiceImpl;
import com.yiliao.service.service1_7.SpeedDatingService;
import com.yiliao.timer.RoomTimer;
import com.yiliao.util.DateUtils;
import com.yiliao.util.MessageUtil;
import com.yiliao.util.SystemConfig;

import net.sf.json.JSONObject;

@Service("speedDatingService")
public class SpeedDatingServiceImpl extends ICommServiceImpl implements SpeedDatingService {

	//存储开启速配的所有主播信息
	public static List<Map<String, Object>> speedDatingList = Collections.synchronizedList(new ArrayList<Map<String, Object>>());
	
	/**
	 * 开启速配
	 */
	@Override
	public MessageUtil openSpeedDating(int userId,int roomId) {
		try {
			
			//用户信息
			Map<String, Object> userData = this.getMap("SELECT t_idcard,t_handImg,t_nickName,t_city,t_age,t_autograph,t_vocation,t_id FROM t_user WHERE t_id = ? ", userId);
			
			//拼接推流地址
			userData.put("rtmp", SystemConfig.getValue("play_addr")+userId+"/"+roomId);
			
			logger.info("速配链接-->{}",userData.get("rtmp"));
			//存入房间号
			userData.put("roomId", roomId);
			
			//判断房间是否还存在
			 Room room = RoomTimer.useRooms.get(roomId);
			 if(null == room) {
				 return new MessageUtil(-2,"房间已丢失.");
			 }
			 
			 room.setCoverLinkUserId(userId);
			 
			//装载到开启速配列表中
			speedDatingList.add(userData);
			
			//获取当前用户是否在速配设置中
			List<Map<String, Object>> speed_man = this.getQuerySqlList("SELECT t_begin_time,t_end_time FROM t_speed_manage WHERE t_user_id = ?", userId);
			
			if(speed_man.isEmpty()) {
				//插入速配时间记录
				this.executeSQL("INSERT INTO t_speeddating (t_user_id, t_begin_time) VALUES (?, ?);", 
						userId,DateUtils.format(new Date(), DateUtils.FullDatePattern));
			}else {
				
				String str[] = DateUtils.format(new Date(), "HH:mm").split(":");
				
				String begin_time[] = speed_man.get(0).get("t_begin_time").toString().split(":");
				String end_time[] = speed_man.get(0).get("t_end_time").toString().split(":");
				//如果开始时间的小时小于当前时间的小时
				//和开始时间的分钟小于当前时间的分钟
				//且 结束时间的小时大于了当前时间的小时
				//或者 结束时间的小时等于了当前时间的小时 且 分钟大于当前时间的分钟
				if(Integer.parseInt(begin_time[0]) < Integer.parseInt(str[0]) || 
						(Integer.parseInt(begin_time[0]) == Integer.parseInt(str[0]) && Integer.parseInt(begin_time[1]) < Integer.parseInt(str[1]))) {
					//判断结束时间
					if(Integer.parseInt(end_time[0]) > Integer.parseInt(str[0]) ||
							(Integer.parseInt(end_time[0]) == Integer.parseInt(str[0]) 
							&& Integer.parseInt(end_time[1]) > Integer.parseInt(str[1]))) {
						//插入速配时间记录
						this.executeSQL("INSERT INTO t_speeddating (t_user_id, t_begin_time) VALUES (?, ?);", 
								userId,DateUtils.format(new Date(), DateUtils.FullDatePattern));
					}
				}
			}
//			logger.info("--主播{}开启速配,结束原来的速配--");
//			endSpeedDating(userId);
			return new MessageUtil(1, "开启成功!");
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}开启速配异常!",userId);
			return new MessageUtil(0, "程序异常!");
		}
	}

	/**
	 * 用户拉取速配主播
	 */
	@Override
	public MessageUtil getSpeedDatingAnchor(int userId) {
		try {
			
			if(speedDatingList.size() == 0) {
				return new MessageUtil(-1, "暂无主播在线.");
			}
			//随机获取开启了速配的主播
			Map<String, Object> userData =speedDatingList.get(new Random().nextInt(speedDatingList.size()));
			
			logger.info("当前开启速配总数->{}",speedDatingList.size());
			logger.info("当前用户拉取到是速配记录->{}",JSONObject.fromObject(userData).toString());
			
			//根据主播编号得到当前用户是否关注了主播
			List<Map<String,Object>> sqlList = this.getQuerySqlList("SELECT * FROM t_follow WHERE t_follow_id = ? AND  t_cover_follow = ? ", userId,userData.get("t_id"));
			
			if(!sqlList.isEmpty()) {
				userData.put("isFollow", 1);
			}else {
				userData.put("isFollow", 0);
			}
			
			return new MessageUtil(1, userData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取输配主播异常!", userId);
			return new MessageUtil(0, "程序异常!");
		}
	}

	/**
	 * 结束速配
	 */
	@Override
	public MessageUtil endSpeedDating(int userId,int methodType) {
		try {
			
			//断流回调
			if(methodType == 0) {
				//修改当前主播的大房间主播未关闭
				this.executeSQL("UPDATE t_big_room_man SET t_is_debut = 0 WHERE t_user_id = ? ", userId);
			}
			
			logger.info("--用户{}结束速配--",userId);
			int index = -1 ;
			for (int i = 0; i < speedDatingList.size(); i++) {
				
				Map<String,Object> m = speedDatingList.get(i);
				if(m.get("t_id").toString().equals(""+userId)) {
					index = i;
					break;
				}
			}
			
			if(-1 == index) {
				logger.info("未找到速配记录!");
				return new MessageUtil(-1, "用户未开启速配.");
			}
			
			speedDatingList.remove(index);
			
			//获取用户是否在速配中
			List<Map<String, Object>> speedList = this.getQuerySqlList("SELECT * FROM t_speeddating WHERE t_end_time IS NULL AND t_user_id = ? ", userId);
			
			if(!speedList.isEmpty()) {
				
				speedList.forEach(s ->{
					long logoutTime = System.currentTimeMillis();
					try {
						long time = logoutTime - DateUtils.parse(s.get("t_begin_time").toString(), DateUtils.FullDatePattern).getTime();
					
						this.executeSQL("UPDATE t_speeddating SET t_end_time = ?,t_duration = ? WHERE t_id = ? ;",
								DateUtils.format(logoutTime, DateUtils.FullDatePattern), (time / 1000),	s.get("t_id"));
					} catch (ParseException e) {
						e.printStackTrace();
					}
				});
			}
			logger.info("{}速配已结束",userId);
			return new MessageUtil(1, "操作成功!");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}结束速配异常!", userId);
			return new MessageUtil(0, "程序异常!");
		}
	}

	@Override
	public MessageUtil getUserSpeedTime(int userId) {
		try {
			//获取当前用户的日统计
			String count_time = DateUtils.format(new Date(), DateUtils.defaultDatePattern);
			
			Map<String, Object> map = this.getMap("SELECT SUM(t_duration) AS dayTime FROM t_speeddating WHERE t_user_id = ? AND t_begin_time BETWEEN ? AND ? ",
					userId,count_time+" 00:00:00",count_time + " 23::59:59");
			
			//获取月
			   int year = DateUtils.nowCal().get(Calendar.YEAR);
			   int month = DateUtils.nowCal().get(Calendar.MONTH) + 1;
			   
			   
			   Map<String, Object> month_totalTime = this.getMap("SELECT SUM(t_duration) AS totalTime FROM t_speeddating WHERE t_user_id = ? AND t_begin_time BETWEEN ? AND ? ",
					   userId,DateUtils.getFirstDayOfMonth(year, month),
					   DateUtils.getLastDayOfMonth(year, month));
			   map.put("dayTime", null ==map.get("dayTime")?0:DateUtils.getConvert(Integer.parseInt(map.get("dayTime").toString())));
			   
			   map.put("monthTime",null ==month_totalTime.get("totalTime")?0:DateUtils.getConvert(Integer.parseInt(month_totalTime.get("totalTime").toString())));
			   
			   return new MessageUtil(1, map);
			
		} catch (Exception e) {
			e.printStackTrace();
			return new MessageUtil(0, "程序异常!");
		}
	}

}
