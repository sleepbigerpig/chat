package com.yiliao.domain;

import java.sql.Timestamp;

public class SmsDescribe {

	/**手机号*/
	private String phone;
	/**验证码 */
	private String smsCode;
	/**发送时间*/
	private Timestamp time;
	/** 图形验证码 */
	private String imgVerify;
	
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	public String getSmsCode() {
		return smsCode;
	}
	public void setSmsCode(String smsCode) {
		this.smsCode = smsCode;
	}
	public Timestamp getTime() {
		return time;
	}
	public void setTime(Timestamp time) {
		this.time = time;
	}
	public String getImgVerify() {
		return imgVerify;
	}
	public void setImgVerify(String imgVerify) {
		this.imgVerify = imgVerify;
	}
	
	

}
