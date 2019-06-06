package com.yiliao.service.impl;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

import com.yiliao.service.LoginService;
import com.yiliao.util.Md5Util;
import com.yiliao.util.MessageUtil;


@Service("loginRoomService")
public class LoginServiceImpl extends ICommServiceImpl implements LoginService {
	
	private MessageUtil mu = null;
	 
	/**
	 * 后台登录
	 */
	public MessageUtil login(String userName,String password,HttpServletRequest request){
		MessageUtil mu=new MessageUtil();
		
		StringBuffer loginSql = new StringBuffer();
		
		loginSql.append("SELECT r.t_id FROM t_role r ");
		loginSql.append("LEFT JOIN t_admin a ON a.t_role_id = r.t_id ");
		loginSql.append("WHERE	a.t_user_name = ? AND t_pass_word = ? ");
		 
		List<Map<String, Object>> roleId = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(loginSql.toString(),userName,Md5Util.encodeByMD5(password));
		
		String jumpUrl = "";
		
		if(null != roleId &&  !roleId.isEmpty()){
			
			request.getSession().setMaxInactiveInterval(30*60);
			request.getSession().setAttribute("loginName", userName);
			
			mu.setM_istatus(1);
			mu.setM_strMessage("登录成功！");
			mu.setM_object(jumpUrl);
		}else{
			mu.setM_istatus(0);
			mu.setM_strMessage("登录失败！");
		}
		return mu;
	}

	 
	

	
	
	
}
