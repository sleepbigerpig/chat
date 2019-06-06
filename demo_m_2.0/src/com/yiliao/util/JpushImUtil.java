package com.yiliao.util;

import cn.jiguang.common.resp.APIConnectionException;
import cn.jiguang.common.resp.APIRequestException;
import cn.jiguang.common.resp.ResponseWrapper;
import cn.jmessage.api.JMessageClient;
import cn.jmessage.api.chatroom.ChatRoomListResult;
import cn.jmessage.api.chatroom.ChatRoomResult;
import cn.jmessage.api.chatroom.CreateChatRoomResult;
import cn.jmessage.api.common.model.chatroom.ChatRoomPayload;

public class JpushImUtil {

	static JMessageClient client = new JMessageClient(SystemConfig.getValue("app_key"), SystemConfig.getValue("master_secret"));

	
	/**
	 * 删除聊天室
	 */
	public void deleteChatRoom(int chatRoomId) throws APIConnectionException, APIRequestException {
		ResponseWrapper responseWrapper = client.deleteChatRoom(chatRoomId);
		System.out.println(11);
	}
	/**
	 * 创建聊天室
	 */
	public static Long createChatRoom(String idCard,String chatRoomName) throws APIConnectionException, APIRequestException {
		CreateChatRoomResult chatRoom = client.createChatRoom(ChatRoomPayload.newBuilder().setOwnerUsername(idCard).setName(chatRoomName).build());
		
		return chatRoom.getChatroom_id();
	}
	/**
	 * 查询聊天室是否还有用户
	 * @param chatRoomId
	 * @param isBreak
	 */
	public void getChatRoomDatails(int chatRoomId,boolean isBreak) {
		
		ChatRoomListResult chatRoomInfo;
		try {
			chatRoomInfo = client.getBatchChatRoomInfo(17881213);
			
			System.out.println(chatRoomInfo);
			
			ChatRoomResult roomResult = chatRoomInfo.getRooms()[0];
			//获取当前房间里面是否还有人存在
			Integer userCount = roomResult.getTotalMemberCount();
			
			if(null != userCount && userCount == 1 && isBreak) {
				//删除该聊天室
				deleteChatRoom(chatRoomId);
			}
			
		} catch (APIConnectionException e) {
			e.printStackTrace();
		} catch (APIRequestException e) {
			e.printStackTrace();
		}
	}
 

	
	
	public static void main(String[] args) {
		
		
		JMessageClient client = new JMessageClient(SystemConfig.getValue("app_key"), SystemConfig.getValue("master_secret"));
//		CreateChatRoomResult chatRoom;
//		  
//			client.sendMessage(MessagePayload.newBuilder().);
		
		
	}

}
