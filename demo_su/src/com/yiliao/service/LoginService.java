package com.yiliao.service;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.yiliao.util.MessageUtil;

public interface LoginService {
	
	/**
	 * 后台登录
	 * @param userName
	 * @param password
	 * @param request
	 * @param response
	 * @return
	 */
	public MessageUtil login(String userName,String password,HttpServletRequest request);
 
 
	
	 
	
	
 

}
