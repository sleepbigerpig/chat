$(function(){
    //生成用户数据
   var url = path+"/admin/getBigRoomManList.htm" ;
    $('#utable').bootstrapTable({
        url: url,
        method: 'post',
        contentType: "application/x-www-form-urlencoded",
        queryParamsType:'', 
        queryParams: function(params){
        	return {
        		pageSize: params.limit,
        		page:params.pageNumber
        	}
        }, 
        //【其它设置】
        locale:'zh-CN',//中文支持
        pagination: true,//是否开启分页（*）
        pageNumber:1,//初始化加载第一页，默认第一页
        pageSize: 5,//每页的记录行数（*）
        sidePagination: "server", //分页方式：client客户端分页，server服务端分页（*）
        //【设置列】
        columns: [
              	{
              		title:'idcard',
              		align:'center',
              		field:'t_idcard'
              	},
              	{
              		title:'主播昵称',
              		align:'center',
              		field:'t_nickName',
              	},
              	{
              		title:'主播头像',
              		align:'center',
              		field:'t_handImg',
              		formatter:function(value){
              			return  '<img src="'+value+'" style="cursor:pointer" title="点击预览" width="40" height="40" onclick="on_click_preview(\''+value+'\');">';
              		}
              	},
              	{
              		title:'主播封面',
              		align:'center',
              		field:'t_cover_img',
              		formatter:function(value){
              			 return '<img src="'+value+'" style="cursor:pointer" title="点击预览" width="40" height="40" onclick="on_click_preview(\''+value+'\');">';
              		}
              	},
              	{
              		title:'排序值',
              		align:'center',
              		field:'t_sort',
              	},
              	{
              		title:'状态',
              		align:'center',
              		field:'t_is_debut',
              		formatter:function(value,row){
              			if(0 == value)  return '未开播';
              			return '直播中=>(房间号)'+row.t_room_id;
              		}
              	},
              	{
              		title:'操作',
              		field:'',
              		align:'center',
              		formatter:function(value,row,index){
              			var res ='';
              			var valueStr = JSON.stringify(row);
              			res = res+"<a href='javascript:on_click_open_update_model("+valueStr+");'  class='btn btn-default' style='height: 25px;line-height: 0.5;background-color: #87CEFA;'>修改</a>&nbsp;&nbsp;";
              			res = res +'<a href="javascript:on_click_operation('+row.t_id+');"  class="btn btn-default" style="height: 25px;line-height: 0.5;background-color: #87CEFA;color:red;">删除</a>';
              			return res;
              		}
              	}
        ]
    });
    
    
});

/**点击预览*/
function on_click_preview(url){
	$("#hand_img").attr("src",url);
	$("#img_preview").modal('show');
}
/**
 * 新增或者修改
 * @param id
 * @param nickName
 */
function on_click_open_update_model(obj){
	if(null != obj){
		$('#t_id').val(obj.t_id);
		$('#anthorId').val(obj.t_idcard);
		$('#t_sort').val(obj.t_sort);
	}else{
		$('#anthorId').val('');
		$('#t_sort').val(99);
	}
	$("#myModal").modal('show');
}

function on_click_submit(){
	var isOK = vaildate_is_submit([
		   {'name':'anthorId','type':'int'},
		   {'name':'t_sort','type':'int'},
		 ]);
	if(isOK){
		//保存数据
		$.ajax({
			type:'POST',
			url:path + '/admin/mixBigRoom.htm',
			data:{
				t_id:$('#t_id').val(),
				idcard:$('#anthorId').val(),
				sort:$('#t_sort').val()
			},
			dataType : 'json',
			success:function(data){
				if (data.m_istatus >= 1) {
					$("#myModal").modal('hide');
					$('#my_comfirm').empty();
					$('#my_comfirm').append(data.m_strMessage);
					$("#myComfirm").modal('show');
					$("#utable").bootstrapTable('refreshOptions',{page : 1});
				} else  {
					$("#myModal").modal('hide');
					$('#my_comfirm').empty();
					$('#my_comfirm').append(data.m_strMessage);
					$("#myComfirm").modal('show');
//					window.location.href = path + '/error.html';
				}
			}
		});
	}
}
/**
 * 点击操作
 */
function on_click_operation(id){
	$("#del_id").val(id);
	$("#myDel").modal('show');
}

function on_click_del(){
	if($('#del_id').val() != null &&  $('#del_id').val() != ""){
		$.ajax({
			type:'POST',
			url:path + '/admin/delBigRoomAnchor.htm',
			data:{
				id:$('#del_id').val()
			},
		   dataType : 'json',
		   success:function(data){
			   if (data.m_istatus == 1) {
					$("#myDel").modal('hide');
					$('#my_comfirm').empty();
					$('#my_comfirm').append(data.m_strMessage);
					$("#myComfirm").modal('show');
					$("#utable").bootstrapTable('refreshOptions',{page : 1});
				} else  {
					window.location.href = path + '/error.html';
				}
		   }
		})
	}
}

function isRealNum(val){
    // isNaN()函数 把空串 空格 以及NUll 按照0来处理 所以先去除
    if(val === "" || val ==null){
        return false;
    }
    if(!isNaN(val)){
        return true;
    }else{
        return false;
    }
}




 



