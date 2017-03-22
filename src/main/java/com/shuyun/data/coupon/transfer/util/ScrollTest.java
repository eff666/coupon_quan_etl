package com.shuyun.data.coupon.transfer.util;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import java.net.InetAddress;

/**
 * elasticSearch-2.3.3
 */
public class ScrollTest {

    private static Client client;

    static {
        try {
            Settings settings = Settings.settingsBuilder().put("cluster.name", "elasticsearch-cluster").build();
            TransportClient transportClient = TransportClient.builder().settings(settings).build();
            transportClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(""), 9500));
            transportClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(""), 9500));
            client = transportClient;
        }catch (Exception e){

        }
    }

    /*
     * 利用from-size分页，获取较小数据量查询
     */
    private void getSearchData(QueryBuilder queryBuilder) {

        SearchResponse searchResponse = client
                .prepareSearch("")
                .setTypes("")
                .setQuery(queryBuilder)
                .setFrom(0)
                .setSize(100)
                .execute()
                .actionGet();

        SearchHits searchHits = searchResponse.getHits();
        for (SearchHit searchHit : searchHits) {
//            Integer id = (Integer) searchHit.getSource().get("id");
//            ids.add(id);
        }
    }

    /*
     * 利用scrollId深度分页，获取大量数据
     */
    private void getSearchDataByScrolls(QueryBuilder queryBuilder) {

        SearchResponse searchResponse = client.prepareSearch()
                .setIndices("")
                .setTypes("")
                .setScroll(TimeValue.timeValueMinutes(1)) //游标维持时间
                .setSearchType(SearchType.SCAN)//用Scan提高性能，但第一次不返回结果，返回scrollId
                .setSize(1000)//实际返回的数量为1000*index的主分片数
                .execute()
                .actionGet();

        TimeValue timeValue = new TimeValue(80000);
        while(true) {
            try {
                //第一次查询，只返回数量和一个scrollId
                //注意第一次运行没有结果
                for (SearchHit hit : searchResponse.getHits().getHits()) {
                    //
                }
                //使用上次的scrollId继续访问
                //初始搜索请求和每个后续滚动请求返回一个新的滚动ID,只有最近的滚动ID才能被使用
                searchResponse = client.prepareSearchScroll(searchResponse.getScrollId()).setScroll(timeValue).execute().actionGet();
                if (searchResponse.getHits().getHits().length == 0) {
                    break;
                }
            } catch (Exception e) {

            }
        }
    }
}
