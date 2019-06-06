package com.yiliao.service.impl.version2_0;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.mina.core.session.IoSession;
import org.springframework.stereotype.Service;

import com.yiliao.domain.Room;
import com.yiliao.domain.UserIoSession;
import com.yiliao.domain.WalletDetail;
import com.yiliao.evnet.PushBigUserCount;
import com.yiliao.evnet.PushLinkUser;
import com.yiliao.service.ProhibitService;
import com.yiliao.service.impl.ICommServiceImpl;
import com.yiliao.service.version2_0.BigRoomService;
import com.yiliao.timer.RoomTimer;
import com.yiliao.util.MessageUtil;
import com.yiliao.util.Mid;
import com.yiliao.util.SpringConfig;
import com.yiliao.util.StopThread;
import com.yiliao.util.SystemConfig;

import net.sf.json.JSONObject;

@Service(value = "bigRoomService")
public class BigRoomServiceImpl extends ICommServiceImpl implements BigRoomService {

	@SuppressWarnings("serial")
	@Override
	public MessageUtil getBigRoomList(int page) {
		try {
			// 获取大房间所有主播
			Map<String, Object> map = this.getMap("SELECT COUNT(t_id) AS total FROM t_big_room_man");

			StringBuffer body = new StringBuffer();
			body.append("SELECT u.t_cover_img,u.t_nickName,br.t_user_id,br.t_is_debut,br.t_room_id,br.t_chat_room_id ");
			body.append("FROM t_big_room_man br LEFT JOIN t_user u ON br.t_user_id = u.t_id ");
			body.append("WHERE br.t_room_id IS NOT NULL ");
			body.append("ORDER BY br.t_is_debut DESC,br.t_sort ASC ");
			body.append("LIMIT ?,10");
			// 分页获取大房间主播
			List<Map<String, Object>> sqlList = this.getQuerySqlList(body.toString(), (page - 1) * 10);

			sqlList.forEach(s ->{
				
				Map<String, Object> viewerCount = this.getMap("SELECT COUNT(t_id) AS total FROM t_big_room_viewer WHERE t_big_room_id  = ? ", s.get("t_room_id"));
			    s.put("viewerCount", viewerCount.get("total"));
			});
			
			int pageCount = Integer.parseInt(map.get("total").toString()) % 10 == 0
					? Integer.parseInt(map.get("total").toString()) / 10
					: Integer.parseInt(map.get("total").toString()) / 10 + 1;

			return new MessageUtil(1, new HashMap<String, Object>() {
				{
					put("pageCount", pageCount);
					put("data", sqlList);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			return new MessageUtil(0, "程序异常!");
		}
	}

	/**
	 * 开启直播
	 */
	@Override
	public MessageUtil openLiveTelecast(int userId) {
		try {

			// 判定该用户是否拥有直播权限
			List<Map<String, Object>> anchors = this.getQuerySqlList("SELECT * FROM t_big_room_man WHERE t_user_id = ?",
					userId);
			//
			if (null == anchors || anchors.isEmpty()) {
				return new MessageUtil(-1, "您暂无大房间直播权限!请联系客服.");
			}
			
			//获取用户是否是主播
			List<Map<String, Object>> userRole = this.getQuerySqlList("SELECT * FROM t_user WHERE t_id = ? AND t_role = 1", userId);
			
			if(null == userRole || userRole.isEmpty()) {
				return new MessageUtil(-1, "您暂无大房间直播权限!请联系客服.");
			}

			// 生产房间号
			int roomId = (userId + 10000) * 100;

			this.executeSQL("UPDATE t_big_room_man set t_is_debut = 1 ,t_room_id = ? WHERE t_user_id = ? ", roomId,
					userId);

			// 是否对房间中的用户推送拉去流的信息

			Room room = new Room(roomId);
			room.setLaunchUserId(userId);
			room.setLaunchUserLiveCode(SystemConfig.getValue("play_addr") + userId + "/" + roomId);
			// 暂定

			/*********** 监控程序 ***********/
			if (null != room) {

				RoomTimer.useRooms.put(roomId, room);
				// 异步通知
				this.applicationContext.publishEvent(new PushLinkUser(room));
			}
			/***************************/

			return new MessageUtil(1, new HashMap<String, Object>() {
				{
					put("roomId", roomId);
					put("chatRoomId", anchors.get(0).get("t_chat_room_id"));
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			return new MessageUtil(0, "程序异常!");
		}
	}

	/**
	 * 关闭直播
	 */
	@Override
	public MessageUtil closeLiveTelecast(int userId, int type) {
		try {

			this.executeSQL("UPDATE t_big_room_man set t_is_debut = 0 WHERE t_user_id = ? ", userId);

			// 删除房间信息
			RoomTimer.useRooms.remove((userId + 10000) * 100);

			if (type == 1) {
				new StopThread((userId + 10000) * 100).start();
			} else {
				ProhibitService prohibitService = (ProhibitService) SpringConfig.getInstance()
						.getBean("prohibitService");
				prohibitService.handleIllegalityUser(userId, "监控后台封号.");
			}

			return new MessageUtil(1, "直播以关闭.");

		} catch (Exception e) {
			e.printStackTrace();
			return new MessageUtil(0, "程序异常!");
		}
	}

	/**
	 * 用户加入房间
	 */
	@Override
	public MessageUtil userMixBigRoom(int userId, int anchorId) {
		try {
			// 获取当前用户是否在大房间中
			List<Map<String, Object>> sqlList = this.getQuerySqlList("SELECT * FROM t_big_room_viewer WHERE t_user_id = ? ", userId);

			if (null != sqlList && !sqlList.isEmpty()) {
				// 删除原有的房间
				this.executeSQL("DELETE FROM t_big_room_viewer WHERE t_user_id = ?; ", userId);
			}

			// 获取主播的房间信息
			List<Map<String, Object>> bigRoom = this.getQuerySqlList(
					"SELECT * FROM t_big_room_man WHERE t_user_id = ? AND t_room_id > 0 AND t_is_debut = 1 ;",
					anchorId);

			// 获取当前主播的头像昵称关注数
			Map<String, Object> map = this.getMap(
					"SELECT u.t_handImg,u.t_nickName,COUNT(f.t_id) AS followNumber FROM t_user u LEFT JOIN t_follow f ON f.t_cover_follow = u.t_id WHERE u.t_id = ?",
					anchorId);
			// 获取当前用户是否关注
			List<Map<String, Object>> isFollow = this.getQuerySqlList(
					"SELECT * FROM t_follow WHERE t_cover_follow = ? AND t_follow_id = ?;", anchorId, userId);

			if (null == isFollow || isFollow.isEmpty()) {
				map.put("isFollow", 0);
			} else {
				map.put("isFollow", 1);
			}

			//设置主播是否开播
			map.put("isDebut", bigRoom.isEmpty()?0:1);
			
			// 如果用户和主播ID 不形同 那么加入到房间中
			if (userId != anchorId) {
				// 把用户写入到数据库中
				this.executeSQL("INSERT INTO t_big_room_viewer (t_big_room_id, t_user_id) VALUES ( ?, ?);",	(anchorId+10000)*100, userId);
			}

			// 获取消费最高的5个用户
			StringBuffer body = new StringBuffer();
			body.append("SELECT u.t_id,u.t_handImg,SUM(o.t_amount) AS total ");
			body.append("FROM t_order o LEFT JOIN t_user u ON u.t_id = o.t_consume ");
			body.append("WHERE o.t_cover_consume = ? AND t_consume_type IN (");
			body.append(WalletDetail.CHANGE_CATEGORY_TEXT).append(",");
			body.append(WalletDetail.CHANGE_CATEGORY_VIDEO).append(",");
			body.append(WalletDetail.CHANGE_CATEGORY_PRIVATE_PHOTO).append(",");
			body.append(WalletDetail.CHANGE_CATEGORY_PRIVATE_VIDEO).append(",");
			body.append(WalletDetail.CHANGE_CATEGORY_PHONE).append(",");
			body.append(WalletDetail.CHANGE_CATEGORY_WEIXIN).append(",");
			body.append(WalletDetail.CHANGE_CATEGORY_RED_PACKET).append(",");
			body.append(WalletDetail.CHANGE_CATEGOR_GIFT).append(",");
			body.append(WalletDetail.CHANGE_CATEGOR_DYNAMIC_PHOTO).append(",");
			body.append(WalletDetail.CHANGE_CATEGOR_DYNAMIC_VIDEO);
			body.append(") ");
			body.append("GROUP BY o.t_consume");

			// 分页获取数据
			List<Map<String, Object>> dataList = this
					.getQuerySqlList("SELECT * FROM (" + body + ") aa ORDER BY aa.total DESC LIMIT 5;", anchorId);
			map.put("devoteList", dataList);

			// 获取当前主播有多少人在房间中
			Map<String, Object> viewerM = this.getMap("SELECT COUNT(t_id) AS viewer FROM t_big_room_viewer WHERE t_big_room_id = ?",
					(userId+10000)*100);

			map.put("viewer", viewerM.get("viewer"));

			/********* 推送视频提示语 ***********/
			// 链接人
			// 获取提示信息
			List<Map<String, Object>> tipsList = this.getQuerySqlList("SELECT t_video_hint FROM t_system_setup ");
			// 如果后台填写了 提示信息
			if (!tipsList.isEmpty()) {
				// 链接人session
				map.put("warning", tipsList.get(0).get("t_video_hint"));
			}

			/******************************/

			// 获取加入房间的用户昵称
			Map<String, Object> userN = this.getMap("SELECT t_nickName FROM t_user WHERE t_id = ? ", userId);
				// 异步推送
			this.applicationContext.publishEvent(new PushBigUserCount(new HashMap<String, Object>() {
				{
					put("roomId", (anchorId+10000)*100);
					put("userName", userN.get("t_nickName"));
				}
			}));

			return new MessageUtil(1, map);

		} catch (Exception e) {
			e.printStackTrace();
			return new MessageUtil(0, "程序异常!");
		}
	}

	@Override
	public MessageUtil userQuitBigRoom(int userId) {
		try {
			// 删除原有的房间
			this.executeSQL("DELETE FROM t_big_room_viewer WHERE t_user_id = ?; ", userId);

			return new MessageUtil(1, "已退出!");
		} catch (Exception e) {
			e.printStackTrace();
			return new MessageUtil(0, "程序异常!");
		}
	}

	/**
	 * 获取贡献值排行榜
	 */
	@Override
	public MessageUtil getContributionList(int userId, int page) {
		try {

			StringBuffer body = new StringBuffer();
			body.append(
					"SELECT u.t_id,u.t_nickName,u.t_handImg,u.t_is_vip,SUM(o.t_amount) AS total,SUM(b.t_profit_money+b.t_recharge_money+b.t_share_money) AS balance ");
			body.append("FROM t_order o LEFT JOIN t_user u ON u.t_id = o.t_consume ");
			body.append("LEFT JOIN t_balance b ON b.t_user_id = u.t_id ");
			body.append("WHERE o.t_cover_consume = ? AND t_consume_type IN (");
			body.append(WalletDetail.CHANGE_CATEGORY_TEXT).append(",");
			body.append(WalletDetail.CHANGE_CATEGORY_VIDEO).append(",");
			body.append(WalletDetail.CHANGE_CATEGORY_PRIVATE_PHOTO).append(",");
			body.append(WalletDetail.CHANGE_CATEGORY_PRIVATE_VIDEO).append(",");
			body.append(WalletDetail.CHANGE_CATEGORY_PHONE).append(",");
			body.append(WalletDetail.CHANGE_CATEGORY_WEIXIN).append(",");
			body.append(WalletDetail.CHANGE_CATEGORY_RED_PACKET).append(",");
			body.append(WalletDetail.CHANGE_CATEGOR_GIFT).append(",");
			body.append(WalletDetail.CHANGE_CATEGOR_DYNAMIC_PHOTO).append(",");
			body.append(WalletDetail.CHANGE_CATEGOR_DYNAMIC_VIDEO);
			body.append(") ");
			body.append("GROUP BY o.t_consume");
			// 获取总记录数
			Map<String, Object> total = this.getMap("SELECT COUNT(*) as total FROM (" + body + ") aa", userId);

			// 分页获取数据
			List<Map<String, Object>> dataList = this.getQuerySqlList(
					"SELECT * FROM (" + body + ") aa ORDER BY aa.total DESC LIMIT ?,10;", userId, (page - 1) * 10);

			// 获取充值金币 用于分配用户的充值级别
			String qSql = "SELECT SUM(r.t_recharge_money) AS money FROM t_recharge  r WHERE r.t_user_id = ? AND r.t_order_state = 1";

			for (Map<String, Object> m : dataList) {
				// 金币档
				m.put("goldfiles", this.goldFiles(new BigDecimal(m.get("balance").toString()).intValue()));

				m.remove("t_create_time");
				m.remove("balance");

				List<Map<String, Object>> regList = this.getQuerySqlList(qSql, m.get("t_id"));
				// 充值级别
				if (null == regList || regList.isEmpty() || null == regList.get(0).get("money")) {
					m.put("grade", this.grade(0));
				} else {
					m.put("grade", this.grade(new BigDecimal(regList.get(0).get("money").toString()).intValue()));
				}

			}
			return new MessageUtil(1, new HashMap<String, Object>() {
				{
					put("pageCount",
							Integer.parseInt(total.get("total").toString()) % 10 == 0
									? Integer.parseInt(total.get("total").toString()) / 10
									: Integer.parseInt(total.get("total").toString()) / 10 + 1);
					put("data", dataList);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			return new MessageUtil(0, "程序异常!");
		}
	}

	public int goldFiles(int gold) {
		if (gold <= 500) {
			return 1;
		} else if (501 <= gold && gold <= 1000) {
			return 2;
		} else if (1001 <= gold && gold <= 2000) {
			return 3;
		} else if (2001 <= gold && gold <= 3000) {
			return 4;
		} else {
			return 5;
		}
	}

	/** 充值档 */
	public int grade(int money) {

		if (money <= 1000) {
			return 1;
		} else if (money > 1000 && money <= 10000) {
			return 2;
		} else if (money >= 10001) {
			return 3;
		}
		return 0;
	}

	/**
	 * 获取用户信息
	 */
	@Override
	public MessageUtil getUserIndexData(int userId) {
		try {

			// 获取当前用户编号是用户还是主播
			Map<String, Object> map = this.getMap(
					"SELECT t_idcard,t_nickName,t_age,t_sex,t_handImg,t_vocation,t_role,t_autograph,t_city  FROM t_user WHERE t_id = ? ",
					userId);
			// 用户
			if ("0".equals(map.get("t_role").toString())) {
				Map<String, Object> myFollowCount = this
						.getMap("SELECT COUNT(t_id) AS total FROM t_follow WHERE t_follow_id = ?", userId);
				map.put("followCount", myFollowCount.getOrDefault("total", 0));
				// 获取我赠送的
				StringBuffer body = new StringBuffer();
				body.append("SELECT SUM(t_amount) AS totalAmount FROM t_order WHERE t_consume = ? ");
				body.append("AND t_consume_type IN (");
				body.append(WalletDetail.CHANGE_CATEGORY_TEXT).append(",");
				body.append(WalletDetail.CHANGE_CATEGORY_VIDEO).append(",");
				body.append(WalletDetail.CHANGE_CATEGORY_PRIVATE_PHOTO).append(",");
				body.append(WalletDetail.CHANGE_CATEGORY_PRIVATE_VIDEO).append(",");
				body.append(WalletDetail.CHANGE_CATEGORY_PHONE).append(",");
				body.append(WalletDetail.CHANGE_CATEGORY_WEIXIN).append(",");
				body.append(WalletDetail.CHANGE_CATEGORY_RED_PACKET).append(",");
				body.append(WalletDetail.CHANGE_CATEGOR_GIFT).append(",");
				body.append(WalletDetail.CHANGE_CATEGOR_DYNAMIC_PHOTO).append(",");
				body.append(WalletDetail.CHANGE_CATEGOR_DYNAMIC_VIDEO);
				body.append(");");
				Map<String, Object> consumeMap = this.getMap(body.toString(), userId);

				map.put("totalMoney", consumeMap.getOrDefault("totalAmount", 0));

			} else { // 主播
				Map<String, Object> followCountMap = this
						.getMap("SELECT COUNT(t_id) AS total FROM t_follow WHERE t_cover_follow = ?", userId);
				map.put("followCount", followCountMap.getOrDefault("total", 0));
				// 获取我收到的

				StringBuffer body = new StringBuffer();
				body.append("SELECT SUM(t_amount) AS totalAmount FROM t_order WHERE t_cover_consume = ? ");
				body.append("AND t_consume_type IN (");
				body.append(WalletDetail.CHANGE_CATEGORY_TEXT).append(",");
				body.append(WalletDetail.CHANGE_CATEGORY_VIDEO).append(",");
				body.append(WalletDetail.CHANGE_CATEGORY_PRIVATE_PHOTO).append(",");
				body.append(WalletDetail.CHANGE_CATEGORY_PRIVATE_VIDEO).append(",");
				body.append(WalletDetail.CHANGE_CATEGORY_PHONE).append(",");
				body.append(WalletDetail.CHANGE_CATEGORY_WEIXIN).append(",");
				body.append(WalletDetail.CHANGE_CATEGORY_RED_PACKET).append(",");
				body.append(WalletDetail.CHANGE_CATEGOR_GIFT).append(",");
				body.append(WalletDetail.CHANGE_CATEGOR_DYNAMIC_PHOTO).append(",");
				body.append(WalletDetail.CHANGE_CATEGOR_DYNAMIC_VIDEO);
				body.append(");");
				Map<String, Object> profitMap = this.getMap(body.toString(), userId);

				map.put("totalMoney", profitMap.getOrDefault("totalAmount", 0));
			}

			return new MessageUtil(1, map);
		} catch (Exception e) {
			e.printStackTrace();
			return new MessageUtil(0, "程序异常!");
		}
	}

	/**
	 * 获取在线用户列表
	 */
	@Override
	public MessageUtil getRoomUserList(int userId, int page) {
		try {
			StringBuffer body = new StringBuffer();
			body.append(
					"SELECT u.t_id,u.t_nickName,u.t_handImg,u.t_is_vip,SUM(b.t_profit_money+b.t_recharge_money+b.t_share_money) AS balance ");
			body.append("FROM t_big_room_viewer rv LEFT JOIN t_big_room_man br ON rv.t_big_room_id = br.t_room_id ");
			body.append(
					"LEFT JOIN  t_user u ON rv.t_user_id = u.t_id LEFT JOIN t_balance b ON b.t_user_id = rv.t_user_id ");
			body.append("WHERE br.t_user_id = ? ");
			body.append("GROUP BY rv.t_user_id ");
			// 统计数量
			Map<String, Object> total = this.getMap("SELECT COUNT(*) AS total FROM (" + body + ") aa", userId);

			int pageCount = Integer.parseInt(total.get("total").toString()) % 10 == 0
					? Integer.parseInt(total.get("total").toString()) / 10
					: Integer.parseInt(total.get("total").toString()) / 10 + 1;

			// 获取列表
			List<Map<String, Object>> sqlList = this.getQuerySqlList(
					"SELECT * FROM (" + body + ") aa ORDER BY aa.balance DESC LIMIT ?,10", userId, (page - 1) * 10);

			// 获取充值金币 用于分配用户的充值级别
			String qSql = "SELECT SUM(r.t_recharge_money) AS money FROM t_recharge  r WHERE r.t_user_id = ? AND r.t_order_state = 1";

			for (Map<String, Object> m : sqlList) {
				// 金币档
				m.put("goldfiles", this.goldFiles(new BigDecimal(m.get("balance").toString()).intValue()));

				m.remove("t_create_time");
				m.remove("balance");

				List<Map<String, Object>> regList = this.getQuerySqlList(qSql, m.get("t_id"));
				// 充值级别
				if (null == regList || regList.isEmpty() || null == regList.get(0).get("money")) {
					m.put("grade", this.grade(0));
				} else {
					m.put("grade", this.grade(new BigDecimal(regList.get(0).get("money").toString()).intValue()));
				}

			}

			return new MessageUtil(1, new HashMap<String, Object>() {
				{
					put("pageCount", pageCount);
					put("data", sqlList);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
			return new MessageUtil(0, "程序异常!");
		}
	}

	/**
	 * 获取主播封面
	 */
	@Override
	public MessageUtil getUserCoverImg(int userId) {
		try {

			Map<String, Object> coverMap = this.getMap("SELECT t_cover_img,t_nickName FROM t_user WHERE t_id = ?",
					userId);

			if (null == coverMap.get("t_cover_img") || StringUtils.isBlank(coverMap.get("t_cover_img").toString())) {
				return new MessageUtil(-1, "暂未设置封面!");
			}

			return new MessageUtil(1, coverMap);

		} catch (Exception e) {
			e.printStackTrace();
			return new MessageUtil(0, "程序异常!");
		}
	}

	@Override
	public void sendBigUserCount(Map<String, Object> map) {
		try {

			// 查询出该房间共多少人
			Map<String, Object> mapUser = this.getMap("SELECT COUNT(t_id) totalCount FROM t_big_room_viewer WHERE t_big_room_id = ? ", map.get("roomId"));

			if (null != mapUser && 0 < Integer.parseInt(mapUser.get("totalCount").toString())) {

				// 给主播推送消息
				 List<Map<String, Object>> bigRooms = this.getQuerySqlList("SELECT t_user_id FROM t_big_room_man WHERE t_room_id = ? ", map.get("roomId"));
				if (null != bigRooms && !bigRooms.isEmpty()) {
					IoSession session = UserIoSession.getInstance()
							.getMapIoSession(Integer.parseInt(bigRooms.get(0).get("t_user_id").toString()));
					if (null != session) {
						session.write(JSONObject.fromObject(new HashMap<String, Object>() {
							{
								put("mid", Mid.sendBigUserCountMsg);
								put("userCount", mapUser.getOrDefault("totalCount", 0));
								put("sendUserName", map.get("userName"));
							}
						}).toString());
					}
				}

				// 计算总页数
				int pageCount = Integer.parseInt(mapUser.get("totalCount").toString()) % 5000 == 0
						? Integer.parseInt(mapUser.get("totalCount").toString()) / 5000
						: Integer.parseInt(mapUser.get("totalCount").toString()) / 5000 + 1;

				String qSql = " SELECT t_user_id FROM t_big_room_viewer WHERE t_big_room_id = ?  LIMIT ?,5000; ";

				for (int i = 1; i <= pageCount; i++) {
					// 得到数据
					this.getQuerySqlList(qSql, map.get("roomId"), (i - 1) * 5000).forEach(s -> {
						IoSession session = UserIoSession.getInstance()
								.getMapIoSession(Integer.parseInt(s.get("t_user_id").toString()));
						if (null != session) {
							session.write(JSONObject.fromObject(new HashMap<String, Object>() {
								{
									put("mid", Mid.sendBigUserCountMsg);
									put("userCount", mapUser.getOrDefault("totalCount", 0));
									put("sendUserName", map.get("userName"));
								}
							}).toString());
						}

					});
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public MessageUtil getTotalBigRoomList() {
		try {
			StringBuffer body = new StringBuffer();
			body.append("SELECT u.t_cover_img,u.t_nickName,br.t_user_id,br.t_is_debut,br.t_room_id,br.t_chat_room_id ");
			body.append("FROM t_big_room_man br LEFT JOIN t_user u ON br.t_user_id = u.t_id ");
			body.append("WHERE br.t_room_id IS NOT NULL AND br.t_is_debut = 1 ");
			body.append("ORDER BY br.t_sort ASC ");
			// 分页获取大房间主播
			List<Map<String, Object>> sqlList = this.getQuerySqlList(body.toString());

			sqlList.forEach(s ->{
				Map<String, Object> viewerCount = this.getMap("SELECT COUNT(t_id) AS total FROM t_big_room_viewer WHERE t_big_room_id  = ? ", s.get("t_room_id"));
			    s.put("viewerCount", viewerCount.get("total"));
			});
			
			return new MessageUtil(1, sqlList);
		} catch (Exception e) {
			e.printStackTrace();
			return new MessageUtil(0, "程序异常!");
		}
	}

}
