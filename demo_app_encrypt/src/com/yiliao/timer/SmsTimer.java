package com.yiliao.timer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimerTask;




import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiliao.domain.SmsDescribe;

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
 * @类名称 Access_tokenControl
 * @描述 (定时清理验证码的方法)
 */

public class SmsTimer {

	/** 存储用户的验证码 */
	public static Map<String, SmsDescribe> verificationCode = new HashMap<String, SmsDescribe>();

	Logger logger = LoggerFactory.getLogger(SmsTimer.class);

	/**
	 * @方法名 delSmsCode
	 * @说明 (清理超时的验证码)
	 * @param 参数
	 *            设定文件
	 * @return void 返回类型
	 * @作者 石德文
	 * @throws 异常
	 */
	public void delSmsCode() {
		try {
			logger.info("--开始执行清理短信--");
			Iterator<Entry<String, SmsDescribe>> it = SmsTimer.verificationCode.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, SmsDescribe> itEntry = it.next();
				SmsDescribe sd = itEntry.getValue();
				if(null != sd && null !=sd.getTime()) {
					long time = System.currentTimeMillis()-sd.getTime().getTime();
					//本条记录大于了5分钟 删除数据
					if(time>1000*60*5){
						it.remove();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	 

}
