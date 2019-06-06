package com.yiliao.control.version2_0;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yiliao.service.version2_0.BigRoomManService;
/**
 * 大房间管理
 * @author Administrator
 *
 */
import com.yiliao.util.MessageUtil;
@Controller
@RequestMapping(value = {"admin"})
public class BigRoomManControl {

	@Autowired
	private BigRoomManService bigRoomManService;
	
	/**
	 * 获取大房间直播的主播列表
	 * @param page
	 * @return
	 */
	@RequestMapping(value= {"getBigRoomManList"},method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> getBigRoomManList(int page){
		
		return bigRoomManService.getBigRoomManList(page);
	}
	
	/**
	 * 取消主播大房间资格
	 * @param id
	 * @return
	 */
	@RequestMapping(value = {"delBigRoomAnchor"},method  = RequestMethod.POST)
	@ResponseBody
	public MessageUtil delBigRoomAnchor(int id) {
	
		return this.bigRoomManService.delBigRoomAnchor(id);
	}
	
	/**
	 * 主播加入大房间
	 * @param idcard
	 * @param sort
	 * @return
	 */
	@RequestMapping(value = {"mixBigRoom"},method = RequestMethod.POST)
	@ResponseBody
	public MessageUtil mixBigRoom(Integer t_id,int idcard,int sort) {
		
		return this.bigRoomManService.mixBigRoom(t_id, idcard, sort);
	}
	
	@RequestMapping("test")
	@ResponseBody
	public void test() {
//		System.out.println("开始创建聊天室");
//		JpushImUtil imUtil = new JpushImUtil();
//		try {
//			imUtil.createChatRoom();
//		} catch (APIConnectionException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (APIRequestException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		System.out.println("创建完成1");
	}
	
}
