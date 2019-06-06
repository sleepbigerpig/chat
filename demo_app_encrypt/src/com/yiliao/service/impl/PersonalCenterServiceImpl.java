package com.yiliao.service.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import com.yiliao.domain.MessageEntity;
import com.yiliao.domain.VideoSignature;
import com.yiliao.domain.WalletDetail;
import com.yiliao.evnet.PushMesgEvnet;
import com.yiliao.service.PersonalCenterService;
import com.yiliao.util.AcThread;
import com.yiliao.util.CheckImgUtil;
import com.yiliao.util.DateUtils;
import com.yiliao.util.KeyWordUtil;
import com.yiliao.util.MessageUtil;

@Service("personalCenterService")
public class PersonalCenterServiceImpl extends ICommServiceImpl implements PersonalCenterService {

	private MessageUtil mu = null;

	/**
	 * 查询主页数据
	 */
	@Override
	public MessageUtil getPersonalCenter(int userId) {

		try {

			logger.info("去数据库中查询{}的用户资料.", userId);

			// 查询头像 和昵称和用户金币
			Map<String, Object> userMap = this.getMap(
					"SELECT u.t_nickName AS nickName,u.t_phone,u.t_sex,u.t_age,u.t_handImg AS handImg,u.t_role,u.t_autograph,u.t_is_vip,u.t_is_not_disturb,SUM(b.t_profit_money+b.t_recharge_money+b.t_share_money) AS amount,SUM(b.t_profit_money+b.t_share_money) AS extractGold,u.t_idcard FROM t_user u LEFT JOIN t_balance b ON (u.t_id = b.t_user_id) WHERE u.t_id =  ?",
					userId);

			userMap.put("amount",
					new BigDecimal(null == userMap.get("amount") ? "0" : userMap.get("amount").toString()).intValue());

			ExecutorService pool = Executors.newFixedThreadPool(20);
			// 是否是VIP
			List<Map<String, Object>> isVip = new ArrayList<>();
			pool.submit(new AcThread(2, isVip,
					"SELECT DATE_FORMAT(t_end_time,'%Y-%m-%d %T') AS endTime FROM t_vip WHERE t_user_id = ?", userId));

			List<Map<String, Object>> gouidMap = new ArrayList<>();
			pool.submit(new AcThread(2, gouidMap, "SELECT * FROM t_guild WHERE t_user_id  = ? ", userId));

			// 获取用户是否加入了公会
			List<Map<String, Object>> applyGulid = new ArrayList<>();
			pool.submit(new AcThread(2, applyGulid,
					"SELECT g.t_guild_name FROM t_anchor_guild a LEFT JOIN t_guild g ON g.t_id = a.t_guild_id WHERE a.t_anchor_id = ?  ",
					userId));

			// cps推广联盟
			List<Map<String, Object>> cpsList = new ArrayList<>();
			pool.submit(new AcThread(2, cpsList, "SELECT t_audit_status FROM t_cps WHERE t_user_id = ?", userId));

			StringBuffer total = new StringBuffer();
			total.append("SELECT COUNT(t_id) AS total,'1' AS total_type FROM t_album WHERE t_user_id = ? ");
			total.append("UNION ");
			total.append(
					"SELECT COUNT(t_id) AS total,'2' AS total_type FROM t_dynamic WHERE t_user_id = ? AND t_is_del = 0 ");
			total.append("UNION ");
			total.append("SELECT COUNT(t_id) AS total,'3' AS total_type FROM t_follow WHERE t_follow_id =? ");

			List<Map<String, Object>> tottal_data = this.getQuerySqlList(total.toString(), userId, userId, userId);

			tottal_data.forEach(s -> {
				if (s.get("total_type").toString().equals("1")) {
					userMap.put("albumCount", s.get("total"));
				}
				if (s.get("total_type").toString().equals("2")) {
					userMap.put("dynamCount", s.get("total"));
				}
				if (s.get("total_type").toString().equals("3")) {
					userMap.put("followCount", s.get("total"));
				}
			});

			pool.shutdown();

			while (!pool.isTerminated()) {
				logger.info("--获取个人接口异步查询等待汇总--");
				Thread.sleep(100);
			}

			// 判断是否用户是否开通VIP
			if (null == isVip || isVip.isEmpty()) {
				userMap.put("endTime", "");
			} else {
				userMap.put("endTime", isVip.get(0).get("endTime"));
			}

			// 判断是否管理公会
			if (null == gouidMap || gouidMap.isEmpty()) {
				userMap.put("isGuild", 0);
			} else {
				userMap.put("guildName", gouidMap.get(0).get("t_guild_name"));

				// 公会状态 0 1 2 isGuild 1:审核中 2.已通过 3.已下架
				if (gouidMap.get(0).get("t_examine").toString().equals("0"))
					userMap.put("isGuild", 1);
				else if (gouidMap.get(0).get("t_examine").toString().equals("1"))
					userMap.put("isGuild", 2);
				else
					userMap.put("isGuild", 3);
			}
			// 判断是否加入了公会
			if (null == applyGulid || applyGulid.isEmpty()) {
				userMap.put("isApplyGuild", 0);
			} else {
				// 获取该用户是否加入公会
				userMap.put("guildName", applyGulid.get(0).get("t_guild_name"));
				userMap.put("isApplyGuild", 1);
			}

			// 获取用户是否是cps推广联盟主
			if (cpsList.isEmpty()) {
				userMap.put("isCps", -1);
			} else {
				int state = (Integer) cpsList.get(0).get("t_audit_status");
				userMap.put("isCps", state + 1);
			}

			// 获取当前用户的徒弟数
			Map<String, Object> spprenticeMap = this
					.getMap("SELECT COUNT(t_id) AS spprentice FROM t_user WHERE t_referee = ?", userId);
			// 获取徒孙
			Map<String, Object> discipleMap = this.getMap(
					"SELECT COUNT(u.t_id) AS twoSpreadCount FROM t_user u LEFT JOIN t_user s ON u.t_referee = s.t_id WHERE s.t_referee = ?;",
					userId);

			userMap.put("spprentice", Integer.parseInt(spprenticeMap.get("spprentice").toString())
					+ Integer.parseInt(discipleMap.get("twoSpreadCount").toString()));

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(userMap);

		} catch (Exception e) {
			e.printStackTrace();
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 查询钱包明细 1.收入明细 2.支出明细 3.充值明细 4.提现明细 (non-Javadoc)
	 * 
	 * @see com.yiliao.service.app.PersonalCenterService#getWalletDetail(int, int,
	 * java.lang.String)
	 */
	@Override
	public MessageUtil getWalletDetail(int queyType, int userId, int year, int month, int state, int page) {
		String querySql = "";
		try {
			switch (queyType) {
			case 1: // 1.收入明细

				String countSql = " SELECT count(*) AS total,SUM(money) totalMoney  FROM ( SELECT DATE_FORMAT(f.t_change_time,'%d') AS tTime,sum(f.t_value) AS money FROM t_wallet_detail f WHERE f.t_user_id = ?  AND f.t_change_type = 0 AND f.t_value >=1 AND f.t_change_time BETWEEN ? AND ? GROUP BY tTime) aa ";

				Map<String, Object> countMap = this.getMap(countSql, userId, DateUtils.getFirstDayOfMonth(year, month),
						DateUtils.getLastDayOfMonth(year, month));

				int pageCount = Integer.parseInt(countMap.get("total").toString()) % 10 == 0
						? Integer.parseInt(countMap.get("total").toString()) / 10
						: Integer.parseInt(countMap.get("total").toString()) / 10 + 1;

				querySql = "SELECT * FROM ( SELECT DATE_FORMAT(f.t_change_time,'%d') AS tTime,sum(f.t_value) AS totalMoney FROM t_wallet_detail f WHERE f.t_user_id = ? AND f.t_change_type = 0 AND f.t_value >=1   AND f.t_change_time BETWEEN ? AND ? GROUP BY tTime ) AS tba ORDER BY tba.tTime DESC limit ? ,10";
				List<Map<String, Object>> income = this.getQuerySqlList(querySql, userId,
						DateUtils.getFirstDayOfMonth(year, month), DateUtils.getLastDayOfMonth(year, month),
						(page - 1) * 10);

				for (Map<String, Object> map : income) {
					map.put("totalMoney", new BigDecimal(map.get("totalMoney").toString()).intValue());
				}

				mu = new MessageUtil(1, new HashMap<String, Object>() {
					{
						put("pageCount", pageCount);
						put("data", income);
						put("monthTotal", null == countMap.get("totalMoney") ? 0
								: new BigDecimal(countMap.get("totalMoney").toString()).intValue());
					}
				});
				break;
			case 2: // 2.支出明细

				countSql = " SELECT count(*) AS total,SUM(money) totalMoney  FROM ( SELECT DATE_FORMAT(f.t_change_time,'%d') AS tTime,sum(f.t_value) AS money FROM t_wallet_detail f WHERE f.t_user_id = ?  AND f.t_change_type = 1 AND f.t_change_time BETWEEN ? AND ? GROUP BY tTime) aa";

				countMap = this.getMap(countSql, userId, DateUtils.getFirstDayOfMonth(year, month),
						DateUtils.getLastDayOfMonth(year, month));

				pageCount = Integer.parseInt(countMap.get("total").toString()) % 10 == 0
						? Integer.parseInt(countMap.get("total").toString()) / 10
						: Integer.parseInt(countMap.get("total").toString()) / 10 + 1;

				querySql = "SELECT * FROM ( SELECT DATE_FORMAT(f.t_change_time,'%d') AS tTime,sum(f.t_value) AS totalMoney  FROM t_wallet_detail f WHERE f.t_user_id = ? AND f.t_change_type = 1  AND f.t_change_time BETWEEN ? AND ? GROUP BY tTime ) AS tba ORDER BY tba.tTime DESC  limit ? ,10";
				List<Map<String, Object>> expenditure = this.getQuerySqlList(querySql, userId,
						DateUtils.getFirstDayOfMonth(year, month), DateUtils.getLastDayOfMonth(year, month),
						(page - 1) * 10);

				for (Map<String, Object> map : expenditure) {
					map.put("totalMoney", new BigDecimal(map.get("totalMoney").toString()).intValue());
				}
				mu = new MessageUtil(1, new HashMap<String, Object>() {
					{
						put("pageCount", pageCount);
						put("data", expenditure);
						put("monthTotal", null == countMap.get("totalMoney") ? 0
								: new BigDecimal(countMap.get("totalMoney").toString()).intValue());
					}
				});

				break;
			case 3: // 3.充值明细

				countSql = " SELECT count(*) totalCount FROM t_wallet_detail f WHERE f.t_user_id = ? AND f.t_change_type = 0 AND t_change_category = 0 AND f.t_change_time BETWEEN ? AND ?";

				countMap = this.getMap(countSql, userId, DateUtils.getFirstDayOfMonth(year, month),
						DateUtils.getLastDayOfMonth(year, month));

				pageCount = Integer.parseInt(countMap.get("totalCount").toString()) % 10 == 0
						? Integer.parseInt(countMap.get("totalCount").toString()) / 10
						: Integer.parseInt(countMap.get("totalCount").toString()) / 10 + 1;

				// 查询用户的充值明细
				querySql = "SELECT * FROM ( SELECT DATE_FORMAT(f.t_change_time,'%d') AS tTime,sum(f.t_value) AS totalMoney  FROM t_wallet_detail f WHERE f.t_user_id = ? AND f.t_change_type = 0 AND t_change_category = 0  AND f.t_change_time BETWEEN ? AND ? GROUP BY tTime ) AS tba ORDER BY tba.tTime DESC limit ? ,10";
				List<Map<String, Object>> recharge = this.getQuerySqlList(querySql, userId,
						DateUtils.getFirstDayOfMonth(year, month), DateUtils.getLastDayOfMonth(year, month),
						(page - 1) * 10);

				for (Map<String, Object> map : recharge) {
					map.put("totalMoney", new BigDecimal(map.get("totalMoney").toString()).intValue());
				}

				// 统计月充值汇总
				String monthRsql = "SELECT sum(f.t_value) AS totalMoney FROM t_wallet_detail f WHERE f.t_user_id = ? AND f.t_change_type = 0 AND f.t_change_category = 0 AND f.t_value >= 1 AND f.t_change_time BETWEEN ? AND ? ";

				Map<String, Object> monthRMap = this.getMap(monthRsql, userId,
						DateUtils.getFirstDayOfMonth(year, month), DateUtils.getLastDayOfMonth(year, month));

				mu = new MessageUtil(1, new HashMap<String, Object>() {
					{
						put("pageCount", pageCount);
						put("data", recharge);
						put("monthTotal", null == monthRMap.get("totalMoney") ? 0
								: new BigDecimal(monthRMap.get("totalMoney").toString()).intValue());
					}
				});
				break;
			case 4: // 4.提现明细

				countSql = " SELECT count(*) totalCount,SUM(f.t_money) AS totalMoney FROM t_put_forward f WHERE f.t_user_id = ? ";

				if (-1 != state) {
					countSql = countSql + " AND f.t_order_state = " + state;
				}

				countSql = countSql + " AND f.t_create_time BETWEEN ? AND ? ";

				countMap = this.getMap(countSql, userId, DateUtils.getFirstDayOfMonth(year, month),
						DateUtils.getLastDayOfMonth(year, month));

				pageCount = Integer.parseInt(countMap.get("totalCount").toString()) % 10 == 0
						? Integer.parseInt(countMap.get("totalCount").toString()) / 10
						: Integer.parseInt(countMap.get("totalCount").toString()) / 10 + 1;

				querySql = " SELECT DATE_FORMAT(f.t_create_time,'%m-%d %T') AS tTime,f.t_money,f.t_order_state,f.t_describe,fd.t_type FROM t_put_forward f LEFT JOIN t_put_forward_data fd ON f.t_data_id = fd.t_id WHERE f.t_user_id = ? ";

				if (-1 != state) {
					if (state != 0) {
						querySql = querySql + " AND f.t_order_state = " + state;
					} else {
						querySql = querySql + "AND f.t_order_state BETWEEN 0 AND 1 ";
					}
				}

				querySql = querySql + " AND f.t_create_time BETWEEN ? AND ? ORDER BY  f.t_create_time DESC limit ?,10";

				List<Map<String, Object>> findBySQLTOMap = this.getQuerySqlList(querySql, userId,
						DateUtils.getFirstDayOfMonth(year, month), DateUtils.getLastDayOfMonth(year, month),
						(page - 1) * 10);

				mu = new MessageUtil(1, new HashMap<String, Object>() {
					{
						put("pageCount", pageCount);
						put("data", findBySQLTOMap);
						put("monthTotal", null == countMap.get("totalMoney") ? 0
								: new BigDecimal(countMap.get("totalMoney").toString()).intValue());
					}
				});
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			mu = new MessageUtil(0, "程序异常!");
		} finally {
			querySql = null;
		}
		return mu;
	}

	/*
	 * 获取用户实名认证状态 (non-Javadoc)
	 * 
	 * @see
	 * com.yiliao.service.app.PersonalCenterService#getUserIsIdentification(int)
	 */
	@Override
	public MessageUtil getUserIsIdentification(int userId) {
		try {

			String sql = "SELECT t_certification_type FROM t_certification WHERE t_user_id = ?";

			List<Map<String, Object>> bySQLTOMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, userId);

			if (null == bySQLTOMap || bySQLTOMap.isEmpty()) {
				mu = new MessageUtil(-1, "未申请实名认证!");
			} else {
				mu = new MessageUtil();
				mu.setM_istatus(1);
				mu.setM_object(bySQLTOMap.get(0));
			}

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取实名认证状态异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 提交实名认证资料 (non-Javadoc)
	 * 
	 * @see com.yiliao.service.app.PersonalCenterService#submitData(int,
	 * java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public MessageUtil submitData(int userId, String t_user_photo, String t_user_video, String t_nam,
			String t_id_card) {
		try {

			// 获取该身份证好是否已经认证过了
			String sql = "SELECT * FROM t_certification WHERE t_user_id = ? ";

			List<Map<String, Object>> dateList = this.getQuerySqlList(sql, userId);

			if (null == dateList || dateList.isEmpty()) {

				sql = "INSERT INTO t_certification (t_user_id, t_user_photo, t_user_video, t_nam, t_id_card, t_certification_type, t_create_time) "
						+ "VALUES (?, ?,?,?, ?, 0,?); ";

				this.executeSQL(sql, userId, t_user_photo, t_user_video, t_nam, t_id_card,
						DateUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));

				// 把当前用户更新为主播
//				String upSql = "UPDATE t_user SET  t_role=1  WHERE t_id= ? ";
//				this.executeSQL(upSql, userId);
//
				// 查询出系统默认的收费设置
//				List<Map<String, Object>> systemSetUp = this.getFinalDao().getIEntitySQLDAO()
//						.findBySQLTOMap("SELECT t_default_text,t_default_video,t_default_phone,t_default_weixin FROM t_system_setup");
//				
//				if(!systemSetUp.isEmpty()) {
//					// 给主播插入默认的收费设置
//					this.updateAnchorChargeSetup(userId, 
//							new BigDecimal(systemSetUp.get(0).get("t_default_video").toString()),
//							new BigDecimal(systemSetUp.get(0).get("t_default_text").toString()), 
//							new BigDecimal(systemSetUp.get(0).get("t_default_phone").toString()), 
//							new BigDecimal(systemSetUp.get(0).get("t_default_weixin").toString()), 
//							null, null);
//				}			

				mu = new MessageUtil(1, "资料已提交!");

			} else {
				sql = "UPDATE t_certification SET t_user_photo = ? ,t_user_video = ?,t_nam= ?,t_id_card=?,t_certification_type = ? ,t_create_time = ? WHERE t_user_id = ? ";
				this.executeSQL(sql, t_user_photo, t_user_video, t_nam, t_id_card, 0,
						DateUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"), userId);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}提交实名认证资料异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 统计贡献值 (non-Javadoc)
	 * 
	 * @see com.yiliao.service.app.PersonalCenterService#getUserShareCount(int)
	 */
	@Override
	public MessageUtil getUserShareCount(int userId) {
		try {
			// 获取我总的推广数
			String sql = "SELECT count(*) as totalShare  FROM t_user WHERE t_referee = ?";
			Map<String, Object> shareMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql,
					userId);

			// 获取我推广的充值用户
			sql = "select count(*) as total from t_user u LEFT JOIN t_balance b on b.t_user_id = u.t_id where b.t_user_id IS NOT NULL AND u.t_referee = ? ";
			Map<String, Object> rechargeMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql,
					userId);
			shareMap.put("recharge", rechargeMap.get("total"));

			// 获取我推广的潜水用户
			sql = "select count(*) as total from t_user u LEFT JOIN t_balance b on b.t_user_id = u.t_id where b.t_user_id IS NULL AND u.t_referee = ? ";
			Map<String, Object> divingMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql,
					userId);
			shareMap.put("diving", divingMap.get("total"));
			// 统计贡献值
			sql = "SELECT SUM(t_value) as gold FROM t_wallet_detail WHERE t_user_id = ? AND t_change_type = 0 AND t_change_category = 11";
			Map<String, Object> goldMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql, userId);
			shareMap.put("gold", null == goldMap.get("gold") ? 0 : goldMap.get("gold"));

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(shareMap);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取推广数异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}

		return mu;
	}

	/*
	 * 获取我的推广列表 (non-Javadoc)
	 * 
	 * @see com.yiliao.service.app.PersonalCenterService#getShareUserList(int, int)
	 */
	@Override
	public MessageUtil getShareUserList(int userId, int page, int type) {

		try {

			List<Map<String, Object>> spreadData = null;

			int pageCount = 0;
			// 1级用户
			if (type == 1) {

				String count = "select count(u.t_id) as totalCount  from t_user u  where  u.t_referee = ?";

				Map<String, Object> countMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(count,
						userId);

				pageCount = Integer.parseInt(countMap.get("totalCount").toString()) % 10 == 0
						? Integer.parseInt(countMap.get("totalCount").toString()) / 10
						: Integer.parseInt(countMap.get("totalCount").toString()) / 10 + 1;

				StringBuffer oneSql = new StringBuffer(350);
				oneSql.append("SELECT * FROM (");
				oneSql.append(" SELECT u.t_id,u.t_handImg,u.t_nickName,u.t_phone,u.t_role, ");
				oneSql.append(
						"  DATE_FORMAT(u.t_create_time, '%Y-%m-%d') AS t_create_time,SUM(r.t_redpacket_gold) AS totalValue ");
				oneSql.append(" FROM t_user u LEFT JOIN t_redpacket_log r ON r.t_hair_userId = u.t_id ");
				oneSql.append(" WHERE ");
				oneSql.append("  u.t_referee = ? ");
				oneSql.append(" GROUP BY u.t_id ");
				oneSql.append(") aa ORDER BY aa.totalValue DESC LIMIT ?,10 ;");

				spreadData = this.getQuerySqlList(oneSql.toString(), userId, (page - 1) * 10);
			} else if (type == 2) { // 二级推广用户

				String count = "select count(u.t_id) as totalCount  from t_user u LEFT JOIN t_user s ON u.t_referee = s.t_id where  s.t_referee = ? ";

				Map<String, Object> countMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(count,
						userId);

				pageCount = Integer.parseInt(countMap.get("totalCount").toString()) % 10 == 0
						? Integer.parseInt(countMap.get("totalCount").toString()) / 10
						: Integer.parseInt(countMap.get("totalCount").toString()) / 10 + 1;

				StringBuffer twoSql = new StringBuffer(480);
				twoSql.append("SELECT * FROM (");
				twoSql.append(
						" SELECT u.t_id,u.t_handImg,u.t_nickName,u.t_phone,DATE_FORMAT(u.t_create_time,'%Y-%m-%d') AS t_create_time ");
				twoSql.append("  ,aa.totalValue FROM t_user u LEFT JOIN ( ");
				twoSql.append("   SELECT t_hair_userId,SUM(t_redpacket_gold) AS totalValue FROM t_redpacket_log ");
				twoSql.append(
						" WHERE t_receive_userId = ? AND t_redpacket_type = 1 GROUP BY t_hair_userId ) aa ON aa.t_hair_userId = u.t_id ");
				twoSql.append("LEFT JOIN t_user ul ON u.t_referee = ul.t_id ");
				twoSql.append("WHERE ul.t_referee = ? ) bb ORDER BY bb.totalValue DESC LIMIT ?,10;");

				spreadData = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(twoSql.toString(), userId, userId,
						(page - 1) * 10);
			}

			spreadData.forEach(s -> {
				if (null == s.get("t_nickName")) {
					s.put("t_nickName",
							"聊友:" + s.get("t_phone").toString().substring(s.get("t_phone").toString().length() - 4));
				}
				s.remove("t_phone");
				s.put("spreadMoney",
						null == s.get("totalValue") ? 0 : new BigDecimal(s.get("totalValue").toString()).intValue());
			});

			Map<String, Object> res = new HashMap<>();
			res.put("pageCount", pageCount);
			res.put("data", spreadData);
			mu = new MessageUtil(1, res);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取分享用户列表异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 获取潜水列表 (non-Javadoc)
	 * 
	 * @see com.yiliao.service.app.PersonalCenterService#getDivingUserList(int, int)
	 */
	@Override
	public MessageUtil getDivingUserList(int userId, int page) {
		try {

			// 获取数据
			String sql = "select u.t_handImg,u.t_nickName,u.t_create_time from t_user u LEFT JOIN t_balance b on b.t_user_id = u.t_id where b.t_user_id IS NULL AND u.t_referee = ? ORDER BY u.t_create_time DESC LIMIT ?,10";
			List<Map<String, Object>> findBySQLTOMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, userId,
					(page - 1) * 10);

			// 统计总记录书
			String countSql = "select count(*) as totalCount  from t_user u LEFT JOIN t_balance b on b.t_user_id = u.t_id where b.t_user_id IS NULL AND u.t_referee = ?";

			Map<String, Object> totalMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(countSql,
					userId);

			int pageCount = Integer.parseInt(totalMap.get("totalCount").toString()) % 10 == 0
					? Integer.parseInt(totalMap.get("totalCount").toString()) / 10
					: Integer.parseInt(totalMap.get("totalCount").toString()) / 10 + 1;

			for (Map<String, Object> m : findBySQLTOMap) {
				m.put("t_create_time",
						DateUtils.format(Timestamp.valueOf(m.get("t_create_time").toString()), "yyyy-MM-dd"));
			}
			mu = new MessageUtil(1, new HashMap<String, Object>() {
				{
					put("pageCount", pageCount);
					put("data", findBySQLTOMap);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取潜水用户列表异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/**
	 * 查看其他用户的个人资料
	 */
	@Override
	public MessageUtil getSeeUserData(int seeUserId, int coverUserId) {
		try {
			// 修改被查看人的浏览次数
			String sql = "UPDATE t_user u SET u.t_browse_sum = (u.t_browse_sum+1) WHERE u.t_id = ?";

			this.getFinalDao().getIEntitySQLDAO().executeSQL(sql, coverUserId);

			// 判断当前人今天是否浏览过了
			sql = "SELECT * FROM t_browse WHERE t_browse_user = ? AND t_cover_browse = ? AND t_create_time BETWEEN ? AND ? ";

			List<Map<String, Object>> browse = this.getQuerySqlList(sql, seeUserId, coverUserId,
					DateUtils.format(new Date()) + " 00:00:00", DateUtils.format(new Date()) + " 23:59:59");

			if (null == browse || browse.isEmpty()) {
				// 保存浏览记录
				sql = "INSERT INTO t_browse (t_browse_user, t_cover_browse, t_create_time) VALUES (?, ?, ?)";

				this.executeSQL(sql, seeUserId, coverUserId, DateUtils.format(new Date(), DateUtils.FullDatePattern));

			} else {
				sql = "UPDATE t_browse SET  t_create_time=? WHERE t_browse_id=?;";

				this.executeSQL(sql, DateUtils.format(new Date(), DateUtils.FullDatePattern),
						Integer.parseInt(browse.get(0).get("t_browse_id").toString()));
			}

			// 查询被看人的数据
			sql = "SELECT u.t_sex,u.t_handImg,u.t_age,u.t_role,u.t_onLine,u.t_idcard,u.t_nickName,u.t_vocation,u.t_weixin,u.t_phone,DATE_FORMAT(u.t_login_time,'%Y-%m-%d %T') AS t_login_time,"
					+ "u.t_height,u.t_weight,u.t_constellation,u.t_city,u.t_autograph FROM t_user u WHERE u.t_id = ? ";

			Map<String, Object> dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql,
					coverUserId);

			// 获取当前用户状态
			sql = "SELECT t_state FROM t_anchor WHERE t_user_id = ? ";

			Map<String, Object> onLineMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql,
					coverUserId);
			dataMap.put("t_onLine", onLineMap.get("t_state"));

			// 查询被看人的粉丝数
			sql = "SELECT count(*) as totalCount FROM t_follow WHERE t_cover_follow = ? ";

			Map<String, Object> totalCount = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql,
					coverUserId);

			Random random = new Random();
			int s = random.nextInt(10000) % (10000 - 5000 + 1) + 5000;

			dataMap.put("totalCount", Integer.parseInt(totalCount.get("totalCount").toString()) + s);

			// 查询当前人是否已经关注了该主播或者用户
			sql = "SELECT COUNT(*) AS follow  FROM t_follow WHERE t_cover_follow = ? AND t_follow_id = ?";

			List<Map<String, Object>> isFollow = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, coverUserId,
					seeUserId);
			// 判断当前用户是否关注
			dataMap.put("isFollow", isFollow.get(0).get("follow"));

			// 查询用户是否已经查看过微信号
			sql = "SELECT * FROM t_order WHERE t_consume_type = 6 AND t_consume = ? AND t_cover_consume = ?";
			List<Map<String, Object>> isWeixin = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, seeUserId,
					coverUserId);
			dataMap.put("isWeixin", null == isWeixin ? 0 : isWeixin.isEmpty() ? 0 : 1);

			// 查询用户是否已经查看用户的手机号
			sql = "SELECT * FROM t_order WHERE t_consume_type = 5 AND t_consume = ? AND t_cover_consume = ?";

			List<Map<String, Object>> isPhone = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, seeUserId,
					coverUserId);
			dataMap.put("isPhone", null == isPhone ? 0 : isPhone.isEmpty() ? 0 : 1);

			// 查询接听率
			sql = "SELECT t_reception FROM t_reception_rate WHERE t_user_id = ?";

			List<Map<String, Object>> list = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, coverUserId);

			// 获取主播的接听率
			dataMap.put("t_reception",
					list.isEmpty() ? "100%" : Double.valueOf((list.get(0).get("t_reception").toString())) + "%");

			// 主播标签列表
			sql = "SELECT t_user_lable_id,l.t_label_name FROM t_user_label u LEFT JOIN t_label l ON u.t_lable_id = l.t_id WHERE t_user_id = ? AND l.t_is_enable = 0";

			List<Map<String, Object>> labList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, coverUserId);

			dataMap.put("lable", labList);

			// 查询轮播图片
			sql = "SELECT  t_img_url  FROM t_cover_examine WHERE t_user_id = ? AND t_is_examine = 1";

			List<Map<String, Object>> carouselMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql,
					coverUserId);

			dataMap.put("lunbotu", carouselMap);

			// 查询视频 微信 手机号需要的金币数
			sql = "SELECT t_video_gold,t_phone_gold,t_weixin_gold,t_text_gold FROM t_anchor_setup WHERE t_user_id = ?";

			List<Map<String, Object>> dataList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, coverUserId);

