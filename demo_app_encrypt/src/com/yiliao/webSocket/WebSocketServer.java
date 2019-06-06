package com.yiliao.webSocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

/**
 * 
 * @author amu
 * 
 */
public class WebSocketServer {
	
	public static final int PORT = 2048;

	public static void main(String[] args) throws IOException {

		List<Integer> arr = Arrays.asList(1,2,3,4,5);
		
		 
		System.out.println(arr.indexOf(2));
		 
	}

	/**
	 * 启动Websocket
	 */
	public static void run() {
		try {
			WebSocketIoHandler handler = new WebSocketIoHandler();
			
			NioSocketAcceptor acceptor = new NioSocketAcceptor();
			
			acceptor.getFilterChain().addLast("logger", new LoggingFilter());
			
			acceptor.getFilterChain().addLast("threadPool", new ExecutorFilter(Executors.newCachedThreadPool()));
			
			acceptor.setHandler(handler);
			
			acceptor.bind(new InetSocketAddress(PORT));
			System.out.println(" WebSocket start  prot: " + PORT);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
