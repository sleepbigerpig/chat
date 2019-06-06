package com.yiliao.timer;

import java.util.Map;
import java.util.TimerTask;

import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiliao.webSocket.WebSocketIoHandler;

/**
 * 视频鉴黄获取结果
 * 
 * @author Administrator
 *
 */

public class WebSocketTimer {

	Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * 断开websocket的链接
	 */
	public void sotpIoSession() {
		try {

			logger.info("清理前集合中的iosession:{}",WebSocketIoHandler.sessionMap);

			WebSocketIoHandler ws = new WebSocketIoHandler();

			for (Map.Entry<String, IoSession> m : WebSocketIoHandler.sessionMap.entrySet()) {
				ws.sessionClosed(m.getValue());
			}

			WebSocketIoHandler.sessionMap.clear();

//			logger.info("清理后集合中的iosession:{}",WebSocketIoHandler.sessionMap);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
