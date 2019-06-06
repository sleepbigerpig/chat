package com.yiliao.appSocket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import net.sf.json.JSONObject;

import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiliao.domain.LoginInfo;
import com.yiliao.domain.UserIoSession;
import com.yiliao.domain.UserLoginReq;
import com.yiliao.domain.UserLoginRes;
import com.yiliao.service.LoginService;
import com.yiliao.util.GoogleCacheUtil;
import com.yiliao.util.Mid;
import com.yiliao.util.SpringConfig;

public class ServiceMessageHandler extends MinaServerHanlder {

	Logger logger = LoggerFactory.getLogger(getClass());
	
	// 登陆后修改用户状态
	LoginService loginService = (LoginService) SpringConfig.getInstance().getBean("loginAppService");

	public void messageHandler(IoSession session, Object message) {

		JSONObject mes = JSONObject.fromObject(message);

		logger.info("当前登陆数据-->{}", mes.toString());
		// 用户登陆
		if (Mid.userLoginReq == mes.getInt("mid")) {

			UserLoginReq ulq = (UserLoginReq) JSONObject.toBean(mes,
					UserLoginReq.class);
			
			int resLoginCount = loginService.socketOnLine(ulq.getUserId(),session);
			//判断用户是否登陆成功!
			//如果为0表示 没有此用户
			//关闭session
			if(1 != resLoginCount) {
				session.closeNow();
				return;
			}
			
			//判断用户的iosession 是否存在
			if(UserIoSession.getInstance().loginUserMap.containsValue(session)) {
				UserIoSession.getInstance().getMapIoSession(ulq.getUserId()).closeNow();
			}
			// 存储iosession
			UserIoSession.getInstance().putMapIoSesson(ulq.getUserId(), session);
			// 存储登陆用户消息
			LoginInfo li = new LoginInfo();
			li.setT_is_vip(ulq.getT_is_vip());
			li.setT_role(ulq.getT_role());
			li.setUserId(ulq.getUserId());
			li.setT_sex(ulq.getT_sex());
			li.setLoginTime(System.currentTimeMillis());
			li.setTimes(this.randomCommon(1, 10, 5));

			//判断权限  0.标示用户
			if (li.getT_role() == 0) {
				UserIoSession.getInstance().putLoginUserMap(li.getUserId(), li);
			} else {
				if (li.getT_sex() == 0) {
					UserIoSession.getInstance().putLoginGirlAnchorMap(
							li.getUserId(), li);
				} else {
					UserIoSession.getInstance().putLoginMaleAnchorMap(
							li.getUserId(), li);
				}
			}
			// 构建登陆成功消息
			UserLoginRes urs = new UserLoginRes();
			urs.setMid(Mid.userLoginRes);
			urs.setState(1);
			urs.setMessage("登陆成功!");

			session.write(JSONObject.fromObject(urs).toString());
			//清理主页缓存
			GoogleCacheUtil.homePage.invalidateAll();
			
			//获取用户是否需要模拟呼叫
//			if(li.getT_sex() == 0 && li.getT_role() == 0) {
//				
//				
//			}
			
		} else if(Mid.superviseLogReq == mes.getInt("mid")){ //子服务器登陆
			
		    JSONObject json = new JSONObject();
		    json.put("mid", Mid.superviseLogRes);
		    json.put("msg", "hole,欢迎你登陆【监控服务器】.");
			logger.info("---子服务器监控服务器已登陆---");
			session.write(json);
		}

	}

	/**
	 * 随机指定范围内N个不重复的数 最简单最基本的方法
	 * 
	 * @param min
	 *            指定范围最小值
	 * @param max
	 *            指定范围最大值
	 * @param n
	 *            随机数个数
	 */
	public List<Integer> randomCommon(int min, int max, int n) {
		if (n > (max - min + 1) || max < min) {
			return null;
		}
		List<Integer> arr = new ArrayList<Integer>();
		arr.add(2);
		int count = 0;
		while (count < n) {
			int num = (int) (Math.random() * (max - min)) + min;
			if (!arr.contains(num)) {
				arr.add(num);
				count++;
			}
		}
		return arr;
	}

	public static void main(String[] args) {

		ServiceMessageHandler smh = new ServiceMessageHandler();

		System.out.println(smh.randomCommon(1, 10, 4));
	}

	/**
	 * 清理掉线数据
	 * 
	 * @param session
	 */
	public void exceptionCaught(IoSession session) {

		// 循环迭代数据
		for (Entry<Integer, IoSession> m : UserIoSession.getInstance()
				.getMapIoSesson().entrySet()) {
			if (session.equals(m.getValue())) {
				UserIoSession.getInstance().getMapIoSesson().remove(m.getKey());
				logger.info("用户异常掉线清理用户session-->userId->{},sesison->{}",
						m.getKey(), session);
				LoginService loginAppService = (LoginService) SpringConfig
						.getInstance().getBean("loginAppService");
				loginAppService.socketBreak(m.getKey());
			}
		}
	}
}
