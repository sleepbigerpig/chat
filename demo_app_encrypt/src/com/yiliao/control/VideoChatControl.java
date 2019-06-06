package com.yiliao.control;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yiliao.service.VideoChatService;
import com.yiliao.util.BaseUtil;
import com.yiliao.util.MessageUtil;
import com.yiliao.util.RSACoderUtil;

import net.sf.json.JSONObject;

/**
 * 视频聊天控制层
 * 
 * @author Administrator
 * 
 */
@Controller
@RequestMapping("app")
public class VideoChatControl {

	@Autowired
	private VideoChatService videoChatService;

	/**
	 * 获取速配房间号
	 * 
	 * @param userId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = { "getSpeedDatingRoom" }, method = { RequestMethod.POST })
	@ResponseBody
	public MessageUtil getSpeedDatingRoom(HttpServletRequest req) {
		// 解密参数
		JSONObject param = RSACoderUtil.privateDecrypt(req);
		if (!BaseUtil.params(param.getOrDefault("userId", 0))) {
			// 返回数据
			return new MessageUtil(-500, "服务器拒绝执行请求!");
		}
		return videoChatService.getSpeedDatingRoom(param.getInt("userId"));
	}

	/**
	 * 获取IM签名
	 * 
	 * @param userId
	 * @param response
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping("getImUserSig")
	@ResponseBody
	public MessageUtil getImUserSig(HttpServletRequest req) {
		// 解密参数
		JSONObject param = RSACoderUtil.privateDecrypt(req);
		if (!BaseUtil.params(param.getOrDefault("userId", 0))) {
			// 返回数据
			return new MessageUtil(-500, "服务器拒绝执行请求!");
		}

		return this.videoChatService.getImUserSig(param.getInt("userId"));

	}

	/**
	 * 根据用户编号获取签名
	 * 
	 * @param userId
	 * @param response
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping("getVideoChatAutograph")
	@ResponseBody
	public MessageUtil getVideoChatUserSig(HttpServletRequest req) {
		// 解密参数
		JSONObject param = RSACoderUtil.privateDecrypt(req);
		if (!BaseUtil.params(param.getOrDefault("userId", 0), param.getOrDefault("anthorId", 0))) {
			// 返回数据
			return new MessageUtil(-500, "服务器拒绝执行请求!");
		}

		return this.videoChatService.getVideoChatUserSig(param.getInt("userId"), param.getInt("anthorId"));

	}

	/**
	 * 获取PriavteMapKey
	 * 
	 * @param userId   用户编号
	 * @param roomId   房间号
	 * @param response
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping("getVideoChatPriavteMapKey")
	@ResponseBody
	public MessageUtil getVideoChatPriavteMapKey(HttpServletRequest req) {
		// 解密参数
		JSONObject param = RSACoderUtil.privateDecrypt(req);
		if (!BaseUtil.params(param.getOrDefault("userId", 0), param.getOrDefault("roomId", 0))) {
			// 返回数据
			return new MessageUtil(-500, "服务器拒绝执行请求!");
		}

		return this.videoChatService.getVideoChatPriavteMapKey(param.getInt("userId"), param.getInt("roomId"));

	}

	/**
	 * 用户对主播发起视频聊天
	 * 
	 * @param launchUserId
	 * @param coverLinkUserId
	 * @param response
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping("launchVideoChat")
	@ResponseBody
	public MessageUtil launchVideoChat(HttpServletRequest req) {
		// 解密参数
		JSONObject param = RSACoderUtil.privateDecrypt(req);
		if (!BaseUtil.params(param.getOrDefault("userId", 0), param.getOrDefault("coverLinkUserId", 0),
				param.getOrDefault("roomId", 0)) || param.getInt("userId") == param.getInt("coverLinkUserId")) {
			// 返回数据
			return new MessageUtil(-500, "服务器拒绝执行请求!");
		}

		return this.videoChatService.launchVideoChat(param.getInt("userId"), param.getInt("coverLinkUserId"),
				param.getInt("roomId"));

	}

	/**
	 * 开始计时
	 * 
	 * @param launchUserId
	 * @param coverLinkUserId
	 * @param roomId
	 * @param response
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping("videoCharBeginTiming")
	@ResponseBody
	public MessageUtil videoCharBeginTiming(HttpServletRequest req) {
		// 解密参数
		JSONObject param = RSACoderUtil.privateDecrypt(req);
		if (!BaseUtil.params(param.getOrDefault("anthorId", 0), param.getOrDefault("userId", 0),
				param.getOrDefault("roomId", 0)) || param.getInt("anthorId") == param.getInt("userId")) {
			// 返回数据
			return new MessageUtil(-500, "服务器拒绝执行请求!");
		}

		return this.videoChatService.videoCharBeginTiming(param.getInt("anthorId"), param.getInt("userId"),
				param.getInt("roomId"));

	}

	/**
	 * 断开链接
	 * 
	 * @param userId
	 * @param roomId
	 * @param response
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping("breakLink")
	@ResponseBody
	public MessageUtil breakLink(HttpServletRequest req) {
		// 解密参数
		JSONObject param = RSACoderUtil.privateDecrypt(req);
		if (!BaseUtil.params(param.getOrDefault("userId", 0), param.getOrDefault("roomId", 0))) {
			// 返回数据
			return new MessageUtil(-500, "服务器拒绝执行请求!");
		}
		return this.videoChatService.breakLink(param.getInt("roomId"), 1);

	}

	/**
	 * 用户挂端链接
	 * 
	 * @param userId
	 * @param response
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping("userHangupLink")
	@ResponseBody
	public MessageUtil userHangupLink(HttpServletRequest req) {
		// 解密参数
		JSONObject param = RSACoderUtil.privateDecrypt(req);
		if (!BaseUtil.params(param.getOrDefault("userId", 0))) {
			// 返回数据
			return new MessageUtil(-500, "服务器拒绝执行请求!");
		}

		return this.videoChatService.userHangupLink(param.getInt("userId"));

	}

	/**
	 * 主播对用户发起聊天
	 * 
	 * @param anchorUserId
	 * @param userId
	 * @param response
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping("anchorLaunchVideoChat")
	@ResponseBody
	public MessageUtil anchorLaunchVideoChat(HttpServletRequest req) {
		// 解密参数
		JSONObject param = RSACoderUtil.privateDecrypt(req);
		if (!BaseUtil.params(param.getOrDefault("anchorUserId", 0), param.getOrDefault("userId", 0),
				param.getOrDefault("roomId", 0)) || param.getInt("anchorUserId") == param.getInt("userId")) {
			// 返回数据
			return new MessageUtil(-500, "服务器拒绝执行请求!");
		}

		return this.videoChatService.anchorLaunchVideoChat(param.getInt("anchorUserId"), param.getInt("userId"),
				param.getInt("roomId"));

	}

	/***
	 * 获取当前用户是否被呼叫
	 * 
	 * @param userId
	 * @param response
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping("getUuserCoverCall")
	@ResponseBody
	public MessageUtil getUuserCoverCall(HttpServletRequest req) {
		// 解密参数
		JSONObject param = RSACoderUtil.privateDecrypt(req);
		if (!BaseUtil.params(param.getOrDefault("userId", 0))) {
			// 返回数据
			return new MessageUtil(-500, "服务器拒绝执行请求!");
		}
		return this.videoChatService.getUuserCoverCall(param.getInt("userId"));

	}

}
