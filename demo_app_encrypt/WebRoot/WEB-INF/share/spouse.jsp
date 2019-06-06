<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%
	String path = request.getContextPath();
	String basePath = request.getScheme() + "://"
			+ request.getServerName() + ":" + request.getServerPort()
			+ path + "/";
%>

<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>马云2018精彩演讲,你是要死工资还是要赚钱的机会?</title>
<meta name="viewport"
	content="width=device-width, initial-scale=1.0, user-scalable=0, minimum-scale=1.0, maximum-scale=1.0">
<meta name="format-detection" content="telephone=no" />
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/reset.css">
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/style.css">
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
	<img src="${pageContext.request.contextPath}/img/img_1.jpg">
	<img src="${pageContext.request.contextPath}/img/img_2.jpg">
	<img src="${pageContext.request.contextPath}/img/img_3.jpg">
	<img src="${pageContext.request.contextPath}/img/img_4.jpg">
	<img src="${pageContext.request.contextPath}/img/img_5.jpg">
	<img src="${pageContext.request.contextPath}/img/img_6.jpg">
	<img src="${pageContext.request.contextPath}/img/img_7.jpg">
	<img src="${pageContext.request.contextPath}/img/img_8.jpg">
	<img src="${pageContext.request.contextPath}/img/img_9.jpg">
	<div class="share">
		<img src="${pageContext.request.contextPath}/img/img_10.jpg"> <a class="share_btn" >立即分享</a>
	</div>
	<div class="copy">
		<img src="${pageContext.request.contextPath}/img/img_11.jpg">
		<div class="copy_txt">
			<p class="vx_number">
				微信号:<span class="txt_bar" id="textDiv1" onclick="execClick();"
					oncopy="execCopy(event,'textDiv1');">mmm500005</span>
			</p>
			<p>点击复制</p>
			<a href="javascript:on_click_download();" class="download_btn"></a>
		</div>
	</div>
	<div id="screen">
    	<img src="${pageContext.request.contextPath}/img/share.png"> 
        <p>点击右上角<br/>将它分享到朋友圈<br/>或指定的朋友</p>
    </div>
</body>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery-2.0.3.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/screen.js"></script>
<script>
	function execClick() {
		document.execCommand("copy");
	}
	function execCopy(event, textDiv) {
		var thisDiv = document.getElementById(textDiv);
		if (isIE()) {
			if (window.clipboardData) {
				window.clipboardData.setData("Text", thisDiv.textContent);
				alert(window.clipboardData.getData("Text"));
			}
		} else {
			event.preventDefault();
			if (event.clipboardData) {
				event.clipboardData.setData("text/plain", thisDiv.textContent);
				alert(event.clipboardData.getData("text"));
			}
		}
	}

	function isIE() {
		var input = window.document.createElement("input");
		//"!window.ActiveXObject" is evaluated to true in IE11
		if (window.ActiveXObject === undefined)
			return null;
		if (!window.XMLHttpRequest)
			return 6;
		if (!window.document.querySelector)
			return 7;
		if (!window.document.addEventListener)
			return 8;
		if (!window.atob)
			return 9;
		//"!window.document.body.dataset" is faster but the body is null when the DOM is not
		//ready. Anyway, an input tag needs to be created to check if IE is being
		//emulated
		if (!input.dataset)
			return 10;
		return 11;
	}
	
	//点击下载
	function on_click_download() {
		if (Mobile_Iphone.indexOf('iPhone') >= 0) {
			//苹果下载地址
			window.location.href = '';
		} else {
			window.location.href = '';
		}
	}
	 
	
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
		  
		$('.share_btn').click(function(){
				$('#screen').show();
			});
		$('#screen').click(function(){
				$(this).hide();
		});
	});

</script>
</html>