package com.yiliao.service.impl;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.session.IoSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;

import com.yiliao.domain.MessageEntity;
import com.yiliao.domain.OnLineRes;
import com.yiliao.domain.Room;
import com.yiliao.domain.UserIoSession;
import com.yiliao.domain.WalletDetail;
import com.yiliao.evnet.PushLinkUser;
import com.yiliao.evnet.PushMesgEvnet;
import com.yiliao.service.GoldComputeService;
import com.yiliao.service.VideoChatService;
import com.yiliao.timer.RoomTimer;
import com.yiliao.timer.VideoTiming;
import com.yiliao.util.DateUtils;
import com.yiliao.util.MessageUtil;
import com.yiliao.util.Mid;
import com.yiliao.util.SpringConfig;
import com.yiliao.util.StopThread;
import com.yiliao.util.SystemConfig;
import com.yiliao.util.WebRTCSigApi;

import net.sf.json.JSONObject;

/**
 * 视频聊天
 * 
 * @author Administrator
 */
@Service("videoChatService")
public class VideoChatServiceImpl extends ICommServiceImpl implements VideoChatService {

	private MessageUtil mu = null;

	private GoldComputeService goldComputeService = (GoldComputeService) SpringConfig.getInstance()
			.getBean("goldComputeService");

	@Override
	public MessageUtil getSpeedDatingRoom(int userId) {
		try {
			
			if (RoomTimer.freeRooms.size() == 0) {
				return new MessageUtil(0, "房间暂未生产.");
			}
			// 给用户分配房间号
			Room rm = RoomTimer.freeRooms.get(0);
			rm.setCreateTime(System.currentTimeMillis());
			// 删除可用当前可用房间
			RoomTimer.freeRooms.remove(0);

			RoomTimer.useRooms.put(rm.getRoomId(), rm);
			
			return new MessageUtil(1, rm.getRoomId());
			
		} catch (Exception e) {
			 e.printStackTrace();
			 logger.error("{}获取速配房间号异常!",userId);
		}
		return null;
	}
	
