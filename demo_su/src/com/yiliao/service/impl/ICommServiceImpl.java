package com.yiliao.service.impl;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.yiliao.dao.core.FinalDao;
import com.yiliao.exception.BusinessException;
import com.yiliao.service.ICommService;

/**
 * 公共业务接口实现类
 * @author ZhouShuhua
 */
@Service("iCommServiceImpl")
public class ICommServiceImpl implements ICommService {
	


	private FinalDao finalDao;
	
	public String m_strResult;
	
	public String getM_strResult() {
		return m_strResult;
	}

	public void setM_strResult(String mStrResult) {
		m_strResult = mStrResult;
	}

	protected Logger logger = LoggerFactory.getLogger(getClass());
	
	// setter/gette
	public FinalDao getFinalDao() {
		return finalDao;
	}

	public void setFinalDao(FinalDao finalDao) {
		this.finalDao = finalDao;
	}
	
	
	/**
	 * 保存对象
	 */
	@Override
	public Serializable add(Object obj) throws BusinessException {
		
		logger.debug("添加对象"+obj.getClass().getName());
		
		// 添加
		return this.finalDao.getIEntityHQLDAO().save(obj);
	}
	
	public void update(Object obj){
		
		logger.debug("更新对象"+obj.getClass().getName());
		
		this.finalDao.getIEntityHQLDAO().update(obj);;
	
	}
}
