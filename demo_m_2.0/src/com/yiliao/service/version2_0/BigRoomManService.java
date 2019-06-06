package com.yiliao.service.version2_0;

import java.util.Map;

import com.yiliao.util.MessageUtil;

public interface BigRoomManService {

	/**
	 * 获取大房间主播列表
	 * @param page
	 * @return
	 */
	Map<String, Object> getBigRoomManList(int page);
	/**
	 * 删除主播大房间权限
	 * @param id
	 * @return
	 */
	MessageUtil delBigRoomAnchor(int id);
	
	/**
	 * 主播加入大房间
	 * @param t_id
	 * @param idcard
	 * @param sort
	 * @return
	 */
	MessageUtil mixBigRoom(Integer t_id,int idcard,int sort);
}
