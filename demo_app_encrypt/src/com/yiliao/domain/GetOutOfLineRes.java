package com.yiliao.domain;

/**
 * 给用户推送封号消息
 * @author Administrator
 *
 */
public class GetOutOfLineRes extends Mid {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**消息内容*/
	private String message ;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	} 

}