			if (null != dataList && !dataList.isEmpty()) {
				dataMap.put("anchorSetup", dataList);
			}
			
			//如果当前被查看人为主播的时候 
			//须获取当前主播是否为大房间主播  是否开播
			if(dataMap.get("t_role").toString().equals("1")) {
                  
				List<Map<String, Object>> bigRooms = this.getQuerySqlList("SELECT t_is_debut,t_room_id,t_chat_room_id FROM t_big_room_man WHERE t_user_id = ?", coverUserId);
				if(null == bigRooms || bigRooms.isEmpty())
				    dataMap.put("bigRoomData", new HashMap<String, Object>());
				else 
					dataMap.put("bigRoomData", bigRooms.get(0));
			}

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(dataMap);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取用户资料异常!", seeUserId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 获取个人资料 用于用户编辑资料回显 (non-Javadoc)
	 * 
	 * @see com.yiliao.service.app.PersonalCenterService#getPersonalData(int)
	 */
	@Override
	public MessageUtil getPersonalData(int userId) {
		try {
			// 查询个人信息
			String sql = "SELECT u.t_nickName,u.t_age,u.t_handImg,u.t_weixin,u.t_phone,u.t_height,u.t_weight,u.t_constellation,u.t_city,u.t_synopsis,u.t_autograph,u.t_vocation FROM t_user u WHERE u.t_id = ?";

			Map<String, Object> userData = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql,
					userId);

			// 获取标签
			sql = "SELECT t_user_lable_id,l.t_label_name FROM t_user_label u LEFT JOIN t_label l ON u.t_lable_id = l.t_id WHERE t_user_id = ? AND l.t_is_enable = 0";

			List<Map<String, Object>> labList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, userId);

