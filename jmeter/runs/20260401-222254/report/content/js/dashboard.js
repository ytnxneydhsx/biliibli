/*
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
var showControllersOnly = false;
var seriesFilter = "";
var filtersOnlySampleSeries = true;

/*
 * Add header in statistics table to group metrics by category
 * format
 *
 */
function summaryTableHeader(header) {
    var newRow = header.insertRow(-1);
    newRow.className = "tablesorter-no-sort";
    var cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 1;
    cell.innerHTML = "请求";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 3;
    cell.innerHTML = "执行情况";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 7;
    cell.innerHTML = "响应时间（毫秒）";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 1;
    cell.innerHTML = "吞吐量";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 2;
    cell.innerHTML = "网络（KB/秒）";
    newRow.appendChild(cell);
}

/*
 * Populates the table identified by id parameter with the specified data and
 * format
 *
 */
function createTable(table, info, formatter, defaultSorts, seriesIndex, headerCreator) {
    var tableRef = table[0];

    // Create header and populate it with data.titles array
    var header = tableRef.createTHead();

    // Call callback is available
    if(headerCreator) {
        headerCreator(header);
    }

    var newRow = header.insertRow(-1);
    for (var index = 0; index < info.titles.length; index++) {
        var cell = document.createElement('th');
        cell.innerHTML = info.titles[index];
        newRow.appendChild(cell);
    }

    var tBody;

    // Create overall body if defined
    if(info.overall){
        tBody = document.createElement('tbody');
        tBody.className = "tablesorter-no-sort";
        tableRef.appendChild(tBody);
        var newRow = tBody.insertRow(-1);
        var data = info.overall.data;
        for(var index=0;index < data.length; index++){
            var cell = newRow.insertCell(-1);
            cell.innerHTML = formatter ? formatter(index, data[index]): data[index];
        }
    }

    // Create regular body
    tBody = document.createElement('tbody');
    tableRef.appendChild(tBody);

    var regexp;
    if(seriesFilter) {
        regexp = new RegExp(seriesFilter, 'i');
    }
    // Populate body with data.items array
    for(var index=0; index < info.items.length; index++){
        var item = info.items[index];
        if((!regexp || filtersOnlySampleSeries && !info.supportsControllersDiscrimination || regexp.test(item.data[seriesIndex]))
                &&
                (!showControllersOnly || !info.supportsControllersDiscrimination || item.isController)){
            if(item.data.length > 0) {
                var newRow = tBody.insertRow(-1);
                for(var col=0; col < item.data.length; col++){
                    var cell = newRow.insertCell(-1);
                    cell.innerHTML = formatter ? formatter(col, item.data[col]) : item.data[col];
                }
            }
        }
    }

    // Add support of columns sort
    table.tablesorter({sortList : defaultSorts});
}

