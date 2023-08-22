$(function () {

    window.operateEvents = {
        'click .uploadBtn': function (e, value, row, index) {
            $("#uploadId").val(row.id);
            $("#uploadWin").modal("toggle");
        }
    };

    $('#alarmInfoTable').bootstrapTable({
        url: "/alert_info/select_by_page",//请求路径
        method: 'get',
        /*
                    contentType: 'application/x-www-form-urlencoded; charset=UTF-8',//post请求需设置
        */
        striped: true, //是否显示行间隔色
        pageNumber: 1, //初始化加载第一页
        pagination: true,//是否分页
        pageSize: 5,//单页记录数
        pageList: [5, 10],//可选择单页记录数
        showRefresh: false,//刷新按钮
        sidePagination: "server",
        queryParams: function (params) {//上传服务器的参数
            return {
                currentPage: params.offset / params.limit + 1,
                size: params.limit
            };
            /*var patientName = $("#patientName").val();
            var inpatientNo = $("#inpatientNo").val();
            var operationRoom = $("#operationRoom").val();
            var chiefSurgeon = $("#chiefSurgeon").val();
            var operationStTime = $("#operationStTime").data("datetimepicker").getDate();
            var operationEnTime = $("#operationEnTime").data("datetimepicker").getDate();
            var jsonData = {
                patientName: patientName,
                inpatientNo: inpatientNo,
                operationRoom: operationRoom,
                chiefSurgeon: chiefSurgeon,
                operationStTime: operationStTime,
                operationEnTime: operationEnTime,
            };
            var data = {};
            return data;*/
        },
        responseHandler: function (res) {
            if (res == null || res.code !== 200) {
                console.log("无数据");
                return res;
            }
            let data = res.data;
            if (data == null || data.records == null) {
                console.log("无数据");
                return res;
            }
            return {
                "rows": data.records,
                "total": data.total
            };
        },
        formatLoadingMessage: function () {
            return "数据加载中...";
        },
        formatShowingRows: function (pageFrom, pageTo, totalRows) {
            return "第" + pageFrom + "-" + pageTo + "行，总共" + totalRows + "行";
        },
        //每页显示
        formatRecordsPerPage: function (pageNumber) {
            return pageNumber + '行每页';
        },
        /*onClickRow:function(row, $element, field)
        {
            gotoVideo(row);
        },*/
        columns: [
            {
                title: 'id',
                field: 'id',
                visible: false
            },
            {
                title: '故障地点',
                field: 'location',
                align: 'center'
            },
            {
                title: '故障情况',
                field: 'message',
                align: 'center'
            },
            {
                title: '报警源',
                field: 'source',
                align: 'center'
            },
            {
                title: '状态',
                field: 'status',
                align: 'center'
            },
            {
                title: '故障时间',
                field: 'time',
                align: 'center'
            },
            {
                title: '故障类型',
                field: 'type',
                align: 'center'
            },
            {
                title: '上传文件',
                align: 'center',
                field: 'operate',
                events: operateEvents,
                formatter: function (value, row, index) {
                    return [
                        '<button type="button" class="uploadBtn btn btn-primary btn-sm" style="margin-right:15px;">上传</button>',
                    ].join('');
                }
            }
        ]
    });

    $("#uploadFile").fileinput({
        dropZoneTitle: "请上传文件！",
        uploadUrl: "/upload/fileUpload",
        language: "zh",
        autoReplace: false,
        showCaption: false,
        showUpload: true,
        overwriteInitial: true,
        showUploadedThumbs: true,
        browseClass: "btn btn-info btn-lg",
        removeClass: "btn btn-danger btn-lg",
        uploadClass: "btn btn-success btn-lg",
        showPreview: true,                   //显示上传图片的大小信息
        maxFileCount: 5,
        minFileCount: 1,
        maxFileSize: 1024000,//文件最大153600kb=150M
        msgFilesTooMany: "选择上传的文件数量({n}) 超过允许的最大数值{m}！",
        msgSizeTooLarge: "文件{name}({size} KB) 超过允许的最大文件 {maxSize} KB!",
        initialPreviewShowDelete: false,
        showRemove: true,//是否显示删除按钮
        showClose: true,
        layoutTemplates: {
            actionUpload: '',
        },
        uploadAsync: false,
        previewSettings: {
            image: {
                width: "50%",
                height: "50%"
            },
        },
        uploadExtraData: function () {
            var uploadId = $("#uploadId").val();
            var formData = {
                uploadId: uploadId,
            }
            return formData;
        }

    }).on('filebatchselected', function (evt, file) {
        // 选择图片后执行，用于判断选择的图片是否超出限制，超出限制就禁用选择按钮
        // 比如只允许上传4张，我分两次上传，第一次上传两张，第二次上传3张，此时手动禁用选择按钮
        if (file.length > 5) {
            $('#uploadFile').attr('disabled', 'disabled')
            $('#uploadFile').closest('div.btn-file').addClass('btn-disabled');
        }
    }).on('fileclear', function (evt, file) {
        // 点击右上角叉叉执行
        $('#uploadFile').removeAttr('disabled')
        $('#uploadFile').closest('div.btn-file').removeClass('btn-disabled')
        //$uploadAdvBox.find('.fileinput-upload-button').removeAttr('disabled')
    }).on('filebatchuploadsuccess', function (event, data, previewId, index) {//同步上传回调
        if (data && data["response"] && data["response"]["data"] && data["response"]["data"]["flag"] === 'true') {
            alert("上传成功！");
        } else {
            alert("上传失败！")
        }
    }).on('filebatchuploaderror', function (event, data, previewId, index) {//同步上传回调
        alert("上传失败！")
    });
});

