package com.yiliao.service;

import net.sf.json.JSONObject;

public interface ProhibitService {
	
	
	public void handleGetOutOfLine(JSONObject json);
	
	
	public void handleIrregularitiesUser(int roomId,int userid,String videoUrl);
	
	/***
	 * IM回调信息
	 * @param sendUser
	 * @param acceptUser
	 * @param centent
	 */
	public void handleImCallBack(int sendUser,int acceptUser,String centent);
	
	
	
	void handleIllegalityUser(Integer userId,String videoUrl);
	
	

}
