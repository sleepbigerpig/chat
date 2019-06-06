package com.yiliao.appSocket;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.keepalive.KeepAliveFilter;
import org.apache.mina.filter.keepalive.KeepAliveRequestTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiliao.domain.UserIoSession;

/**
 * 处理心跳异常的程序
 * @author Administrator
 *
 */
public class KeepAliveRequestTimeoutHandlerImpl implements KeepAliveRequestTimeoutHandler {

	
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Override
	public void keepAliveRequestTimedOut(KeepAliveFilter filter,
			IoSession session) throws Exception {
		    session.closeNow();
		    logger.info("心跳检测异常!干掉用户session");
		    UserIoSession.getInstance().delMapIoSession(session);
	}

}
