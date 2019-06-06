package com.yiliao.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiliao.service.LoginService;
import com.yiliao.timer.DestroyRoomTimer;
import com.yiliao.timer.RoomTimer;
import com.yiliao.timer.SealUserTimer;
import com.yiliao.timer.SendFictitiousMsgTimer;
import com.yiliao.timer.SendTipsMsg;
import com.yiliao.timer.SimulationVideoTimer;
import com.yiliao.timer.SmsTimer;
import com.yiliao.timer.VideoTiming;
import com.yiliao.timer.VipTimer;
import com.yiliao.timer.VirtualAnchorTimer;
import com.yiliao.timer.WebSocketTimer;
import com.yiliao.timer.YellowingTimer;
import com.yiliao.util.SpringConfig;

public class TimerTask extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	Logger logger = LoggerFactory.getLogger(TimerTask.class);

	public void run() {

	}

	/**
	 * The doGet method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to get.
	 * 
	 * @param request  the request send by the client to the server
	 * @param response the response send by the server to the client
	 * @throws ServletException if an error occurred
	 * @throws IOException      if an error occurred
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");
		out.println("<HTML>");
		out.println("  <HEAD><TITLE>A Servlet</TITLE></HEAD>");
		out.println("  <BODY>");
		out.print("    This is ");
		out.print(this.getClass());
		out.println(", using the GET method");
		out.println("  </BODY>");
		out.println("</HTML>");
		out.flush();
		out.close();
	}

	/**
	 * The doPost method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to post.
	 * 
	 * @param request  the request send by the client to the server
	 * @param response the response send by the server to the client
	 * @throws ServletException if an error occurred
	 * @throws IOException      if an error occurred
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");
		out.println("<HTML>");
		out.println("  <HEAD><TITLE>A Servlet</TITLE></HEAD>");
		out.println("  <BODY>");
		out.print("    This is ");
		out.print(this.getClass());
		out.println(", using the POST method");
		out.println("  </BODY>");
		out.println("</HTML>");
		out.flush();
		out.close();
	}

	/**
	 * Initialization of the servlet. <br>
	 *
	 * @throws ServletException if an error occurs
	 */
	public void init() throws ServletException {

		// 开启线程池执行线程
		ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(10);
		/*
		 * new SmsTimer(), SmsTimer 定时器类 第一个参数 第一次启动时间 第二个参数 间隔多久启动
		 */
		scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					// 清理短信
					new SmsTimer().delSmsCode();
					// 拉取鉴黄结果
					new YellowingTimer().yellowing();
					// 发起虚拟视频
					new SimulationVideoTimer().run();

				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}, 30 * 1000, 1000 * 60, TimeUnit.MILLISECONDS);
		//更新数据定时器
		scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					// 发送模拟消息
					new SendFictitiousMsgTimer().sendFictitiousMsg();
					// vip计时
					new VipTimer().handleVIPExpire();
					// 封号
					new SealUserTimer().handleVIPExpire();
//					// 清理空闲房间
//					new DestroyRoomTimer().destroyRoom();
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
		}, 30 * 1000, 1000 * 60, TimeUnit.MILLISECONDS);

		// 开启视频聊天计时
		// 每秒钟执行一次
		scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					new VideoTiming().TimerTiming();
					new VideoTiming().clearUser();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 1000 * 10, 1000, TimeUnit.MILLISECONDS);
		
		
		//速配提示语
		scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					//提示消息
					new SendTipsMsg().sendSpreedTipsMsg();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 10000, 1000*60*5, TimeUnit.MILLISECONDS);

		// 服务器重启时 所有用户全部为下线状态
		LoginService loginAppService = (LoginService) SpringConfig.getInstance().getBean("loginAppService");
		loginAppService.startUpOnLine();
		// 半小时更新一次虚拟主播状态
		scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				new VirtualAnchorTimer().handleVIPExpire();
			}
		}, 1000 * 10, 1000 * 60 * 30, TimeUnit.MILLISECONDS);

		// 每隔2分钟 断开websocket的链接
		scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				new WebSocketTimer().sotpIoSession();
			}
		}, 1000 * 10, 1000 * 60 * 2, TimeUnit.MILLISECONDS);

		// 启动时生成房间
		new RoomTimer().productionFreeRoom();

	}

}
