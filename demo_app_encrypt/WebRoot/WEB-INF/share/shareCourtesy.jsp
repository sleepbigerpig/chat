<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%
String path = request.getContextPath();
String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";
%>

<!doctype html>
<html>
<head design-width="750">
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,minimum-scale=1.0,maximum-scale=1.0,user-scalable=no"/>
<meta name="apple-mobile-web-app-capable" content="yes">
<meta name="apple-mobile-web-app-status-bar-style" content="black">
<meta name="format-detection" content="telephone=no">
<title>Demo</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/banner/reset.css" /><!--重置样式-->
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/banner/style.css" /><!--页面样式-->
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/banner/common.css" /><!--常用样式库-->
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/banner/animate.min.css" /><!--CSS3动画库-->
<script src="${pageContext.request.contextPath}/js/javascript/auto-size.js"></script><!--设置字体大小-->
</head>
<body>
	<div class="mobile-wrap center bj1">
		
		<div class="wrap-box">
			<div class="box-one">
				<p>为了感谢各位聊友对平台的大力支持，从即日起。凡邀请好友注册成功，平台官方奖励如下：</p>
			</div>
			<div class="box-two">
				<ul>
					<li>
						<b>邀请主播可获收益</b>
						<p>第一级   10%</p>
						<p>第二级    5%</p>
					</li>
					<li>
						<b>邀请用户可获收益</b>
						<p>第一级   5%</p>
						<p>第二级    3%</p>
					</li>
				</ul>
			</div>
		</div>
	
	</div><!--mobile_wrap-->

	<script src="${pageContext.request.contextPath}/js/javascript/jquery-2.2.4.min.js"></script><!--jQ库-->
	<script src="${pageContext.request.contextPath}/js/javascript/swiper-4.2.0.min.js"></script><!--轮播库-->
	<script src="${pageContext.request.contextPath}/js/javascript/MobEpp-1.1.1.js"></script><!--封装函数-->
	<script>
		
	</script>
</body>
</html>

