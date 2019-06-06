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
<title>【在家赚钱】的APP盛大开业，任性发红包了！先到先得，领完为止！</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/gamereset.css" /><!--重置样式-->
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/gamestyle.css" /><!--页面样式-->
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css" /><!--常用样式库-->
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/animate.min.css" /><!--CSS3动画库-->
<script src="${pageContext.request.contextPath}/js/auto-size.js"></script><!--设置字体大小-->
</head>
<body>
	<div class="mobile-wrap center bj1">
			
		<div class="top">
			<a href="#" class="fl">
				<img src="${pageContext.request.contextPath}/img/ico4.png" alt="">
				<span><i>0.00</i>元</span>
			</a>
			<a href="#" class="fr">
				<img src="${pageContext.request.contextPath}/img/edfec8d1ae97fe94_57x66.png" alt="">
				<span><i>10</i>秒</span>
			</a>
		</div>
		
		<div class="show-box-area">
			<div class="begin">开  始</div>
			<div class="pict">
				<img style="width:1.12rem;" src="${pageContext.request.contextPath}/img/64f237684db83917_112x178.png" alt="">
				<img style="width:1.29rem;" src="${pageContext.request.contextPath}/img/ccc7f5b116a4867a_129x176.png" alt="">
				<img style="width:.7rem;" src="${pageContext.request.contextPath}/img/6a69471fdba8ddcc_70x180.png" alt="">
			</div>
		</div>

		<div class="red-envelopes center">
			<div class="left-bao fl">
			</div>
			<div class="right-bao fr">
			</div>

			<div class="arrowtop"><img src="${pageContext.request.contextPath}/img/cc72bc9da6fe27ea_81x81.png" alt=""></div>
			<div class="guang"></div>
			<div class="red-wrap state1">
				<div class="hongbao"></div>
				<div class="gameover">
					<img src="${pageContext.request.contextPath}/img/ico3.png" alt="">
				</div>
				<div class="insert">
					<span></span>
				</div>
			</div>
		</div>

		<div class="loadimg">
			<img src="${pageContext.request.contextPath}/img/110f4f53a5d76db8_750x802.png" alt="">
		</div>

		<audio src="${pageContext.request.contextPath}/img/game.mp3" id="audio" autobuffer></audio>

		<div class="alert-box center">
			<div class="layout">
				<div class="pict">
					<img src="${pageContext.request.contextPath}/img/ico7.png" alt="">
				</div>
				<div class="box">
					<p>恭喜您！获得<i>0</i>个红包<br>共计:</p>
					<b><i>0.00</i>元</b>
					<a href="javascript:on_click_download();">立 即 领 取</a>
				</div>
			</div>
		</div>
	</div><!--mobile_wrap-->
    <input type="hidden" id="userId" value="${userId}">
	<script src="${pageContext.request.contextPath}/js/jquery-2.2.4.min.js"></script><!--jQ库-->
	<script src="${pageContext.request.contextPath}/js/swiper-4.2.0.min.js"></script><!--轮播库-->
	<script src="${pageContext.request.contextPath}/js/MobEpp-1.1.1.js"></script><!--封装函数-->
	<script>

		var num=$(".top .fr i").text()*1;
		var start=false;	//开始划动
		var money=0;	//总金额
		var total=0;	//个数
		$(".begin").click(function(e){
			e.stopPropagation();
			$(this).hide();
			var i=1;
			begin(i-1);
			var time=setInterval(function(){
				begin(i);
				i++;
				if(i==3){
					clearInterval(time);
					setTimeout(function(){
						$(".show-box-area .pict img").hide();
						$(".red-wrap").removeClass('state1');
						$(".red-wrap").addClass('state2');
						$(".guang").fadeIn();
						$(".insert span").first().css("display",'block');
						time1(num);
						start=true;
					},1000);
				}
			},1000);

		});
		function begin(i){
			$(".show-box-area .pict img").eq(i).show().siblings().hide();
		}

		function time1(val){
			var i=val;
			var inter=setInterval(function(){
				i--;
				if(i>-1){
					$(".top .fr i").text(i);
				}else{
					
					$(".alert-box").find('b i').text(money.toFixed(2));
					$(".alert-box").find('p i').text(total);

					start=false;
					$(".red-envelopes .red-wrap .insert span").remove();
					$(".red-envelopes .red-wrap .gameover").show();

					setTimeout(function(){
						$(".alert-box").fadeIn();
					},1000);
					
					clearInterval(inter);
				}
			},1000);
		}

		var xos=0;
		$('.red-wrap').on('touchstart',function(e) {
			e.preventDefault();
			var _touch = e.originalEvent.targetTouches[0];
			xos=_touch.pageY;
		});
		 
		$('.red-wrap').on('touchmove',function(e) {
		   e.preventDefault();
		   var _touch = e.originalEvent.targetTouches[0];
		   var _x= _touch.pageY;
			
		});
		 
		$('.red-wrap').on('touchend',function(e) {
		   e.preventDefault();
		   var _touch = e.originalEvent.changedTouches[0];
		   var _x= _touch.pageX;

			if(_x-xos<50 && start){
				total++;
				var sum=MathRand();
				money+=sum;
				$(".top .fl i").text(money.toFixed(2));
				$(".hongbao").append('<span>+'+sum+'</span>');
				console.log(total-1);
				$(".red-envelopes .red-wrap .insert span").eq(total-1).addClass('dong');
				setTimeout(function(){
					$(".red-envelopes .red-wrap .insert").append('<span style="display:block;z-index:'+total+';"></span>');
				},100);
				$(".left-bao,.right-bao").append('<span></span>');
			}
		});


		function MathRand(val){ 
			 var Num=""; 
			 for(var i=0;i<3;i++) 
			 { 
				 Num+=Math.floor(Math.random()*9); 
			 }
				 return Num/100;
		 }

		 $(".alert-box a").click(function(){
			$(".alert-box").fadeOut();
		 });


		var audio = document.getElementById('audio');  
		audio.play();
	 
		 
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
		};
	</script>
</body>
</html>

