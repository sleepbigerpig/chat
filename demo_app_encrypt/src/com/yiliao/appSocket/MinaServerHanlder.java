package com.yiliao.appSocket;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiliao.domain.UserIoSession;
import com.yiliao.timer.MinaTimer;

public class MinaServerHanlder extends IoHandlerAdapter {

	Logger logger = LoggerFactory.getLogger(this.getClass());

	static ServiceMessageHandler serMessageHandler;

	static {
		serMessageHandler = new ServiceMessageHandler();
	}
	
	// 异常捕捉
	public void exceptionCaught(IoSession session, Throwable cause) {
//		System.out.println("客户端异常掉线");
		this.serMessageHandler.exceptionCaught(session);
	}

	// 服务器与客户端创建连接
	public void sessionCreated(IoSession session) throws Exception {
//		System.out.println("服务器与客户端创建连接...");
		super.sessionCreated(session);
		//开启线程
		new  MinaTimer(session).start();
	}

	public void sessionOpened(IoSession session) throws Exception {
//		System.out.println("服务器与客户端连接打开...");
		super.sessionOpened(session);
	}

	// 消息的接收处理
	public void messageReceived(IoSession session, Object message)
			throws Exception {
		// TODO Auto-generated method stub
		super.messageReceived(session, message);// 消息的接受
		// 传递自定义解编码器传递数组和解析数组丢包断包的
		// String a = (String) message;
		// System.out.println("接收到的数据：" + a);
		messageHandler(session, message);

	}

	// 消息发送后调用
	public void messageSent(IoSession session, Object message) throws Exception {
		// TODO Auto-generated method stub
		super.messageSent(session, message);
//		System.out.println("服务器发送消息成功...");
	}

	// session关闭
	public void sessionClosed(IoSession session) throws Exception {
		// TODO Auto-generated method stub
		super.sessionClosed(session);
//		System.out.println("断开连接：");
		UserIoSession.getInstance().delMapIoSession(session);
	}

	@SuppressWarnings("static-access")
	private void messageHandler(IoSession session, Object message) {
		try {
			this.serMessageHandler.messageHandler(session, message);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
