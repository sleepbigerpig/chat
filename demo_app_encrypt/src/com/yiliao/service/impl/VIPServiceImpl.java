package com.yiliao.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.yiliao.domain.VIPSetmeal;
import com.yiliao.service.VIPService;
import com.yiliao.util.MessageUtil;

/**
 * vip 服务层实现类
 * @author Administrator
 *
 */
@Service("vIPService")
public class VIPServiceImpl extends ICommServiceImpl implements VIPService {
	
	private MessageUtil mu = null;

	@Override
	public MessageUtil getVIPSetMealList() {
		try {
			
			String sql = "SELECT t_id,t_setmeal_name,t_cost_price,t_money,t_gold,t_duration FROM t_vip_setmeal WHERE t_is_enable = ? ";
			
			List<Map<String, Object>> vipList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, VIPSetmeal.IS_ENABLE_NO);
			
			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(vipList);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取VIP套餐异常!", e);
            mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/**
	 * 修改VIP已到期的数据
	 */
	@Override
	public void updateVIPExpire() {
		
		try {
			String updateSql = "UPDATE t_vip v,t_user u SET u.t_is_vip = 1 WHERE v.t_user_id = u.t_id AND t_end_time <= now();";
		
		    this.getFinalDao().getIEntitySQLDAO().executeSQL(updateSql);
		    
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("修改VIP到期异常!", e);
		}
	}

	@Override
	public void updateVirtualState() {
		try {
			
			StringBuffer sql = new StringBuffer();
			sql.append("UPDATE t_anchor a SET a.t_state = CASE a.t_state  WHEN 0 THEN 1 WHEN 1 THEN 2 WHEN 2 THEN 0 END WHERE a.t_user_id IN (");
			sql.append("SELECT u.t_id  FROM t_user u WHERE t_id IN (SELECT v.t_user_id FROM t_virtual v) AND u.t_disable !=2)");
			
			this.getFinalDao().getIEntitySQLDAO().executeSQL(sql.toString());
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("修改虚拟主播异常!", e);
		}
	}

}
