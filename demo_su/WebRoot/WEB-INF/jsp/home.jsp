<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%
	String path = request.getContextPath();
	String basePath = request.getScheme() + "://"
			+ request.getServerName() + ":" + request.getServerPort()
			+ path + "/";
%>
<!DOCTYPE html>
<html>
<head>
<title>WebSocket Chat Client</title>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<script src="${pageContext.request.contextPath}/js/jquery-1.8.0.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/prototype.js"></script>
<script src="//qzonestyle.gtimg.cn/open/qcloud/video/live/h5/live_connect.js" charset="utf-8"></script>

</head>
<body>
      <span style="color: red;">友情提示：没有用户连麦,屏幕空白,当用户连麦时会自动显示影像,当用户关闭摄像头,图形会禁止</span>
      <input type="hidden" id="userLogin" value="${loginName}">
	  <div id="video">
	  </div>
</body>
<script type="text/javascript">
    var ws = null;
    jQuery(document).ready(function() {
       console.log('进入了方法');
	   open_socket_link();
    });
	
	 function open_socket_link(){
// 		ws = new WebSocket("ws://106.15.88.38:2048");
		ws = new WebSocket("ws://127.0.0.1:2048");
		ws.onopen = function() {
			console.log("--socket链接--");
			 setTimeout("send_mesg(str)", 5000);
		}
		ws.onmessage = function(e) {
			console.log('socket返回-->'+e.data);
			analysis(e.data);
		}
		ws.onclose = function() {
			console.log('socket断开了');
			ws = null;
			open_socket_link();
		}
		ws.onerror = function(){
			open_socket_link();
		    
		}
	} 
	var str="{'mid':'10002','userName':'"+jQuery('#userLogin').val()+"'}";
	
	function send_mesg(obj){
		if (ws) {
			jQuery('#video').empty();
			ws.send(obj);
		}
	}
	
	function analysis(data){
		 var  entity= eval('(' + data + ')'); 
		 jQuery.each(entity, function(key, val) {
			 console.log(val);
			  //新增房间
			  if(this.mid == 10003){
				//链接人
				if(this.roomId <=100000){
					var obj = '<div style="float: left;margin-left: 15px;">';
					obj = obj + '<div id="'+this.roomId+'_1" style="width:200px; height:340px;"></div>';
					obj = obj + '<span>房间号:'+this.roomId+'</span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;';
					obj = obj + '<span><a href="javascript:on_click_disable('+this.roomId+','+this.launchUserId+')">点击封号</a></span>';
					obj = obj + '</br>';
					obj = obj + '<span>IdCard:'+this.launchUserId+'</span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;';
					obj = obj + '<span>连线人:'+this.lanuchName+'</span>';
					obj = obj + '</div>';
					jQuery('#video').append(obj);
				   on_load_video(this.roomId+"_1", this.launchUserLiveCode);
				   //被链接人
				    var obj = '<div style="float: left;margin-left: 15px;">';
					obj = obj + '<div id="'+this.roomId+'_2" style="width:200px; height:340px;"></div>';
					obj = obj + '<span>房间号:'+this.roomId+'</span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;';
					obj = obj + '<span><a href="javascript:on_click_disable('+this.roomId+','+this.coverLinkUserId+')">点击封号</a></span>';
					obj = obj + '</br>';
					obj = obj + '<span>IdCard:'+this.coverLinkUserId+'</span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;';
					obj = obj + '<span>连线人:'+this.coverName+'</span>';
					obj = obj + '</div>';
					jQuery('#video').append(obj);
				    on_load_video(this.roomId+"_2", this.coverLinkUserLiveCode);
				}else {
					
					var obj = '<div style="float: left;margin-left: 15px;">';
					obj = obj + '<div id="'+this.roomId+'_1" style="width:200px; height:340px;"></div>';
					obj = obj + '<span>房间号:'+this.roomId+'</span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;';
					obj = obj + '<span><a href="javascript:on_click_disable('+this.roomId+','+this.launchUserId+')">点击封号</a></span>';
					obj = obj + '</br>';
					obj = obj + '<span>IdCard:'+this.launchUserId+'</span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;';
					obj = obj + '<span>连线人:'+this.lanuchName+'</span>';
					obj = obj + '</div>';
					jQuery('#video').append(obj);
				   on_load_video(this.roomId+"_1", this.launchUserLiveCode);
				}
					
			  }else if(this.mid== 10004){ //删除房间
				  jQuery('#'+this.roomId+'_1').parent().remove();
				  jQuery('#'+this.roomId+'_2').parent().remove();
			  }
			});
	}
	//加载视频
	function on_load_video(id,videl_url){
		new qcVideo.Player(id,{
					"live_url" : videl_url,
					"live_url2" : videl_url,
					"width": 300,
					"height": 510
	   });
	}
	
	function on_click_disable(roomId,userId){
		var msg = '{"mid":10005,"roomId":'+roomId+',"userId":'+userId+'}';
		send_mesg(msg);
	}
	
</script>
</html>