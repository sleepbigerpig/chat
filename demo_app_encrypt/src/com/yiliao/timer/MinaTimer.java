package com.yiliao.timer;

import org.apache.mina.core.session.IoSession;
import org.slf4j.LoggerFactory;

import com.yiliao.domain.UserIoSession;

public class MinaTimer extends Thread {

	private IoSession session;

	public MinaTimer(IoSession session) {
		this.session = session;
	}

	public void run() {

		try {
			Thread.sleep(3000);
			// 判断session 是否存在
			if (UserIoSession.getInstance().mapIoSesson.isEmpty()
					|| !UserIoSession.getInstance().mapIoSesson.containsValue(session)) {

				LoggerFactory.getLogger(getClass()).info("关闭无效的session->{}", session);
				
			    session.closeNow();
				 
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

//		super.stop();
	}
}
