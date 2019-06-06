package com.yiliao.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.mina.core.session.IoSession;

import com.yiliao.domain.GetOutOfLineRes;
import com.yiliao.domain.UserIoSession;
import com.yiliao.service.LoginService;
import com.yiliao.util.BodyReaderHttpServletRequestWrapper;
import com.yiliao.util.MessageUtil;
import com.yiliao.util.Mid;
import com.yiliao.util.RSACoderUtil;
import com.yiliao.util.SpringConfig;

import net.sf.json.JSONObject;

public class LoginFilter implements Filter {

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {

		ServletRequest requestWrapper = null;
		if (req instanceof HttpServletRequest) {
			requestWrapper = new BodyReaderHttpServletRequestWrapper((HttpServletRequest) req);
		}
		// 将父接口强制转化为子对象
		HttpServletRequest request = (HttpServletRequest) requestWrapper;
		HttpServletResponse response = (HttpServletResponse) resp;

		String url = request.getRequestURL().toString();
		System.out.println("请求地址：" + url);

		if (url.indexOf("qqLogin") > 0 || url.indexOf("getVerify") > 0 || url.indexOf("getVerifyCodeIsCorrect") > 0
				|| url.indexOf("weixinLogin") > 0 || url.indexOf("login") > 0 || url.indexOf("userLogin") > 0
				|| url.indexOf("sendPhoneVerificationCode") > 0 || url.indexOf("getLongSetUpList") > 0
				|| url.indexOf("tencentCallback") > 0 || url.indexOf("wxPayCallBack") > 0
				|| url.indexOf("alipay_callback") > 0 || url.indexOf("getTakeOutMode") > 0
				|| url.indexOf("getIosSwitch") > 0 || url.indexOf("jumpShare") > 0 || url.indexOf("addShareInfo") > 0
				|| url.indexOf("jumpCivilization") > 0 || url.indexOf("jumpShareCourtesy") > 0 || url.indexOf("onloadGlanceOver") > 0
				|| url.indexOf("getJsonpRequest") > 0 || url.indexOf("imCallBack") > 0 || url.indexOf("getStyleSetUp") > 0
				|| url.indexOf("endSpeedDating") > 0  || url.indexOf("sendSocketNotice") > 0 || url.indexOf("register") > 0
				|| url.indexOf("upPassword") > 0 ) {

			if (null == requestWrapper) {
				chain.doFilter(req, response);
			} else {
				chain.doFilter(requestWrapper, response);
			}
		} else {
			
			JSONObject plain_param = RSACoderUtil.privateDecrypt(request);
			System.out.println("解密后的参数-->" + plain_param.toString());
			// 验证是否传递参数
			if (null == plain_param || (null == plain_param.get("userId") && null == plain_param.get("anchorUserId"))) {
				MessageUtil mu = new MessageUtil(-500, "服务器拒绝执行请求!");
				response.setContentType("text/html;charset=UTF-8");
				PrintWriter out = response.getWriter();
				out.print(JSONObject.fromObject(mu).toString());
				out.close();
				return;
			}

			int userId = 0;

			if (url.indexOf("anchorLaunchVideoChat") > 0) {
				userId = plain_param.getInt("anchorUserId");
			} else {
				userId = plain_param.getInt("userId");
			}
			
			LoginService loginService = (LoginService) SpringConfig.getInstance().getBean("loginAppService");
			// 用户未被封号返回false 否则返回true
			if (!loginService.getUserIsDisable(userId)) {
				if (null == requestWrapper) {
					chain.doFilter(req, response);
				} else {
					chain.doFilter(requestWrapper, response);
				}
			} else {

				MessageUtil mu = new MessageUtil(-1010, "您因违反平台相关规定已被封号!");

				response.setContentType("text/html;charset=UTF-8");
				PrintWriter out = response.getWriter();
				out.print(JSONObject.fromObject(mu).toString());
				out.close();
				
				//获取用户是否存在session 
				// socket推送
				IoSession launSession = UserIoSession.getInstance().getMapIoSession(userId);
				if (null != launSession) {
					GetOutOfLineRes gof = new GetOutOfLineRes();
					gof.setMid(Mid.getOutOfLineRes);
//					gof.setMessage(message);
					launSession.write(JSONObject.fromObject(gof).toString());
				}
				return;
			}
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub

	}

}
