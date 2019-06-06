package com.yiliao.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.yiliao.service.FollowService;
import com.yiliao.util.DateUtils;
import com.yiliao.util.MessageUtil;

/**
 * 关注相关服务实现类
 * 
 * @author Administrator
 * 
 */
@Service("followService")
public class FollowServiceImpl extends ICommServiceImpl implements
		FollowService {

	public MessageUtil mu = null;

	/*
	 * 分页获取浏览记录 (non-Javadoc)
	 * 
	 * @see com.yiliao.service.app.FollowService#getFollowList(int, int)
	 */
	@Override
	public MessageUtil getFollowList(int userId, int page) {
		try {

			String sql = "SELECT u.t_id,u.t_cover_img,u.t_handImg,u.t_nickName,u.t_age,u.t_city,u.t_autograph, a.t_state,f.t_cover_follow FROM t_follow f LEFT JOIN t_user u ON f.t_cover_follow = u.t_id LEFT JOIN t_anchor a ON a.t_user_id = u.t_id  WHERE f.t_follow_id = ? ORDER BY a.t_state ASC,f.t_create_time DESC LIMIT ?,10 ";

			List<Map<String, Object>> dataList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, userId,(page-1)*10);
			
			//统计总数据
			sql = "SELECT count(f.t_id) AS total FROM t_follow f LEFT JOIN t_user u ON f.t_cover_follow = u.t_id WHERE f.t_follow_id = ? ";
			
			Map<String, Object> total = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql, userId);
			
			int pageCount = Integer.parseInt(total.get("total")
					.toString()) % 10 == 0 ? Integer.parseInt(total.get(
					"total").toString()) / 10
					: Integer.parseInt(total.get("total")
							.toString()) / 10 + 1;
					
			for(Map<String, Object> m : dataList){
				m.put("avgScore", null==m.get("t_cover_follow")?5:
					this.avgScore(Integer.parseInt(m.get("t_cover_follow").toString())));
			}
			
			
			mu = new MessageUtil();
			mu.setM_object(new HashMap<String,Object>(){{
				put("pageCount", pageCount);
				put("data", dataList);
			}});
			mu.setM_istatus(1);
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取浏览记录异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	
	/**
	 * 计算平均分
	 * @param userId
	 */
	public int avgScore(int userId){
		
		String sql = "SELECT AVG(t_score) AS avgScore FROM t_user_evaluation WHERE t_anchor_id = ?";
		
		Map<String, Object> avgScore = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(sql, userId);
		
		Double avg= Double.valueOf(null ==avgScore.get("avgScore")?"5":avgScore.get("avgScore").toString());
		
		
		return new BigDecimal(avg).setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
	}
	/*
	 * 添加关注 (non-Javadoc)
	 * 
	 * @see com.yiliao.service.app.FollowService#saveFollow(int, int)
	 */
	@Override
	public MessageUtil saveFollow(int followUserId, int coverFollowUserId) {
		try {

			List<Map<String,Object>> sqlList = this.getQuerySqlList("SELECT t_id FROM t_follow  WHERE t_follow_id = ? AND t_cover_follow = ?", followUserId,coverFollowUserId);
			//用户未关注过
			if(sqlList.isEmpty()) {
				
				String sql = "INSERT INTO t_follow (t_follow_id, t_cover_follow, t_create_time) VALUES ( ?, ?, ?);";
				
				this.executeSQL(sql, followUserId,coverFollowUserId,DateUtils.format(new Date(),DateUtils.FullDatePattern));
			}
			
			mu = new MessageUtil(1, "关注成功!");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}关注{}异常!",followUserId,coverFollowUserId,e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}


	/*
	 * 删除关注(non-Javadoc)
	 * @see com.yiliao.service.app.FollowService#delFollow(int)
	 */
	@Override
	public MessageUtil delFollow(int followId,int coverFollow) {
		try {
			
			String sql = "DELETE FROM t_follow WHERE t_follow_id = ? and t_cover_follow = ?";
            this.getFinalDao().getIEntitySQLDAO().executeSQL(sql, followId,coverFollow);
            
            mu = new MessageUtil(1, "取消关注成功!");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}取消关注异常!", followId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}


	/*
	 * 获取用户是否关注指定用户(non-Javadoc)
	 * @see com.yiliao.service.FollowService#getSpecifyUserFollow(int, int)
	 */
	@Override
	public MessageUtil getSpecifyUserFollow(int userId, int coverFollow) {
		try {
			List<Map<String,Object>> sqlList = this.getQuerySqlList("SELECT t_id FROM t_follow  WHERE t_follow_id = ? AND t_cover_follow = ?", userId,coverFollow);
			//用户未关注过
			
			if(sqlList.isEmpty()) {
				return new MessageUtil(1,0);
			}else {
				return new MessageUtil(1, 1);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取是否关注{}异常!",userId,coverFollow,e);
			return new MessageUtil(0, "程序异常!");
		}
	}

}
