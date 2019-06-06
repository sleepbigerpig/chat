package com.yiliao.timer;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiliao.domain.ActiveMsgRes;
import com.yiliao.domain.LoginInfo;
import com.yiliao.domain.UserIoSession;
import com.yiliao.service.PersonalCenterService;
import com.yiliao.util.Mid;
import com.yiliao.util.SpringConfig;

import net.sf.json.JSONObject;

/**
 * 
 * @项目工程名
 * @Module ID <(模块)类编号，可以引用系统设计中的类编号> Comments <对此类的描述，可以引用系统设计中的描述>
 * @JDK 版本(version) JDK1.6.45
 * @命名空间 com.yiliao.util
 * @作者(Author) 石德文
 * @创建日期 2016年3月15日 上午9:50:05
 * @修改人
 * @修改时间 <修改日期，格式:YYYY-MM-DD> 修改原因描述：
 * @Version 版本号 V1.0
 * @类名称
 * @描述 (发送模拟消息)
 */

public class SendFictitiousMsgTimer {

	Logger logger = LoggerFactory.getLogger(SendFictitiousMsgTimer.class);

	private PersonalCenterService personalCenterService = (PersonalCenterService) SpringConfig.getInstance()
			.getBean("personalCenterService");

	/**
	 * @方法名 delSmsCode
	 * @说明 (给用户推送模拟消息)
	 * @param 参数 设定文件
	 * @return void 返回类型
	 * @作者 石德文
	 * @throws 异常
	 */
	public void sendFictitiousMsg() throws Exception {
		// 迭代所有已经登陆的用户
		for (Map.Entry<Integer, LoginInfo> m : UserIoSession.getInstance().loginUserMap.entrySet()) {
			// 判断登陆用户今天应该发送的5条模拟消息是否发送完成
			if (!m.getValue().getTimes().isEmpty()) {
				// 上次发送的时间加上间隔分钟数 小雨或者等于了当前时间 准备给用户推送消息
				if ((m.getValue().getTimes().get(0) * 60 * 1000 + m.getValue().getLoginTime()) <= System
						.currentTimeMillis()) {
					
					logger.info("当前模拟消息用户->{}",JSONObject.fromObject(m).toString());
					
					// 当用户为女用户时 使用男主播给用户推送
					if (m.getValue().getT_sex() == 0) {
						logger.info("--进入了女用户模拟消息--");
						// 循环真实主播
						for (Map.Entry<Integer, LoginInfo> anch : UserIoSession.getInstance().loginMaleAnchorMap
								.entrySet()) {

							if (!m.getValue().getAnchor().contains(anch.getValue().getUserId())) {
								// 把当前主播加载的已经给用户发送过消息的主播里面
								m.getValue().getAnchor().add(anch.getValue().getUserId());
								logger.info("当前主播性别->{}",anch.getValue().getT_sex());
								// 给主播推送消息
								ActiveMsgRes amr = new ActiveMsgRes();
								amr.setMid(Mid.sendFictitiousMsgRes);
								amr.setActiveType(0);
								String simulationMsg = personalCenterService
										.getSimulationMsg(anch.getValue().getT_sex());
								amr.setMsgContent(simulationMsg);
								amr.setActiveUserId(m.getValue().getUserId());
								IoSession ioSession = UserIoSession.getInstance().getMapIoSession(anch.getKey());
								if (null != ioSession && StringUtils.isNotBlank(simulationMsg)) {
									ioSession.write(JSONObject.fromObject(amr).toString());
									// 清理掉刚刚发送消息的时间轴
									m.getValue().getTimes().remove(0);
								}
							}
						}
					} else {
						logger.info("--进入了男用户模拟消息--");
						// 循环真实主播
						for (Map.Entry<Integer, LoginInfo> anch : UserIoSession.getInstance().loginGirlAnchorMap
								.entrySet()) {
							if (!m.getValue().getAnchor().contains(anch.getValue().getUserId())
									&& !anch.getValue().getTimes().isEmpty()) {
								// 把当前主播加载的已经给用户发送过消息的主播里面
								m.getValue().getAnchor().add(anch.getValue().getUserId());
								logger.info("当前主播性别->{}",anch.getValue().getT_sex());
								// 给主播推送消息
								ActiveMsgRes amr = new ActiveMsgRes();
								amr.setMid(Mid.sendFictitiousMsgRes);
								amr.setActiveType(0);
								String simulationMsg = personalCenterService
										.getSimulationMsg(anch.getValue().getT_sex());
								amr.setMsgContent(simulationMsg);
								amr.setActiveUserId(m.getValue().getUserId());
								IoSession ioSession = UserIoSession.getInstance().getMapIoSession(anch.getKey());
								if (null != ioSession && StringUtils.isNotBlank(simulationMsg)) {
									ioSession.write(JSONObject.fromObject(amr).toString());
									// 清理掉刚刚发送消息的时间轴
									m.getValue().getTimes().remove(0);
								}
							}
						}
					}
					m.getValue().setLoginTime(System.currentTimeMillis());
				}
			}
		}
	}

}
