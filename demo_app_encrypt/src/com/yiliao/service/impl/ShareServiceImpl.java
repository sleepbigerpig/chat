package com.yiliao.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.yiliao.service.ActivityService;
import com.yiliao.service.ShareService;
import com.yiliao.util.DateUtils;
import com.yiliao.util.MessageUtil;
import com.yiliao.util.SpringConfig;
import com.yiliao.util.SystemConfig;

@Service("shareService")
public class ShareServiceImpl extends ICommServiceImpl implements ShareService {

	/*
	 * 保存设备信息(non-Javadoc)
	 * @see com.yiliao.service.ShareService#addShareInfo(int, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void addShareInfo(int userId, String equipment, String system_moble,
			String ipAddress) {
		try {
			
			String sql = "INSERT INTO t_device (t_phone_type, t_system_version, t_ip_address, t_referee_id, t_is_use, t_create_time) VALUES (?, ?, ?, ?, ?, ?);";
			
			
			this.getFinalDao().getIEntitySQLDAO().executeSQL(sql, equipment.trim(),system_moble.trim(),ipAddress.trim(),userId,0,DateUtils.format(new Date(), DateUtils.FullDatePattern));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * 添加分享记录(non-Javadoc)
	 * @see com.yiliao.service.ShareService#addShareCount(int)
	 */
	@Override
	public MessageUtil addShareCount(int userId) {
		MessageUtil mu = null;
		try {
			String inSql = "INSERT INTO t_share_notes (t_user_id, t_target, t_date) VALUES (?,?,?) ";
			
			this.getFinalDao().getIEntitySQLDAO().executeSQL(inSql, userId,1,DateUtils.format(new Date() ,DateUtils.FullDatePattern));
			
			mu = new MessageUtil(1, "添加成功!");
			
			//查询用户已分享次数
			String qSql = " SELECT count(t_id) AS total FROM t_share_notes WHERE t_user_id = ? ";
			Map<String, Object> totalMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(qSql, userId);
			
			mu.setM_object(totalMap.get("total"));
			//获取用户的性别
			qSql = " SELECT t_id FROM t_user WHERE t_id = ?  AND t_sex = 0 ";
			List<Map<String, Object>> userMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql, userId);
			//判断是否已经分享了3次 且用户性别必须为女
			if(Integer.parseInt(totalMap.get("total").toString()) >= 3 && !userMap.isEmpty()){
				//在次判断用户是否已经领取了奖品了
				qSql = "SELECT t_id  AS total FROM t_award_record WHERE t_user_id = ? ";
				List<Map<String, Object>> dataMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql, userId);
			    //用户未中过奖
				if(null == dataMap || dataMap.isEmpty()){
					 //判断用户是否已经实名认证了
					qSql = "SELECT * FROM t_certification WHERE t_user_id = ? ";
					List<Map<String, Object>> identificationMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql, userId);
					//用户必须实名认证才能中奖
					if(!identificationMap.isEmpty()){
						//获取用户可参与的活动
						qSql = " SELECT t_id  FROM t_activity WHERE t_is_enable = 0 AND t_join_term = 3 ";
						List<Map<String, Object>> actMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql);
						if(!actMap.isEmpty()){
							ActivityService activityService = (ActivityService) SpringConfig.getInstance().getBean("activityService");
							activityService.shareRedPacket(userId, Integer.parseInt(actMap.get(0).get("t_id").toString()));
						}
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("添加分享记录!", e);
			mu = new MessageUtil(0,"程序异常!");
		}
		return mu;
	}

	@Override
	public Map<String, Object> getDownLoadUrl() {
		
		String qSql = "SELECT t_android_download,t_ios_download FROM t_system_setup; ";
		
		return this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(qSql);
	}

	@Override
	public MessageUtil getSpreadUrl(int userId) {
		try {
			
			List<Map<String,Object>> sqlList = getQuerySqlList("SELECT t_domain_name FROM t_domainnamepool ORDER BY RAND() LIMIT 1");
			MessageUtil mu = new MessageUtil();
			if(null == sqlList || sqlList.isEmpty()) {
				mu.setM_istatus(1);
				mu.setM_object(SystemConfig.getValue("share_url")+userId);
			}else {
				String value = SystemConfig.getValue("share_url");
				mu.setM_istatus(1);
				mu.setM_object(sqlList.get(0).get("t_domain_name")+value.substring(value.indexOf("/share"))+userId);
			}
			return mu;
		} catch (Exception e) {
			e.printStackTrace();
			return new MessageUtil(0, "程序异常!");
		}
	}

}
