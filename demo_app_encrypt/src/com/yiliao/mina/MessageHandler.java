package com.yiliao.mina;

public class MessageHandler extends MinaClientHanlder {

	/**
	 * 消息处理
	 * @param message
	 */
	public void messageHandler(Object message){
		
		System.out.println("以下为接收到消息:");
		
		System.out.println(message);
		
	}
}
