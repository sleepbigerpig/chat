package com.yiliao.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.mina.core.session.IoSession;
import org.springframework.stereotype.Service;

import com.yiliao.domain.Room;
import com.yiliao.service.WebSocketService;
import com.yiliao.timer.RoomTimer;
import com.yiliao.webSocket.WebSocketIoHandler;

/**
 * 客户端监控获取的数据
 * 
 * @author Administrator
 *
 */
@Service("webSocketService")
public class WebSocketServiceImpl extends ICommServiceImpl implements WebSocketService {

	private Map<Integer, IoSession> del = new HashMap<Integer, IoSession>();

	List<String> arr = new ArrayList<String>();

	String qSql = "SELECT t_nickName,t_phone FROM t_user WHERE t_id = ? ";

	@Override
	public void getUseRoomList() {
		// 如果监控服务器未登陆 将不在推送数据
		if (WebSocketIoHandler.sessionMap.isEmpty()) {
			return;
		}
		// 遍历正在使用的房间
		for (Room room : RoomTimer.useRooms.values()) {

			if (room.getLaunchUserId() != 0) {

				// 得到发起人 和 被链接人的昵称
				Map<String, Object> lanMap = this.getMap(qSql, room.getLaunchUserId());

				room.setLanuchName(getNickName(lanMap));

				if (room.getRoomId() < 100000) {

					Map<String, Object> covMap = this.getMap(qSql, room.getCoverLinkUserId());

					room.setCoverName(getNickName(covMap));
				}

				IoSession session = getIosession();

				JSONObject fromObject = JSONObject.fromObject(room);
				fromObject.put("mid", 10003);

				del.put(room.getRoomId(), session);
				// 发送到前端服务器
				WebSocketIoHandler.sendMessage(session, JSONArray.fromObject(fromObject).toString());
			}
		}
	}

	/** 返回用户昵称 */
	private String getNickName(Map<String, Object> map) {
		if (null == map.get("t_nickName")) {
			return "聊友:" + map.get("t_phone").toString().substring(map.get("t_phone").toString().length() - 4);
		} else {
			return map.get("t_nickName").toString();
		}
	}

	/**
	 * 产生新的房间推送
	 */
	@Override
	public void singleRoomSend(Room room) {
		try {
			// 如果监控服务器未登陆 将不在推送数据
			if (WebSocketIoHandler.sessionMap.isEmpty()) {
				return;
			}
			Thread.sleep(1000);
			// 得到发起人 和 被链接人的昵称
//			logger.info("当前发起人-->{}",room.getLaunchUserId());
			if (null != room && room.getLaunchUserId() != 0) {

				Map<String, Object> lanMap = this.getMap(qSql, room.getLaunchUserId());

				room.setLanuchName(getNickName(lanMap));

				if (room.getRoomId() < 100000) {
					Map<String, Object> covMap = this.getMap(qSql, room.getCoverLinkUserId());
					room.setCoverName(getNickName(covMap));
				}

				IoSession session = getIosession();

				del.put(room.getRoomId(), session);

				JSONObject fromObject = JSONObject.fromObject(room);

				fromObject.put("mid", 10003);

				logger.info("已发送到监控服务器->{}", fromObject.toString());
				// 发送到前端服务器
				WebSocketIoHandler.sendMessage(session, JSONArray.fromObject(fromObject).toString());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 挂断房间推送
	 */
	@Override
	public void stopRoomSend(int roomId) {
		try {
			// 如果监控服务器未登陆 将不在推送数据
			if (WebSocketIoHandler.sessionMap.isEmpty()) {
				return;
			}
			// 根据房间好取得session
			IoSession session = del.get(roomId);

			if (null != session) {
				JSONObject fromObject = new JSONObject();
				fromObject.put("roomId", roomId);
				fromObject.put("mid", 10004);

				logger.info("{}房间关闭,推送给监控平台!", roomId);
				// 发送到前端服务器
				WebSocketIoHandler.sendMessage(session, JSONArray.fromObject(fromObject).toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * 获取Map中的 未发送的地址
	 * 
	 * @return
	 */
	public IoSession getIosession() {

		for (Map.Entry<String, IoSession> m : WebSocketIoHandler.sessionMap.entrySet()) {
			if (!arr.contains(m.getKey())) {
				logger.info("获取的当前登陆[{}]", m.getKey());
				arr.add(m.getKey());
				return m.getValue();
			}
		}
		// 如果以上设置都未返回 那么下面进行初始化操作

		for (Map.Entry<String, IoSession> m : WebSocketIoHandler.sessionMap.entrySet()) {
			arr.clear();
			logger.info("获取的当前登陆[{}]", m.getKey());
			arr.add(m.getKey());
			return m.getValue();
		}
		return null;
	}

}
