package com.yiliao.timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiliao.service.VIPService;
import com.yiliao.util.SpringConfig;

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
 * @描述 (定时更新虚拟主播状态)
 */

public class VirtualAnchorTimer {

	Logger logger = LoggerFactory.getLogger(VirtualAnchorTimer.class);

	// 获取VIP service
	private static VIPService vipService = null;

	static {
		vipService = (VIPService) SpringConfig.getInstance().getBean("vIPService");
	}

	/**
	 * @方法名 delSmsCode
	 * @说明 (更新虚拟主播状态)
	 * @param 参数 设定文件
	 * @return void 返回类型
	 * @作者 石德文
	 * @throws 异常
	 */
	public void handleVIPExpire() {
		logger.info("--更新虚拟主播状态---");
		vipService.updateVirtualState();
	}

}
