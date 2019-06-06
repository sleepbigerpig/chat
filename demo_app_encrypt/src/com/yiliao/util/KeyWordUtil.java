package com.yiliao.util;

import java.util.Arrays;
import java.util.List;

public class KeyWordUtil {

	/**
	 *  过滤字符串  出现关键字的地方已*号代替
	 * @param keyWords
	 * @param str
	 * @return
	 */
	public static String  filterKeyWord(String[] keyWords,String str) {
		
		 char[] str_char = str.toCharArray();
		 
		 for(char c : str_char) {
			 for(String s : keyWords) {
				 if(String.valueOf(c).equals(s)) {
					 str = str.replaceAll(String.valueOf(c), "*");
				 }
			 }
		 }
		
//		for(String s : keyWords) {
//			char[] charArray = s.toCharArray();
//			int count = 0;
//			for(char c : charArray) {
//				 if(str.indexOf(c)>=0){
//					 count = count+1;
//				 }
//			}
//			if(count == charArray.length) {
//				for(char c : charArray) {
//					 str = str.replaceAll(String.valueOf(c), "*");
//				}
//				return str;
//			}
//		}
		System.out.println("循环完毕后返回");
		return str;
	}
	
	
	public static void main(String[] args) {
		 
		String[] str = {"招聘","脱","秀","炮","约","逼","官方","运营","客服","宜聊","平台","骗子","闪播","培训","主播","微信","扣扣","开发","软件",
				"0","1","2","3","4","5","6","7","8","9","www","cn","com","net"};
		
		
		System.out.println(filterKeyWord(str, "家我QQ234"));
	}
}
