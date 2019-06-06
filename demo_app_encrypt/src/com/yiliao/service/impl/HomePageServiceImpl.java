package com.yiliao.service.impl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import com.yiliao.domain.LoginInfo;
import com.yiliao.domain.UserIoSession;
import com.yiliao.domain.WalletDetail;
import com.yiliao.service.HomePageService;
import com.yiliao.util.DateUtils;
import com.yiliao.util.MessageUtil;

/**
 * 主页服务层
 * 
 * @author Administrator
 * 
 */
@Service("homePageService")
public class HomePageServiceImpl extends ICommServiceImpl implements HomePageService {

	private MessageUtil mu = null;

	/**
	 * app主页
	 * 
	 * @param userId    请求人编号
	 * @param page      页码
	 * @param queryType 类型 -1:全部 0.女 1.男
	 * @param condition 查询条件(昵称 or ID号)
	 * @param response
	 */
	@Override
	public MessageUtil getHomePageList(int userId, int page, Integer queryType) {
		try {

			String inStr = "";
			for (Entry<Integer, LoginInfo> m : UserIoSession.getInstance().loginGirlAnchorMap.entrySet()) {
				inStr = inStr + m.getKey() + ",";
			}

			logger.info("instr->{}",inStr);
			
			StringBuffer homeSql = new StringBuffer();
			homeSql.append("SELECT u.t_id,u.t_cover_img,u.t_handImg,u.t_nickName,u.t_age,u.t_city,u.t_vocation,u.t_autograph,'0' AS so ");
			homeSql.append("FROM t_user u LEFT JOIN t_virtual v ON v.t_user_id = u.t_id ");
			homeSql.append("LEFT JOIN  t_certification c ON c.t_user_id = u.t_id ");
			homeSql.append("WHERE v.t_user_id IS NULL AND t_onLine = 0 AND t_role = 1 AND t_disable = 0 ");
			homeSql.append("AND t_cover_img IS NOT NULL AND t_sex = 0 AND c.t_certification_type = 1 ");
			homeSql.append("AND u.t_id IN (").append(StringUtils.isNotBlank(inStr) ? inStr.substring(0, inStr.length() - 1) : 0).append(") ");;
			homeSql.append("UNION ");
			homeSql.append("SELECT u.t_id,u.t_cover_img,u.t_handImg,u.t_nickName,u.t_age,u.t_city,u.t_vocation,u.t_autograph,'1' AS so ");
			homeSql.append("FROM t_user u LEFT JOIN t_virtual v ON v.t_user_id = u.t_id ");
			homeSql.append("LEFT JOIN  t_certification c ON c.t_user_id = u.t_id ");
			homeSql.append("WHERE v.t_user_id IS NULL AND t_onLine = 0 AND t_role = 1 AND t_disable = 0 ");
			homeSql.append("AND t_cover_img IS NOT NULL AND t_sex = 0 AND c.t_certification_type = 1 ");
			homeSql.append("UNION ");
			homeSql.append("SELECT u.t_id,u.t_cover_img,u.t_handImg,u.t_nickName,u.t_age,u.t_city,u.t_vocation,u.t_autograph,'2' AS so ");
			homeSql.append("FROM t_user u  LEFT JOIN t_virtual v ON v.t_user_id = u.t_id  ");
			homeSql.append("WHERE  v.t_user_id IS NOT NULL AND t_onLine = 0 AND t_role = 1 ");
			homeSql.append("AND t_disable = 0 AND t_cover_img IS NOT NULL AND t_sex = 0 "); 
			
			// 统计总记录数
			Map<String, Object> total = this.getMap("SELECT COUNT(aa.t_id) AS total FROM (" + homeSql + ") aa");
			
			StringBuffer limitSql = new StringBuffer();
			limitSql.append("SELECT aa.*,a.t_state,s.t_video_gold,AVG(IFNULL(e.t_score, 5)) AS t_score FROM(");
			limitSql.append(homeSql);
			limitSql.append(") aa LEFT JOIN t_anchor a ON a.t_user_id = aa.t_id ");
			limitSql.append("LEFT JOIN t_user_evaluation e ON e.t_user_id = aa.t_id ");
			limitSql.append("LEFT JOIN t_anchor_setup s ON s.t_user_id = aa.t_id ");
			limitSql.append("GROUP BY aa.t_id ");
			limitSql.append("ORDER BY aa.so ASC,a.t_state ASC,t_score DESC LIMIT ?,10");
			// 获取数据
			List<Map<String, Object>> dataList = getQuerySqlList(limitSql.toString(),(page - 1) * 10);
			
		
			int pageCount = Integer.parseInt(total.get("total").toString()) % 10 == 0
					? Integer.parseInt(total.get("total").toString()) / 10
					: Integer.parseInt(total.get("total").toString()) / 10 + 1;
			// 计算评分
			for (Map<String, Object> m : dataList) {
				// 得到该主播是否存在免费的公共视频
				Map<String, Object> userVideo = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(
						"SELECT count(t_id) AS total  FROM t_album WHERE t_user_id = ? AND t_file_type=1 AND t_is_private = 0 AND t_is_del = 0 AND t_auditing_type = 1",
						Integer.parseInt(m.get("t_id").toString()));
				// 是否存在公共视频
				m.put("t_is_public", Integer.parseInt(userVideo.get("total").toString()) > 0 ? 1 : 0);
				m.put("t_score", avgScore(Integer.parseInt(m.get("t_id").toString())));
			}

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(new HashMap<String, Object>() {
				{
					put("pageCount", pageCount);
					put("data", dataList);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取主播列表异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/**
	 * 计算平均分
	 * 
	 * @param userId
	 */
	public int avgScore(int userId) {

		String sql = "SELECT AVG(t_score) AS avgScore FROM t_user_evaluation WHERE t_anchor_id = ?";

		Map<String, Object> avgScore = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql, userId);

		Double avg = Double.valueOf(null == avgScore.get("avgScore") ? "5" : avgScore.get("avgScore").toString());

		return new BigDecimal(avg).setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
	}

	/*
	 * 获取主播播放页数据 (non-Javadoc)
	 * 
	 * @see com.yiliao.service.app.HomePageService#getAnchorPlayPage(int)
	 */
	@Override
	public MessageUtil getAnchorPlayPage(int consumeUserId, int coverConsumeUserId, Integer albumId,
			Integer queryType) {

		try {

			System.out.println(DateUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss:SSS"));

			Map<String, Object> data_map = null;

			if (null == queryType || queryType == 0) { // 查询相册

				if (null != albumId && 0 != albumId)
					data_map = this.getMap(
							"SELECT t_id,t_video_img ,t_addres_url ,t_see_count,t_title,t_user_id FROM t_album  WHERE t_id = ? AND t_file_type = 1 LIMIT 1 ",
							albumId);
				else
					data_map = this.getMap(
							"SELECT t_id,t_video_img ,t_addres_url,t_see_count,t_title,t_user_id FROM t_album  WHERE t_is_private = 0 AND t_file_type = 1 AND t_user_id = ? LIMIT 1 ",
							coverConsumeUserId);
				// 相册查看次数+1
				this.executeSQL(" UPDATE t_album SET t_see_count = t_see_count+1 WHERE t_id = ?  ",
						data_map.get("t_id"));

				data_map.remove("t_id");
			} else if (queryType == 1) { // 查询动态

				data_map = this.getMap(
						"SELECT t_cover_img_url AS t_video_img,t_file_url AS t_addres_url,t_see_count,'title' AS t_title,t_dynamic_id FROM t_dynamic_file WHERE t_id = ? AND t_file_type = 1 LIMIT 1 ",
						albumId);
				// 获取用户编号
				data_map.put("t_user_id",
						this.getMap("SELECT t_user_id  FROM t_dynamic WHERE t_id = ? ", data_map.get("t_dynamic_id"))
								.get("t_user_id"));
				data_map.remove("t_dynamic_id");
				// 跟新
				this.executeSQL(" UPDATE t_dynamic_file SET t_see_count = t_see_count+1 WHERE t_id = ? ", albumId);
			}

			int user_id = Integer.parseInt(data_map.get("t_user_id").toString());
			// 装载用户信息
			data_map.putAll(this.getMap("SELECT t_handImg,t_nickName,t_age,t_city,t_weixin FROM t_user WHERE t_id = ? ",
					user_id));
			// 装载主播的视频收费和微信收费
			data_map.putAll(this.getMap(
					"SELECT t_video_gold AS videoGold,t_weixin_gold FROM t_anchor_setup WHERE t_user_id = ? ",
					user_id));
			// 装载主播的评分
			data_map.putAll(
					this.getMap("SELECT AVG(t_score) AS t_score FROM t_user_evaluation WHERE t_user_id = ? ", user_id));
			// 装载主播当前在线状态
			data_map.putAll(this.getMap("SELECT t_state AS t_onLine FROM t_anchor WHERE t_user_id = ? ", user_id));

			Map<String, Object> laud = this
					.getMap("SELECT COUNT(t_id) AS totalLaud FROM t_user_laud WHERE t_cover_user_id = ? ", user_id);

			Random random = new Random();
			int s = random.nextInt(10000) % (10000 - 5000 + 1) + 5000;
			// 总的点赞数
			data_map.put("laudtotal", Integer.parseInt(laud.get("totalLaud").toString()) + s);

			// 获取该用户是否已经查看过微信号了
			String sql = "SELECT * FROM t_order WHERE t_consume_type = 6 AND t_consume = ? AND t_cover_consume = ?";
			List<Map<String, Object>> isWeiXin = this.getQuerySqlList(sql,consumeUserId, coverConsumeUserId);

			data_map.put("isSee", null == isWeiXin ? 0 : isWeiXin.isEmpty() ? 0 : 1);

			// 当前用户是否对发布者进行点赞
			sql = "SELECT * FROM t_user_laud WHERE t_laud_user_id = ? AND t_cover_user_id = ? ";

			List<Map<String, Object>> laudList = this.getQuerySqlList(sql,consumeUserId, coverConsumeUserId);

			data_map.put("isLaud", laudList.isEmpty() ? 0 : 1);

			// 获取用户是否关注主播
			sql = " SELECT * FROM t_follow WHERE t_follow_id = ? AND t_cover_follow = ?  ";

			List<Map<String, Object>> followList = this.getQuerySqlList(sql,consumeUserId, coverConsumeUserId);

			data_map.put("isFollow", followList.isEmpty() ? 0 : 1);
			
			//获取主播是否正在大房间进行直播
			List<Map<String, Object>> bigRooms = this.getQuerySqlList("SELECT t_is_debut,t_room_id,t_chat_room_id FROM t_big_room_man WHERE t_user_id = ?", coverConsumeUserId);
			if(null == bigRooms || bigRooms.isEmpty())
				data_map.put("bigRoomData", new HashMap<String, Object>());
			else 
				data_map.put("bigRoomData", bigRooms.get(0));

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(data_map);
			System.out.println(DateUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss:SSS"));
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取主播列表异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 获取banner列表(non-Javadoc)
	 * 
	 * @see com.yiliao.service.app.HomePageService#getBannerList()
	 */
	@Override
	public MessageUtil getBannerList() {
		try {
			String sql = "SELECT t_img_url,t_link_url FROM t_banner WHERE t_is_enable = 0 AND t_type = 0 ";

			List<Map<String, Object>> findBySQLTOMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql);

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(findBySQLTOMap);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取banner列表异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getVideoList(int page, int userId, int queryType) {
		try {
			StringBuffer sb = new StringBuffer();
			sb.append(
					"SELECT d.t_id,d.t_title,d.t_user_id,d.t_video_img,d.t_addres_url,d.t_is_private,d.t_money,u.t_nickName,u.t_handImg  ");
			sb.append("FROM t_album d LEFT JOIN t_user u ON d.t_user_id = u.t_id ");
			sb.append("WHERE t_file_type = 1 AND t_is_del = 0 AND d.t_auditing_type = 1 AND u.t_role = 1 ");
			if (queryType > -1) {
				sb.append(" AND t_is_private = ").append(queryType);
			}
			sb.append(" ORDER BY d.t_id DESC LIMIT ?,10;");

			List<Map<String, Object>> dataList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sb.toString(),
					(page - 1) * 10);

			String sql = "SELECT COUNT(d.t_id) AS total FROM t_album d LEFT JOIN t_user u ON d.t_user_id = u.t_id WHERE t_file_type = 1 AND t_is_del = 0 AND d.t_auditing_type = 1 AND u.t_role = 1 ";
			if (queryType > -1) {
				sql = sql + " AND t_is_private = " + queryType;
			}

			Map<String, Object> total = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql);

			int pageCount = Integer.parseInt(total.get("total").toString()) % 10 == 0
					? Integer.parseInt(total.get("total").toString()) / 10
					: Integer.parseInt(total.get("total").toString()) / 10 + 1;

			// 查询当前数据是否已经被查看过了
			sql = "SELECT * FROM t_order WHERE t_consume = ?  AND t_cover_consume = ? AND t_consume_type = ?  AND t_consume_score = ?";

			for (Map<String, Object> m : dataList) {

				List<Map<String, Object>> findBySQLTOMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql,
						userId, Integer.parseInt(m.get("t_user_id").toString()),
						WalletDetail.CHANGE_CATEGORY_PRIVATE_VIDEO, Integer.parseInt(m.get("t_id").toString()));

				if (null == findBySQLTOMap || findBySQLTOMap.isEmpty()) {
					m.put("is_see", 0);
				} else {
					m.put("is_see", 1);
				}
			}

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(new HashMap<String,Object>(){{
				put("data", dataList);
				put("pageCount", pageCount);
			}});

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取视频列表异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 获取同城数据(non-Javadoc)
	 * 
	 * @see com.yiliao.service.HomePageService#getCityWideList(int, int)
	 */
	@Override
	public MessageUtil getCityWideList(int page, int userId) {
		try {

			// sql.append("SELECT * FROM (");

			StringBuffer sql = new StringBuffer();
			sql.append("SELECT 	* FROM 	( ");
			sql.append(
					"SELECT 	u.t_id,u.t_cover_img,a.t_state,u.t_nickName,u.t_autograph,'1' AS t_user_type,u.t_city ");
			sql.append("FROM  t_user u ");
			sql.append("LEFT JOIN t_anchor a ON a.t_user_id = u.t_id ");
			sql.append("LEFT JOIN t_certification c ON c.t_user_id = u.t_id ");
			sql.append("WHERE	u.t_role = 1 AND u.t_disable = 0 AND u.t_cover_img IS NOT NULL ");
			sql.append("AND u.t_city = (SELECT t_city FROM t_user WHERE t_id = ? ) ");
			sql.append("AND u.t_sex <> ( SELECT t_sex FROM t_user WHERE t_id = ? ) ");
			sql.append("AND c.t_certification_type = 1 ");
			sql.append("GROUP BY u.t_id ");
			sql.append("UNION ");
			sql.append("SELECT u.t_id,u.t_cover_img,a.t_state,u.t_nickName,u.t_autograph,'2' AS t_user_type,u.t_city ");
			sql.append("FROM t_user u ");
			sql.append("LEFT JOIN t_anchor a ON a.t_user_id = u.t_id ");
			sql.append("LEFT JOIN t_virtual v ON v.t_user_id = u.t_id ");
			sql.append("WHERE u.t_role = 1 AND u.t_disable = 0 AND u.t_cover_img IS NOT NULL ");
			sql.append("AND u.t_city = ( SELECT t_city FROM t_user WHERE t_id = ? ) ");
			sql.append("AND u.t_sex <> ( SELECT t_sex FROM t_user WHERE t_id = ? ) ");
			sql.append("AND v.t_user_id IS NOT NULL ");
			sql.append("GROUP BY u.t_id ");
			sql.append(" ) AS aa ");
			sql.append("UNION ALL ");
			sql.append("SELECT * FROM ( ");
			sql.append("SELECT u.t_id,u.t_cover_img,a.t_state,u.t_nickName,u.t_autograph,'1' AS t_user_type,u.t_city ");
			sql.append("FROM t_user u ");
			sql.append("LEFT JOIN t_anchor a ON a.t_user_id = u.t_id ");
			sql.append("LEFT JOIN t_certification c ON c.t_user_id = u.t_id ");
			sql.append("WHERE u.t_role = 1 AND u.t_disable = 0 AND u.t_cover_img IS NOT NULL ");
			sql.append(
					"AND (u.t_city is not NULL OR u.t_city <>  ( SELECT  t_city  FROM  t_user  WHERE  t_id = ?  )) ");
			sql.append("AND u.t_sex <> ( SELECT t_sex FROM t_user WHERE t_id = ? ) ");
			sql.append("AND c.t_certification_type = 1 ");
			sql.append("GROUP BY u.t_id ");
			sql.append("UNION ");
			sql.append("SELECT u.t_id,u.t_cover_img,a.t_state,u.t_nickName,u.t_autograph,'2' AS t_user_type,u.t_city ");
			sql.append("FROM t_user u ");
			sql.append("LEFT JOIN t_anchor a ON a.t_user_id = u.t_id ");
			sql.append("LEFT JOIN t_virtual v ON v.t_user_id = u.t_id ");
			sql.append("WHERE u.t_role = 1 AND u.t_disable = 0 AND u.t_cover_img IS NOT NULL ");
			sql.append(
					"AND (u.t_city is not NULL OR u.t_city <>  ( SELECT  t_city  FROM  t_user  WHERE  t_id = ?  )) ");
			sql.append("AND u.t_sex <> ( SELECT t_sex FROM t_user WHERE t_id = ? ) ");
			sql.append("AND v.t_user_id IS NOT NULL ");
			sql.append("GROUP BY u.t_id ");
			sql.append(") AS bb ");

			List<Map<String, Object>> dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(
					"SELECT * FROM (" + sql.toString() + ") AS cc  LIMIT ?,10", userId, userId, userId, userId, userId,
					userId, userId, userId, (page - 1) * 10);

			Map<String, Object> total = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(
					"SELECT COUNT(*) AS total FROM (" + sql.toString() + ") AS cc ", userId, userId, userId, userId,
					userId, userId, userId, userId);

			int pageCount = Integer.parseInt(total.get("total").toString()) % 10 == 0
					? Integer.parseInt(total.get("total").toString()) / 10
					: Integer.parseInt(total.get("total").toString()) / 10 + 1;

			// 得到用户是否存在免费视频
			for (Map<String, Object> m : dataMap) {
				// 得到该主播是否存在免费的公共视频
				Map<String, Object> userVideo = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(
						"SELECT count(t_id) AS total  FROM t_album WHERE t_user_id = ? AND t_file_type=1 AND t_is_private = 0 AND t_is_del = 0 ",
						Integer.parseInt(m.get("t_id").toString()));
				// 是否存在公共视频
				m.put("t_is_public", Integer.parseInt(userVideo.get("total").toString()) > 0 ? 1 : 0);

				m.remove("t_user_type");

			}

			mu = new MessageUtil(1,new HashMap<String,Object>(){{
				put("pageCount", pageCount);
				put("data", dataMap);
			}});

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取同城异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 获取搜索列表(non-Javadoc)
	 * 
	 * @see com.yiliao.service.HomePageService#getSearchList(int, java.lang.String)
	 */
	@Override
	public MessageUtil getSearchList(int userId, int page, String condition) {
		try {

			StringBuffer bodySql = new StringBuffer();

			bodySql.append("SELECT t_id,t_nickName,t_phone,t_handImg,t_role,t_onLine,t_idcard ");
			bodySql.append("FROM t_user WHERE t_disable = 0 AND ( t_nickName LIKE '%" + condition
					+ "%' OR t_idcard LIKE '%" + condition + "%') ORDER BY t_onLine ASC LIMIT ?,10;");

			// 获取列表
			List<Map<String, Object>> sqlList = this.getQuerySqlList(bodySql.toString(), (page - 1) * 10);

			// 获取总记录数
			Map<String, Object> total = getMap(
					"SELECT COUNT(t_id) AS total FROM t_user WHERE t_disable = 0 AND ( t_nickName LIKE '%" + condition
							+ "%' OR t_idcard LIKE '%" + condition + "%' ) ");

			// 得到总页数
			int pageCount = Integer.parseInt(total.get("total").toString()) % 10 == 0
					? Integer.parseInt(total.get("total").toString()) / 10
					: Integer.parseInt(total.get("total").toString()) / 10 + 1;

			sqlList.forEach(s -> {
				if (1 == Integer.parseInt(s.get("t_role").toString())) {

					// 获取主播在先状态
					List<Map<String, Object>> querySqlList = getQuerySqlList(
							"SELECT t_state FROM t_anchor WHERE t_user_id = ?", s.get("t_id"));

					if (!querySqlList.isEmpty()) {
						s.put("t_onLine", querySqlList.get(0).get("t_state"));
					}
					// 得到该主播是否存在免费的公共视频
					Map<String, Object> userVideo = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(
							"SELECT count(t_id) AS total  FROM t_album WHERE t_user_id = ? AND t_file_type=1 AND t_is_private = 0 AND t_is_del = 0 AND t_auditing_type = 1 ",
							Integer.parseInt(s.get("t_id").toString()));
					// 是否存在公共视频
					s.put("t_is_public", Integer.parseInt(userVideo.get("total").toString()) > 0 ? 1 : 0);
				} else {
					s.put("t_is_public", 0);
				}
				// 判断用户是否有昵称
				if (null == s.get("t_nickName")) {
					s.put("t_nickName",
							"聊友:" + s.get("t_phone").toString().substring(s.get("t_phone").toString().length() - 4));
				}
				s.remove("t_phone");
			});

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(new HashMap<String, Object>() {
				{
					put("pageCount", pageCount);
					put("data", sqlList);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取搜索列表异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/**
	 * 主播获取用户
	 */
	@Override
	public MessageUtil getOnLineUserList(int userId, int page, String search, Integer searchType) {
		try {

			String soStr = "";

			for (Entry<Integer, LoginInfo> m : UserIoSession.getInstance().loginUserMap.entrySet()) {
				if (m.getValue().getT_role() == 0) {
					soStr = soStr + m.getKey() + ",";
				}
			}

			StringBuffer body = new StringBuffer();
			body.append("SELECT ");
			body.append("aa.balance,u.t_id,u.t_nickName,u.t_role,u.t_handImg,u.t_sex,u.t_phone,u.t_constellation,u.t_is_vip,u.t_onLine,DATE_FORMAT(u.t_create_time,'%Y-%m-%d %T') AS t_create_time,'1' AS so ");
			body.append("FROM ( ");
			body.append("SELECT SUM(b.t_profit_money + b.t_recharge_money + b.t_share_money) AS balance,b.t_user_id FROM t_balance b	GROUP BY b.t_user_id ");
			body.append(") aa LEFT JOIN t_user u ON aa.t_user_id = u.t_id ");
			body.append("WHERE 1 = 1 AND u.t_role = 0 AND u.t_onLine = 0 AND u.t_sex = 1 AND u.t_id IN (");
			body.append(StringUtils.isNotBlank(soStr) ? soStr.substring(0, soStr.length() - 1) : 0).append(")");
			body.append("UNION ");
			body.append("SELECT ");
			body.append("aa.balance,u.t_id,u.t_nickName,u.t_role,u.t_handImg,u.t_sex,u.t_phone,u.t_constellation,u.t_is_vip,u.t_onLine,DATE_FORMAT(u.t_create_time,'%Y-%m-%d %T') AS t_create_time,'2' AS so ");
			body.append("FROM ( SELECT SUM(b.t_profit_money + b.t_recharge_money + b.t_share_money) AS balance,b.t_user_id FROM t_balance b	GROUP BY b.t_user_id ");
			body.append(") aa LEFT JOIN t_user u ON aa.t_user_id = u.t_id ");
			body.append("WHERE 1 = 1 AND u.t_role = 0 AND u.t_onLine = 0 AND u.t_sex = 1 AND u.t_id  NOT IN (")
					.append(StringUtils.isNotBlank(soStr) ? soStr.substring(0, soStr.length() - 1) : 0).append(") ");

			// 获取总记录数
			Map<String, Object> total = this.getMap("SELECT COUNT(bb.t_id) AS total FROM (" + body + ") bb");

			logger.info("SELECT * FROM ( " + body + ") bb ORDER BY bb.so ASC,bb.t_create_time DESC LIMIT ?,10");

			// 获取分页数据
			List<Map<String, Object>> sqlList = getQuerySqlList(
					"SELECT * FROM ( " + body + ") bb ORDER BY bb.so ASC,bb.t_onLine ASC,bb.t_create_time DESC LIMIT ?,10",
					(page - 1) * 10);
			// 获取在线用户
			int onLineUserCount = Integer.parseInt(total.get("total").toString());

			int pageCount = onLineUserCount % 10 == 0 ? onLineUserCount / 10 : onLineUserCount / 10 + 1;

			// 获取充值金币 用于分配用户的充值级别
			String qSql = "SELECT SUM(r.t_recharge_money) AS money FROM t_recharge  r WHERE r.t_user_id = ? AND r.t_order_state = 1";

			for (Map<String, Object> m : sqlList) {
				if (null == m.get("t_nickName")) {
					m.put("t_nickName",
							"聊友:" + m.get("t_phone").toString().substring(m.get("t_phone").toString().length() - 4));
				}
				m.remove("t_phone");
				// 金币档
				m.put("goldfiles", this.goldFiles(new BigDecimal(m.get("balance").toString()).intValue()));

				m.remove("t_create_time");
				m.remove("balance");

				List<Map<String, Object>> regList = this.getQuerySqlList(qSql, m.get("t_id"));

				if (null == regList || regList.isEmpty() || null == regList.get(0).get("money")) {
					m.put("grade", this.grade(0));
				} else {
					m.put("grade", this.grade(new BigDecimal(regList.get(0).get("money").toString()).intValue()));
				}

			}
			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(new HashMap<String, Object>() {
				{
					put("pageCount", pageCount);
					put("data", sqlList);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("主播获取粉丝列表.", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	public int goldFiles(int gold) {
		if (gold <= 500) {
			return 1;
		} else if (501 <= gold && gold <= 1000) {
			return 2;
		} else if (1001 <= gold && gold <= 2000) {
			return 3;
		} else if (2001 <= gold && gold <= 3000) {
			return 4;
		} else {
			return 5;
		}
	}

	/** 充值档 */
	public int grade(int money) {

		if (money <= 1000) {
			return 1;
		} else if (money > 1000 && money <= 10000) {
			return 2;
		} else if (money >= 10001) {
			return 3;
		}
		return 0;
	}

	/**
	 * 获取魅力排行榜
	 */
	@Override
	public MessageUtil getGlamourList(int userId, int queryType) {
		try {

			StringBuffer body = new StringBuffer();
			body.append("SELECT * FROM (");
			body.append("SELECT SUM(d.t_value) AS gold,u.t_id,u.t_nickName,u.t_idcard,u.t_handImg,u.t_phone ");
			body.append("FROM t_wallet_detail d LEFT JOIN  t_user u ON d.t_user_id=u.t_id ");
			body.append("WHERE d.t_change_type = 0 AND d.t_change_category  BETWEEN 1 AND 6 AND u.t_disable = 0 ");
			body.append("AND  d.t_change_time BETWEEN ? AND ? ");
			body.append("GROUP BY u.t_id ");
			body.append(") aa ORDER BY aa.gold DESC LIMIT ? ");

			List<Map<String, Object>> dataMap = null;

			switch (queryType) {
			case 1:

				String lastDay = DateUtils.getYesterday(new Date());

				dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(body.toString(), lastDay + " 00:00:00",
						lastDay + " 23:59:59", getRetNumber(1));

				break;
			case 2:

				Map<String, String> lastWeek = DateUtils.getLastWeek(new Date());

				dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(body.toString(),
						lastWeek.get("monday") + " 00:00:00", lastWeek.get("sunday") + " 23:59:59", getRetNumber(1));

				break;
			case 3:

				String month = DateUtils.getLastMonth(new Date());

				dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(body.toString(),
						DateUtils.getFirstDayOfMonth(0, Integer.parseInt(month)),
						DateUtils.getLastDayOfMonth(0, Integer.parseInt(month)), getRetNumber(1));

				break;
			default:

				StringBuffer qSql = new StringBuffer();
				qSql.append("SELECT * FROM (");
				qSql.append("SELECT SUM(d.t_value) AS gold,u.t_id,u.t_idcard,u.t_nickName,u.t_handImg,u.t_phone ");
				qSql.append(
						"FROM t_wallet_detail d LEFT JOIN  t_user u ON d.t_user_id=u.t_id WHERE d.t_change_type = 0 AND d.t_change_category  BETWEEN 1 AND 6 AND u.t_disable = 0 ");
				qSql.append("GROUP BY u.t_id ");
				qSql.append(") aa ORDER BY aa.gold DESC LIMIT ? ");

				dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql.toString(), getRetNumber(1));

				break;
			}

			for (Map<String, Object> m : dataMap) {
				if (null == m.get("t_nickName")) {
					m.put("t_nickName",
							"聊友:" + m.get("t_phone").toString().substring(m.get("t_phone").toString().length()));
				}
				m.remove("t_phone");
				m.put("gold", new BigDecimal(m.get("gold").toString()).intValue());
			}

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(dataMap);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取魅力排行榜异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 获取消费排行榜(non-Javadoc)
	 * 
	 * @see com.yiliao.service.HomePageService#getConsumeList(int, int)
	 */
	@Override
	public MessageUtil getConsumeList(int userId, int queryType) {
		try {

			StringBuffer qSql = new StringBuffer();
			qSql.append("SELECT * FROM ( ");
			qSql.append("SELECT SUM(d.t_value) AS gold,u.t_id,u.t_idcard,u.t_nickName,u.t_handImg,u.t_phone  ");
			qSql.append(
					"FROM t_wallet_detail d LEFT JOIN  t_user u ON d.t_user_id=u.t_id WHERE d.t_change_type = 1 AND (d.t_change_category !=8 AND d.t_change_category  BETWEEN 1 AND 9 ) AND u.t_disable = 0 ");
			qSql.append(" AND  d.t_change_time BETWEEN ? AND ? ");
			qSql.append("GROUP BY u.t_id ");
			qSql.append(") aa ORDER BY aa.gold DESC LIMIT ?");

			List<Map<String, Object>> dataMap = null;

			switch (queryType) {
			case 1:

				String lastDay = DateUtils.getYesterday(new Date());

				dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql.toString(), lastDay + " 00:00:00",
						lastDay + " 23:59:59", getRetNumber(2));

				break;
			case 2:

				Map<String, String> lastWeek = DateUtils.getLastWeek(new Date());

				dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql.toString(),
						lastWeek.get("monday") + " 00:00:00", lastWeek.get("sunday") + " 23:59:59", getRetNumber(2));

				break;
			case 3:

				String month = DateUtils.getLastMonth(new Date());

				dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql.toString(),
						DateUtils.getFirstDayOfMonth(0, Integer.parseInt(month)),
						DateUtils.getLastDayOfMonth(0, Integer.parseInt(month)), getRetNumber(2));

				break;
			default:

				qSql = new StringBuffer();

				qSql.append("SELECT * FROM ( ");
				qSql.append("SELECT SUM(d.t_value) AS gold,u.t_id,u.t_idcard,u.t_nickName,u.t_handImg,u.t_phone ");
				qSql.append(
						"FROM t_wallet_detail d LEFT JOIN  t_user u ON d.t_user_id=u.t_id WHERE d.t_change_type = 1 AND (d.t_change_category !=8 AND d.t_change_category  BETWEEN 1 AND 9 ) AND u.t_disable = 0 ");
				qSql.append("GROUP BY u.t_id ");
				qSql.append(") aa ORDER BY aa.gold DESC LIMIT ? ");

				dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql.toString(), getRetNumber(2));

				break;
			}

			// 处理用户没有昵称的数据
			for (Map<String, Object> m : dataMap) {
				if (null == m.get("t_nickName")) {
					m.put("t_nickName",
							"聊友:" + m.get("t_phone").toString().substring(m.get("t_phone").toString().length() - 4));
				}
				m.remove("t_phone");
				m.put("gold", new BigDecimal(m.get("gold").toString()).intValue());
			}

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(dataMap);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取消费排行榜异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 获取豪礼排行榜(non-Javadoc)
	 * 
	 * @see com.yiliao.service.HomePageService#getCourtesyList(int, int)
	 */
	@Override
	public MessageUtil getCourtesyList(int userId, int queryType) {
		try {

			StringBuffer qSql = new StringBuffer();
			qSql.append("SELECT * FROM ( ");
			qSql.append("SELECT SUM(d.t_value) AS gold,u.t_id,u.t_idcard,u.t_nickName,u.t_handImg,u.t_phone  ");
			qSql.append(
					"FROM t_wallet_detail d LEFT JOIN  t_user u ON d.t_user_id=u.t_id WHERE d.t_change_type = 0 AND (d.t_change_category =7 OR d.t_change_category  = 9 ) AND u.t_disable = 0 ");
			qSql.append(" AND  d.t_change_time BETWEEN ? AND ? ");
			qSql.append(" GROUP BY u.t_id ");
			qSql.append(") aa ORDER BY aa.gold DESC LIMIT ?");
			List<Map<String, Object>> dataMap = null;
			switch (queryType) {
			case 1:

				String lastDay = DateUtils.getYesterday(new Date());
				dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql.toString(), lastDay + " 00:00:00",
						lastDay + " 23:59:59", getRetNumber(3));
				break;
			case 2:

				Map<String, String> lastWeek = DateUtils.getLastWeek(new Date());

				dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql.toString(),
						lastWeek.get("monday") + " 00:00:00", lastWeek.get("sunday") + " 23:59:59", getRetNumber(3));

				break;
			case 3:

				String month = DateUtils.getLastMonth(new Date());

				dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql.toString(),
						DateUtils.getFirstDayOfMonth(0, Integer.parseInt(month)),
						DateUtils.getLastDayOfMonth(0, Integer.parseInt(month)), getRetNumber(3));

				break;
			default:

				qSql = new StringBuffer();

				qSql.append("SELECT * FROM ( ");
				qSql.append("SELECT SUM(d.t_value) AS gold,u.t_id,u.t_idcard,u.t_nickName,u.t_handImg,u.t_phone ");
				qSql.append(
						"FROM t_wallet_detail d LEFT JOIN  t_user u ON d.t_user_id=u.t_id WHERE d.t_change_type = 0 AND (d.t_change_category =7 OR d.t_change_category  = 9 ) AND u.t_disable = 0 ");
				qSql.append("GROUP BY u.t_id ");
				qSql.append(") aa ORDER BY aa.gold DESC LIMIT ? ");

				dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql.toString(), getRetNumber(3));

				break;
			}

			// 处理用户没有昵称的数据
			for (Map<String, Object> m : dataMap) {
				if (null == m.get("t_nickName")) {
					m.put("t_nickName",
							"聊友:" + m.get("t_phone").toString().substring(m.get("t_phone").toString().length()));
				}
				m.remove("t_phone");
				m.put("gold", new BigDecimal(m.get("gold").toString()).intValue());
			}

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(dataMap);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取豪礼排行榜异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/**
	 * 获取返回多少条数据
	 * 
	 * @param type 1.魅力榜 2.消费榜 3.豪礼榜
	 * @return
	 */
	public int getRetNumber(int type) {

		String qSql = " SELECT * FROM t_ranking_control ";

		List<Map<String, Object>> dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql);

		switch (type) {
		case 1:
			return Integer.parseInt(dataMap.get(0).get("t_charm_number").toString());
		case 2:
			return Integer.parseInt(dataMap.get(0).get("t_consumption_number").toString());
		default:
			return Integer.parseInt(dataMap.get(0).get("t_courtesy_number").toString());
		}
	}

	@Override
	public MessageUtil getAnchorProfitDetail(int userId, int queryType) {
		try {

			StringBuffer qSql = new StringBuffer();
			qSql.append(
					"SELECT SUM(t_value) AS gold,t_change_category FROM t_wallet_detail WHERE t_user_id = ? AND t_change_type = 0 AND  t_change_category = 1 AND t_change_time BETWEEN ? AND ?  GROUP BY t_user_id ");
			qSql.append("UNION ");
			qSql.append(
					"SELECT SUM(t_value) AS gold,t_change_category FROM t_wallet_detail WHERE t_user_id = ? AND t_change_type = 0 AND  t_change_category = 2 AND t_change_time BETWEEN ? AND ? GROUP BY t_user_id ");
			qSql.append("UNION ");
			qSql.append(
					"SELECT SUM(t_value) AS gold,t_change_category FROM t_wallet_detail WHERE t_user_id = ? AND t_change_type = 0 AND  t_change_category = 3 AND t_change_time BETWEEN ? AND ? GROUP BY t_user_id ");
			qSql.append("UNION ");
			qSql.append(
					"SELECT SUM(t_value) AS gold,t_change_category FROM t_wallet_detail WHERE t_user_id = ? AND t_change_type = 0 AND  t_change_category = 4 AND t_change_time BETWEEN ? AND ? GROUP BY t_user_id ");
			qSql.append("UNION ");
			qSql.append(
					"SELECT SUM(t_value) AS gold,t_change_category FROM t_wallet_detail WHERE t_user_id = ? AND t_change_type = 0 AND  t_change_category = 5 AND t_change_time BETWEEN ? AND ? GROUP BY t_user_id ");
			qSql.append("UNION ");
			qSql.append(
					"SELECT SUM(t_value) AS gold,t_change_category FROM t_wallet_detail WHERE t_user_id = ? AND t_change_type = 0 AND  t_change_category = 6 AND t_change_time BETWEEN ? AND ? GROUP BY t_user_id ");

			List<Map<String, Object>> dataList = null;

			switch (queryType) {
			case 1:
				String lastDay = DateUtils.getYesterday(new Date());
				dataList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql.toString(), userId,
						lastDay + " 00:00:00", lastDay + " 23:59:59", userId, lastDay + " 00:00:00",
						lastDay + " 23:59:59", userId, lastDay + " 00:00:00", lastDay + " 23:59:59", userId,
						lastDay + " 00:00:00", lastDay + " 23:59:59", userId, lastDay + " 00:00:00",
						lastDay + " 23:59:59", userId, lastDay + " 00:00:00", lastDay + " 23:59:59");

				break;
			case 2:

				Map<String, String> lastWeek = DateUtils.getLastWeek(new Date());

				dataList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql.toString(), userId,
						lastWeek.get("monday") + " 00:00:00", lastWeek.get("sunday") + " 23:59:59", userId,
						lastWeek.get("monday") + " 00:00:00", lastWeek.get("sunday") + " 23:59:59", userId,
						lastWeek.get("monday") + " 00:00:00", lastWeek.get("sunday") + " 23:59:59", userId,
						lastWeek.get("monday") + " 00:00:00", lastWeek.get("sunday") + " 23:59:59", userId,
						lastWeek.get("monday") + " 00:00:00", lastWeek.get("sunday") + " 23:59:59", userId,
						lastWeek.get("monday") + " 00:00:00", lastWeek.get("sunday") + " 23:59:59");

				break;
			case 3:
				String month = DateUtils.getLastMonth(new Date());

				dataList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql.toString(), userId,
						DateUtils.getFirstDayOfMonth(new Date().getYear(), Integer.parseInt(month)),
						DateUtils.getLastDayOfMonth(new Date().getYear(), Integer.parseInt(month)), userId,
						DateUtils.getFirstDayOfMonth(new Date().getYear(), Integer.parseInt(month)),
						DateUtils.getLastDayOfMonth(new Date().getYear(), Integer.parseInt(month)), userId,
						DateUtils.getFirstDayOfMonth(new Date().getYear(), Integer.parseInt(month)),
						DateUtils.getLastDayOfMonth(new Date().getYear(), Integer.parseInt(month)), userId,
						DateUtils.getFirstDayOfMonth(new Date().getYear(), Integer.parseInt(month)),
						DateUtils.getLastDayOfMonth(new Date().getYear(), Integer.parseInt(month)), userId,
						DateUtils.getFirstDayOfMonth(new Date().getYear(), Integer.parseInt(month)),
						DateUtils.getLastDayOfMonth(new Date().getYear(), Integer.parseInt(month)), userId,
						DateUtils.getFirstDayOfMonth(new Date().getYear(), Integer.parseInt(month)),
						DateUtils.getLastDayOfMonth(new Date().getYear(), Integer.parseInt(month)));

				break;

			default:

				qSql = new StringBuffer();
				qSql.append(
						"SELECT SUM(t_value) AS gold,t_change_category FROM t_wallet_detail WHERE t_user_id = ? AND t_change_type = 0 AND  t_change_category = 1 GROUP BY t_user_id ");
				qSql.append("UNION ");
				qSql.append(
						"SELECT SUM(t_value) AS gold,t_change_category FROM t_wallet_detail WHERE t_user_id = ? AND t_change_type = 0 AND  t_change_category = 2 GROUP BY t_user_id ");
				qSql.append("UNION ");
				qSql.append(
						"SELECT SUM(t_value) AS gold,t_change_category FROM t_wallet_detail WHERE t_user_id = ? AND t_change_type = 0 AND  t_change_category = 3 GROUP BY t_user_id ");
				qSql.append("UNION ");
				qSql.append(
						"SELECT SUM(t_value) AS gold,t_change_category FROM t_wallet_detail WHERE t_user_id = ? AND t_change_type = 0 AND  t_change_category = 4 GROUP BY t_user_id ");
				qSql.append("UNION ");
				qSql.append(
						"SELECT SUM(t_value) AS gold,t_change_category FROM t_wallet_detail WHERE t_user_id = ? AND t_change_type = 0 AND  t_change_category = 5 GROUP BY t_user_id ");
				qSql.append("UNION ");
				qSql.append(
						"SELECT SUM(t_value) AS gold,t_change_category FROM t_wallet_detail WHERE t_user_id = ? AND t_change_type = 0 AND  t_change_category = 6 GROUP BY t_user_id ");

				dataList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql.toString(), userId, userId, userId,
						userId, userId, userId);

				break;
			}

			List<Integer> arr = Arrays.asList(WalletDetail.CHANGE_CATEGORY_TEXT, WalletDetail.CHANGE_CATEGORY_VIDEO,
					WalletDetail.CHANGE_CATEGORY_PRIVATE_PHOTO, WalletDetail.CHANGE_CATEGORY_PRIVATE_VIDEO,
					WalletDetail.CHANGE_CATEGORY_PHONE, WalletDetail.CHANGE_CATEGORY_WEIXIN);

			// 迭代 判断数据是否存在
			for (Integer in : arr) {
				boolean isOk = false;
				for (Map<String, Object> m : dataList) {
					if (Integer.parseInt(m.get("t_change_category").toString()) == in) {
						isOk = true;
					}
					m.put("gold", new BigDecimal(m.get("gold").toString()).intValue());
				}
				if (!isOk) {
					dataList.add(new HashMap<String, Object>() {
						{
							put("gold", 0);
							put("t_change_category", in);
						}
					});
				}
			}

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(dataList);
		} catch (Exception e) {
			logger.error("获取主播个人收益明细异常!{}", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getStyleSetUp() {
		try {

			String qSql = " SELECT t_mark FROM t_style_setup WHERE t_state = 1 ";

			List<Map<String, Object>> dataList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql);

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(dataList.isEmpty() ? null : dataList.get(0));

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取风格异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getHomeNominateList(int userId, int page) {
		try {

			StringBuffer homeSql = new StringBuffer();
			homeSql.append("SELECT * FROM (");
			homeSql.append(" SELECT u.t_id,u.t_cover_img,a.t_state,u.t_handImg,u.t_nickName,u.t_age,u.t_city,u.t_vocation,u.t_autograph,s.t_video_gold,AVG(IFNULL(e.t_score,5)) AS t_score ,'1' AS t_user_type,sp.t_is_nominate,sp.t_sort  ");
			homeSql.append(" FROM t_user u LEFT JOIN t_anchor a ON a.t_user_id = u.t_id LEFT JOIN t_user_evaluation e ON e.t_user_id = u.t_id ");
			homeSql.append(" LEFT JOIN t_certification c ON c.t_user_id = u.t_id  LEFT JOIN t_anchor_setup s ON  s.t_user_id = u.t_id");
			homeSql.append(" LEFT JOIN t_spread sp ON sp.t_user_id = u.t_id  LEFT JOIN t_anchor ans ON ans.t_user_id = u.t_id ");
			homeSql.append(" WHERE u.t_role = 1 AND ans.t_state != 2 AND u.t_sex = 0 AND u.t_disable = 0  AND u.t_cover_img is not null AND c.t_certification_type =1 ");
			homeSql.append(" AND sp.t_is_nominate = 1 ");
			homeSql.append(" GROUP BY u.t_id ");
			homeSql.append(" UNION ");
			homeSql.append(" SELECT u.t_id,u.t_cover_img,a.t_state,u.t_handImg,u.t_nickName,u.t_age,u.t_city,u.t_vocation,u.t_autograph,s.t_video_gold,AVG(IFNULL(e.t_score,5)) AS t_score ,'2' AS t_user_type,sp.t_is_nominate,sp.t_sort ");
			homeSql.append(" FROM t_user u LEFT JOIN t_anchor a ON a.t_user_id = u.t_id LEFT JOIN t_user_evaluation e ON e.t_user_id = u.t_id  ");
			homeSql.append(" LEFT JOIN t_virtual v ON v.t_user_id = u.t_id  LEFT JOIN t_anchor_setup s ON  s.t_user_id = u.t_id");
			homeSql.append(" LEFT JOIN t_spread sp ON sp.t_user_id = u.t_id LEFT JOIN t_anchor ans ON ans.t_user_id = u.t_id  ");
			homeSql.append("  WHERE u.t_role = 1 AND ans.t_state != 2  AND u.t_sex = 0  AND u.t_disable = 0  AND u.t_cover_img is not null  AND v.t_user_id IS NOT NULL ");
			homeSql.append(" AND sp.t_is_nominate = 1 ");
			homeSql.append(" GROUP BY u.t_id ");
			homeSql.append(" ) AS  aa  WHERE aa.t_id !=0 ORDER BY aa.t_sort ASC,aa.t_state ASC,aa.t_user_type ASC, aa.t_score DESC ");

			List<Map<String, Object>> dataList = this.getQuerySqlList(homeSql.toString());
		 
			// 计算评分
			for (Map<String, Object> m : dataList) {
				// 得到该主播是否存在免费的公共视频
				Map<String, Object> userVideo = this.getMap(
						"SELECT count(t_id) AS total  FROM t_album WHERE t_user_id = ? AND t_file_type=1 AND t_is_private = 0 AND t_is_del = 0 AND  t_auditing_type = 1 ",
						Integer.parseInt(m.get("t_id").toString()));
				// 是否存在公共视频
				m.put("t_is_public", Integer.parseInt(userVideo.get("total").toString()) > 0 ? 1 : 0);
				m.put("t_score", avgScore(Integer.parseInt(m.get("t_id").toString())));
			}

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(new HashMap<String,Object>(){{
				put("data", dataList);
			}});

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取推荐主播列表异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getTryCompereList(int userId, int page) {
		try {
			StringBuffer homeSql = new StringBuffer();
			homeSql.append("SELECT * FROM (");
			homeSql.append(" SELECT u.t_id,u.t_cover_img,a.t_state,u.t_handImg,u.t_nickName,u.t_age,");
			homeSql.append(" u.t_city,s.t_video_gold,AVG(IFNULL(e.t_score,5)) AS t_score ,'1' AS t_user_type ");
			homeSql.append(" FROM t_user u LEFT JOIN t_anchor a ON a.t_user_id = u.t_id ");
			homeSql.append(" LEFT JOIN t_user_evaluation e ON e.t_user_id = u.t_id ");
			homeSql.append(" LEFT JOIN t_certification c ON c.t_user_id = u.t_id  ");
			homeSql.append(" LEFT JOIN t_anchor_setup s ON  s.t_user_id = u.t_id ");
			homeSql.append(" LEFT JOIN t_free_anthor f ON  f.t_user_id = u.t_id ");
			homeSql.append(" LEFT JOIN t_virtual v ON v.t_user_id = u.t_id ");
			// AND u.t_onLine = 0
			homeSql.append(
					" WHERE u.t_role = 1 AND u.t_sex = 0 AND u.t_disable = 0  AND u.t_cover_img is not null AND (c.t_certification_type =1 OR v.t_user_id IS NOT NULL) AND f.t_id is not null ");
			homeSql.append(
					"  AND s.t_video_gold <= ( SELECT e.t_gold FROM t_enroll e ORDER BY e.t_gold DESC LIMIT 1) ");
			homeSql.append(" GROUP BY u.t_id ");
			homeSql.append(" ) AS  aa   ORDER BY aa.t_state ASC,aa.t_user_type ASC, aa.t_score DESC LIMIT ?,10 ;");

			List<Map<String, Object>> dataList = this.getQuerySqlList(homeSql.toString(), (page - 1) * 10);

			StringBuffer countSql = new StringBuffer();
			countSql.append("SELECT count(aa.t_id) as total FROM (");
			countSql.append(" SELECT u.t_id,u.t_cover_img,a.t_state,u.t_handImg,u.t_nickName,u.t_age,u.t_city,");
			countSql.append(" AVG(IFNULL(e.t_score,5)) AS t_score ,'1' AS t_user_type ");
			countSql.append(" FROM t_user u LEFT JOIN t_anchor a ON a.t_user_id = u.t_id ");
			countSql.append(" LEFT JOIN t_user_evaluation e ON e.t_user_id = u.t_id ");
			countSql.append(" LEFT JOIN t_certification c ON c.t_user_id = u.t_id ");
			countSql.append(" LEFT JOIN t_anchor_setup s ON  s.t_user_id = u.t_id ");
			countSql.append(" LEFT JOIN t_free_anthor f ON  f.t_user_id = u.t_id ");
			countSql.append(" LEFT JOIN t_virtual v ON v.t_user_id = u.t_id ");
			countSql.append(
					" WHERE u.t_role = 1  AND u.t_sex = 0 AND u.t_disable = 0 AND u.t_cover_img is not null AND (c.t_certification_type =1 OR v.t_user_id IS NOT NULL) AND f.t_id is not null ");
			countSql.append(
					" AND s.t_video_gold <= ( SELECT e.t_gold FROM t_enroll e ORDER BY e.t_gold DESC LIMIT 1) ");
			countSql.append(" GROUP BY u.t_id ");
			countSql.append(" ) AS  aa ;");

			Map<String, Object> totalMap = this.getMap(countSql.toString());

			int pageCount = Integer.parseInt(totalMap.get("total").toString()) % 10 == 0
					? Integer.parseInt(totalMap.get("total").toString()) / 10
					: Integer.parseInt(totalMap.get("total").toString()) / 10 + 1;
			// compute avg score
			for (Map<String, Object> m : dataList) {
				// get compere whether exist public video
				Map<String, Object> userVideo = this.getMap(
						"SELECT count(t_id) AS total  FROM t_album WHERE t_user_id = ? AND t_file_type=1 AND t_is_private = 0 AND t_is_del = 0 AND t_auditing_type = 1 ",
						Integer.parseInt(m.get("t_id").toString()));
				m.put("t_is_public", Integer.parseInt(userVideo.get("total").toString()) > 0 ? 1 : 0);
				m.put("t_score", avgScore(Integer.parseInt(m.get("t_id").toString())));
			}

			mu = new MessageUtil(1,new HashMap<String,Object>(){{
				put("pageCount", pageCount);
				put("data", dataList);
			}});

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取试看主播列表异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getNewCompereList(int userId, int page) {
		try {
			StringBuffer homeSql = new StringBuffer();
			homeSql.append("SELECT * FROM (");
			homeSql.append(" SELECT u.t_id,u.t_cover_img,a.t_state,u.t_handImg,u.t_nickName,u.t_age,u.t_autograph,");
			homeSql.append(" u.t_city,s.t_video_gold,AVG(IFNULL(e.t_score,5)) AS t_score ,'1' AS t_user_type ");
			homeSql.append(" FROM t_user u LEFT JOIN t_anchor a ON a.t_user_id = u.t_id ");
			homeSql.append(" LEFT JOIN t_user_evaluation e ON e.t_user_id = u.t_id ");
			homeSql.append(" LEFT JOIN t_certification c ON c.t_user_id = u.t_id  ");
			homeSql.append(" LEFT JOIN t_anchor_setup s ON  s.t_user_id = u.t_id ");
			homeSql.append(
					" WHERE u.t_role = 1  AND u.t_sex = 0 AND u.t_disable = 0  AND u.t_cover_img is not null AND c.t_certification_type =1 ");
			homeSql.append(" AND u.t_create_time > '").append(DateUtils.getSevenDaysBefore()).append("'");
			homeSql.append(" GROUP BY u.t_id ");
			homeSql.append(" ) AS  aa   ORDER BY aa.t_state ASC,aa.t_user_type ASC, aa.t_score DESC LIMIT ?,10 ;");

			List<Map<String, Object>> dataList = this.getQuerySqlList(homeSql.toString(), (page - 1) * 10);

			StringBuffer countSql = new StringBuffer();
			countSql.append("SELECT count(aa.t_id) as total FROM (");
			countSql.append(" SELECT u.t_id,u.t_cover_img,a.t_state,u.t_handImg,u.t_nickName,u.t_age,u.t_city,");
			countSql.append(" AVG(IFNULL(e.t_score,5)) AS t_score ,'1' AS t_user_type ");
			countSql.append(" FROM t_user u LEFT JOIN t_anchor a ON a.t_user_id = u.t_id ");
			countSql.append(" LEFT JOIN t_user_evaluation e ON e.t_user_id = u.t_id ");
			countSql.append(" LEFT JOIN t_certification c ON c.t_user_id = u.t_id ");
			countSql.append(
					" WHERE u.t_role = 1  AND u.t_sex = 0 AND u.t_disable = 0 AND u.t_cover_img is not null AND c.t_certification_type =1 ");
			countSql.append(" AND u.t_create_time > '").append(DateUtils.getSevenDaysBefore()).append("'");
			countSql.append(" GROUP BY u.t_id ");
			countSql.append(" ) AS  aa ;");

			Map<String, Object> totalMap = this.getMap(countSql.toString());

			int pageCount = Integer.parseInt(totalMap.get("total").toString()) % 10 == 0
					? Integer.parseInt(totalMap.get("total").toString()) / 10
					: Integer.parseInt(totalMap.get("total").toString()) / 10 + 1;
			// compute avg score
			for (Map<String, Object> m : dataList) {
				// get compere whether exist public video
				Map<String, Object> userVideo = this.getMap(
						"SELECT count(t_id) AS total  FROM t_album WHERE t_user_id = ? AND t_file_type=1 AND t_is_private = 0 AND t_is_del = 0 AND t_auditing_type = 1 ",
						Integer.parseInt(m.get("t_id").toString()));
				m.put("t_is_public", Integer.parseInt(userVideo.get("total").toString()) > 0 ? 1 : 0);
				m.put("t_score", avgScore(Integer.parseInt(m.get("t_id").toString())));
			}

			mu = new MessageUtil(1, new HashMap<String,Object>() {{
				put("pageCount", pageCount);
				put("data", dataList);
			}});

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取新主播列表异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getIosBannerList() {
		try {

			String sql = "SELECT t_img_url,t_link_url FROM t_banner WHERE t_is_enable = 0 AND t_type = 1 ";

			List<Map<String, Object>> findBySQLTOMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql);

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(findBySQLTOMap);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取banner列表异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

}
