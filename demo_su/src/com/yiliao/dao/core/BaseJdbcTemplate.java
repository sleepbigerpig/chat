package com.yiliao.dao.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * jdbc操作
 * @author ZhouShuhua
 *
 */
public class BaseJdbcTemplate extends JdbcTemplate {
	
	protected Logger logger = LoggerFactory.getLogger(getClass());
	
	private String dbType;
	
	
	/** ORACLE 分页查询模版 */
	private static final String ORACLE_PAGESQL_TEMPLATE = "SELECT * FROM (SELECT XX.*,ROWNUM ROW_NUM FROM (${sql}) XX) ZZ where ZZ.ROW_NUM BETWEEN ${startNum} AND ${endNum}";
	
	/**
	 * SQLSERVER 分页查询模板
	 */
	private static final String SQLSERVER_PAGESQL_TEMPLATE = "SELECT * FROM (SELECT TOP(${endNum}) ROW_NUMBER() OVER(Order by isNull(1,1)) as NUMS,* FROM (${sql}) TB) TB WHERE NUMS > ${startNum}";
	
	/**
	 * 注入数据库类型
	 * @param dbType
	 */
	public void setDbType(String dbType) {
		this.dbType = dbType;
	}

	
	

}
