<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%
	String path = request.getContextPath();
	String basePath = request.getScheme() + "://"
			+ request.getServerName() + ":" + request.getServerPort()
			+ path + "/";
%>

<!doctype html>
<html>
<head design-width="750">
<meta charset="utf-8">
<meta name="viewport"
	content="width=device-width,minimum-scale=1.0,maximum-scale=1.0,user-scalable=no" />
<meta name="apple-mobile-web-app-capable" content="yes">
<meta name="apple-mobile-web-app-status-bar-style" content="black">
<meta name="format-detection" content="telephone=no">
<title>Demo</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/banner/reset.css" />
<!--重置样式-->
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/banner/style.css" />
<!--页面样式-->
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/banner/common.css" />
<!--常用样式库-->
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/banner/animate.min.css" />
<!--CSS3动画库-->
<script src="${pageContext.request.contextPath}/js/javascript/auto-size.js"></script>
<!--设置字体大小-->
</head>
<body>
	<div class="mobile-wrap center bj2">
		<div class="box-wrapper">
			<div class="box">
				<h2>具体条款</h2>
				<b>一、 色情违规行为</b>
				<p>1、昵称、动态、相册出现色情、性暗示等内容；</p>
				<p>2、私信聊天过程中发布色情裸露照片，具有挑逗性文字内容；</p>
				<p>3、视频聊天中出现色情、裸露、诱惑、挑逗等行为。</p>


				<b>二、 诱导诈骗</b>
				<p>任何以加微信/QQ或其他平台裸聊的行为，或者以恋爱为理由讨要财务的，多为诈骗，欢迎用户举报。</p>

				<b>二、 诱导诈骗</b>
				<p>任何以加微信/QQ或其他平台裸聊的行为，或者以恋爱为理由讨要财务的，多为诈骗，欢迎用户举报。</p>


				<b>三、 垃圾广告</b>
				<p>昵称、动态、相册、私信、视频等出现广告信息，涉及推销任何物品、服务、app等均视为违规行为。</p>

				<b>四、 其他违规</b>
				<p>1、传播危害国家政治政权的内容；</p>
				<p>2、传播涉及封建迷信的内容；</p>
				<p>3、传播暴力、恐怖、血腥的内容；</p>
				<p>4、传播其他违法内容。</p>
			</div>

			<div class="box">
				<h2>处理方法</h2>
				<strong>为了保证平台的基本秩序，让大家拥有良好的聊天环境，促进平台健康发展，对于以上违规行为，我们将严厉打击，情节严重者，我们将向网监部门举报。</strong>
			</div>
		</div>
		<div class="copyright">
			<p>*最终解释权归平台团队所有</p>
		</div>

	</div>
	<!--mobile_wrap-->
	<script src="${pageContext.request.contextPath}/js/jquery-2.2.4.min.js"></script>
	<!--jQ库-->
	<script src="${pageContext.request.contextPath}/js/swiper-4.2.0.min.js"></script>
	<!--轮播库-->
	<script src="${pageContext.request.contextPath}/js/MobEpp-1.1.1.js"></script>
	<!--封装函数-->

</body>
</html>

