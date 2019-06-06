package com.yiliao.control;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.yiliao.service.LoginService;
import com.yiliao.util.MessageUtil;

/**
* CopyRright (c)2016-版权所有: 
* @项目工程名 WXManage                                      
* @Module ID   <(模块)类编号，可以引用系统设计中的类编号>    
    * Comments  <对此类的描述，可以引用系统设计中的描述>                                           
* @JDK 版本(version) JDK1.6.45                           
* @命名空间  com.yiliao.control               
* @作者(Author) 石德文           
* @创建日期  2016年3月8日  下午4:22:35 
* @修改人                                            
* @修改时间  <修改日期，格式:YYYY-MM-DD>                                    
    * 修改原因描述：  
* @Version 版本号 V1.0   
* @类名称 LoginControl
* @描述  (登陆)
 */
@Controller
@RequestMapping(value="/admin")
public class LoginControl{
	
	
	@Autowired
	private LoginService loginRoomService;//登录业务层
	
	/**
	 * @说明  TODO(登录)
	 */
	@RequestMapping(value="login")
	public ModelAndView login(String userName,String password,HttpServletRequest request,ModelAndView model){
		
		request.getSession().removeAttribute("logerr");
		MessageUtil mu = loginRoomService.login(userName,password,request);
		if(mu.getM_istatus() ==1){
			return new ModelAndView("home");
		}else{
			request.getSession().setAttribute("logerr", "用户名或者密码错误!");
			return  new ModelAndView("redirect:/login.jsp");
		}
	}
	
	
}
