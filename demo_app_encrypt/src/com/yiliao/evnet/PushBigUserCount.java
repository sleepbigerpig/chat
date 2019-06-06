package com.yiliao.evnet;

import java.util.Map;

import org.springframework.context.ApplicationEvent;

public class PushBigUserCount extends ApplicationEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PushBigUserCount(Map<String, Object> source) {
		super(source);
	}

}
