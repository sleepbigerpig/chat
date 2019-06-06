package com.yiliao.mina;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import com.yiliao.appSocket.ByteArrayCodecFactory;

public class MinaClient {

	private static final String HOSTNAME = "58.87.100.71";
	private static final int PORT = 10026;
	private static final long CONNECT_TIMEOUT = 30 * 1000L; // 30 seconds

	/**
	 * 启动客服端链接
	 * @param userId
	 */
	public void run(int userId) {
			System.out.println("当前第->"+userId);
			// TODO Auto-generated method stub
			// 创建Socket
			NioSocketConnector connector = new NioSocketConnector();
			// 设置传输方式
			/*DefaultIoFilterChainBuilder chain = connector.getFilterChain();

			ProtocolCodecFilter filter = new ProtocolCodecFilter(
					new ObjectSerializationCodecFactory());
			chain.addLast("objectFilter", filter);*/
			
			connector.getFilterChain().addLast("codec",	new ProtocolCodecFilter(new ByteArrayCodecFactory(Charset.forName("UTF-8"))));// 自定义解编码器

			// 设置消息处理
			connector.setHandler(new MinaClientHanlder(userId));
			// 超时设置
			connector.setConnectTimeoutCheckInterval(CONNECT_TIMEOUT);
			// 连接
			ConnectFuture cf = connector.connect(new InetSocketAddress(HOSTNAME, PORT));

			cf.awaitUninterruptibly();

			cf.getSession().getCloseFuture().awaitUninterruptibly();

			connector.dispose();

		
	}

	 

}