	@Override
	public MessageUtil getImUserSig(int userId) {
		try {
			String userSig = WebRTCSigApi.getInstance().genUserSig(userId + "", 7200);

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(userSig);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("IM获取用户签名异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getVideoChatUserSig(int userId, int anthorId) {
		try {

			if (RoomTimer.freeRooms.size() == 0) {
				return new MessageUtil(0, "房间暂未生产.");
			}
			// 给用户分配房间号
			Room rm = RoomTimer.freeRooms.get(0);
			rm.setCreateTime(System.currentTimeMillis());
			// 删除可用当前可用房间
			RoomTimer.freeRooms.remove(0);

			RoomTimer.useRooms.put(rm.getRoomId(), rm);

			JSONObject json = new JSONObject();
			json.put("roomId", rm.getRoomId());

			// 获取用户金币数
			Map<String, Object> userBalance = this.getMap(
					"SELECT SUM(t_recharge_money+t_profit_money+t_share_money) AS totalBalance FROM t_balance WHERE t_user_id = ? ",
					userId);

			// 获取主播联系需要的金币
			// 获取被链接人每分钟需要消耗多少金币
			String videoGoldSql = "SELECT t_video_gold FROM t_anchor_setup WHERE t_user_id = ?";

			 List<Map<String, Object>> anchorList = this.getQuerySqlList(videoGoldSql, anthorId);
			 
			 if(anchorList.isEmpty()) {
				 logger.info("当前主播编号-->{}",anthorId);
				 return new MessageUtil(-1, "--参数异常--");
			 }
				 
			 Map<String, Object> videoGold = anchorList.get(0);

			// 用户金额
			BigDecimal userBal = new BigDecimal(userBalance.get("totalBalance").toString());
			// 主播聊天金额
			BigDecimal anthBal = new BigDecimal(videoGold.get("t_video_gold").toString());
			
			// 判断用户的金币是否满足聊天计时
			if (null == userBal || userBal.compareTo(anthBal) == -1 ) {
				json.put("onlineState", -1);
			} else {
				if (userBal.compareTo(BigDecimal.valueOf(0)) > 0) {

					if (userBal.compareTo(anthBal) == 0) {
						json.put("onlineState", 1);
					}else if (anthBal.multiply(BigDecimal.valueOf(2)).setScale(0, BigDecimal.ROUND_DOWN)
							.compareTo(userBal) >= 0 && userBal.compareTo(anthBal) > 0) {
						json.put("onlineState", 1);
					}
				}
			}

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(json);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("用户获取签名异常!", e);
			mu = new MessageUtil(0, "程序异常!");

		}
		return mu;
	}

	/*
	 * 获取privateMapKey(non-Javadoc)
	 * 
	 * @see com.yiliao.service.VideoChatService#getVideoChatPriavteMapKey(int, int)
	 */
	@Override
	public MessageUtil getVideoChatPriavteMapKey(int userId, int roomId) {
		try {

			String privateMapKey = WebRTCSigApi.getInstance().genPrivateMapKey(userId + "", roomId, 300);

			mu = new MessageUtil();

			mu.setM_istatus(1);
			mu.setM_object(privateMapKey);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("用户获取privateMapKey异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/**
	 * 判断2个性别是否相同
	 * 
	 * @param userId
	 * @param coverUserId
	 * @return
	 */
	private boolean sameSex(int userId, int coverUserId) {

		String qSql = "SELECT t_sex FROM  t_user WHERE t_id = ? UNION SELECT t_sex FROM  t_user WHERE t_id = ?";
		List<Map<String, Object>> sqlList = this.getQuerySqlList(qSql, userId, coverUserId);

		if (Integer.parseInt(sqlList.get(0).get("t_sex").toString()) == Integer
				.parseInt(sqlList.get(1).get("t_sex").toString())) {
			return true;
		}
		return false;
	}

	/**
	 * 用户发起聊天
	 */
	@Override
	public MessageUtil launchVideoChat(int launchUserId, int coverLinkUserId, int roomId) {
		try {

			// 销毁用户所有存在的房间信息 并把房间返回房间池
			userHangupLink(launchUserId);
			
			if(launchUserId  == coverLinkUserId){
				return new MessageUtil(-7, "不能和自己进行视频聊天.");
			}

			// 获取用户的所有金币
			String goldSql = "SELECT SUM(t_recharge_money+t_profit_money+t_share_money) AS totalGold FROM t_balance WHERE t_user_id = ?";

			Map<String, Object> totalGold = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(goldSql,
					launchUserId);

			// 获取被链接人每分钟需要消耗多少金币
			String videoGoldSql = "SELECT t_video_gold FROM t_anchor_setup WHERE t_user_id = ?";
			
			List<Map<String,Object>> sqlList = this.getQuerySqlList(videoGoldSql, coverLinkUserId);
			
			if(sqlList.isEmpty()) {

				return new MessageUtil(-6, "主播资料未完善!无法发起聊天.");
			}

			// 判断用户的金币是否满足聊天计时
			if (null == totalGold.get("totalGold") || new BigDecimal(totalGold.get("totalGold").toString())
					.compareTo(new BigDecimal(sqlList.get(0).get("t_video_gold").toString())) < 0) {
				return new MessageUtil(-4, "余额不足!请充值.");
			}

			// 验证发起人和被链接人是否是同性别
			if(sameSex(launchUserId, coverLinkUserId)) {
				return new MessageUtil(-5,"同性别无法进行聊天!");
			}
			// 查询当前用户呼叫的主播是否是虚拟主播
			String qSql = " SELECT * FROM t_virtual WHERE t_user_id = ? ";

			List<Map<String, Object>> virList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql,
					coverLinkUserId);
			// 如果被呼叫的用户不是虚拟主播 那么走正常流程 否则让用户直接呼叫
			if (null == virList || virList.isEmpty()) {

				IoSession ioSession = UserIoSession.getInstance().getMapIoSession(coverLinkUserId);
 
				//获取主播是否正在连线中
				if(RoomTimer.getUserIsBusy(coverLinkUserId)){

					return new MessageUtil(-2, "你拨打的用户正忙,请稍后在拨.");
				}
				
				//获取主播是否正在大房间直播中
				List<Map<String,Object>> list = this.getQuerySqlList("SELECT t_is_debut FROM t_big_room_man WHERE t_user_id = ? ", coverLinkUserId);
				
				if(null != list && !list.isEmpty() && "1".equals(list.get(0).get("t_is_debut").toString())) {
					return new MessageUtil(-2, "你拨打的用户正忙,请稍后在拨.");
				}
				
				// 判断主播是否设置为勿扰
				String sql = "SELECT * FROM t_user WHERE t_id = ? AND t_is_not_disturb = 1";
				List<Map<String, Object>> users = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql,
						coverLinkUserId);

				if (null != users && !users.isEmpty()) {
					return new MessageUtil(-3, "Sorry,对方设置了勿扰.");
				}
				// 把链接人
				// 记录用户的链接信息
				Room room = new Room(roomId);
				room.setLaunchUserId(launchUserId);
				room.setCoverLinkUserId(coverLinkUserId);
				room.setCallUserId(launchUserId);

				//写入到通话记录中
				saveCallLog(launchUserId, coverLinkUserId,roomId);

				room.setLaunchUserLiveCode(SystemConfig.getValue("play_addr")+room.getLaunchUserId()+"/"+roomId);
				room.setCoverLinkUserLiveCode(SystemConfig.getValue("play_addr")+room.getCoverLinkUserId()+"/"+roomId);
				
				RoomTimer.useRooms.put(roomId, room);
				//
				OnLineRes or = new OnLineRes();
				or.setMid(Mid.onLineRes);
				or.setRoomId(roomId);
				or.setConnectUserId(launchUserId);

				logger.info("{}用户对{}发起了聊天,对方session->{},房间号->{}", launchUserId, coverLinkUserId, ioSession, roomId);

				if (null != ioSession)
					ioSession.write(JSONObject.fromObject(or).toString());

				// 获取链接人的昵称
				Map<String, Object> userMap = this.getMap("SELECT t_nickName FROM t_user WHERE t_id = ?", launchUserId);

				this.applicationContext.publishEvent(new PushMesgEvnet(
						new MessageEntity(coverLinkUserId, userMap.get("t_nickName")+"邀请您进行视频聊天!", 0, new Date(),6,roomId,launchUserId,0)));
			}else{

				// 查询当前虚拟主播是否在忙碌或者离线状态
				qSql = " SELECT t_state FROM t_anchor WHERE t_user_id = ? ";

				Map<String, Object> viaMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(qSql,
						coverLinkUserId);

				if ("1".equals(viaMap.get("t_state").toString())) {
					return new MessageUtil(-2, "你拨打的用户正忙,请稍后在拨.");
				} else if ("2".equals(viaMap.get("t_state").toString())) {
					return new MessageUtil(-1, "对方不在线!");
				}
			}

			mu = new MessageUtil(1, "正在链接.");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("发起视频聊天异常!当前链接人【{}】,被链接人【{}】", launchUserId, coverLinkUserId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/**
	 * 开始计时
	 */
	@Override
	public MessageUtil videoCharBeginTiming(int anthorId, int userId, int roomId) {
		try {

			logger.info("[{}]房间开始计时,anthorId->{},userId->{}", roomId, anthorId, userId);
			//获取用户是否正在计时当中
			if(VideoTiming.timingUser.containsKey(userId)) {
				//打印当前计时信息
				logger.info("当前用户的计时信息为-->{}",JSONObject.fromObject(VideoTiming.timingUser.get(userId)).toString());
				//判断当前用户的计时信息 是否和最新需要计时的信息一致
				Map<String, Integer> map = VideoTiming.timingUser.get(userId);
				//判断当前计时中的主播和房间号是否一样 
				//如果一致,表示重复调用计时 不用执行了
				if(map.get("roomId") == roomId && map.get("anthorId") == anthorId) {
					return new MessageUtil(-6, "当前用户正在计时中.");
				}else { //否则 调用挂断链接 程序继续执行
					this.breakLink(map.get("roomId"), 6);
				}
			}
			
			// 获取用户的所有金币
			String goldSql = "SELECT SUM(t_recharge_money+t_profit_money+t_share_money) AS totalGold FROM t_balance WHERE t_user_id = ?";

			Map<String, Object> totalGold = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(goldSql,
					userId);

			if (null == totalGold.get("totalGold") || new BigDecimal(0).compareTo(new BigDecimal(totalGold.get("totalGold").toString())) >= 0) {
				logger.info("{}用户余额为0或者负数", userId);
				return new MessageUtil(-1, "余额不足!请充值.");
			}else {
				//判断用户余额是否住够聊天
				// 获取被链接人每分钟需要消耗多少金币
				String videoGoldSql = "SELECT t_video_gold FROM t_anchor_setup WHERE t_user_id = ?";
				List<Map<String,Object>> sqlList = this.getQuerySqlList(videoGoldSql, anthorId);
				
				if(new BigDecimal(sqlList.get(0).get("t_video_gold").toString())
						.compareTo(new BigDecimal(totalGold.get("totalGold").toString())) == 1 ) {
					logger.info("用户余额-->{}",totalGold.get("totalGold").toString());
					return new MessageUtil(-1, "余额不足!请充值.");
				}
				
			}

			// 获取被链接人每分钟需要消耗多少金币
			String videoGoldSql = "SELECT t_video_gold FROM t_anchor_setup WHERE t_user_id = ?";
			
			 List<Map<String,Object>> sqlList = this.getQuerySqlList(videoGoldSql, anthorId);
			
			 if(sqlList.isEmpty()) {
				 return new MessageUtil(-2, "主播资料未完善!");
			 }
			 
			 //判断房间是否还存在
			 Room room = RoomTimer.useRooms.get(roomId);
			 if(null == room) {
				 room = VideoTiming.getRoom(roomId);
			 }
			 
			 if(null == room) {
				 return new MessageUtil(-5, "房间已丢失.");
			 }
			 //判断当前房间是否已经有人了
			 if(0 !=room.getLaunchUserId() && room.getLaunchUserId() != userId) {
				 return new MessageUtil(-4, "啊！被别人抢走了,下次手速要快哦.");
			 }else if(room.getLaunchUserId() == 0) {
				 //用户加入速配房间
				 room.setLaunchUserId(userId);
				 room.setCallUserId(userId);
				//写入到通话记录中
			    saveCallLog(userId, anthorId,roomId);
			    
				room.setLaunchUserLiveCode(SystemConfig.getValue("play_addr")+room.getLaunchUserId()+"/"+roomId);
				room.setCoverLinkUserLiveCode(SystemConfig.getValue("play_addr")+room.getCoverLinkUserId()+"/"+roomId);
			    
			 }
			 
			 //房间已销毁
			 if(room.getCoverLinkUserId() != anthorId || room.getLaunchUserId() != userId) {
				 logger.info("{}房间已经销毁.",roomId);
				 return new MessageUtil(-3, "对方挂断视频请求.");
			 }
			 
			 logger.info("{}用户余额{}", userId, new BigDecimal(totalGold.get("totalGold").toString()).intValue());
			 logger.info("{}主播每分钟视频收费{}", anthorId,new BigDecimal(sqlList.get(0).get("t_video_gold").toString()).intValue());
			
			// 加入到计时器中
			Map<String, Integer> map = new HashMap<String, Integer>();
			map.put("gold", new BigDecimal(totalGold.get("totalGold").toString()).intValue());
			map.put("deplete", new BigDecimal(sqlList.get(0).get("t_video_gold").toString()).intValue());
			map.put("timing", 0);
			map.put("roomId", roomId); //房间号
			map.put("anthorId", anthorId); //主播编号
 
			
			logger.info("{}房间开始计时,anthorId->{},userId->{},加入到计时系统开始",roomId,anthorId,userId);

			VideoTiming.timingUser.put(userId, map);

			logger.info("{}房间开始计时,anthorId->{},userId->{},加入到计时系统完成", roomId, anthorId, userId);

			// 链接信息写入到数据库中
			this.executeSQL("INSERT INTO t_room_time (t_room_id, t_call_user_id, t_answer_user_id,t_an_vi_gold, t_create_time) VALUES (?, ?, ?, ?, ?);",
					roomId,userId,anthorId,map.get("deplete"),DateUtils.format(new Date(), DateUtils.FullDatePattern));

		
			TransactionStatus status = this.getStatus(); // 获得事务状态
			// 修改主播状态为忙碌
			this.executeSQL("UPDATE t_anchor SET t_state=1 WHERE  t_user_id = ?;", anthorId);
			//提交事物
			this.getTxManager().commit(status);
			
			/***********监控程序***********/
			if(null != room) {
				//异步通知 
				this.applicationContext.publishEvent(new PushLinkUser(RoomTimer.useRooms.get(roomId)));
			}
			/***************************/
			
			/*********推送视频提示语***********/
			//链接人
			//获取提示信息
			 List<Map<String, Object>> tipsList = this.getQuerySqlList("SELECT t_video_hint FROM t_system_setup ");
			 //如果后台填写了 提示信息
			 if(!tipsList.isEmpty()) {
				 //链接人session
				 IoSession callUserIoSession = UserIoSession.getInstance().getMapIoSession(room.getLaunchUserId());
				 if(null != callUserIoSession && !callUserIoSession.isClosing()) {
					 //推送 提示信息
					 callUserIoSession.write(JSONObject.fromObject(new HashMap<String,Object>(){{
						 put("mid", Mid.sendVideoTipsMsg);
						 put("msgContent", tipsList.get(0).get("t_video_hint"));
					 }}).toString());
				 }
				 //被链接人 session
				 IoSession coverUserIoSession = UserIoSession.getInstance().getMapIoSession(room.getCoverLinkUserId());
				 if(null != coverUserIoSession && !coverUserIoSession.isClosing()) {
					 coverUserIoSession.write(JSONObject.fromObject(new HashMap<String,Object>(){{
						 put("mid", Mid.sendVideoTipsMsg);
						 put("msgContent", tipsList.get(0).get("t_video_hint"));
					 }}).toString());
				 }
			 }
			 
			/******************************/

			mu = new MessageUtil(1, "开始计时");

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("计时异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 用户挂断链接(non-Javadoc)
	 * 
	 * @see com.yiliao.service.VideoChatService#breakLink(int, int)
	 */
	@Override
	public MessageUtil breakLink(int roomId, int type) {
		try {
			
			logger.info("roomId ->{},开始结算.,挂断类型->{},1.用户主动挂断链接,"
					+ "2.用户断开链接或者发起呼叫断开链接,3.用户违规,4.socket断开链接,5.计费时间不足挂断链接,6.发起新的计时(上次挂断失败.)",roomId,type);
			/*获取房间信息*/
 
			Room room = RoomTimer.useRooms.get(roomId);
			
			if((null != room && 0 != room.getLaunchUserId() && 0 != room.getCoverLinkUserId()) || (null != VideoTiming.getRoom(roomId))){
			    logger.info("---{}房间进入了结算代码块--",roomId);
				if(null == room) {
					room = VideoTiming.getRoom(roomId);
				}
				
				// 给链接用户推送挂断信息
				IoSession ioSession = UserIoSession.getInstance().getMapIoSession(room.getLaunchUserId());
				if (null != ioSession) {
					ioSession.write(JSONObject.fromObject(new com.yiliao.domain.Mid(Mid.brokenLineRes)).toString());
				}

				// 给被链接人推送挂断信息
				IoSession coverIoSession = UserIoSession.getInstance().getMapIoSession(room.getCoverLinkUserId());
				if (null != coverIoSession) {
					coverIoSession.write(JSONObject.fromObject(new com.yiliao.domain.Mid(Mid.brokenLineRes)).toString());
				}

				Map<String, Integer> map = VideoTiming.timingUser.get(room.getLaunchUserId());

				logger.info("获取到的计费信息-{}", map);
				// 存在计时信息
				if (null != map && !map.isEmpty()) {

					// 清理要挂断信息的key
					if (!VideoTiming.arr.isEmpty() && VideoTiming.arr.indexOf(room.getLaunchUserId()) >= 0)
						VideoTiming.arr.remove(VideoTiming.arr.indexOf(room.getLaunchUserId()));

					// 删除正在计时的用户数据
					VideoTiming.timingUser.remove(room.getLaunchUserId());
					
					//计算本次链接链接分钟数
					int time = map.get("timing")%60==0?map.get("timing")/60:map.get("timing")/60+1;
					
					//计算本次链接一共消耗了多少金币
					int gold = map.get("deplete")*time;
					
					logger.info("[{}]房间挂断链接,当前计时分钟数->{},消耗金币->{}",roomId,time,gold);
					//如果当前消费金币数大于了用户的余额
					//那么减少1分钟的计费
					if(gold > map.get("gold")) {
						gold = (map.get("timing")/60) * map.get("deplete");
					}
					
					//保存消费记录
					int orderId = this.saveOrder(room.getLaunchUserId(), room.getCoverLinkUserId(), 0, WalletDetail.CHANGE_CATEGORY_VIDEO, new BigDecimal(gold),roomId,time);
					//扣除用户金币 
					if(goldComputeService.userConsume(room.getLaunchUserId(), WalletDetail.CHANGE_CATEGORY_VIDEO, new BigDecimal(gold),orderId)){

						// 分配用户的消费的金币
						goldComputeService.distribution(new BigDecimal(gold), room.getLaunchUserId(),
								room.getCoverLinkUserId(), WalletDetail.CHANGE_CATEGORY_VIDEO, orderId);

					} else {
						 logger.info("--当前用户{}在房间号{}中和{}聊天{}秒扣费异常,其中用户余额{}，主播每分钟消耗{}--",room.getLaunchUserId(),roomId,room.getCoverLinkUserId(),
								 map.get("timing"),map.get("gold"),map.get("deplete"));
					}
					
					logger.info("当前房间信息-->{}",JSONObject.fromObject(room).toString());
					//把通话时间写入到通话记录中
					if(room.getCallUserId() == room.getLaunchUserId()) {
						updateCallLog(room.getLaunchUserId(), room.getCoverLinkUserId(),roomId, time);
					}else {
						updateCallLog(room.getCoverLinkUserId(),room.getLaunchUserId(),roomId , time);
					}

					receptionRate(room.getCoverLinkUserId(), 1);
				} else { // 接听率插入
					receptionRate(room.getCoverLinkUserId(), 2);
				}

				// 挂断链接
				StopThread st = new StopThread(room.getRoomId());
				st.start();

				// 修改用户为空闲状态
				String stateSql = "UPDATE t_anchor SET t_state = 0 WHERE  t_user_id = ?;";
				this.getFinalDao().getIEntitySQLDAO().executeSQL(stateSql, room.getCoverLinkUserId());

				mu = new MessageUtil(1, "已断开链接!");

			} else {
				// 根据房间号查询通话记录
				List<Map<String, Object>> sqlList = this
						.getQuerySqlList("SELECT * FROM t_room_time WHERE t_room_id = ?", roomId);
				if (sqlList.isEmpty()) {
					logger.info("---{}房间无法获取任何信息--", roomId);
					mu = new MessageUtil(1, "房间已销毁!");
				} else {
					// 根据用户编号得到主播信息
					String sql = "SELECT t_role FROM t_user WHERE t_id = ? ";
					Map<String, Object> map = this.getMap(sql, sqlList.get(0).get("t_answer_user_id"));

					BigDecimal _video_gold =new BigDecimal(map.get("t_an_vi_gold").toString());
					
					int userId = Integer.parseInt(map.get("t_call_user_id").toString());
					int anchorId = Integer.parseInt(map.get("t_answer_user_id").toString()); 

					// 开始时间
					long begin_time = DateUtils.parse(sqlList.get(0).get("t_create_time").toString(), DateUtils.FullDatePattern).getTime();
					// 得到时间差
					Long time_difference = (System.currentTimeMillis() - begin_time) / 1000;
					// 计算本次链接链接分钟数
					int time = time_difference.intValue() % 60 == 0 ? time_difference.intValue() / 60
							: time_difference.intValue() / 60 + 1;
					// 计算本次链接一共消耗了多少金币
					BigDecimal gold = _video_gold.multiply(new BigDecimal(time)).setScale(2, BigDecimal.ROUND_DOWN);

					// 给链接用户推送挂断信息
					IoSession ioSession = UserIoSession.getInstance()
							.getMapIoSession(Integer.parseInt(sqlList.get(0).get("t_answer_user_id").toString()));
					if (null != ioSession) {
						ioSession.write(JSONObject.fromObject(new com.yiliao.domain.Mid(Mid.brokenLineRes)).toString());
					}

					// 给被链接人推送挂断信息
					IoSession coverIoSession = UserIoSession.getInstance()
							.getMapIoSession(Integer.parseInt(sqlList.get(0).get("t_answer_user_id").toString()));
					if (null != coverIoSession) {
						coverIoSession.write(JSONObject.fromObject(new com.yiliao.domain.Mid(Mid.brokenLineRes)).toString());
					}

					logger.info("[{}]房间查询sql挂断链接,当前计时分钟数->{},消耗金币->{}", sqlList.get(0).get("t_room_id"), time, gold);
					//获取用户的真实余额	
					 sql = "SELECT u.t_id,u.t_sex,u.t_nickName,u.t_role,u.t_referee,b.t_recharge_money,b.t_profit_money,b.t_share_money FROM t_user u LEFT JOIN t_balance b ON b.t_user_id = u.t_id WHERE u.t_id = ?";

					List<Map<String, Object>> data = getQuerySqlList(sql, userId);
					
					BigDecimal totalGold = new BigDecimal(data.get(0).get("t_recharge_money")
							.toString()).add(
							new BigDecimal(data.get(0).get("t_profit_money").toString())).add(
							new BigDecimal(data.get(0).get("t_share_money").toString()));
					
					if(totalGold.compareTo(gold) < 0) {
						gold = _video_gold.multiply(new BigDecimal(time_difference.intValue()/60)).setScale(2, BigDecimal.ROUND_DOWN);
						//如果用户的金币还是小于聊天分钟数
						//那么扣除用户的所有金币
						if(totalGold.compareTo(gold) < 0) {
							gold = totalGold;
						}
					}
					
					// 保存消费记录
					int orderId = this.saveOrder(userId, anchorId, 0,WalletDetail.CHANGE_CATEGORY_VIDEO, gold, roomId, time);
					
					// 扣除用户金币
					goldComputeService.userConsume(userId, WalletDetail.CHANGE_CATEGORY_VIDEO, gold,orderId);
					// 分配用户的消费的金币
					goldComputeService.distribution(gold, userId, anchorId,WalletDetail.CHANGE_CATEGORY_VIDEO, orderId);

					// 把通话时间写入到通话记录中
					if (userId == Integer.parseInt(sqlList.get(0).get("t_call_user_id").toString())) {
						updateCallLog(userId, anchorId, roomId, time);
					} else {
						updateCallLog(anchorId, userId, roomId, time);
					}

					receptionRate(anchorId, 1);
					// 修改用户为空闲状态
					String stateSql = "UPDATE t_anchor SET t_state = 0 WHERE  t_user_id = ?;";
					this.getFinalDao().getIEntitySQLDAO().executeSQL(stateSql, anchorId);
				}

				// 挂断链接
				StopThread st = new StopThread(roomId);
				st.start();
				mu = new MessageUtil(1, "已断开链接!");
			}
			// 根据房间好删除数据
			this.executeSQL("DELETE FROM t_room_time WHERE t_room_id = ?", roomId);
			// 在正在使用的房间列表中删除数据
			RoomTimer.useRooms.remove(roomId);
			// 房间加入到空闲房间池中
			RoomTimer.freeRooms.add(new Room(roomId));

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("挂断链接异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/**
	 * 获取订单Id
	 * 
	 * @return
	 */
	public int getOrderId() {
		List<Map<String, Object>> arr = this.getFinalDao().getIEntitySQLDAO()
				.findBySQLTOMap("SELECT t_id FROM t_order ORDER BY t_id DESC LIMIT 1;");
		return arr.isEmpty() ? 1 : (Integer) arr.get(0).get("t_id") + 1;
	}

	/**
	 * 用户编号
	 * 
	 * @param userId
	 * @param type   1.接通2.未接听
	 */
	private void receptionRate(int userId, int type) {

		// 查询该主播是否存在数据
		String querySql = "SELECT * FROM t_reception_rate WHERE t_user_id = ?";
		List<Map<String, Object>> dataList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(querySql, userId);
		// 新增数据
		if (null == dataList || dataList.isEmpty()) {

			String inSql = "INSERT t_reception_rate (t_user_id, t_count, t_reception_count, t_refuse_count, t_reception) VALUES (?, ?, ?, ?, ?);";

			this.getFinalDao().getIEntitySQLDAO().executeSQL(inSql, userId, 1, type == 1 ? 1 : 0, type == 1 ? 0 : 1,
					type == 1 ? 100 : 0);
		} else {

			int count = Integer.parseInt(dataList.get(0).get("t_count").toString()) + 1;

			int t_reception_count = Integer.parseInt(dataList.get(0).get("t_reception_count").toString())
					+ (type == 1 ? 1 : 0);

			int t_refuse_count = Integer.parseInt(dataList.get(0).get("t_refuse_count").toString())
					+ (type == 1 ? 0 : 1);

			// 修改数据
			String upSql = "UPDATE t_reception_rate SET t_count=?, t_reception_count=?, t_refuse_count=?, t_reception=? WHERE t_id=?;";

			this.executeSQL(upSql, count, t_reception_count, t_refuse_count,
					calculationPercent(count, t_reception_count),
					Integer.parseInt(dataList.get(0).get("t_id").toString()));
		}

	}

	/**
	 * 计算百分比
	 * 
	 * @param total
	 * @param number
	 * @return
	 */
	public String calculationPercent(int total, int number) {

		NumberFormat numberFormat = NumberFormat.getInstance();

		// 设置精确到小数点后2位
		numberFormat.setMaximumFractionDigits(2);

		return numberFormat.format((float) number / (float) total * 100);
	}

	/**
	 * 存储订单记录
	 * 
	 * @param consume       消费者
	 * @param cover_consume 被消费者
	 * @param consume_score 消费资源数据编号
	 * @param consume_type  消费类型
	 * @param amount        消费金额
	 */
	private int saveOrder(int consume, int cover_consume, int consume_score, int consume_type, BigDecimal amount,
			int roomId, int logTime) {

		String sql = "INSERT INTO t_order (t_consume, t_cover_consume, t_consume_type, t_consume_score, t_amount, t_create_time,t_room_id,t_log_time) VALUES (?, ?, ?, ?, ?, ?,?,?)";

		return this.getFinalDao().getIEntitySQLDAO().saveData(sql, consume, cover_consume, consume_type, consume_score,
				amount, DateUtils.format(new Date(), DateUtils.FullDatePattern), roomId, logTime);
	}

	/**
	 * 用户调用挂断
	 */
	@Override
	public MessageUtil userHangupLink(int userId) {
		try {

			List<Integer> rooms = RoomTimer.getUserIdReturnRoomId(userId);
			
			if(!rooms.isEmpty()) {
				rooms.forEach(s ->{
					mu = this.breakLink(s,2);
				});
			}
 
			rooms = VideoTiming.getByUserResRoom(userId);
			if (!rooms.isEmpty()) {
				rooms.forEach(s -> {
					// 挂断链接
					mu = this.breakLink(s, 2);
				});
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("用户挂断链接异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/**
	 * 主播对用户发起聊天
	 */
	@Override
	public MessageUtil anchorLaunchVideoChat(int anchorUserId, int userId, int roomId) {
		try {
			    //清理主播所在的所有房间 且把房间返回房间池
			    userHangupLink(anchorUserId);
			    
			    if(anchorUserId  == userId){
					return new MessageUtil(-7, "不能和自己进行视频聊天.");
				}
				
				IoSession ioSession = UserIoSession.getInstance().getMapIoSession(userId);
				
				//获取被链接人是否正在连线中
				if(RoomTimer.getUserIsBusy(userId) || VideoTiming.getUserExist(userId)){
					//写入到通话记录中
					saveCallLog(anchorUserId, userId,roomId);
					
					return new MessageUtil(-2, "你拨打的用户正忙,请稍后在拨.");
				}
				//判断对方是否设置了勿扰
				String sql = "SELECT * FROM t_user WHERE t_id = ? AND t_is_not_disturb = 1";
				List<Map<String, Object>> users = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, userId);
				
				if(null!=users && !users.isEmpty()){
					return new MessageUtil(-3, "Sorry,对方设置了勿扰.");
				}
				//判断2个连线人是否是同性
				if(sameSex(userId, anchorUserId)) {
					return new MessageUtil(-5, "同性别无法进行聊天!");
				}
				
				//获取被链接人的余额是否足够进行视频聊天
				//获取用户的所有金币
				String goldSql = "SELECT SUM(t_recharge_money+t_profit_money+t_share_money) AS totalGold FROM t_balance WHERE t_user_id = ?";
				
				Map<String, Object> totalGold = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(goldSql, userId);
				
				//获取被链接人每分钟需要消耗多少金币
				String videoGoldSql = "SELECT t_video_gold FROM t_anchor_setup WHERE t_user_id = ?";
				
				List<Map<String,Object>> sqlList = this.getQuerySqlList(videoGoldSql, anchorUserId);
				
				if(sqlList.isEmpty()) {
					return new MessageUtil(-6, "您的资料为完善!无法进行呼叫.");
				}
				
				//把链接人
				//记录用户的链接信息
				Room room = new Room(roomId);
				
				room.setLaunchUserId(userId);
				room.setCoverLinkUserId(anchorUserId);
				room.setCallUserId(anchorUserId);
				
				//写入到通话记录中   
				saveCallLog(anchorUserId, userId,roomId);

				room.setLaunchUserLiveCode(SystemConfig.getValue("play_addr")+room.getLaunchUserId()+"/"+roomId);
				room.setCoverLinkUserLiveCode(SystemConfig.getValue("play_addr")+room.getCoverLinkUserId()+"/"+roomId);
				//默认足够支付聊天
				int satisfy = 1;
				//判断用户的金币是否满足聊天计时
				if(null == totalGold.get("totalGold") || new BigDecimal(totalGold.get("totalGold").toString()).compareTo(new BigDecimal(sqlList.get(0).get("t_video_gold").toString())) < 0){
					satisfy = -1 ;
				}
				
				RoomTimer.useRooms.put(roomId, room);
				//
				OnLineRes or = new OnLineRes();
				or.setMid(Mid.anchorLinkUserRes);
				or.setRoomId(roomId);
				or.setConnectUserId(anchorUserId);
				or.setSatisfy(satisfy);
				
				logger.info("当前链接人->{},当前被链接人->{},当前房间号->{},对方session->{}",anchorUserId,userId,roomId,ioSession);
				
				/** 激光推送 **/
				//获取链接人的昵称
				Map<String, Object> userMap = this.getMap("SELECT t_nickName FROM t_user WHERE t_id = ?", anchorUserId);
				
				this.applicationContext.publishEvent(new PushMesgEvnet(
						new MessageEntity(userId, userMap.get("t_nickName")+"邀请您进行视频聊天!", 0, new Date(),6,roomId,anchorUserId,satisfy)));
				/** 激光推送 **/
				if(ioSession != null) {
					ioSession.write(JSONObject.fromObject(or).toString());
				}

			mu = new MessageUtil(1, "正在链接.");

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("主播对用户发起聊天异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/**
	 * 新增通话记录
	 * 
	 * @param userId
	 * @param coverUserId
	 */
	private void saveCallLog(int userId, int coverUserId, int roomId) {
		// 当对方未接听 数据写入到通话记录中
		this.executeSQL(
				"INSERT INTO t_call_log (t_callout_user, t_answer_user,t_room_id, t_create_time) VALUES (?,?,?,?);",
				userId, coverUserId, roomId, DateUtils.format(new Date(), DateUtils.FullDatePattern));
	}

	/**
	 * 修改通话记录
	 * 
	 * @param userId
	 * @param coverUserId
	 * @param time
	 */

	private void updateCallLog(int userId,int coverUserId,int roomId,int time) {
	
		logger.info("userId->{},coverUserId->{},roomId->{}",userId,coverUserId,roomId);
		
		List<Map<String,Object>> sqlList = getQuerySqlList("SELECT t_id FROM t_call_log WHERE t_callout_user = ? AND t_answer_user = ? AND t_room_id = ?  ;",
				userId,coverUserId,roomId);
		
		if(!sqlList.isEmpty()) {
			this.executeSQL("UPDATE t_call_log SET t_call_time = ? WHERE t_id = ? ", time , sqlList.get(0).get("t_id"));
		}else {
			logger.info("{}连线{}主播在{}房间聊天,没有聊天记录.",userId,coverUserId,roomId);
		}

	}

	@Override
	public MessageUtil getUuserCoverCall(int userId) {
		try {

			// 房间号集合
			List<Integer> rooms = RoomTimer.getUserIdReturnRoomId(userId);

			if (!rooms.isEmpty()) {
				Room room = RoomTimer.useRooms.get(rooms.get(0));

				// 获取用户角色
				Map<String, Object> map = this.getMap("SELECT t_role FROM t_user WHERE  t_id = ? ", userId);

				int satisfy = 1;

				if (Integer.parseInt(map.get("t_role").toString()) == 0) {
					// 获取被链接人的余额是否足够进行视频聊天
					// 获取用户的所有金币
					String goldSql = "SELECT SUM(t_recharge_money+t_profit_money+t_share_money) AS totalGold FROM t_balance WHERE t_user_id = ?";

					Map<String, Object> totalGold = this.getFinalDao().getIEntitySQLDAO()
							.findBySQLUniqueResultToMap(goldSql, userId);

					// 获取被链接人每分钟需要消耗多少金币
					String videoGoldSql = "SELECT t_video_gold FROM t_anchor_setup WHERE t_user_id = ?";

					Map<String, Object> videoGold = this.getFinalDao().getIEntitySQLDAO()
							.findBySQLUniqueResultToMap(videoGoldSql, room.getCoverLinkUserId());
					// 判断用户的金币是否满足聊天计时
					if (null == totalGold.get("totalGold") || new BigDecimal(totalGold.get("totalGold").toString())
							.compareTo(new BigDecimal(videoGold.get("t_video_gold").toString())) < 0) {
						satisfy = -1;
					}
				}

				Map<String, Object> rmap = new HashMap<String, Object>();
				rmap.put("connectUserId", room.getLaunchUserId());
				rmap.put("coverLinkUserId", room.getCoverLinkUserId());
				rmap.put("roomId", room.getRoomId());
				rmap.put("satisfy", satisfy);

				return new MessageUtil(1, rmap);

			}
			return new MessageUtil(1, new HashMap<>());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取是否被呼叫异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}
}
