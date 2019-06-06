package com.yiliao.service;

import com.yiliao.util.MessageUtil;

public interface VideoChatService {
	
	/**
	 * 获取速配房间号
	 * @param userId
	 * @return
	 */
	MessageUtil getSpeedDatingRoom(int userId);
	
	/**
	 * IM获取用户签名
	 * @param userId
	 * @return
	 */
	public MessageUtil getImUserSig(int userId);
	/**
	 * 获取用户的用户的usersig 和 privateMapKey
	 * @param userId
	 * @return
	 */
	public MessageUtil getVideoChatUserSig(int userId,int anthorId);
	
	/**
	 * 获取PrivateMapKey
	 * @param userId
	 * @param roomId
	 * @return
	 */
	public MessageUtil getVideoChatPriavteMapKey(int userId,int roomId);
	
	/**
	 * 发起视频聊天
	 * @param launchUserId
	 * @param coverLinkUserId
	 * @param roomId
	 * @return
	 */
	public MessageUtil launchVideoChat(int launchUserId,int coverLinkUserId,int roomId);
	
	
	/**
	 * 聊天开始计时
	 * @param anthorId
	 * @param userId
	 * @param roomId
	 * @return
	 */
	public MessageUtil videoCharBeginTiming(int anthorId,int userId,int roomId);
	
	/**
	 * 挂断链接
	 * @param userId
	 * @param roomId
	 * @return
	 */
	public MessageUtil breakLink(int roomId,int type);
	/**
	 * 用户申请挂断
	 * @param userId
	 * @return
	 */
	public MessageUtil userHangupLink(int userId);
	
	/**
	 * 主播对用户发起聊天
	 * @param anchorUserId
	 * @param userId
	 * @return
	 */
	public MessageUtil anchorLaunchVideoChat(int anchorUserId,int userId,int roomId);
	
	/**
	 * 获取当前用户是否被呼叫
	 * @param userId
	 * @return
	 */
	public MessageUtil getUuserCoverCall(int userId);
	


}