$(document).ready(function() {

    // Customize table sorter default options
    $.extend( $.tablesorter.defaults, {
        theme: 'blue',
        cssInfoBlock: "tablesorter-no-sort",
        widthFixed: true,
        widgets: ['zebra']
    });

    var data = {"OkPercent": 66.33092571731495, "KoPercent": 33.669074282685045};
    var dataset = [
        {
            "label" : "失败",
            "data" : data.KoPercent,
            "color" : "#FF6347"
        },
        {
            "label" : "成功",
            "data" : data.OkPercent,
            "color" : "#9ACD32"
        }];
    $.plot($("#flot-requests-summary"), dataset, {
        series : {
            pie : {
                show : true,
                radius : 1,
                label : {
                    show : true,
                    radius : 3 / 4,
                    formatter : function(label, series) {
                        return '<div style="font-size:8pt;text-align:center;padding:2px;color:white;">'
                            + label
                            + '<br/>'
                            + Math.round10(series.percent, -2)
                            + '%</div>';
                    },
                    background : {
                        opacity : 0.5,
                        color : '#000'
                    }
                }
            }
        },
        legend : {
            show : true
        }
    });

    // Creates APDEX table
    createTable($("#apdexTable"), {"supportsControllersDiscrimination": true, "overall": {"data": [0.6629779338678683, 500, 1500, "总计"], "isController": false}, "titles": ["Apdex", "T（可容忍阈值）", "F（不可接受阈值）", "标签"], "items": [{"data": [0.6389484552458105, 500, 1500, "WebSocket 请求-响应采样器"], "isController": false}, {"data": [0.9953333333333333, 500, 1500, "WebSocket 打开连接"], "isController": false}, {"data": [0.467, 500, 1500, "WebSocket 关闭连接"], "isController": false}]}, function(index, item){
        switch(index){
            case 0:
                item = item.toFixed(3);
                break;
            case 1:
            case 2:
                item = formatDuration(item);
                break;
        }
        return item;
    }, [[0, 0]], 3);

    // Create statistics table
    createTable($("#statisticsTable"), {"supportsControllersDiscrimination": true, "overall": {"data": ["总计", 15091, 5081, 33.669074282685045, 490.80769995361663, 0, 20001, 26.0, 65.0, 6000.0, 6001.0, 8.501714116983605E-6, 6.717660582885406E-7, 4.050939042938649E-7], "isController": false}, "titles": ["标签", "样本数", "失败数", "错误率", "平均值", "最小值", "最大值", "中位数", "90 分位", "95 分位", "99 分位", "事务/秒", "接收", "发送"], "items": [{"data": ["WebSocket 请求-响应采样器", 12591, 4545, 36.09721229449607, 499.90207290921995, 0, 6003, 25.0, 48.0, 6000.0, 6001.0, 7.0933061060857835E-6, 2.744481066610936E-7, 1.3172686081301123E-7], "isController": false}, {"data": ["WebSocket 打开连接", 1500, 3, 0.2, 105.49266666666662, 53, 20001, 57.0, 65.0, 73.95000000000005, 310.97, 3.956603967682459, 1.8278119337110572, 1.2301097191009014], "isController": false}, {"data": ["WebSocket 关闭连接", 1000, 533, 53.3, 954.2729999999981, 0, 6003, 25.0, 6000.0, 6001.0, 6002.0, 5.633632205384295E-7, 6.936959812274471E-9, 1.0642283337983771E-8], "isController": false}]}, function(index, item){
        switch(index){
            // Errors pct
            case 3:
                item = item.toFixed(2) + '%';
                break;
            // Mean
            case 4:
            // Mean
            case 7:
            // Median
            case 8:
            // Percentile 1
            case 9:
            // Percentile 2
            case 10:
            // Percentile 3
            case 11:
            // Throughput
            case 12:
            // Kbytes/s
            case 13:
            // Sent Kbytes/s
                item = item.toFixed(2);
                break;
        }
        return item;
    }, [[0, 0]], 0, summaryTableHeader);

    // Create error table
    createTable($("#errorsTable"), {"supportsControllersDiscrimination": false, "titles": ["错误类型", "错误次数", "占全部错误比例", "占全部样本比例"], "items": [{"data": ["WebSocket I/O 错误/连接超时", 3, 0.059043495374926196, 0.01987939831687761], "isController": false}, {"data": ["采样器错误/配置为复用已有连接，但当前没有连接", 24, 0.47234796299940957, 0.15903518653502088], "isController": false}, {"data": ["无连接可关闭", 2, 0.0393623302499508, 0.01325293221125174], "isController": false}, {"data": ["WebSocket I/O 错误/读取超时", 1149, 22.613658728596732, 7.613809555364124], "isController": false}, {"data": ["WebSocket I/O 错误/连接被重置", 171, 3.3654792363707933, 1.1331257040620237], "isController": false}, {"data": ["WebSocket I/O 错误/连接被对端重置", 3732, 73.45010824640819, 24.729971506195746], "isController": false}]}, function(index, item){
        switch(index){
            case 2:
            case 3:
                item = item.toFixed(2) + '%';
                break;
        }
        return item;
    }, [[1, 1]]);

        // Create top5 errors by sampler
    createTable($("#top5ErrorsBySamplerTable"), {"supportsControllersDiscrimination": false, "overall": {"data": ["总计", 15091, 5081, "WebSocket I/O 错误/连接被对端重置", 3732, "WebSocket I/O 错误/读取超时", 1149, "WebSocket I/O 错误/连接被重置", 171, "采样器错误/配置为复用已有连接，但当前没有连接", 24, "WebSocket I/O 错误/连接超时", 3], "isController": false}, "titles": ["采样器", "样本数", "错误数", "错误", "错误数", "错误", "错误数", "错误", "错误数", "错误", "错误数", "错误", "错误数"], "items": [{"data": ["WebSocket 请求-响应采样器", 12591, 4545, "WebSocket I/O 错误/连接被对端重置", 3358, "WebSocket I/O 错误/读取超时", 992, "WebSocket I/O 错误/连接被重置", 171, "采样器错误/配置为复用已有连接，但当前没有连接", 24, "", ""], "isController": false}, {"data": ["WebSocket 打开连接", 1500, 3, "WebSocket I/O 错误/连接超时", 3, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["WebSocket 关闭连接", 1000, 533, "WebSocket I/O 错误/连接被对端重置", 374, "WebSocket I/O 错误/读取超时", 157, "无连接可关闭", 2, "", "", "", ""], "isController": false}]}, function(index, item){
        return item;
    }, [[0, 0]], 0);

});
