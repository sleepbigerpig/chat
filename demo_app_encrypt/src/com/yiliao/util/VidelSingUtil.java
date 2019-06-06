package com.yiliao.util;

import java.util.TreeMap;

import com.yiliao.util.Module.Vod;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


public class VidelSingUtil {
 
	public static void main(String[] args) {
		try {
//			yellowing("5285890780458401008");
//			pullYellowingResult("5285890781659076503");
			
//			pullVideoNotice();
		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}

	/**
	 * 发起鉴黄
	 * @throws Exception
	 */
	public static void yellowing(String fileId,String SecretId,String SecretKey) throws Exception {
		

		/* 如果是循环调用下面举例的接口，需要从此处开始你的循环语句。切记！ */
		TreeMap<String, Object> config = new TreeMap<String, Object>();
		config.put("SecretId", SecretId);
		config.put("SecretKey", SecretKey);
		/* 请求方法类型 POST、GET */
		config.put("RequestMethod", "GET");
		/* 区域参数，可选: gz:广州; sh:上海; hk:香港; ca:北美;等。 */
		config.put("DefaultRegion", "cq");

		/*
		 * 你将要使用接口所在的模块，可以从 官网->云api文档->XXXX接口->接口描述->域名
		 * 中获取，比如域名：cvm.api.qcloud.com，module就是 new Cvm()。
		 */
		/*
		 * 示例：DescribeInstances
		 * 的api文档地址：http://www.qcloud.com/wiki/v2/DescribeInstances
		 */
		QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Vod(),config);

		TreeMap<String, Object> params = new TreeMap<String, Object>();
		/* 将需要输入的参数都放入 params 里面，必选参数是必填的。 */
		/* DescribeInstances 接口的部分可选参数如下 */
		params.put("fileId", fileId);
		params.put("terrorismReview.definition", "10");
		params.put("pornReview.definition", "10");
//		params.put("priority", 2);
		/* 在这里指定所要用的签名算法，不指定默认为HmacSHA1 */
		// params.put("SignatureMethod", "HmacSHA256");

		/* generateUrl方法生成请求串,可用于调试使用 */
		// System.out.println(module.generateUrl("DescribeInstances", params));
		String result = null;
		try {
			/* call 方法正式向指定的接口名发送请求，并把请求参数params传入，返回即是接口的请求结果。 */
			result = module.call("ProcessFile", params);
			JSONObject json_result = JSONObject.fromObject(result);
			System.out.println(json_result);
		} catch (Exception e) {
			System.out.println("error..." + e.getMessage());
		}

	}
	
	
	
	/**
	 * 拉取鉴黄结果
	 * @param videoId
	 * @throws Exception
	 */
	public static int pullYellowingResult(String videoId,String SecretId,String SecretKey) throws Exception {
		
		System.out.println("开始获取鉴黄结果,文件ID-->"+videoId);
		/* 如果是循环调用下面举例的接口，需要从此处开始你的循环语句。切记！ */
		TreeMap<String, Object> config = new TreeMap<String, Object>();
		config.put("SecretId", SecretId);
		config.put("SecretKey", SecretKey);
		/* 请求方法类型 POST、GET */
		config.put("RequestMethod", "GET");
		/* 区域参数，可选: gz:广州; sh:上海; hk:香港; ca:北美;等。 */
		config.put("DefaultRegion", "cq");

		/*
		 * 你将要使用接口所在的模块，可以从 官网->云api文档->XXXX接口->接口描述->域名
		 * 中获取，比如域名：cvm.api.qcloud.com，module就是 new Cvm()。
		 */
		/*
		 * 示例：DescribeInstances
		 * 的api文档地址：http://www.qcloud.com/wiki/v2/DescribeInstances
		 */
		QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Vod(),
				config);

		TreeMap<String, Object> params = new TreeMap<String, Object>();
		/* 将需要输入的参数都放入 params 里面，必选参数是必填的。 */
		/* DescribeInstances 接口的部分可选参数如下 */
		params.put("fileId", videoId);
		params.put("infoFilter.0", "contentReviewInfo");
		/* 在这里指定所要用的签名算法，不指定默认为HmacSHA1 */
		// params.put("SignatureMethod", "HmacSHA256");

		/* generateUrl方法生成请求串,可用于调试使用 */
		// System.out.println(module.generateUrl("DescribeInstances", params));
		int res ;
		