			userData.put("lable", labList);

			// 获取封面
			sql = "SELECT t_id,t_img_url,t_first,t_is_examine FROM t_cover_examine WHERE t_user_id = ?  ";

			List<Map<String, Object>> coverList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, userId);

			userData.put("coverList", coverList);

			// 获取用户充值等级
			String qSql = "SELECT SUM(t_profit_money + t_recharge_money + t_share_money) AS balance FROM t_balance WHERE t_user_id = ? ";

			Map<String, Object> banlance = this.getMap(qSql, userId);

			userData.put("goldLevel", this.goldFiles(new BigDecimal(banlance.get("balance").toString()).intValue()));

			// 获取免费视频
			List<Map<String, Object>> sqlList = this.getQuerySqlList(
					"SELECT t_video_img ,t_addres_url FROM t_album  WHERE t_file_type = 1 AND t_is_private = 0 AND t_user_id = ? LIMIT 1 ",
					userId);

			if (!sqlList.isEmpty()) {
				userData.put("t_video_img", sqlList.get(0).get("t_video_img").toString());
				userData.put("t_addres_url", sqlList.get(0).get("t_addres_url").toString());
			} else {
				userData.put("t_video_img", "");
				userData.put("t_addres_url", "");
			}

			mu = new MessageUtil();

			mu.setM_istatus(1);
			mu.setM_object(userData);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取个人资料异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/**
	 * 金币等级
	 * 
	 * @param gold
	 * @return
	 */
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

	/*
	 * 修改个人资料 (non-Javadoc)
	 * 
	 * @see com.yiliao.service.app.PersonalCenterService#updatePersonalData(int,
	 * java.lang.String, java.lang.String, int, java.lang.Double, java.lang.String,
	 * java.lang.String, java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public MessageUtil updatePersonalData(int userId, String t_nickName, String t_phone, Integer t_height,
			Double t_weight, String t_constellation, String t_city, String t_synopsis, String t_autograph,
			String t_vocation, String t_weixin, Integer t_age, String t_handImg) {
		try {

			String sql = "update t_user u set ";

			if (null != t_nickName && !"".equals(t_nickName.trim())) {
				logger.info("t_nickName ->{}", t_nickName);
				// 获取系统设置关键字过滤
				List<Map<String, Object>> querySqlList = this
						.getQuerySqlList("SELECT t_nickname_filter FROM t_system_setup");
				if (querySqlList.isEmpty() || querySqlList.get(0).isEmpty()) {
					sql = sql + " u.t_nickName = '" + t_nickName + "',";
				} else {
					String[] keyWord = querySqlList.get(0).get("t_nickname_filter").toString().split(",");
					sql = sql + " u.t_nickName = '" + KeyWordUtil.filterKeyWord(keyWord, t_nickName) + "',";
				}

			}

			if (null != t_handImg && !"".equals(t_handImg.trim())) {

				String[] str = { t_handImg };

				// 获取秘钥信息
				String qSql = " SELECT t_app_id,t_secret_id,t_secret_key,t_bucket FROM t_object_storage ";

				Map<String, Object> querySqlList = this.getMap(qSql);

				// 调用鉴黄系统 鉴定封面是否违规
				Map<String, Object> imagePorn = CheckImgUtil.imagePorn(str, querySqlList.get("t_app_id").toString(),
						querySqlList.get("t_secret_id").toString(), querySqlList.get("t_secret_key").toString(),
						querySqlList.get("t_bucket").toString());

				List<String> pornUrl = (List<String>) imagePorn.get("pornUrl");
				// 如果存在疑是违禁图片 存储到模糊图片表中 后期人工审核
				if (null != pornUrl && !pornUrl.isEmpty()) {
					String inseSql = "INSERT INTO t_vague_check (t_img_url, t_data_type, t_user_id, t_create_time) VALUES (?, ?, ?, ?)";
					this.getFinalDao().getIEntitySQLDAO().executeSQL(inseSql, pornUrl.get(1), 0, userId,
							DateUtils.format(new Date(), DateUtils.FullDatePattern));
				} else {
					sql = sql + " u.t_handImg = '" + t_handImg + "',";
				}
			}

			// 获取当前手机号是否已经存在了
			String phoneSql = "SELECT * FROM t_user  WHERE t_phone = ? AND t_id != ?";

			List<Map<String, Object>> da = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(phoneSql, t_phone,userId);

			if (null != da && !da.isEmpty()) {
				return new MessageUtil(0, "该手机号已被其他用户绑定!");
			}

			if (null != t_phone && !"".equals(t_phone.trim())) {
				sql = sql + " u.t_phone = '" + t_phone + "',";
			}
			if (null != t_height && 0 != t_height) {
				sql = sql + " u.t_height = " + t_height + ",";
			}
			if (null != t_weight && 0 != t_weight) {
				sql = sql + "u.t_weight = " + t_weight + ", ";
			}
			if (null != t_constellation && !"".equals(t_constellation.trim())) {
				sql = sql + " u.t_constellation = '" + t_constellation + "',";
			}
			if (null != t_city && !"".equals(t_city.trim())) {
				sql = sql + " u.t_city = '" + t_city + "',";
			}
			if (null != t_synopsis && !"".equals(t_synopsis.trim())) {
				sql = sql + "u.t_synopsis = '" + t_synopsis + "', ";
			}
			if (null != t_autograph && !"".equals(t_autograph.trim())) {
				sql = sql + " u.t_autograph = '" + t_autograph + "',";
			}
			if (null != t_vocation && !"".equals(t_vocation.trim())) {
				sql = sql + " u.t_vocation = '" + t_vocation + "',";
			}
			if (null != t_age && 0 != t_age) {
				sql = sql + " u.t_age = " + t_age + ",";
			}
			if (null != t_weixin && !"".equals(t_weixin.trim())) {
				sql = sql + " u.t_weixin = '" + t_weixin + "',";
			}

			if (sql.indexOf(",") > 0) {

				sql = sql.substring(0, sql.lastIndexOf(","));

				sql = sql + "  WHERE u.t_id = ?";

				logger.info("更新sql->{}", sql);

				this.getFinalDao().getIEntitySQLDAO().executeSQL(sql, userId);
			}

//			GoogleCacheUtil.userCache.invalidate(userId);
			mu = new MessageUtil(1, "编辑资料成功!");

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}修改资料异常", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 获取标签列表 (non-Javadoc)
	 * 
	 * @see com.yiliao.service.app.PersonalCenterService#getLabelList(int)
	 */
	@Override
	public MessageUtil getLabelList(int userId) {
		try {

			String sql = "SELECT l.t_id,l.t_label_name FROM t_label l WHERE l.t_sex  = (SELECT u.t_sex FROM t_user u WHERE u.t_id = ? ) AND l.t_is_enable= 0 ";

			List<Map<String, Object>> labels = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, userId);

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(labels);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取标签列表异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil delUserLabel(int labelId) {
		try {

			String sql = "DELETE FROM t_user_label WHERE t_user_lable_id = ?";

			this.getFinalDao().getIEntitySQLDAO().executeSQL(sql, labelId);

			mu = new MessageUtil(1, "删除成功!");

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("用户删除标签异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil saveUserLabel(int userId, String labelId) {
		try {

			String[] label = labelId.split(",");

			String delSql = "DELETE FROM t_user_label WHERE t_user_id= ? ";

			this.getFinalDao().getIEntitySQLDAO().executeSQL(delSql, userId);

			String sql = "INSERT INTO t_user_label (t_user_id, t_lable_id) VALUES ( ?, ?) ";

			for (int i = 0; i < label.length; i++) {
				this.getFinalDao().getIEntitySQLDAO().executeSQL(sql, userId, Integer.parseInt(label[i]));
			}

			mu = new MessageUtil(1, "保存标签成功!");

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}用户新增标签异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 获取主播收费设置 (non-Javadoc)
	 * 
	 * @see com.yiliao.service.app.PersonalCenterService#getAnchorChargeSetup(int)
	 */
	@Override
	public MessageUtil getAnchorChargeSetup(int userId) {
		try {

			String sql = "SELECT * FROM t_anchor_setup s WHERE s.t_user_id = ?";

			List<Map<String, Object>> findBySQLTOMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql,
					userId);

			if (null != findBySQLTOMap && !findBySQLTOMap.isEmpty()) {

				findBySQLTOMap.get(0).remove("t_id");
			}

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(null == findBySQLTOMap ? null : findBySQLTOMap.isEmpty() ? null : findBySQLTOMap.get(0));

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取主播收费设置", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 修改主播收费设置 (non-Javadoc)
	 * 
	 * @see
	 * com.yiliao.service.app.PersonalCenterService#updateAnchorChargeSetup(int,
	 * java.lang.Integer, java.lang.Integer, java.lang.Integer, java.lang.Integer,
	 * java.lang.Integer, java.lang.Integer)
	 */
	@Override
	public MessageUtil updateAnchorChargeSetup(int t_user_id, BigDecimal t_video_gold, BigDecimal t_text_gold,
			BigDecimal t_phone_gold, BigDecimal t_weixin_gold) {

		try {

			String query = "select count(t_id) as total from t_anchor_setup where t_user_id = ?";

			Map<String, Object> map = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(query,
					t_user_id);

			// 新增
			if (0 == Integer.parseInt(map.get("total").toString())) {

				String inSql = "INSERT INTO t_anchor_setup (t_user_id, t_video_gold, t_text_gold, t_phone_gold, t_weixin_gold) VALUES (?,?,?,?,?);";

				this.getFinalDao().getIEntitySQLDAO().executeSQL(inSql, t_user_id, t_video_gold, t_text_gold,
						t_phone_gold, t_weixin_gold);

			} else { // 修改
				String sql = "UPDATE t_anchor_setup SET ";
				if (t_video_gold.compareTo(BigDecimal.valueOf(0)) > 0) {
					// 验证主播
					// 金币数修改为10——50这个区间。主播可以选择：10,15,20,25,30,35,40,45,50.
					// 新主播最高只能设置每分钟30，提现达到100后，
					// 才有设置40的选项，提现500后才有设置成50的选项
					// 获取主播是否存在提现
//					String qSql = " SELECT SUM(t_money) AS totalMoney FROM t_put_forward  WHERE t_user_id  = ? ";
//					Map<String, Object> putMoney = this.getMap(qSql, t_user_id);
//					
//					int totalMoney = new BigDecimal(null == putMoney.get("totalMoney")?"0":putMoney.get("totalMoney").toString()).intValue();
//					
//					if((totalMoney < 100 && t_video_gold<=30 && t_video_gold > 0)
//							|| (totalMoney >=100 && totalMoney < 500 && t_video_gold <=40 && t_video_gold > 0)
//							|| (totalMoney >= 500 && t_video_gold <= 50 && t_video_gold > 0)) {

					sql = sql + " t_video_gold = " + t_video_gold + ",";
//					}else {
//						return new MessageUtil(-1, "视频聊天收费异常!");
//					}
				}
				if (t_text_gold.compareTo(BigDecimal.valueOf(0)) > 0) {
					sql = sql + " t_text_gold = " + t_text_gold + ",";
				}
				if (t_weixin_gold.compareTo(BigDecimal.valueOf(0)) > 0) {
					sql = sql + " t_weixin_gold = " + t_weixin_gold + ",";
				}

				sql = sql + " t_phone_gold = " + t_phone_gold + ",";

				if (sql.indexOf(",") > 0) {

					sql = sql.substring(0, sql.lastIndexOf(","));

					sql = sql + "  WHERE t_user_id = ?";

					this.executeSQL(sql, t_user_id);
				}
			}

			mu = new MessageUtil(1, "修改收费设置成功!");

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("修改获取主播收费设置", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 获取礼物列表 (non-Javadoc)
	 * 
	 * @see com.yiliao.service.app.PersonalCenterService#getGiftList(int)
	 */
	@Override
	public MessageUtil getGiftList(int userId, int page) {
		try {

			String countSql = "SELECT count(*) as totalCount FROM t_order o LEFT JOIN t_gift g ON o.t_consume_score=g.t_gift_id WHERE o.t_cover_consume = ? AND (t_consume_type = 9 OR t_consume_type = 7)";

			Map<String, Object> total = this.getMap(countSql, userId);

			int pageCount = Integer.parseInt(total.get("totalCount").toString()) % 10 == 0
					? Integer.parseInt(total.get("totalCount").toString()) / 10
					: Integer.parseInt(total.get("totalCount").toString()) / 10 + 1;

			String sql = "SELECT g.t_gift_still_url,o.t_consume_type,o.t_amount FROM t_order o LEFT JOIN t_gift g ON o.t_consume_score=g.t_gift_id WHERE o.t_cover_consume = ? AND (t_consume_type = 9 OR t_consume_type = 7) ORDER BY o.t_create_time DESC limit ?,10";

			List<Map<String, Object>> giftList = this.getQuerySqlList(sql, userId, (page - 1) * 10);

			mu = new MessageUtil(1, new HashMap<String, Object>() {
				{
					put("pageCount", pageCount);
					put("data", giftList);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("修改获取主播收费设置", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/**
	 * 获取用户评价列表
	 */
	@Override
	public MessageUtil getEvaluationList(int userId) {

		try {

			String sql = "SELECT e.t_user_id,e.t_user_hand,e.t_user_nick,group_concat(l.t_label_name) AS t_label_name "
					+ "FROM t_user_evaluation e LEFT JOIN t_discuss_record r ON e.t_id = r.t_evaluation_id LEFT JOIN t_label l ON r.t_label_id = l.t_id "
					+ "WHERE e.t_anchor_id =?  GROUP BY e.t_id  ORDER BY e.t_create_time DESC LIMIT 0,20 ";

			List<Map<String, Object>> dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, userId);

			for (Map<String, Object> m : dataMap) {
				if (null == m.get("t_user_nick")) {
					Map<String, Object> map = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(
							"SELECT t_phone FROM t_user WHERE t_id = ?", m.get("t_user_id"));
					m.put("t_user_nick", "聊友:"
							+ map.get("t_phone").toString().substring(map.get("t_phone").toString().length() - 4));
				}
			}

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(dataMap);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取用户评价列表异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getUserPersonalData(int userId) {

		try {

			// 查询被看人的数据
			String sql = "SELECT u.t_handImg,u.t_nickName,u.t_vocation,DATE_FORMAT(u.t_login_time,'%Y-%m-%d %T') AS t_login_time,"
					+ "u.t_height,u.t_weight,u.t_constellation,u.t_city,u.t_autograph FROM t_user u WHERE u.t_id = ? ";

			Map<String, Object> dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql, userId);

			sql = "SELECT t_user_lable_id,l.t_label_name FROM t_user_label u LEFT JOIN t_label l ON u.t_lable_id = l.t_id WHERE t_user_id = ? AND l.t_is_enable = 0";

			List<Map<String, Object>> labList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, userId);

			dataMap.put("lable", labList);

			// 查询轮播图片
			sql = "SELECT  t_img_url  FROM t_cover_examine WHERE t_user_id = ? AND t_is_examine = 1";

			List<Map<String, Object>> carouselMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, userId);

			dataMap.put("lunbotu", carouselMap);

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(dataMap);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取用户个人资料异常", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}

		return mu;
	}

	/*
	 * 获取分享统计 (non-Javadoc)
	 * 
	 * @see com.yiliao.service.app.PersonalCenterService#getShareTotal(int)
	 */
	@Override
	public MessageUtil getShareTotal(int userId) {
		try {
			// 推荐总数
			String sql = "SELECT COUNT(*) AS oneSpreadCount FROM t_user WHERE t_referee = ? ";
			Map<String, Object> shareCount = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql,
					userId);

			sql = "SELECT COUNT(u.t_id) AS twoSpreadCount FROM t_user u LEFT JOIN t_user s ON u.t_referee = s.t_id WHERE s.t_referee = ?";

			Map<String, Object> twoMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql, userId);

			shareCount.put("twoSpreadCount", twoMap.get("twoSpreadCount"));
			// 总收益
			sql = "SELECT SUM(t_value) AS total FROM t_wallet_detail WHERE t_user_id = ? AND t_change_type = 0 AND t_change_category = 11";
			Map<String, Object> profitTotal = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql,
					userId);
			shareCount.put("profitTotal", null == profitTotal.get("total") ? 0
					: new BigDecimal(profitTotal.get("total").toString()).intValue());

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(shareCount);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}分享统计异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getNickRepeat(String nickName) {
		try {

			String sql = " SELECT count(*) AS total FROM t_user WHERE t_nickName = CONVERT(? USING utf8) COLLATE utf8_unicode_ci ";

			Map<String, Object> toMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql, nickName);

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(0 == Integer.parseInt(toMap.get("total").toString()) ? true : false);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("验证昵称异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 获取我的私藏 (non-Javadoc)
	 * 
	 * @see com.yiliao.service.app.PersonalCenterService#getMyPrivate(int, int)
	 */
	@Override
	public MessageUtil getMyPrivate(int userId, int page) {
		try {

			String sql = "SELECT c.t_id,u.t_handImg,u.t_nickName,d.t_title,d.t_addres_url,d.t_file_type FROM t_private_collection c LEFT JOIN t_album d ON c.t_album_id = d.t_id LEFT JOIN t_user u ON d.t_user_id = u.t_id WHERE c.t_user_id = ?  ORDER BY c.t_id DESC LIMIT ?,10";

			List<Map<String, Object>> dynamicList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, userId,
					(page - 1) * 10);

			sql = "SELECT count(*) as total  FROM t_private_collection c LEFT JOIN t_album d ON c.t_album_id = d.t_id LEFT JOIN t_user u ON d.t_user_id = u.t_id WHERE c.t_user_id = ? ";

			Map<String, Object> total = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql, userId);

			int pageCount = Integer.parseInt(total.get("total").toString()) % 10 == 0
					? Integer.parseInt(total.get("total").toString()) / 10
					: Integer.parseInt(total.get("total").toString()) / 10 + 1;

			mu = new MessageUtil(1, new HashMap<String, Object>() {
				{
					put("pageCount", pageCount);
					put("data", dynamicList);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取相册列表异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 删除我的私藏 (non-Javadoc)
	 * 
	 * @see com.yiliao.service.app.PersonalCenterService#delMyPrivate(int)
	 */
	@Override
	public MessageUtil delMyPrivate(int privateId) {
		try {

			String sql = "DELETE FROM t_private_collection WHERE t_id = ?";

			this.getFinalDao().getIEntitySQLDAO().executeSQL(sql, privateId);

			mu = new MessageUtil(1, "删除成功!");

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("删除我的私藏异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 分页获取浏览记录 (non-Javadoc)
	 * 
	 * @see com.yiliao.service.app.PersonalCenterService#getBrowseList(int, int)
	 */
	@Override
	public MessageUtil getBrowseList(int userId, int page) {
		try {

			String sql = "SELECT u.t_id,u.t_handImg,u.t_nickName,DATE_FORMAT(b.t_create_time,'%Y-%m-%d %T') AS t_create_time "
					+ "  FROM t_browse b LEFT JOIN t_user u ON b.t_cover_browse = u.t_id "
					+ "  WHERE b.t_browse_user = ? ORDER BY t_create_time DESC LIMIT ?,10";
			// 查询数据
			List<Map<String, Object>> blowList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, userId,
					(page - 1) * 10);

			// 查询总记录数 并计算出总页数
			sql = "SELECT count(b.t_browse_id) AS totalCount FROM t_browse b LEFT JOIN t_user u ON b.t_cover_browse = u.t_id WHERE b.t_browse_user = ? ";

			Map<String, Object> total = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql, userId);

			int pageCount = Integer.parseInt(total.get("totalCount").toString()) % 10 == 0
					? Integer.parseInt(total.get("totalCount").toString()) / 10
					: Integer.parseInt(total.get("totalCount").toString()) / 10 + 1;

			mu = new MessageUtil(1, new HashMap<String, Object>() {
				{
					put("pageCount", pageCount);
					put("data", blowList);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取浏览记录异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/**
	 * 投诉用户
	 */
	@Override
	public MessageUtil saveComplaint(int userId, int coverUserId, String comment, String img) {
		try {

			String sql = "INSERT INTO t_report (t_user_id,t_cover_user_id,t_comment,t_img,t_create_time) VALUES (?, ?, ?, ?, ?);";

			// 保存投诉数据
			this.getFinalDao().getIEntitySQLDAO().executeSQL(sql, userId, coverUserId, comment, img,
					DateUtils.format(new Date(), DateUtils.FullDatePattern));

			mu = new MessageUtil(1, "投诉成功!");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("投诉异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/**
	 * 存储封面图片
	 */
	@SuppressWarnings("unchecked")
	@Override
	public MessageUtil saveCoverImg(int userId, String coverImg, int t_first) {

		try {
			// 分割字符串
			String[] str = coverImg.split(",");

			// 获取秘钥信息
			String qSql = " SELECT t_app_id,t_secret_id,t_secret_key,t_bucket FROM t_object_storage ";

			Map<String, Object> querySqlList = this.getMap(qSql);

			// 调用鉴黄系统 鉴定封面是否违规
			Map<String, Object> imagePorn = CheckImgUtil.imagePorn(str, querySqlList.get("t_app_id").toString(),
					querySqlList.get("t_secret_id").toString(), querySqlList.get("t_secret_key").toString(),
					querySqlList.get("t_bucket").toString());
			// 得到正常图片的数量
			List<String> imgUrl = (List<String>) imagePorn.get("imgUrl");
			 
			
			String sql = "INSERT INTO t_cover_examine (t_img_url, t_user_id, t_first,t_is_examine,t_create_time) VALUES (?,?,?,?,?);";
			 
			int coverId = this.getFinalDao().getIEntitySQLDAO().saveData(sql, imgUrl.get(0), userId, t_first,0,
						DateUtils.format(new Date(), DateUtils.FullDatePattern));
 

			mu = new MessageUtil(1, "上传成功!");
			mu.setM_object(coverId);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("保存封面异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}
	

	private void delCosImg(String imgUrl) {
		
		Map<String, Object> map = getMap("SELECT * FROM t_object_storage WHERE t_state= 0");
		
		// 1 初始化用户身份信息(secretId, secretKey)
		COSCredentials cred = new BasicCOSCredentials(map.get("t_secret_id")
				.toString(), map.get("t_secret_key").toString());
		// 2 设置bucket的区域, COS地域的简称请参照
		// https://cloud.tencent.com/document/product/436/6224
		ClientConfig clientConfig = new ClientConfig(new Region(map.get("t_region").toString()));
		// 3 生成cos客户端
		COSClient cosclient = new COSClient(cred, clientConfig);
		
		//处理图片
		
		cosclient.deleteObject(map.get("t_bucket").toString(), imgUrl.substring(imgUrl.indexOf("com/")+4));
		
		System.out.println("---删除图片完成---");
		
		cosclient.shutdown();
		
	}
	/*
	 * 获取日明细(non-Javadoc)
	 * 
	 * @see com.yiliao.service.PersonalCenterService#getWalletDateDetails(int,
	 * java.lang.String, java.lang.Integer)
	 */
	@Override
	public MessageUtil getWalletDateDetails(int userId, String time, Integer state, int page) {

		try {
			String countSql;
			String querySql;
			switch (state) {
			case 1: // 收入
				countSql = " SELECT count(*) totalCount FROM t_wallet_detail f WHERE f.t_user_id = ? AND f.t_change_type = 0 AND f.t_value >=1 AND f.t_change_time BETWEEN ? AND ?";

				Map<String, Object> countMap = getMap(countSql, userId, time + " 00:00:00", time + " 23:59:59");

				int pageCount = Integer.parseInt(countMap.get("totalCount").toString()) % 10 == 0
						? Integer.parseInt(countMap.get("totalCount").toString()) / 10
						: Integer.parseInt(countMap.get("totalCount").toString()) / 10 + 1;

				querySql = "SELECT DATE_FORMAT(f.t_change_time,'%Y-%m-%d %T') AS tTime,f.t_value AS totalMoney ,f.t_change_category FROM t_wallet_detail f WHERE f.t_user_id = ? AND f.t_change_type = 0 AND f.t_value >=1   AND f.t_change_time BETWEEN ? AND ? ORDER BY f.t_change_time DESC  limit ? ,10";
				List<Map<String, Object>> income = getQuerySqlList(querySql, userId, time + " 00:00:00",
						time + " 23:59:59", (page - 1) * 10);

				for (Map<String, Object> map : income) {
					map.put("totalMoney", new BigDecimal(map.get("totalMoney").toString()).intValue());

				}

				mu = new MessageUtil(1, new HashMap<String, Object>() {
					{
						put("pageCount", pageCount);
						put("data", income);
					}
				});
				break;
			case 2: // 消费

				countSql = " SELECT count(*) totalCount FROM t_wallet_detail f WHERE f.t_user_id = ? AND f.t_change_type = 1 AND f.t_change_time BETWEEN ? AND ?";

				countMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(countSql, userId,
						time + " 00:00:00", time + " 23:59:59");

				pageCount = Integer.parseInt(countMap.get("totalCount").toString()) % 10 == 0
						? Integer.parseInt(countMap.get("totalCount").toString()) / 10
						: Integer.parseInt(countMap.get("totalCount").toString()) / 10 + 1;

				querySql = " SELECT DATE_FORMAT(f.t_change_time,'%Y-%m-%d %T') AS tTime,f.t_value AS totalMoney ,f.t_change_category  FROM t_wallet_detail f WHERE f.t_user_id = ? AND f.t_change_type = 1  AND f.t_change_time BETWEEN ? AND ?  ORDER BY t_change_time DESC   limit ? ,10";
				List<Map<String, Object>> expenditure = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(querySql,
						userId, time + " 00:00:00", time + " 23:59:59", (page - 1) * 10);

				for (Map<String, Object> map : expenditure) {
					map.put("totalMoney", new BigDecimal(map.get("totalMoney").toString()).intValue());
				}

				mu = new MessageUtil(1, new HashMap<String, Object>() {
					{
						put("pageCount", pageCount);
						put("data", expenditure);
					}
				});

				break;

			case 3: // 充值

				countSql = " SELECT count(*) totalCount FROM t_wallet_detail f WHERE f.t_user_id = ? AND f.t_change_type = 0 AND t_change_category = 0 AND f.t_change_time BETWEEN ? AND ?";

				countMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(countSql, userId,
						time + " 00:00:00", time + " 23:59:59");

				pageCount = Integer.parseInt(countMap.get("totalCount").toString()) % 10 == 0
						? Integer.parseInt(countMap.get("totalCount").toString()) / 10
						: Integer.parseInt(countMap.get("totalCount").toString()) / 10 + 1;

				// 查询用户的充值明细
				querySql = " SELECT DATE_FORMAT(f.t_change_time,'%Y-%m-%d %T') AS tTime,f.t_value AS totalMoney ,f.t_change_category   FROM t_wallet_detail f WHERE f.t_user_id = ? AND f.t_change_type = 0 AND t_change_category = 0  AND f.t_change_time BETWEEN ? AND ? ORDER BY t_change_time DESC   limit ? ,10";
				List<Map<String, Object>> recharge = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(querySql,
						userId, time + " 00:00:00", time + " 23:59:59", (page - 1) * 10);

				for (Map<String, Object> map : recharge) {
					map.put("totalMoney", new BigDecimal(map.get("totalMoney").toString()).intValue());
				}

				mu = new MessageUtil(1, new HashMap<String, Object>() {
					{
						put("pageCount", pageCount);
						put("data", recharge);
					}
				});
				break;
			}

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取日明细异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/**
	 * 获取视频上传签名
	 */
	@Override
	public MessageUtil getVoideSign() {
		try {
			// 获取腾讯云的appid等信息
			String sql = "SELECT * FROM t_object_storage WHERE t_state= 0";

			Map<String, Object> map = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql);

			VideoSignature vs = new VideoSignature();
			vs.setSecretId(map.get("t_secret_id").toString());
			vs.setSecretKey(map.get("t_secret_key").toString());
			vs.setCurrentTime(System.currentTimeMillis() / 1000);
			vs.setRandom(new Random().nextInt(java.lang.Integer.MAX_VALUE));
			vs.setSignValidDuration(3600 * 24 * 2);

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(vs.getUploadSignature());

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取日明细异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 修改视频状态或者删除数据(non-Javadoc)
	 * 
	 * @see
	 * com.yiliao.service.PersonalCenterService#updateOrDelVideo(java.lang.String ,
	 * int)
	 */
	@Override
	public MessageUtil updateOrDelVideo(String fileId, int res) {
		try {

			String sql = "SELECT t_user_id FROM t_album WHERE t_fileId = ? ";

			Map<String, Object> userMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql, fileId);

			if (res == 1) { // 通过鉴黄

				String upSql = " UPDATE t_album set t_auditing_type= 0 WHERE t_fileId = ?;";
				this.getFinalDao().getIEntitySQLDAO().executeSQL(upSql, fileId);
			} else { // 未通过鉴黄

				// 异步通知
				this.applicationContext.publishEvent(
						new PushMesgEvnet(new MessageEntity(Integer.parseInt(userMap.get("t_user_id").toString()),
								"很抱歉!您上传的视频无法通过审核,存在禁止传播的内容.", 0, new Date())));

				String delSql = "DELETE FROM t_album WHERE t_fileId = ? ";
				this.getFinalDao().getIEntitySQLDAO().executeSQL(delSql, fileId);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * 添加意见反馈(non-Javadoc)
	 * 
	 * @see com.yiliao.service.PersonalCenterService#addFeedback(java.lang.String,
	 * java.lang.String, java.lang.String, int)
	 */
	@Override
	public MessageUtil addFeedback(String t_phone, String content, String t_img_url, int t_user_id) {
		try {

			String inseSql = "INSERT INTO t_feedback (t_phone, t_content, t_img_url, t_user_id, t_create_time,t_is_handle) VALUES (?,?,?,?,?,0);";

			this.getFinalDao().getIEntitySQLDAO().executeSQL(inseSql, t_phone, content, t_img_url, t_user_id,
					DateUtils.format(new Date(), DateUtils.FullDatePattern));

			mu = new MessageUtil(1, "反馈成功!");

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("添加意见反馈异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/**
	 * 生产RMB订单记录
	 * 
	 * @param userId          用编号
	 * @param t_setmeal_id    套餐编号
	 * @param t_recharge_type 充值类型 0.VIP 1.金币
	 * @param t_payment_type  支付类型 0.支付宝 1.微信
	 * @param response
	 */
	@Override
	public MessageUtil createRMBorder(int userId, int t_setmeal_id, int t_recharge_type, int t_payment_type) {
		try {

			BigDecimal payMoney = null;
			// 根据套餐编号得到本次充值的RMB金额
			// t_recharge_type 0.VIP 1.金币
			if (t_recharge_type == 0) {
				// 得到充值VIP需要支付的金额
				String querySql = "SELECT t_money FROM t_vip_setmeal  WHERE t_id = ? ;";
				Map<String, Object> map = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(querySql,
						t_setmeal_id);
				payMoney = new BigDecimal(map.get("t_money").toString());

			} else {
				// 得到充值金币需要支付的金额
				String querySql = "SELECT t_money FROM t_set_meal WHERE t_id = ?";
				Map<String, Object> map = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(querySql,
						t_setmeal_id);
				payMoney = new BigDecimal(map.get("t_money").toString());
			}

			String inseSql = "INSERT INTO t_recharge (t_user_id, t_recharge_money, t_order_no, t_recharge_type, t_payment_type, t_setmeal_id, t_order_state, t_create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
			// 生产订单记录
			this.getFinalDao().getIEntitySQLDAO().executeSQL(inseSql, userId, payMoney,
					"no_" + userId + "_" + System.currentTimeMillis(), t_recharge_type, t_payment_type, t_setmeal_id, 0,
					DateUtils.format(new Date(), DateUtils.FullDatePattern));

			// 创建第三方订单

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("创建RMB订单记录异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 获取意见反馈列表(non-Javadoc)
	 * 
	 * @see com.yiliao.service.PersonalCenterService#getFeedBackList(int, int)
	 */
	@Override
	public MessageUtil getFeedBackList(int page, int userId) {
		try {

			String sql = "SELECT t_id,t_content,t_is_handle FROM t_feedback WHERE t_user_id = ? LIMIT ?,20  ";

			List<Map<String, Object>> findBySQLTOMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, userId,
					(page - 1) * 20);

			String totalSql = "SELECT count(t_id) AS total FROM t_feedback WHERE t_user_id = ? ";

			Map<String, Object> total = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(totalSql,
					userId);

			int pageCount = Integer.parseInt(total.get("total").toString()) % 20 == 0
					? Integer.parseInt(total.get("total").toString()) / 20
					: Integer.parseInt(total.get("total").toString()) / 20 + 1;

			mu = new MessageUtil(1, new HashMap<String, Object>() {
				{
					put("pageCount", pageCount);
					put("data", findBySQLTOMap);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取意见反馈列表异常！", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getFeedBackById(int feedBackId) {
		try {

			String sql = "SELECT t_content,t_img_url,DATE_FORMAT(t_create_time,'%Y-%m-%d %H:%i') AS t_create_time,t_handle_res,t_is_handle,DATE_FORMAT(t_handle_time,'%Y-%m-%d %H:%i') AS t_handle_time,t_handle_img  FROM t_feedback WHERE t_id = ? ";

			Map<String, Object> dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql,
					feedBackId);

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(dataMap);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取已经反馈详情异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 用户设置勿扰(non-Javadoc)
	 * 
	 * @see com.yiliao.service.PersonalCenterService#updateUserDisturb(int, int)
	 */
	@Override
	public MessageUtil updateUserDisturb(int userId, int disturb) {
		try {

			String sql = "UPDATE t_user SET t_is_not_disturb = ? WHERE t_id = ? ";

			this.getFinalDao().getIEntitySQLDAO().executeSQL(sql, disturb, userId);

			mu = new MessageUtil(1, "已设置!");

//			GoogleCacheUtil.userCache.invalidate(userId);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("修改勿扰状态异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public void timerUnseal() {
		try {
			this.getFinalDao().getIEntitySQLDAO().executeSQL(
					"UPDATE t_disable d,t_user u SET u.t_disable = 0,d.t_state=1 WHERE d.t_user_id = u.t_id AND d.t_state=0 AND d.t_end_time <= now() ;");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取该用户未领取的红包数
	 */
	@Override
	public MessageUtil getRedPacketCount(int userId) {
		try {

			String sql = "SELECT COUNT(t_redpacket_id) AS total FROM t_redpacket_log WHERE t_receive_userId = ? AND t_redpacket_draw = 0";

			Map<String, Object> total = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql, userId);

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(total);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取红包数异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getUserNew(int userId) {
		try {

			String qSql = "SELECT t_id FROM t_user WHERE t_id = ? AND t_sex = 0";

			List<Map<String, Object>> userList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql, userId);

			mu = new MessageUtil();
			mu.setM_istatus(1);
			// 判断用户是否为指定性别
			if (null != userList && !userList.isEmpty()) {
				// 获取是否还有活动存在
				qSql = "SELECT t_id FROM t_activity WHERE t_is_enable = 0 ";

				List<Map<String, Object>> activityList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql);
				// 判断活动是否已结束
				if (null != activityList && !activityList.isEmpty()) {
					// 统计名额是否已经用完了
					qSql = "SELECT SUM(t_surplus_number) AS total FROM t_activity_detail WHERE t_activity_id = ? ";
					Map<String, Object> total = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(qSql,
							activityList.get(0).get("t_id"));
					// 名额已使用完
					if (Integer.parseInt(total.get("total").toString()) > 0) {

						// 获取该用户是否已经获得过了
						qSql = " SELECT t_id FROM t_award_record  WHERE t_user_id = ? AND t_activity_id = ? ";

						List<Map<String, Object>> awardList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql,
								userId, activityList.get(0).get("t_id"));
						// 用户是否已经获得了奖励
						if (null == awardList || awardList.isEmpty()) {
							// 判断用户是否已经分享满3次了
							qSql = " SELECT * FROM t_share_notes  WHERE t_user_id = ? ";
							List<Map<String, Object>> shareList = this.getFinalDao().getIEntitySQLDAO()
									.findBySQLTOMap(qSql, userId);
							// 用户是否满足 完成3次分享
							if (null == shareList || shareList.isEmpty() || shareList.size() < 3) {
								mu.setM_object(1);
							} else {
								mu.setM_object(2);
							}
						} else {
							mu.setM_object(-4);
						}
					} else {
						mu.setM_object(-3);
					}
				} else {
					mu.setM_object(-2);
				}
			} else {
				mu.setM_object(-1);
			}

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取用户是否是新人异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getUserMakeMoneyList(int userId, int page) {
		try {

			StringBuffer body = new StringBuffer();
			body.append(
					"SELECT u.t_id,u.t_handImg,u.t_nickName,u.t_phone,DATE_FORMAT(r.t_create_time,'%Y-%m-%d') AS t_create_time,SUM(r.t_redpacket_gold) AS t_redpacket_gold FROM t_user u LEFT JOIN  t_redpacket_log r ON u.t_id = r.t_hair_userId WHERE r.t_hair_userId IN ( ");
			body.append("SELECT t_id  from t_user  where  t_referee = ").append(userId);
			body.append(" UNION ");
			body.append(
					" SELECT u1.t_id  from t_user u1 LEFT JOIN t_user s ON u1.t_referee = s.t_id where  s.t_referee = ")
					.append(userId);
			body.append(" ) AND r.t_redpacket_type = 1 AND r.t_redpacket_draw = 1 GROUP BY u.t_id,t_create_time ");

			Map<String, Object> total = this.getFinalDao().getIEntitySQLDAO()
					.findBySQLUniqueResultToMap("SELECT COUNT(aa.t_id) total FROM ( " + body + " ) aa ");

			List<Map<String, Object>> dataList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(
					"SELECT * FROM (" + body + ") aa ORDER BY aa.t_create_time DESC LIMIT ?,10 ", (page - 1) * 10);

			for (Map<String, Object> m : dataList) {
				if (null == m.get("t_nickName")) {
					m.put("t_nickName",
							"聊友:" + m.get("t_phone").toString().substring(m.get("t_phone").toString().length() - 4));
				}

				m.put("t_redpacket_gold", new BigDecimal(m.get("t_redpacket_gold").toString()).intValue());
				m.remove("t_phone");
			}

			int pageCount = Integer.parseInt(total.get("total").toString()) % 10 == 0
					? Integer.parseInt(total.get("total").toString()) / 10
					: Integer.parseInt(total.get("total").toString()) / 10 + 1;

			mu = new MessageUtil(1, new HashMap<String, Object>() {
				{
					put("pageCount", pageCount);
					put("data", dataList);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取贡献用户异常!", e);
			mu = new MessageUtil(0, "");
		}
		return mu;
	}

	@Override
	public MessageUtil applyGuild(int userId, String guildName, String adminName, String adminPhone, String idCard,
			String handImg, int anchorNumber) {
		try {

			String inSql = "INSERT INTO t_guild (t_user_id, t_guild_name, t_admin_name, t_admin_phone, t_anchor_number, t_extract, t_examine, t_create_time,t_idcard,t_hand_img) VALUES (?,?,?,?,?,?,?,?,?,?)";

			this.getFinalDao().getIEntitySQLDAO().executeSQL(inSql, userId, guildName, adminName, adminPhone,
					anchorNumber, 0, 0, DateUtils.format(new Date(), DateUtils.FullDatePattern), idCard, handImg);

			mu = new MessageUtil(1, "申请成功!");

		} catch (Exception e) {
			e.printStackTrace();
			this.mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getAnchorAddGuild(int userId) {
		try {

			String qSql = " SELECT g.t_id,g.t_guild_name,g.t_admin_name FROM t_guild_invite gu LEFT JOIN t_guild g ON gu.t_guild_id = g.t_id WHERE t_anchor_id = ? ";

			List<Map<String, Object>> dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql, userId);

			if (dataMap.isEmpty()) {
				mu = new MessageUtil();
				mu.setM_istatus(1);
				mu.setM_object(new HashMap<String, Object>());
			} else {
				mu = new MessageUtil();
				mu.setM_istatus(1);
				mu.setM_object(dataMap.get(0));
			}

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("拉取主播是否被邀请加入公会异常!");
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 获取公会统计(non-Javadoc)
	 * 
	 * @see com.yiliao.service.PersonalCenterService#getGuildCount(int)
	 */
	@Override
	public MessageUtil getGuildCount(int userId) {
		try {

			String qSql = "SELECT COUNT(t_id) AS anchorCount FROM t_anchor_guild WHERE t_guild_id = ( SELECT g.t_id FROM t_guild g WHERE g.t_user_id = ? );";

			Map<String, Object> anchorTotal = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(qSql,
					userId);

			qSql = "SELECT SUM(t_value) AS totalGold FROM t_wallet_detail WHERE t_user_id = ? AND t_change_type = 0 AND t_change_category = 14";

			Map<String, Object> totalGold = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(qSql,
					userId);

			mu = new MessageUtil(1, new HashMap<String, Object>() {
				{
					if (null == anchorTotal.get("anchorCount")) {
						put("anchorCount", 0);
					} else {
						put("anchorCount", anchorTotal.get("anchorCount"));
					}
					if (null == totalGold.get("totalGold")) {
						put("totalGold", 0);
					} else {
						put("totalGold", new BigDecimal(totalGold.get("totalGold").toString()).intValue());
					}
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取公会统计异常!");
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 主播是否同意加入公会(non-Javadoc)
	 * 
	 * @see com.yiliao.service.PersonalCenterService#isApplyGuild(int, int, int)
	 */
	@Override
	public MessageUtil isApplyGuild(int guildId, int userId, int isApply) {
		try {
			// 同意加入公会
			if (1 == isApply) {
				String inSql = " INSERT INTO t_anchor_guild (t_guild_id, t_anchor_id, t_create_time) VALUES (?,?,?) ";
				this.getFinalDao().getIEntitySQLDAO().executeSQL(inSql, guildId, userId,
						DateUtils.format(new Date(), DateUtils.FullDatePattern));

				// 判断主播是否是否加入过公会
				String qSql = " SELECT * FROM t_anchor_devote WHERE t_anchor_id = ? ";
				List<Map<String, Object>> dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql, userId);

				if (null == dataMap || dataMap.isEmpty()) {
					// 插入贡献统计
					inSql = " INSERT INTO t_anchor_devote (t_anchor_id, t_devote_value) VALUES (?,?) ";
					this.getFinalDao().getIEntitySQLDAO().executeSQL(inSql, userId, 0);
				} else {
					String uSql = " UPDATE t_anchor_devote SET t_devote_value= 0 WHERE t_anchor_id = ? ";
					this.getFinalDao().getIEntitySQLDAO().executeSQL(uSql, userId);
				}
			}
			// 删除邀请记录
			String qSql = "DELETE FROM  t_guild_invite  WHERE t_guild_id = ? AND t_anchor_id = ? ";
			this.getFinalDao().getIEntitySQLDAO().executeSQL(qSql, guildId, userId);

			mu = new MessageUtil(1, "操作成功!");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("主播是否同意加入公会", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getContributionList(int userId, int page) {
		try {

			StringBuffer body = new StringBuffer();
			body.append(" SELECT gu.t_anchor_id,u.t_handImg,u.t_nickName,u.t_phone  ,d.t_devote_value  ");
			body.append(" FROM t_anchor_guild gu LEFT JOIN t_user u ON  gu.t_anchor_id  = u.t_id ");
			body.append(" LEFT JOIN t_guild g ON gu.t_guild_id = g.t_id ");
			body.append(" LEFT JOIN t_anchor_devote d ON gu.t_anchor_id = d.t_anchor_id ");
			body.append(" WHERE 1=1 ");
			body.append(" AND g.t_user_id =? ");
			body.append(" group BY gu.t_anchor_id ");

			// 查询总记录数
			Map<String, Object> totalMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(
					"SELECT COUNT(aa.t_anchor_id) as totalCount FROM (" + body + ") aa", userId);

			int pageCount = Integer.parseInt(totalMap.get("totalCount").toString()) % 10 == 0
					? Integer.parseInt(totalMap.get("totalCount").toString()) / 10
					: Integer.parseInt(totalMap.get("totalCount").toString()) / 10 + 1;

			// 取得数据

			List<Map<String, Object>> dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(
					"SELECT * FROM (" + body + ") aa  ORDER BY aa.t_devote_value DESC LIMIT ?,10", userId,
					(page - 1) * 10);

			// 迭代处理昵称问题
			for (Map<String, Object> m : dataMap) {
				if (null == m.get("t_nickName")) {
					m.put("t_nickName",
							"聊友:" + m.get("t_phone").toString().substring(m.get("t_phone").toString().length() - 4));
				}
				m.put("totalGold",
						new BigDecimal(null == m.get("t_devote_value") ? "0" : m.get("t_devote_value").toString())
								.intValue());
				m.remove("t_phone");
			}

			mu = new MessageUtil(1, new HashMap<String, Object>() {
				{
					put("pageCount", pageCount);
					put("data", dataMap);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取贡献列表异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getAnthorTotal(int anchorId, int userId) {
		try {

			StringBuffer qSql = new StringBuffer();

			qSql.append(
					" SELECT u.t_handImg,u.t_nickName,u.t_phone,d.t_devote_value FROM t_anchor_devote d LEFT JOIN t_user u ON d.t_anchor_id=u.t_id WHERE d.t_anchor_id = ?");

			Map<String, Object> anMap = this.getFinalDao().getIEntitySQLDAO()
					.findBySQLUniqueResultToMap(qSql.toString(), anchorId);

			qSql = new StringBuffer();
			qSql.append(
					" SELECT SUM(w.t_value) AS totalGold FROM t_anchor_guild gu LEFT JOIN t_order o ON o.t_cover_consume = gu.t_anchor_id ");
			qSql.append(
					" LEFT JOIN t_wallet_detail w ON o.t_id=w.t_sorece_id LEFT JOIN t_guild g ON gu.t_guild_id = g.t_id ");
			qSql.append(" WHERE w.t_change_category=? AND g.t_user_id = ? AND gu.t_anchor_id = ? ");
			qSql.append(" AND w.t_change_time BETWEEN ? AND ? ");

			Map<String, Object> dayGold = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(
					qSql.toString(), WalletDetail.CHANGE_CATEGOR_GUILD_INCOME, userId, anchorId,
					DateUtils.format(new Date()) + " 00:00:00", DateUtils.format(new Date()) + " 23:59:59");

			anMap.put("toDay",
					null == dayGold.get("totalGold") ? 0 : new BigDecimal(dayGold.get("totalGold").toString()));

			if (null == anMap.get("t_nickName")) {
				anMap.put("t_nickName", "聊友:"
						+ anMap.get("t_phone").toString().substring(anMap.get("t_phone").toString().length() - 4));
			}

			anMap.remove("t_phone");

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(anMap);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取主播贡献明细异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getContributionDetail(int anchorId, int userId, int page) {
		try {

			StringBuffer sb = new StringBuffer();
			sb.append(" SELECT SUM(aa.totalGold) AS totalGold,aa.t_change_category,aa.t_change_time FROM ( ");
			sb.append(
					" SELECT w.t_change_category,DATE_FORMAT(w.t_change_time,'%Y-%m-%d') AS t_change_time,SUM(w.t_value) AS totalGold FROM t_anchor_guild gu LEFT JOIN t_order o ON o.t_cover_consume = gu.t_anchor_id ");
			sb.append(
					" LEFT JOIN t_wallet_detail w ON o.t_id=w.t_sorece_id LEFT JOIN t_guild g ON gu.t_guild_id = g.t_id ");
			sb.append(
					" WHERE w.t_change_category=? AND g.t_user_id = ? AND gu.t_anchor_id = ? AND w.t_change_time < ? GROUP BY t_change_time ");
			sb.append(" ) aa GROUP BY aa.t_change_time ");
			Map<String, Object> totalCount = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(
					"SELECT COUNT(*) as total FROM (" + sb + ") bb", WalletDetail.CHANGE_CATEGOR_GUILD_INCOME, userId,
					anchorId, DateUtils.format(new Date()) + " 00:00:00 ");

			int pageCount = Integer.parseInt(totalCount.get("total").toString()) % 10 == 0
					? Integer.parseInt(totalCount.get("total").toString()) / 10
					: Integer.parseInt(totalCount.get("total").toString()) / 10 + 1;

			List<Map<String, Object>> dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(
					"SELECT * FROM (" + sb + ") bb ORDER BY bb.t_change_time DESC LIMIT ?,10",
					WalletDetail.CHANGE_CATEGOR_GUILD_INCOME, userId, anchorId,
					DateUtils.format(new Date()) + " 00:00:00 ", (page - 1) * 10);

			mu = new MessageUtil(1, new HashMap<String, Object>() {
				{
					put("pageCount", pageCount);
					put("data", dataMap);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取主播贡献明细列表异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 获取打赏列表(non-Javadoc)
	 * 
	 * @see com.yiliao.service.PersonalCenterService#getRewardList(int, int)
	 */
	@Override
	public MessageUtil getRewardList(int userId, int page) {
		try {

			String cSql = " SELECT COUNT(o.t_id) AS total FROM t_order o  WHERE (o.t_consume_type =7 OR o.t_consume_type=9) AND o.t_cover_consume= ? ";

			Map<String, Object> totalMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(cSql,
					userId);

			int pageCount = Integer.parseInt(totalMap.get("total").toString()) % 10 == 0
					? Integer.parseInt(totalMap.get("total").toString()) / 10
					: Integer.parseInt(totalMap.get("total").toString()) / 10 + 1;

			StringBuffer qSql = new StringBuffer();
			qSql.append(
					" SELECT u.t_handImg,u.t_nickName,u.t_phone,DATE_FORMAT(o.t_create_time,'%Y-%m-%d %T') AS t_create_time,g.t_gift_still_url,o.t_consume_type,o.t_amount ");
			qSql.append(" FROM t_order o LEFT JOIN t_user u ON o.t_consume = u.t_id ");
			qSql.append(" LEFT JOIN t_gift g ON o.t_consume_score = g.t_gift_id ");
			qSql.append(" WHERE (o.t_consume_type =7 OR o.t_consume_type=9) AND o.t_cover_consume= ? ");
			qSql.append(" ORDER BY o.t_create_time DESC LIMIT ?,10");

			List<Map<String, Object>> dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql.toString(),
					userId, (page - 1) * 10);

			for (Map<String, Object> m : dataMap) {
				if (null == m.get("t_nickName")) {
					m.put("t_nickName",
							"聊友:" + m.get("t_phone").toString().substring(m.get("t_phone").toString().length() - 4));
				}
				m.remove("t_phone");
			}
		 
			mu = new MessageUtil(1, new HashMap<String, Object>() {
				{
					put("pageCount", pageCount);
					put("data", dataMap);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取打赏列表异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getGiveGoldMsg(int userId) {
		try {

			String qSql = " SELECT t_gold FROM t_give_gold_notice WHERE t_is_read = 0 AND t_user_id = ? ";

			List<Map<String, Object>> findBySQLTOMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql,
					userId);

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(findBySQLTOMap);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取新用户赠送消息异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil setUpGiveGoldIsRead(int userId) {
		try {

			String uSql = " UPDATE t_give_gold_notice SET t_is_read = 1 WHERE t_user_id = ? ;";

			this.getFinalDao().getIEntitySQLDAO().executeSQL(uSql, userId);

			mu = new MessageUtil(1, "设置成功!");

		} catch (Exception e) {
			logger.error("{}修改赠送消息为已读异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil addLaud(int laudUserId, int coverLaudUserId) {
		try {

			String inSql = " INSERT INTO t_user_laud (t_laud_user_id, t_cover_user_id, t_create_time) VALUES (?,?,?);";
			this.getFinalDao().getIEntitySQLDAO().executeSQL(inSql, laudUserId, coverLaudUserId,
					DateUtils.format(System.currentTimeMillis(), DateUtils.FullDatePattern));

			mu = new MessageUtil(1, "点赞成功!");

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}用户给{}用户点赞异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil addQueryDynamicCount(int fileId) {
		try {

			// 更新查看次数
			String uSql = " UPDATE t_album SET t_see_count = t_see_count+1 WHERE t_id = ?  ";
			this.getFinalDao().getIEntitySQLDAO().executeSQL(uSql, fileId);

			mu = new MessageUtil(1, "操作成功!");

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Add Dynamic See Frequency error", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil cancelLaud(int userId, int coverUserId) {
		try {

			String dSql = " DELETE FROM t_user_laud WHERE t_laud_user_id = ? AND t_cover_user_id = ? ";

			this.getFinalDao().getIEntitySQLDAO().executeSQL(dSql, userId, coverUserId);

			mu = new MessageUtil(1, "取消成功!");

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}取消{}的点赞异常!", userId, coverUserId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getSpreadAward(int userId) {
		try {

			// 查询分成比例

			List<Map<String, Object>> querySqlList = this
					.getQuerySqlList(" SELECT  t_award_rules FROM t_system_setup ");

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(new HashMap<String, Object>() {
				{
					put("t_award_rules", querySqlList.isEmpty() ? "" : querySqlList.get(0).get("t_award_rules"));
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getAnchorVideoCost(int userId) {
		try {
			String qSql = " SELECT SUM(t_money) AS totalMoney FROM t_put_forward  WHERE t_user_id  = ? ";
			Map<String, Object> putMoney = this.getMap(qSql, userId);

			int totalMoney = new BigDecimal(
					null == putMoney.get("totalMoney") ? "0" : putMoney.get("totalMoney").toString()).intValue();

			if (totalMoney < 100) {
				totalMoney = 30;
			} else if (totalMoney < 500) {
				totalMoney = 40;
			} else {
				totalMoney = 50;
			}

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(totalMoney);

		} catch (Exception e) {
			e.printStackTrace();
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getSpreadBonuses(int userId) {
		try {

			StringBuffer qSql = new StringBuffer(300);
			qSql.append("SELECT * FROM ( ");
			qSql.append(" SELECT u.t_nickName,u.t_phone,t_handImg,SUM(w.t_value) AS totalGold ");
			qSql.append(" FROM t_user u LEFT JOIN  t_wallet_detail w ON w.t_user_id = u.t_id ");
			qSql.append(" WHERE w.t_change_category = 11 GROUP BY w.t_user_id ");
			qSql.append(") aa ORDER BY aa.totalGold DESC LIMIT 20;");

			List<Map<String, Object>> querySqlList = this.getQuerySqlList(qSql.toString());

			querySqlList.forEach(s -> {
				if (null == s.get("t_nickName")) {
					s.put("t_nickName",
							"聊友:" + s.get("t_phone").toString().substring(s.get("t_phone").toString().length() - 4));
				}
				s.remove("t_phone");
				s.put("totalGold", new BigDecimal(s.get("totalGold").toString()).intValue());
			});

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(querySqlList);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取推荐贡献排行榜异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getSpreadUser(int userId) {
		try {

			StringBuffer qSql = new StringBuffer(210);
			qSql.append("SELECT * FROM (");
			qSql.append(" SELECT u.t_nickName,u.t_phone,u.t_handImg,COUNT(ul.t_id) AS totalCount ");
			qSql.append(
					" FROM t_user u LEFT JOIN t_user ul ON ul.t_referee = u.t_id WHERE ul.t_referee > 0 GROUP BY u.t_id ");
			qSql.append(") aa ORDER BY aa.totalCount DESC LIMIT 20 ;");

			List<Map<String, Object>> sqlList = this.getQuerySqlList(qSql.toString());

			sqlList.forEach(s -> {
				if (null == s.get("t_nickName")) {
					s.put("t_nickName",
							"聊友:" + s.get("t_phone").toString().substring(s.get("t_phone").toString().length() - 4));
				}
				s.remove("t_phone");
			});

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(sqlList);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取推荐排行榜异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/**
	 * 获取个人资料
	 */
	@Override
	public MessageUtil getMydata(int userId) {
		try {

			// 查询被看人的数据
			String sql = "SELECT u.t_handImg,u.t_onLine,u.t_idcard,u.t_nickName,u.t_vocation,u.t_weixin,u.t_phone,DATE_FORMAT(u.t_login_time,'%Y-%m-%d %T') AS t_login_time,"
					+ "u.t_height,u.t_weight,u.t_constellation,u.t_city,u.t_autograph,u.t_role FROM t_user u WHERE u.t_id = ? ";

			Map<String, Object> dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql, userId);

			// 当前用户为主播
			if (dataMap.get("t_role").equals("1")) {
				// 获取当前用户状态
				sql = "SELECT t_state FROM t_anchor WHERE t_user_id = ? ";

				Map<String, Object> onLineMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql,
						userId);
				dataMap.put("t_onLine", onLineMap.get("t_state"));

				// 查询接听率
				sql = "SELECT t_reception FROM t_reception_rate WHERE t_user_id = ?";

				List<Map<String, Object>> list = this.getQuerySqlList(sql, userId);

				// 获取主播的接听率
				dataMap.put("t_reception",
						list.isEmpty() ? "100%" : Double.valueOf((list.get(0).get("t_reception").toString())) + "%");

			}
			// 主播标签列表
			sql = "SELECT t_user_lable_id,l.t_label_name FROM t_user_label u LEFT JOIN t_label l ON u.t_lable_id = l.t_id WHERE t_user_id = ? AND l.t_is_enable = 0";

			List<Map<String, Object>> labList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, userId);

			dataMap.put("lable", labList);

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(dataMap);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取用户资料异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 查看主播资料 (non-Javadoc)
	 * 
	 * @see com.yiliao.service.PersonalCenterService#getAnchorData(int, int)
	 */
	@Override
	public MessageUtil getAnchorData(int userId, int anchorId) {
		try {
			// 修改被查看人的浏览次数
			String sql = "UPDATE t_user u SET u.t_browse_sum = (u.t_browse_sum+1) WHERE u.t_id = ?";
			this.executeSQL(sql, anchorId);
			/***********************/

			// 判断当前人今天是否浏览过了
			sql = "SELECT * FROM t_browse WHERE t_browse_user = ? AND t_cover_browse = ? AND t_create_time BETWEEN ? AND ? ";

			List<Map<String, Object>> browse = this.getQuerySqlList(sql, userId, anchorId,
					DateUtils.format(new Date()) + " 00:00:00", DateUtils.format(new Date()) + " 23:59:59");

			if (null == browse || browse.isEmpty()) {
				// 保存浏览记录
				sql = "INSERT INTO t_browse (t_browse_user, t_cover_browse, t_create_time) VALUES (?, ?, ?)";

				this.executeSQL(sql, userId, anchorId, DateUtils.format(new Date(), DateUtils.FullDatePattern));

			} else {
				sql = "UPDATE t_browse SET  t_create_time=? WHERE t_browse_id=?;";

				this.executeSQL(sql, DateUtils.format(new Date(), DateUtils.FullDatePattern),
						Integer.parseInt(browse.get(0).get("t_browse_id").toString()));

			}
			/****************************/

			// 查询被看人的数据
			sql = "SELECT u.t_handImg,u.t_onLine,u.t_idcard,u.t_nickName,u.t_vocation,u.t_weixin,u.t_phone,DATE_FORMAT(u.t_login_time,'%Y-%m-%d %T') AS t_login_time,"
					+ "u.t_height,u.t_weight,u.t_constellation,u.t_city,u.t_autograph FROM t_user u WHERE u.t_id = ? ";

			Map<String, Object> dataMap = this.getMap(sql, anchorId);

			// 获取当前用户状态
			sql = "SELECT t_state FROM t_anchor WHERE t_user_id = ? ";

			Map<String, Object> onLineMap = this.getMap(sql, anchorId);
			dataMap.put("t_onLine", onLineMap.get("t_state"));

			// 查询被看人的粉丝数
			sql = "SELECT count(*) as totalCount FROM t_follow WHERE t_cover_follow = ? ";

			Map<String, Object> totalCount = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql,
					anchorId);

			Random random = new Random();
			int s = random.nextInt(10000) % (10000 - 5000 + 1) + 5000;

			dataMap.put("totalCount", Integer.parseInt(totalCount.get("totalCount").toString()) + s);

			// 查询当前人是否已经关注了该主播或者用户
			sql = "SELECT COUNT(*) AS follow  FROM t_follow WHERE t_cover_follow = ? AND t_follow_id = ?";

			List<Map<String, Object>> isFollow = this.getQuerySqlList(sql, userId, anchorId);
			// 判断当前用户是否关注
			dataMap.put("isFollow", isFollow.get(0).get("follow"));

			// 查询用户是否已经查看过微信号
			sql = "SELECT * FROM t_order WHERE t_consume_type = 6 AND t_consume = ? AND t_cover_consume = ?";
			List<Map<String, Object>> isWeixin = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, userId,
					anchorId);
			dataMap.put("isWeixin", null == isWeixin ? 0 : isWeixin.isEmpty() ? 0 : 1);

			// 查询用户是否已经查看用户的手机号
			sql = "SELECT * FROM t_order WHERE t_consume_type = 5 AND t_consume = ? AND t_cover_consume = ?";

			List<Map<String, Object>> isPhone = this.getQuerySqlList(sql, userId, anchorId);
			dataMap.put("isPhone", null == isPhone ? 0 : isPhone.isEmpty() ? 0 : 1);

			// 查询接听率
			sql = "SELECT t_reception FROM t_reception_rate WHERE t_user_id = ?";

			List<Map<String, Object>> list = this.getQuerySqlList(sql, anchorId);

			// 获取主播的接听率
			dataMap.put("t_reception",
					list.isEmpty() ? "100%" : Double.valueOf((list.get(0).get("t_reception").toString())) + "%");

			// 主播标签列表
			sql = "SELECT t_user_lable_id,l.t_label_name FROM t_user_label u LEFT JOIN t_label l ON u.t_lable_id = l.t_id WHERE t_user_id = ? AND l.t_is_enable = 0";

			List<Map<String, Object>> labList = this.getQuerySqlList(sql, anchorId);

			dataMap.put("lable", labList);

			// 查询轮播图片
			sql = "SELECT  t_img_url  FROM t_cover_examine WHERE t_user_id = ? AND t_is_examine = 1";

			List<Map<String, Object>> carouselMap = this.getQuerySqlList(sql, anchorId);

			dataMap.put("lunbotu", carouselMap);

			// 查询视频 微信 手机号需要的金币数
			sql = "SELECT t_video_gold,t_phone_gold,t_weixin_gold,t_text_gold FROM t_anchor_setup WHERE t_user_id = ?";

			List<Map<String, Object>> dataList = this.getQuerySqlList(sql, anchorId);

			if (null != dataList && !dataList.isEmpty()) {
				dataMap.put("anchorSetup", dataList);
			}

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(dataMap);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取用户资料异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getIntimateAndGift(int userId) {
		try {

			// 获取亲密度最高的3个用户(消费最高)
			StringBuffer intimate = new StringBuffer(230);
			intimate.append("SELECT u.t_handImg FROM ( ");
			intimate.append(" SELECT SUM(t_amount) AS totalGold,t_consume ");
			intimate.append(" FROM t_order WHERE t_cover_consume = ? GROUP BY t_consume ");
			intimate.append(") aa LEFT JOIN t_user u ON u.t_id = aa.t_consume ");
			intimate.append("  ORDER BY aa.totalGold DESC LIMIT 6;");

			List<Map<String, Object>> intimates = this.getQuerySqlList(intimate.toString(), userId);

			// 获取礼物榜最高的3个用户(赠送红包和礼物)

			StringBuffer gift = new StringBuffer(230);
			gift.append(
					"SELECT g.t_gift_still_url FROM t_order o LEFT JOIN t_gift g ON o.t_consume_score = g.t_gift_id  ");
			gift.append("WHERE t_cover_consume = ? AND t_consume_type = 9 ORDER BY o.t_create_time DESC LIMIT 6;");

			List<Map<String, Object>> gifts = this.getQuerySqlList(gift.toString(), userId);

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(new HashMap<String, Object>() {
				{
					put("intimates", intimates);
					put("gifts", gifts);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取亲密排行和礼物排行异常", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}

		return mu;
	}

	/*
	 * 
	 * 获取亲密度列表(non-Javadoc)
	 * 
	 * @see com.yiliao.service.PersonalCenterService#getAnthorIntimateList(int, int)
	 */
	@Override
	public MessageUtil getAnthorIntimateList(int userId, int page) {

		try {

			StringBuffer intimate = new StringBuffer(230);
			intimate.append("SELECT u.t_id,u.t_handImg,u.t_nickName,u.t_phone,aa.totalGold FROM ( ");
			intimate.append(" SELECT SUM(t_amount) AS totalGold,t_consume ");
			intimate.append(" FROM t_order WHERE t_cover_consume = ? AND t_consume !=0 GROUP BY t_consume ");
			intimate.append(") aa LEFT JOIN t_user u ON u.t_id = aa.t_consume ");
			intimate.append("  ORDER BY aa.totalGold DESC LIMIT ?,10;");

			List<Map<String, Object>> intimates = this.getQuerySqlList(intimate.toString(), userId, (page - 1) * 10);

			intimates.forEach(s -> {
				if (null == s.get("t_nickName")) {
					s.put("t_nickName",
							"聊友:" + s.get("t_phone").toString().substring(s.get("t_phone").toString().length() - 4));
				}
				s.remove("t_phone");

				// 获取充值金币 用于分配用户的充值级别
				String qSql = "SELECT SUM(r.t_recharge_money) AS money FROM t_recharge  r WHERE r.t_user_id = ? AND r.t_order_state = 1";

				List<Map<String, Object>> regList = this.getQuerySqlList(qSql, s.get("t_id"));

				if (null == regList || regList.isEmpty() || null == regList.get(0).get("money")) {
					s.put("grade", this.grade(0));
				} else {
					s.put("grade", this.grade(new BigDecimal(regList.get(0).get("money").toString()).intValue()));
				}
			});
			// 统计
			Map<String, Object> map = this
					.getMap(" SELECT COUNT(t_id) AS totalCount FROM t_order WHERE t_cover_consume = ?  ", userId);

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(new HashMap<String, Object>() {
				{
					put("data", intimates);
					put("pageCount",
							Integer.parseInt(map.get("totalCount").toString()) % 10 == 0
									? Integer.parseInt(map.get("totalCount").toString()) / 10
									: Integer.parseInt(map.get("totalCount").toString()) / 1 + 1);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取{}亲密列表异常", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}

		return mu;
	}

	@Override
	public MessageUtil getAnthorGiftList(int userId) {
		try {
			// 获取所有的礼物列表

			List<Map<String, Object>> gifts = this.getQuerySqlList(
					"SELECT t_gift_id,t_gift_name,t_gift_still_url FROM t_gift WHERE t_is_enable = 0;");

			int total = 0;

			for (Map<String, Object> s : gifts) {

				Map<String, Object> map = this.getMap(
						"SELECT COUNT(t_id) AS totalCount FROM t_order WHERE t_cover_consume = ? AND t_consume_score = ?",
						+userId, s.get("t_gift_id"));
				s.put("totalCount", map.get("totalCount"));
				total = total + Integer.parseInt(map.get("totalCount").toString());

				s.remove("t_gift_id");
			}
			;
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("data", gifts);
			map.put("total", total);

			mu = new MessageUtil();

			mu.setM_istatus(1);
			mu.setM_object(map);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取{}礼物排行榜异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
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
	 * 获取模拟消息
	 */
	@Override
	public String getSimulationMsg(int sex) {
		try {

			String qSql = " SELECT t_centent FROM t_simulation WHERE t_sex != ? AND t_id >= ((SELECT MAX(t_id) FROM t_simulation)-(SELECT MIN(t_id) FROM t_simulation)) * RAND() + (SELECT MIN(t_id) FROM t_simulation)  LIMIT 1 ";

			List<Map<String, Object>> sqltoMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql, sex);

			return sqltoMap.isEmpty() ? "" : sqltoMap.get(0).get("t_centent").toString();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取模拟消息异常!", e);
		}
		return null;
	}

	@Override
	public Map<String, Object> getTencentKey() {
		try {

			// 获取秘钥信息
			String qSql = " SELECT t_app_id,t_secret_id,t_secret_key,t_bucket FROM t_object_storage ";

			return this.getMap(qSql);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public MessageUtil setMainCoverImg(int userId,int id) {
		try {
			
			//根据id修改为主封面
			executeSQL("UPDATE t_cover_examine SET t_first = 1 WHERE t_user_id = ?;", userId);
			
			//修改指定的封面为主封面
			executeSQL("UPDATE t_cover_examine SET t_first = 0 WHERE t_id = ?;", id);
			
			
			return new MessageUtil(1, "设置成功!");
			
		} catch (Exception e) {
			e.printStackTrace();
			return new MessageUtil(0, "程序异常!");
		}
	}

	@Override
	public MessageUtil delCoverImg(int id) {
		try {
			//获取ID下的数据
			List<Map<String,Object>> sqlList = getQuerySqlList("SELECT * FROM t_cover_examine WHERE t_id = ?", id);
			
			if(null!=sqlList && !sqlList.isEmpty()) {
				
				delCosImg(sqlList.get(0).get("t_img_url").toString());
				// 删除数据
				executeSQL("DELETE FROM t_cover_examine WHERE  t_id = ?", id);
				
			}
			return new MessageUtil(1, "删除成功!"); 
		} catch (Exception e) {
			e.printStackTrace();
			return new MessageUtil(0, "程序异常!");
		}
	}

}
