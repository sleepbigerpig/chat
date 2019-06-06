<%@ page language="java" import="java.util.*" pageEncoding="utf-8"%>
<%
	String path = request.getContextPath();
	String basePath = request.getScheme() + "://"
			+ request.getServerName() + ":" + request.getServerPort()
			+ path + "/";
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<meta charset="utf-8">
<title>平台APP招募主播啦!!</title>
<style type="text/css">
* {
	margin: 0;
	padding: 0;
}
</style>
<script src="../js/jquery-2.0.3.min.js"></script>
<script src="../js/mobile-detect.js"></script>
<script type="text/javascript">
	//获取初始信息
	var app = navigator.appVersion;
	console.log(app);
	//根据括号进行分割
	var left = app.indexOf('(');
	var right = app.indexOf(')');
	var str = app.substring(left + 1, right);

	var Str = str.split(";");
	//手机型号--苹果 iPhone
	var Mobile_Iphone = Str[0];
	//手机型号--安卓 
	var Mobile_Android = Str[2];
	// 红米手机等特殊型号处理 匹配字符
	var res = /Android/;
	var reslut = res.test(Mobile_Android);

	//设备
	var equipment = '';
	//系统
	var system_moble = '';

	//根据设备型号判断设备系统
	if (Mobile_Iphone.indexOf('iPhone') >= 0) {
		equipment = Mobile_Iphone;
		system_moble = Str[1].substring(4, Str[1].indexOf('like'));
	} else if (Mobile_Iphone == 'Linux') {
		if (reslut) {
			equipment = Str[4].substring(0, 10);
			system_moble = Str[2];
			//alert('访问设备型号' + Str[4].substring(0, 9) + '|系统版本' + Str[2]+'|设备号:'+Str[2]);
		} else {
			equipment = Mobile_Android.substring(0, Mobile_Android.indexOf('Build'));
			system_moble = Str[1];
			//alert('访问设备型号' + Mobile_Android.substring(0, 9) + '|系统版本' + Str[1]+'|设备号:'+Str[2]);
		}
	}
</script>
</head>
<body>
	<input type="hidden" id="userId" value="${userId}">
	<img src="../img/anchors.jpg" width="100%">
	<div style="float: clear:both;margin: -75% 0px 10px 12% ">
		<a href="javascript:on_click_download();"> 
		<img src="../img/buton.png" width="90%"></a>
	</div>
</body>
<script type="text/javascript">
	$(function() {
		  $.ajax({
			type : 'POST',
			url : '../share/addShareInfo.do',
			data : {
				userId : $('#userId').val(),
				equipment : equipment,
				system_moble : system_moble
			},
			dataType : 'json',
			success : function(data) {
				if (data.m_istatus == 1) {
					$("#myDel").modal('hide');
					$("#utable").bootstrapTable('refreshOptions', {
						page : 1
					});
				} else {
					window.location.href = projectPath + '/error.html';
				}
			}
		});
	});
	
	//点击下载
	function on_click_download() {
		if (Mobile_Iphone.indexOf('iPhone') >= 0) {
			//苹果下载地址
			window.location.href = '';
		} else {
			window.location.href = '';
		}
	}
</script>
</html>
