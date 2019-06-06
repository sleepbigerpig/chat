package com.yiliao.mina;

import net.sf.json.JSONObject;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import com.yiliao.domain.UserLoginReq;
import com.yiliao.util.Mid;

public class MinaClientHanlder extends IoHandlerAdapter {

	static MessageHandler messageHandler;
	
	private int userId;

	static {
		messageHandler = new MessageHandler();
	}

	public MinaClientHanlder() {
		// TODO Auto-generated constructor stub
	}

	public MinaClientHanlder(int userId) {
		super();
		this.userId = userId;
	}

	public void sessionOpened(IoSession session) throws Exception {
		
		//向服务器注册当前子服务器
		UserLoginReq mu = new UserLoginReq();
		mu.setMid(Mid.userLoginReq);
		mu.setT_is_vip(0);
		mu.setT_role(0);
		mu.setT_sex(1);
		mu.setUserId(userId);
		session.write(JSONObject.fromObject(mu).toString());
	}

	public void sessionClosed(IoSession session) {
		System.out.println("client close");
	}

	public void messageReceived(IoSession session, Object message)
			throws Exception {
		if(message.equals("0x11")){
			session.write("01010");
		}
		System.out.println(message);
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}
	
	
}
