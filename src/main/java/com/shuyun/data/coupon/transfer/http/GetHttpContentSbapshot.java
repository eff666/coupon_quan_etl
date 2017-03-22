package com.shuyun.data.coupon.transfer.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.shuyun.data.coupon.transfer.parser.JsonParserSnapshot;
import com.shuyun.data.coupon.transfer.pojo.WDetailGoodsSnapshot;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.*;
public class GetHttpContentSbapshot {

    private static final Logger LOG = LoggerFactory.getLogger(GetHttpContent.class);

    //@Autowired
    //private static IElasticService elasticService;
    private static Client client;

    static ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd"))
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    //.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);


    static {
        //LOG.info("现在时间是:" + Calendar.getInstance().getTime());
        try {
            Settings settings = Settings.settingsBuilder().put("cluster.name", "elasticsearch-cluster").build();
            TransportClient transportClient = TransportClient.builder().settings(settings).build();
            transportClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("10.10.138.183"), 9500));
            transportClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("10.10.128.242"), 9500));
            client = transportClient;
        }catch (Exception e){

        }
    }

    private static boolean isNullOrEmpty(String str){
        if(str != null && !"".equalsIgnoreCase(str) && str.length() > 0){
            return true;
        }
        return false;
    }

    private static boolean insertSnapshot(WDetailGoodsSnapshot goods){
        String indexName = "index_goods_detail_snapshot";
        String typeName = "goods_detail_snapshot";
        if (goods == null) {
            LOG.info("插入数据失败，goods为空");
            return false;
        }

        try {
            String goodsId = goods.getGoodsId().toString();
            String ctime = goods.getCtime();
            String id = goodsId + "_" + ctime;
            //LOG.info("start to insert goods， goodId:{}", id);

            SearchResponse searchResponse = client.prepareSearch().setIndices(indexName).setTypes(typeName)
                    .setSearchType(SearchType.DEFAULT)
                    .setScroll(new TimeValue(60000))
                    .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("goodsId", goodsId)))
                    .setFrom(0).setSize(100)
                    .execute().actionGet();

            long totalHits = searchResponse.getHits().getTotalHits();

            if(totalHits > 0){
                goods.setVersion(totalHits + 1);
            } else {
                goods.setVersion(Long.parseLong("1"));
            }

            try {
                String json = objectMapper.writeValueAsString(goods);

                IndexResponse response = client
                        .prepareIndex(indexName, typeName)
                        .setId(id)
                        .setOpType(IndexRequest.OpType.CREATE)
                        .setSource(json.getBytes("utf-8"))
                        .execute()
                        .actionGet();

                if (response != null && response.isCreated()) {
                    //LOG.info("insert goods successes, typeName:{}, goodsId:{}, id:{}", typeName, goodsId, id);
                    return true;
                } else {
                    LOG.info("+++++++++++++++++++++++++++++++++++++++++++insert goods failed, typeName:{}, goodsId:{}, id:{}", typeName, goodsId, id);
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                LOG.error("insert goods failed, typeName:{}, goodsId:{}, id:{}", typeName, goodsId, id);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return false;
    }

    private static WDetailGoodsSnapshot getGoodsSnapshot(JsonParserSnapshot jsonParser, String url){
        WDetailGoodsSnapshot goodsSnapshot = new WDetailGoodsSnapshot();
        goodsSnapshot.setData("IS_NULL");
        goodsSnapshot.setStatus("false");
        try {
            String ret = jsonParser.getRet()[0];
            if (isNullOrEmpty(ret) && "SUCCESS::调用成功".equalsIgnoreCase(ret)) {
                Object dataParser = jsonParser.getData();
                if (dataParser != null) {
                    String data = objectMapper.writeValueAsString(dataParser);
                    goodsSnapshot.setData(objectMapper.writeValueAsString(dataParser));
                    goodsSnapshot.setStatus("true");
                }
            } else {
                LOG.error("商品调用返回失败信息，返回信息：{}，请求URL：{}", ret, url);
            }
            //得到ctime字段
            //SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String strDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            goodsSnapshot.setCtime(strDate);
        }catch (Exception e){
            LOG.error("解析数据data发生错误，请求URL：{} \n错误原因：{} \n错误信息：{}", url, e.getCause(), e.getMessage());
        }
        return goodsSnapshot;
    }


    private static String doGet(String url) {
        //String url = "https://shop.m.taobao.com/shop/coupon.htm?sellerId=696902416&activityId=4aee3e6538d94b518e19b53d3b04f30c";
        //String url="http://hws.m.taobao.com/cache/wdetail/5.0/?id=540459138947";
        String response = null;
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(url);
        try {
            client.executeMethod(method);
            if (method.getStatusCode() == HttpStatus.SC_OK) {
                response = method.getResponseBodyAsString();
            }
        } catch (URIException e) {
            LOG.error("执行HTTP Get请求时，编码查询字符串");
        } catch (IOException e) {
            LOG.error("执行HTTP Get请求" + url + "时，发生异常！");
        } finally {
            method.releaseConnection();
        }
        return response;
    }

    public static void main(String[] args) {

        SearchResponse search = client.prepareSearch().setIndices("index_goods_coupon").setTypes("goods_coupon")
                .setSearchType(SearchType.DEFAULT)
                .setScroll(new TimeValue(60000))
                .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("status", "true")))
                .setFrom(0).setSize(100000)
                .execute().actionGet();

        LOG.info("total hits num:{}", search.getHits().totalHits());

        Set<String> goodsIdList = new HashSet<String>();
        for (SearchHit hit : search.getHits()) {
            //cnt++;
            Map<String, Object> map = hit.getSource();
            String goodsId = map.get("goodid").toString();
            if(isNullOrEmpty(goodsId)){
                goodsIdList.add(goodsId);
            }
        }

        int total = goodsIdList.size();
        LOG.info("insert goods begin, goods_coupon total:{}, set total:{}", search.getHits().totalHits(), total);

        try {
            int process = 0;
            int success = 0;
            String strDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            for(String goodsId : goodsIdList) {
                process++;
                String url = "http://hws.m.taobao.com/cache/wdetail/5.0/?id=" + goodsId;
                String response = doGet(url);
                JsonParserSnapshot jsonParserSnapshot = null;
                try {
                    jsonParserSnapshot = new ObjectMapper().readValue(response, new TypeReference<JsonParserSnapshot>() {
                    });
                } catch (IOException e) {
                    LOG.error("解析response发生错误，URl为：{} \n错误原因：{} \n错误信息：{}", url, e.getCause(), e.getMessage());
                    e.printStackTrace();
                }

                if (jsonParserSnapshot != null) {
                    WDetailGoodsSnapshot goodsSnapshot = new WDetailGoodsSnapshot();
                    try {
                        goodsSnapshot.setData("IS_NULL");
                        goodsSnapshot.setStatus("false");
                        String ret = jsonParserSnapshot.getRet()[0];
                        if (isNullOrEmpty(ret) && "SUCCESS::调用成功".equalsIgnoreCase(ret)) {
                            Object dataParser = jsonParserSnapshot.getData();
                            if (dataParser != null) {
                                goodsSnapshot.setData(objectMapper.writeValueAsString(dataParser));
                                goodsSnapshot.setStatus("true");
                            }
                        } else {
                            LOG.error("商品调用返回失败信息，返回信息：{}，请求URL：{}", ret, url);
                        }
                    }catch (Exception e){
                        LOG.error("解析数据data发生错误，请求URL：{} \n错误原因：{} \n错误信息：{}", url, e.getCause(), e.getMessage());
                    }

                    //得到ctime字段
                    goodsSnapshot.setCtime(strDate);
                    goodsSnapshot.setGoodsId(new BigInteger(goodsId));
                    if (!insertSnapshot(goodsSnapshot)) {
                        LOG.error("插入数据失败, 商品信息: {}", objectMapper.writeValueAsString(goodsSnapshot));
                    }else {
                        LOG.info("insert goods success, typeName: {}, total: {}, processing: {}, success: {}, goodsId: {}", "goods_detail_snapshot", total, process, success++, goodsId);
                    }
                }
            }
            LOG.info("insert goods end, total success: {}", success);
        }catch (Exception e){
            LOG.error(e.getMessage());
        }
    }
}
