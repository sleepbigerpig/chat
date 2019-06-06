package com.yiliao.domain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.core.session.IoSession;

public class ServiceSessionEntity {
	
	
	 

	private static Map<Integer, IoSession> mapIoSesson = new ConcurrentHashMap<Integer, IoSession>();
	
	
	public static void putServerSession(Integer serverId,IoSession session){
		mapIoSesson.put(serverId, session);
	}
	//发送消息
	public static void sendMsg(Integer serverId,Object message){
		if(null!=mapIoSesson.get(serverId)){
//			System.out.println("--连线消息发送给监控服务器--");
			mapIoSesson.get(serverId).write(message);
		}
	}
	
}
