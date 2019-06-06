package com.yiliao.timer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import org.slf4j.Logger;

import com.yiliao.service.PersonalCenterService;
import com.yiliao.util.SpringConfig;
import com.yiliao.util.VidelSingUtil;


/**
 * 视频鉴黄获取结果
 * @author Administrator
 *
 */

public class YellowingTimer{

	//需要拉取鉴黄结果的集合
	public static List<String> fileIdList = Collections.synchronizedList(new ArrayList());
	
	Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());
	
	private static PersonalCenterService personalCenterService = null;
	
	static{
		personalCenterService = (PersonalCenterService) SpringConfig.getInstance().getBean("personalCenterService");
	}

	/**
	 * 拉取鉴黄结果
	 */
	public void yellowing() {
		try {
//			System.out.println("---进入了拉取鉴黄结果定时器 ---");
			
			Map<String, Object> tencentKey = personalCenterService.getTencentKey();
			//需要拉取鉴黄的
			for (int  i = fileIdList.size()-1 ; i >=0 ; i --) {
				
				// 获取鉴黄结果 -1:疑是包含违禁信息  0:鉴黄还未完成  1:鉴黄通过
				int pullYellowingResult = VidelSingUtil.pullYellowingResult(fileIdList.get(i),tencentKey.get("t_secret_id").toString()
						,tencentKey.get("t_secret_key").toString());
				
				logger.info("{}文件号的监黄结果{}",fileIdList.get(i),pullYellowingResult);
				//鉴黄结果
				if(pullYellowingResult !=0){
					this.personalCenterService.updateOrDelVideo(fileIdList.get(i), pullYellowingResult);
					fileIdList.remove(i);
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

 

}
