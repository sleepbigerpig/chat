package com.yiliao.service.impl.version2_0;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.yiliao.service.impl.ICommServiceImpl;
import com.yiliao.service.version2_0.BigRoomManService;
import com.yiliao.util.JpushImUtil;
import com.yiliao.util.MessageUtil;

@Service(value = "bigRoomManService")
public class BigRoomManServiceImpl extends ICommServiceImpl implements BigRoomManService {

	@Override
	public Map<String, Object> getBigRoomManList(int page) {
		//获取总记录数
		Map<String, Object> total = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(
				"SELECT COUNT(t_id) AS total FROM t_big_room_man");
		//获取明细
		List<Map<String,Object>> sqltoMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap("SELECT br.t_id,u.t_idcard,u.t_nickName,u.t_handImg,u.t_cover_img,br.t_sort,br.t_is_debut,br.t_room_id FROM t_big_room_man br LEFT JOIN t_user u ON br.t_user_id = u.t_id  LIMIT ?,10;",
				(page-1)*10);
		
		return new HashMap<String,Object>() {{
			put("total", total.get("total"));
			put("rows", sqltoMap);
		}};
	}

	@Override
	public MessageUtil delBigRoomAnchor(int id) {
		
		int result = this.getFinalDao().getIEntitySQLDAO().executeSQL("DELETE FROM t_big_room_man WHERE t_id = ? ", id);
		
		if(result == 1) {
			return new MessageUtil(1, "删除成功!");
		}
		return new MessageUtil(-1, "删除失败!");
	}

	@Override
	public MessageUtil mixBigRoom(Integer t_id, int idcard, int sort) {
		try {

			//判定是修改还是新增
			if(null != t_id && t_id > 0) { //修改
				
				//获取当前idcard 是否是主播
				List<Map<String,Object>> sqltoMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap
				("SELECT * FROM t_user WHERE t_idcard = ? AND t_role = 1", idcard);
				
				if(null == sqltoMap || sqltoMap.isEmpty()) {
					return new MessageUtil(-1, "该用户不是主播.");
				}
				
			   this.getFinalDao().getIEntitySQLDAO().executeSQL("UPDATE t_big_room_man SET t_user_id = ? ,t_sort = ? WHERE t_id = ?",
					   (idcard-10000),sort,t_id);
				
			   return new MessageUtil(1, "更新成功！");
			}else {
				
				//获取当前idcard 是否是主播
				List<Map<String,Object>> sqltoMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap
				("SELECT * FROM t_user WHERE t_idcard = ? AND t_role = 1", idcard);
				
				if(null == sqltoMap || sqltoMap.isEmpty()) {
					return new MessageUtil(-1, "该用户不是主播.");
				}
				
				//判定该主播是否已经有开播权限了
				List<Map<String, Object>> bigRooms = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap("SELECT * FROM t_big_room_man WHERE t_user_id = ? ", idcard-10000);
				
				if(null != bigRooms && !bigRooms.isEmpty()) {
					return new MessageUtil(-1, "主播已存在!");
				}
				
				Long chatRoomId = JpushImUtil.createChatRoom(idcard+"", "主播聊天室");
				
				this.getFinalDao().getIEntitySQLDAO().executeSQL("INSERT INTO t_big_room_man (t_user_id, t_sort,t_chat_room_id,t_is_debut ) VALUES ( ?, ?, ? ,0);",
						(idcard-10000),sort,chatRoomId);
				return new MessageUtil(1, "新增成功！");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new MessageUtil(0, "程序异常!");
		}
	}

}
