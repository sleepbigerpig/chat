package com.yiliao.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.yiliao.service.NearbyService;
import com.yiliao.util.DateUtils;
import com.yiliao.util.MessageUtil;

/**
 * 附近服务层实现类
 * 
 * @author Administrator
 * 
 */
@Service("nearbyService")
public class NearbyServiceImpl extends ICommServiceImpl implements
		NearbyService {

	private MessageUtil mu = null;

	/*
	 * 上传用户坐标 (non-Javadoc)
	 * 
	 * @see com.yiliao.service.app.NearbyService#uploadCoordinate(int,
	 * java.lang.Double, java.lang.Double)
	 */
	@Override
	public synchronized MessageUtil uploadCoordinate(int userId, Double lat, Double lng) {

		try {

			String sql = "SELECT count(t_id) as total FROM t_coordinate WHERE t_user_id = ? ";
			Map<String, Object> reMap = this.getFinalDao().getIEntitySQLDAO()
					.findBySQLUniqueResultToMap(sql, userId);
			// 判断记录是否存在 如果存在修改记录 如果不存在 新增记录
			if (0 == Integer.parseInt(reMap.get("total").toString())) {
				// 新增
				sql = "INSERT INTO t_coordinate (t_user_id, t_lng, t_lat, t_create_time) VALUES ( ?, ?, ?, ?) ";

				this.executeSQL(sql,userId,lng,lat,DateUtils.format(new Date(),DateUtils.FullDatePattern));
			} else { // 修改记录
				sql = "UPDATE t_coordinate SET  t_lng=?, t_lat=?, t_create_time= ? WHERE t_user_id = ? ";
				this.executeSQL(sql,lng,lat,DateUtils.format(new Date(),DateUtils.FullDatePattern),userId);
			}

			mu = new MessageUtil(1, "更新成功!");

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("用户上传坐标异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 获取附近的用户数据
	 * (non-Javadoc)
	 * @see com.yiliao.service.app.NearbyService#getNearbyList(java.lang.Double, java.lang.Double)
	 */
	@Override
	public MessageUtil getNearbyList(Double lat, Double lng,int userId) {
		try {
			
			//得到系统设置的查询范围
			String sql = "SELECT t_scope FROM t_system_setup";
			Map<String, Object> resMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql);
			
			int scope = Integer.parseInt(resMap.get("t_scope").toString());
			
			sql = "select c.t_user_id,u.t_sex,u.t_handImg,c.t_lat,c.t_lng,u.t_onLine from t_coordinate c LEFT JOIN t_user u ON t_user_id=u.t_id where ( "+
					"acos( " +
					"sin((t_lat*3.1415)/180) * sin((?*3.1415)/180) + "+
					"cos((t_lat*3.1415)/180) * cos((?*3.1415)/180) * cos((t_lng*3.1415)/180 - (?*3.1415)/180)"+
					")*6370.996 "+
					")<= ? AND u.t_sex <> (SELECT t_sex FROM t_user WHERE t_id = ?) AND u.t_role <> ( SELECT t_role FROM t_user WHERE	t_id = ? )";		
			
			List<Map<String, Object>> resList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, lat,lat,lng,scope,userId,userId);
			
			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(resList);
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取附近用户异常!", e);
			mu = new MessageUtil(0,"程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getAnthorDistanceList(int userId, int page,double lat,double lng) {
		try {
			
			//获取当前用户的角色
			Map<String, Object> map = getMap("SELECT t_role,t_sex FROM t_user WHERE t_id = ?", userId);
			
			//得到系统设置的查询范围
			String sql = "SELECT t_scope FROM t_system_setup";
			Map<String, Object> resMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql);
			
			int scope = Integer.parseInt(resMap.get("t_scope").toString());
			
			StringBuffer body = new StringBuffer();
			body.append("SELECT ");
			body.append(" u.t_id,u.t_handImg,u.t_sex,u.t_nickName,u.t_phone,u.t_role,u.t_age,u.t_vocation,u.t_autograph,");
			if(1 == Integer.parseInt(map.get("t_role").toString())) {
				body.append("u.t_onLine,");
			}else {
				body.append("a.t_state AS t_onLine,");
			}
			body.append("aa.*  FROM (  SELECT t_user_id,");
			body.append(" ( 6371 * acos(cos( radians( c.t_lat ) ) * cos( radians( ? ) ) * cos( ");
			body.append("   radians( ? ) - radians( c.t_lng ) ");
			body.append("  ) + sin( radians( c.t_lat ) ) * sin( radians( ? ) ) ");
			body.append(" )) AS distance ");
			body.append("FROM t_coordinate c ");
			body.append("HAVING distance < ? ) aa ");
			body.append("LEFT JOIN t_user u  ON aa.t_user_id = u.t_id LEFT JOIN t_anchor  a ON a.t_user_id = u.t_id ");
			body.append("WHERE u.t_sex <> ").append(map.get("t_sex"));
			body.append(" AND u.t_role <> ").append(map.get("t_role"));
			logger.info("开始执行查询-->{}",DateUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss:SSS"));
			logger.info("参数-->{},{},{},{}",lat,lng,lat,scope);
			logger.info("统计sql->{}","SELECT COUNT(*) AS total FROM ("+body+") bb ");
			Map<String, Object> totalMap = this.getMap("SELECT COUNT(*) AS total FROM ("+body+") bb ", lat,lng,lat,scope);
			
			logger.info("统计执行完成-->{}",DateUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss:SSS"));
			
			body.append(" ORDER BY u.t_onLine ASC,");
			body.append("distance ASC ");
			logger.info("数据sql ->{}",body+ " LIMIT ?,10");
			List<Map<String,Object>> qlist = this.getQuerySqlList(body+ " LIMIT ?,10", lat,lng,lat,scope,(page-1)*10);
			
			logger.info("完成执行查询-->{}",DateUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss:SSS"));
			
			qlist.forEach(s ->{
				 if(null == s.get("t_nickName")) {
					 s.put("t_nickName", "聊友:"+s.get("t_phone").toString().substring(s.get("t_phone").toString().length() -4 ));
				 }
				 s.remove("t_phone");
				 
				 BigDecimal bd = new BigDecimal(null == s.get("distance")?"0":s.get("distance").toString());
				 s.put("distance", bd.setScale(1, BigDecimal.ROUND_DOWN));
			});
			
			return new MessageUtil(1, new HashMap<String,Object>(){{
				put("total", totalMap.get("total"));
				put("data", qlist);
			}});
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取附近的信息异常!", userId);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getUserDeta(int userId, int coverSeeUserId, double lat, double lng) {
		try {
			
			StringBuffer qSql = new  StringBuffer();
			qSql.append("SELECT ");
			qSql.append("u.t_id,u.t_handImg,u.t_sex,c.t_lat,c.t_lng,u.t_onLine,u.t_nickName,u.t_phone,u.t_age,");
			qSql.append("u.t_idcard,u.t_role,u.t_vocation,u.t_autograph,");
			qSql.append("(acos(");
			qSql.append("sin((t_lat * 3.1415) / 180) * sin((? * 3.1415) / 180) +");
			qSql.append("cos((t_lat * 3.1415) / 180) * cos((? * 3.1415) / 180) * cos(");
			qSql.append("(t_lng * 3.1415) / 180 - (? * 3.1415) / 180)) * 6370.996");
			qSql.append(") AS distance ");
			qSql.append("FROM t_coordinate c ");
			qSql.append("LEFT JOIN t_user u ON t_user_id = u.t_id ");
			qSql.append("WHERE u.t_id = ?");
		 
			Map<String, Object> userData = this.getMap(qSql.toString(), lat,lat,lng,coverSeeUserId);
			
			if(null == userData.get("t_nickName")) {
				userData.put("t_nickName","聊友:"+userData.get("t_phone").toString().substring(userData.get("t_phone").toString().length()-4));
			}
			userData.remove("t_phone");
			
			//获取用户是否已经关注当前主播了
			
			List<Map<String,Object>> sqlList = this.getQuerySqlList("SELECT * FROM t_follow WHERE t_follow_id = ? AND t_cover_follow = ?", userId,coverSeeUserId);
			
			userData.put("isFollow", sqlList.isEmpty()?0:1);
			
			
			mu = new MessageUtil(1, userData);
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("地图{}获取{}的信息异常!", userId,coverSeeUserId);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

}