		String result = null;
		try {
			/* call 方法正式向指定的接口名发送请求，并把请求参数params传入，返回即是接口的请求结果。 */
			result = module.call("GetVideoInfo", params);
			
			JSONObject json_result =JSONObject.fromObject(result);
			
			System.out.println(json_result);
			
			if(!json_result.containsKey("contentReviewInfo")){
				System.out.println("鉴黄还未完成!");
				return 0;
			}else{
				
				JSONObject contentReviewInfo = json_result.getJSONObject("contentReviewInfo");
				
				JSONArray pornList = contentReviewInfo.getJSONArray("pornList");
				
				JSONArray terrorismList = contentReviewInfo.getJSONArray("terrorismList");
				
				JSONObject rese = pornList.getJSONObject(0);
				
				JSONObject terrorismInfo = terrorismList.getJSONObject(0);
				
				
				//pass : 通过，review:审查，block:阻止
				if(("pass".equals(rese.getString("suggestion")) && "pass".equals(terrorismInfo.getString("suggestion"))) || rese.getInt("confidence")<=83){
					res = 1;
				}else{
					//删除视频
					delVideo(videoId,SecretId,SecretKey);
					res =  -1 ;
				}
			}
		
		} catch (Exception e) {
			System.out.println("error..." + e.getMessage());
			res  = 0;
		}
        return res;
	}
	
	
	/**
	 *  删除视频
	 * @param videoId
	 * @throws Exception
	 */
	public static void delVideo(String videoId,String SecretId,String SecretKey) throws Exception {
		
		System.out.println("进入了删除视频 fileId-->"+videoId);
		
		/* 如果是循环调用下面举例的接口，需要从此处开始你的循环语句。切记！ */
		TreeMap<String, Object> config = new TreeMap<String, Object>();
		config.put("SecretId", SecretId);
		config.put("SecretKey", SecretKey);
		/* 请求方法类型 POST、GET */
		config.put("RequestMethod", "GET");
		/* 区域参数，可选: gz:广州; sh:上海; hk:香港; ca:北美;等。 */
		config.put("DefaultRegion", "cq");

		/*
		 * 你将要使用接口所在的模块，可以从 官网->云api文档->XXXX接口->接口描述->域名
		 * 中获取，比如域名：cvm.api.qcloud.com，module就是 new Cvm()。
		 */
		/*
		 * 示例：DescribeInstances
		 * 的api文档地址：http://www.qcloud.com/wiki/v2/DescribeInstances
		 */
		QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Vod(),
				config);

		TreeMap<String, Object> params = new TreeMap<String, Object>();
		/* 将需要输入的参数都放入 params 里面，必选参数是必填的。 */
		/* DescribeInstances 接口的部分可选参数如下 */
		params.put("fileId", videoId);
		params.put("priority", 0);
		/* 在这里指定所要用的签名算法，不指定默认为HmacSHA1 */
		// params.put("SignatureMethod", "HmacSHA256");

		/* generateUrl方法生成请求串,可用于调试使用 */
		// System.out.println(module.generateUrl("DescribeInstances", params));
		String result = null;
		try {
			/* call 方法正式向指定的接口名发送请求，并把请求参数params传入，返回即是接口的请求结果。 */
			result = module.call("DeleteVodFile", params);
			JSONObject json_result =JSONObject.fromObject(result);
			System.out.println(json_result);
		} catch (Exception e) {
			System.out.println("error..." + e.getMessage());
		}

	}
	
	
	/**
	 * 拉取事件通知
	 * @throws Exception
	 */
	public static void pullVideoNotice() throws Exception {
		
		/* 如果是循环调用下面举例的接口，需要从此处开始你的循环语句。切记！ */
		TreeMap<String, Object> config = new TreeMap<String, Object>();
		config.put("SecretId", "AKIDNgpocKHTyJTXQ8N3EE7rQo363qU1Mdie");
		config.put("SecretKey", "8LUSFrIl5JME4kFds1zmOwKgw45ygFe4");
		/* 请求方法类型 POST、GET */
		config.put("RequestMethod", "GET");
		/* 区域参数，可选: gz:广州; sh:上海; hk:香港; ca:北美;等。 */
		config.put("DefaultRegion", "cq");

		/*
		 * 你将要使用接口所在的模块，可以从 官网->云api文档->XXXX接口->接口描述->域名
		 * 中获取，比如域名：cvm.api.qcloud.com，module就是 new Cvm()。
		 */
		/*
		 * 示例：DescribeInstances
		 * 的api文档地址：http://www.qcloud.com/wiki/v2/DescribeInstances
		 */
		QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Vod(),
				config);

		TreeMap<String, Object> params = new TreeMap<String, Object>();
		/* 将需要输入的参数都放入 params 里面，必选参数是必填的。 */
		/* DescribeInstances 接口的部分可选参数如下 */
//		params.put("fileId", "5285890780455277566");
//		params.put("priority", 0);
		/* 在这里指定所要用的签名算法，不指定默认为HmacSHA1 */
		// params.put("SignatureMethod", "HmacSHA256");

		/* generateUrl方法生成请求串,可用于调试使用 */
		// System.out.println(module.generateUrl("DescribeInstances", params));
		String result = null;
		try {
			/* call 方法正式向指定的接口名发送请求，并把请求参数params传入，返回即是接口的请求结果。 */
			result = module.call("PullEvent", params);
			JSONObject json_result = JSONObject.fromObject(result);
			System.out.println(json_result);
		} catch (Exception e) {
			System.out.println("error..." + e.getMessage());
		}

	}

	
}
