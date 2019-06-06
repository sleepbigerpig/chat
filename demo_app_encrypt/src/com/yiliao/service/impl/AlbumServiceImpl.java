package com.yiliao.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.yiliao.domain.Dynamic;
import com.yiliao.domain.MessageEntity;
import com.yiliao.domain.WalletDetail;
import com.yiliao.evnet.PushMesgEvnet;
import com.yiliao.service.AlbumService;
import com.yiliao.timer.YellowingTimer;
import com.yiliao.util.CheckImgUtil;
import com.yiliao.util.DateUtils;
import com.yiliao.util.MessageUtil;
import com.yiliao.util.VidelSingUtil;

/**
 * 相册相关
 * 
 * @author Administrator
 *
 */
@Service("albumService")
public class AlbumServiceImpl extends ICommServiceImpl implements AlbumService {

	private MessageUtil mu = null;

	/*
	 * 获取我的相册列表 (non-Javadoc)
	 * 
	 * @see com.yiliao.service.app.PersonalCenterService#getMyPhotoList(int, int)
	 */
	@Override
	public MessageUtil getMyPhotoList(int userId, int page) {
		try {

			String sql = "SELECT d.t_id,d.t_title,d.t_video_img,d.t_addres_url,d.t_file_type,d.t_is_private,t_money,t_auditing_type  FROM t_album d  WHERE d.t_user_id = ? AND d.t_is_del = ? ORDER BY d.t_id DESC LIMIT ?,10";

			List<Map<String, Object>> dynamicList = this.getQuerySqlList(sql, userId, Dynamic.IS_DEL_NO,
					(page - 1) * 10);

			sql = "SELECT count(*) as total  FROM t_album d  WHERE d.t_user_id = ? AND d.t_is_del = ?";

			Map<String, Object> total = this.getMap(sql, userId, Dynamic.IS_DEL_NO);

			int pageCount = Integer.parseInt(total.get("total").toString()) % 10 == 0
					? Integer.parseInt(total.get("total").toString()) / 10
					: Integer.parseInt(total.get("total").toString()) / 10 + 1;

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(new HashMap<String, Object>() {
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

	/**
	 * 上传至相册
	 */
	@SuppressWarnings("unchecked")
	@Override
	public MessageUtil addMyPhotoAlbum(int userId, String t_title, String video_img, String fileId, String url,
			int type, int gold) {
		try {

			// 获取秘钥信息
			String qSql = " SELECT t_app_id,t_secret_id,t_secret_key,t_bucket FROM t_object_storage ";

			Map<String, Object> querySqlList = this.getMap(qSql);

			// 上传图片
			if (type == 0) {
				String[] str = { url };

				// 调用鉴黄系统 鉴定封面是否违规
				Map<String, Object> imagePorn = CheckImgUtil.imagePorn(str, querySqlList.get("t_app_id").toString(),
						querySqlList.get("t_secret_id").toString(), querySqlList.get("t_secret_key").toString(),
						querySqlList.get("t_bucket").toString());

				List<String> pornUrl = (List<String>) imagePorn.get("pornUrl");
				// 如果存在疑是违禁图片 存储到模糊图片表中 后期人工审核
				if (null != pornUrl && !pornUrl.isEmpty()) {
					String inseSql = "INSERT INTO t_vague_check (t_img_url, t_data_type, t_user_id,t_gold, t_create_time) VALUES (?, ?, ?, ?,?)";
					this.executeSQL(inseSql, pornUrl.get(1), 2, userId, gold,
							DateUtils.format(new Date(), DateUtils.FullDatePattern));
					// 异步通知
					this.applicationContext.publishEvent(
							new PushMesgEvnet(new MessageEntity(userId, "您上传的图片存在异常,已进行删除处理.", 0, new Date())));
					return new MessageUtil(0, "您上传的图片存在异常,已进行删除处理.");
				}

				String inseSql = "INSERT INTO t_album (t_user_id, t_title, t_addres_url, t_file_type, t_is_private,  t_money, t_is_del,t_auditing_type,t_create_time) VALUES (? ,?, ?, ?, ?, ?, ?,?,?);";

				this.executeSQL(inseSql, userId, t_title, url, type, gold > 0 ? 1 : 0, gold, 0, 0,
						DateUtils.format(new Date(), DateUtils.FullDatePattern));

			} else { // 上传视频
						// 保存至数据库
				String inseSql = "INSERT INTO t_album (t_user_id, t_title, t_fileId, t_video_img, t_addres_url, t_file_type, t_is_private, t_money, t_is_del, t_auditing_type,t_see_count,t_create_time) VALUES (?,?,?,?,?,?,?,?,?,?,0,?);";

				this.executeSQL(inseSql, userId, t_title, fileId, video_img, url, type, gold > 0 ? 1 : 0, gold, 0, 0,
						DateUtils.format(new Date(), DateUtils.FullDatePattern));
				// 调起鉴黄设置
				VidelSingUtil.yellowing(fileId, querySqlList.get("t_secret_id").toString(),
						querySqlList.get("t_secret_key").toString());
				// 异步通知
				this.applicationContext
						.publishEvent(new PushMesgEvnet(new MessageEntity(userId, "视频已上传,正在审核中.", 0, new Date())));
				// 把文件编号装载到需要拉取鉴黄结果的集合中
				YellowingTimer.fileIdList.add(fileId);
			}

			mu = new MessageUtil(1, "上传成功!");

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("保存至相册异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/**
	 * 获取图片列表或者视频列表
	 */
	@Override
	public MessageUtil getDynamicList(int seeUserId, int coverUserId, int page) {

		try {

			String sql = "SELECT d.t_id,d.t_title,d.t_file_type,d.t_addres_url,d.t_is_private,d.t_money,d.t_video_img,u.t_handImg,u.t_nickName FROM t_album  d LEFT JOIN t_user u ON d.t_user_id = u.t_id WHERE t_user_id = ? AND d.t_auditing_type =1 AND d.t_is_del = 0 ORDER BY d.t_id DESC LIMIT ?,10";

			List<Map<String, Object>> dataList = this.getQuerySqlList(sql, coverUserId, (page - 1) * 10);

			sql = "SELECT count(*) as totalCount FROM t_album  d LEFT JOIN t_user u ON d.t_user_id = u.t_id WHERE t_user_id = ? AND d.t_auditing_type =1 AND d.t_is_del = 0 ";

			Map<String, Object> totalCount = this.getMap(sql, coverUserId);

			// 查询当前数据是否已经被查看过了
			final String qSql = "SELECT * FROM t_order WHERE t_consume = ?  AND t_cover_consume = ? AND t_consume_type = ?  AND t_consume_score = ?";

			dataList.forEach(s -> {

				int file_type = Integer.parseInt(s.get("t_file_type").toString());

				List<Map<String, Object>> findBySQLTOMap = this.getQuerySqlList(qSql, seeUserId, coverUserId,
						file_type == 0 ? WalletDetail.CHANGE_CATEGORY_PRIVATE_PHOTO
								: WalletDetail.CHANGE_CATEGORY_PRIVATE_VIDEO,
						Integer.parseInt(s.get("t_id").toString()));

				if (null == findBySQLTOMap || findBySQLTOMap.isEmpty()) {
					s.put("is_see", 0);
				} else {
					s.put("is_see", 1);
				}
			});

			int pageCount = Integer.parseInt(totalCount.get("totalCount").toString()) % 10 == 0
					? Integer.parseInt(totalCount.get("totalCount").toString()) / 10
					: Integer.parseInt(totalCount.get("totalCount").toString()) / 10 + 1;

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
			logger.error("获取用户的图片或者视频", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 删除我的相册照片或者文件 (non-Javadoc)
	 * 
	 * @see com.yiliao.service.app.PersonalCenterService#delMyPhoto(int)
	 */
	@Override
	public MessageUtil delMyPhoto(int photoId) {
		try {
			String sql = " DELETE FROM t_album WHERE t_id= ?";

			this.executeSQL(sql, photoId);

			mu = new MessageUtil(1, "删除成功!");

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("删除我的照片异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 获取用户相册(non-Javadoc)
	 * 
	 * @see com.yiliao.service.AlbumService#getMyAnnualAlbum(int, int)
	 */
	@Override
	public MessageUtil getMyAnnualAlbum(int userId, int page) {
		try {

			Map<String, Object> linkedHashMap = new HashMap<String, Object>();
			StringBuffer qSql = new StringBuffer();
			qSql.append("SELECT ");
			qSql.append(" t_id,t_video_img,t_addres_url,t_file_type,t_money,t_auditing_type ");
			qSql.append("FROM ");
			qSql.append(" t_album ");
			qSql.append("WHERE ");
			qSql.append(" t_user_id = ? ");
			qSql.append(" AND t_is_del = 0  ");
			qSql.append(" ORDER BY t_create_time DESC  LIMIT ?,15;");

			List<Map<String, Object>> sqlList = this.getQuerySqlList(qSql.toString(), userId, (page - 1) * 15);

			Map<String, Object> toMap = this
					.getMap("SELECT COUNT(*) AS total FROM t_album WHERE t_user_id = ? AND t_is_del = 0 ", userId);

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(new HashMap<String, Object>() {
				{
					put("total", toMap.get("total"));
					put("data", sqlList);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取相册异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	@Override
	public MessageUtil getMyMonthAlbum(int userId, int year, int month, int page) {
		try {

			StringBuffer qSql = new StringBuffer();
			qSql.append("SELECT ");
			qSql.append(" t_id,t_video_img,t_addres_url,t_file_type,t_money,t_auditing_type ");
			qSql.append("FROM ");
			qSql.append(" t_album ");
			qSql.append("WHERE ");
			qSql.append(" t_user_id = ? ");
			qSql.append(" AND t_is_del = 0  ");
			qSql.append(" AND t_create_time BETWEEN ? AND ? ORDER BY t_create_time DESC  LIMIT ?,12;");

			List<Map<String, Object>> querySqlList = this.getQuerySqlList(qSql.toString(), userId,
					DateUtils.getFirstDayOfMonth(year, month), DateUtils.getLastDayOfMonth(year, month),
					(page - 1) * 12);

			// 获取当前月 有多少个视频和照片

			StringBuffer countSql = new StringBuffer();
			countSql.append("SELECT ");
			countSql.append(" COUNT(t_id) AS total ");
			countSql.append("FROM ");
			countSql.append(" t_album ");
			countSql.append("WHERE ");
			countSql.append(" t_user_id = ? ");
			countSql.append(" AND t_is_del = 0  ");
			countSql.append(" AND t_create_time BETWEEN ? AND ? ");

			Map<String, Object> total = this.getMap(countSql.toString(), userId,
					DateUtils.getFirstDayOfMonth(year, month), DateUtils.getLastDayOfMonth(year, month));

			StringBuffer imgSql = new StringBuffer();
			imgSql.append("SELECT ");
			imgSql.append(" COUNT(t_id) AS total ");
			imgSql.append("FROM ");
			imgSql.append(" t_album ");
			imgSql.append("WHERE ");
			imgSql.append(" t_user_id = ? ");
			imgSql.append(" AND t_is_del = 0  ");
			imgSql.append(" AND t_file_type = 0 ");
			imgSql.append(" AND t_create_time BETWEEN ? AND ? ");

			Map<String, Object> imgTotal = this.getMap(imgSql.toString(), userId,
					DateUtils.getFirstDayOfMonth(year, month), DateUtils.getLastDayOfMonth(year, month));

			StringBuffer videoSql = new StringBuffer();
			videoSql.append("SELECT ");
			videoSql.append(" COUNT(t_id) AS total ");
			videoSql.append("FROM ");
			videoSql.append(" t_album ");
			videoSql.append("WHERE ");
			videoSql.append(" t_user_id = ? ");
			videoSql.append(" AND t_is_del = 0  ");
			videoSql.append(" AND t_file_type = 1 ");
			videoSql.append(" AND t_create_time BETWEEN ? AND ? ");

			Map<String, Object> videoTotal = this.getMap(videoSql.toString(), userId,
					DateUtils.getFirstDayOfMonth(year, month), DateUtils.getLastDayOfMonth(year, month));

			mu = new MessageUtil();
			mu.setM_istatus(1);
			mu.setM_object(new HashMap<String, Object>() {
				{
					put("data", querySqlList);
					put("pageCount",
							Integer.parseInt(total.get("total").toString()) % 10 == 0
									? Integer.parseInt(total.get("total").toString()) / 10
									: Integer.parseInt(total.get("total").toString()) / 10 + 1);
					put("imgTotal", imgTotal.get("total"));
					put("videoTotal", videoTotal.get("total"));
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取月相册异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

}
