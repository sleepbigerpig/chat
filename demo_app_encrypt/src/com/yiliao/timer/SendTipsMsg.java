package com.yiliao.timer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.session.IoSession;

import com.yiliao.domain.UserIoSession;
import com.yiliao.service.SystemService;
import com.yiliao.service.impl.impl1_7.SpeedDatingServiceImpl;
import com.yiliao.util.DateUtils;
import com.yiliao.util.Mid;
import com.yiliao.util.SpringConfig;

import net.sf.json.JSONObject;

public class SendTipsMsg {
	
	
	private Map<String,Object> spreedTipsMsgMap = null;
	
	
	private SystemService systemService = (SystemService) SpringConfig.getInstance().getBean("systemService");

	/**
	 * 推送给速配
	 */
	public void sendSpreedTipsMsg() {

//		System.out.println(DateUtils.format(new Date(), DateUtils.FullDatePattern));
//		System.out.println("给速配推送消息");
		//如果提示
		if(null ==spreedTipsMsgMap || spreedTipsMsgMap.isEmpty()) {
			spreedTipsMsgMap = systemService.getSpreedTipsMsg();
		}
		//在次判断提示信息是否存在
		if(!spreedTipsMsgMap.isEmpty()) {
             //执行提示信息
			//得到所有开启速配的主播
			//循环推送消息
			SpeedDatingServiceImpl.speedDatingList.forEach(s -> {
				//获取当前用户的session
				IoSession userIoSession = UserIoSession.getInstance().getMapIoSession(Integer.parseInt(s.get("t_id").toString()));
				if(!userIoSession.isClosing()) {
					userIoSession.write(JSONObject.fromObject(new HashMap<String,Object>(){{
						put("mid", Mid.sendSpreedTipsMsg);
						put("msgContent", spreedTipsMsgMap.get("t_spreed_hint"));
					}}));
				}
			});
 		}
	}

	 
}
