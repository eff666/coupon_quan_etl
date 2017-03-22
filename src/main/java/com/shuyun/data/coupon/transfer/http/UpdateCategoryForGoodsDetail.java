package com.shuyun.data.coupon.transfer.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.shuyun.data.coupon.transfer.parser.JsonParserSnapshot;
import com.shuyun.data.coupon.transfer.pojo.WDetailGoods;
import com.shuyun.data.coupon.transfer.util.Constants;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.engine.DocumentAlreadyExistsException;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

public class UpdateCategoryForGoodsDetail {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateCategoryForGoodsDetail.class);

    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    private static String indexName = "index_goods_detail";
    private static String typeName = "goods_detail";

    public static Client initESClient() {
        Client clients = null;
        try {
            Settings settings = Settings.settingsBuilder().put("cluster.name", "elasticsearch-cluster").build();
            TransportClient transportClient = TransportClient.builder().settings(settings).build();
            transportClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("10.10.138.183"), 9500));
            transportClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("10.10.128.242"), 9500));
            clients = transportClient;
        }catch (Exception e){
            LOG.error("init client error, {}", e.getMessage());
        }
        return clients;
    }

    public static boolean isNullOrEmpty(String str){
        if(str != null && !"".equalsIgnoreCase(str) && str.length() > 0){
            return true;
        }
        return false;
    }

    // 增加或减少天数
    public static Date addDay(Date date, int num) {
        Calendar startDT = Calendar.getInstance();
        startDT.setTime(date);
        startDT.add(Calendar.DAY_OF_MONTH, num);
        return startDT.getTime();
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

    /*
    * 2、从es中取得goodid，根据goodid去网站爬取这个商品的详细类目，更新到es中detailCategory
    */
    public static void updateCategory() {
        Client clients = initESClient();
        try {
            LOG.info("---------------------2、update category for goods_detail begin-----------------");
            //1、更新es中优惠券过期商品，将其status置为false
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String todayString = simpleDateFormat.format(new Date());
            Date date = simpleDateFormat.parse(todayString);
            date = addDay(date, -1);
            long today = date.getTime() / 1000;

            SearchResponse sp = clients.prepareSearch().setIndices("index_goods_coupon").setTypes("goods_coupon")
                    .setSearchType(SearchType.DEFAULT)
                    .setScroll(new TimeValue(60000))
                    .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("status", "true")).mustNot(QueryBuilders.missingQuery("detailCategory")).must(QueryBuilders.rangeQuery("ctime").gte(today)))
                    .setFrom(0).setSize(50000)
                    .execute().actionGet();

            long total = sp.getHits().totalHits();
            int cnt = 0;
            int count = 0;

            for (SearchHit hit : sp.getHits()) {
                cnt++;
                Map<String, Object> map = hit.getSource();
                String goodId = null;
                String topCategory = null;
                String detailCategory = null;
                String category = null;
                try {
                    goodId = map.get("goodid").toString();
                } catch (NullPointerException n) {
                }

                try {
                    topCategory = map.get("topCategory").toString();
                } catch (NullPointerException n) {
                }

                try {
                    detailCategory = map.get("detailCategory").toString();
                } catch (NullPointerException n) {
                }

                try {
                    category = map.get("category").toString();
                } catch (NullPointerException n) {
                }

                if (goodId != null) {
                    try {
                        if(isNullOrEmpty(category) || isNullOrEmpty(detailCategory) || isNullOrEmpty(topCategory)) {
                            GetResponse getResponse = clients.prepareGet(indexName, typeName, goodId)
                                    .execute()
                                    .actionGet();
                            if (getResponse == null) {
                                LOG.error("response is null  with id :{}", goodId);
                            } else if (getResponse.isExists()) {
                                // update logic
                                try {
                                    WDetailGoods goods = new WDetailGoods();
                                    goods.setGoodsId(new BigInteger(goodId));
                                    goods.setCategory(category);
                                    goods.setTopCategory(topCategory);
                                    goods.setDetailCategory(detailCategory);

                                    String json = OBJECT_MAPPER.writeValueAsString(goods);

                                    UpdateResponse updateResponse = clients
                                            .prepareUpdate(indexName, typeName, goodId)
                                            .setVersion(getResponse.getVersion())
                                            .setDoc(json)
                                            .execute()
                                            .actionGet();

                                    if (updateResponse.getVersion() != getResponse.getVersion()) {
                                        LOG.info("update detailCategory is succeed，goodid：[ " + goodId + "]，total：[ " + total + " ]，processing：[ " + cnt + " ]， success：[ " + count++ + " ]");
                                    }
                                } catch (DocumentAlreadyExistsException d){
                                    LOG.info("update document already exist, goodsId:{}", goodId);
                                } catch (VersionConflictEngineException e) {
                                    LOG.error(e.getMessage(), e);
                                    LOG.info("update version conflict and will try again, goodsId:{}", goodId);
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOG.error(e.getMessage());
                    }
                }
            }
            LOG.info("----------------------2、update category for goods_detail end-----------------\n");
        }catch (Exception e){
            LOG.error(e.getMessage());
        } finally {
            clients.close();
        }
    }

    /*
     * 1、对es中的商品进行符合要求的各种验证，将不符合要求的status设为false
     */
    public static void validation() {
        Client clients = initESClient();
        try {
            try {
                //1、更新goods_detail过期商品，将其status置为false
                SearchResponse searchResponse = clients.prepareSearch().setIndices(indexName).setTypes(typeName)
                        .setSearchType(SearchType.DEFAULT)
                        .setScroll(new TimeValue(60000))
                        .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("status", "true")))
                        .setFrom(0).setSize(50000)
                        .execute().actionGet();

                LOG.info("--------------1、更新过期商品开始----------------------" + typeName + ", total goods:" + searchResponse.getHits().getTotalHits());
                long totalHits = searchResponse.getHits().totalHits();
                int cnt = 0;
                int success = 0;
                for (SearchHit hit : searchResponse.getHits()) {
                    cnt++;
                    Map<String, Object> map = hit.getSource();
                    String goodsId = map.get("goodsId").toString();
                    if(isNullOrEmpty(goodsId)) {
                        String url = "http://hws.m.taobao.com/cache/wdetail/5.0/?id=" + goodsId;
                        String response = doGet(url);
                        if (isNullOrEmpty(response)) {
                            JsonParserSnapshot jsonParser = null;
                            try {
                                jsonParser = new ObjectMapper().readValue(response, new TypeReference<JsonParserSnapshot>() {});
                            } catch (Exception e) {
                                LOG.error("解析response发生错误，URL：{} \n错误原因：{} \n错误信息：{}", url, e.getCause(), e.getMessage());
                                e.printStackTrace();
                            }
                            if (jsonParser != null) {
                                String ret = jsonParser.getRet()[0];
                                if (isNullOrEmpty(ret) && "SUCCESS::调用成功".equalsIgnoreCase(ret)) {
                                    if(cnt % 1000 == 0) {
                                        LOG.info("goods response is SUCCESS, processing:{}", cnt);
                                    }
                                } else {
                                    WDetailGoods goods = new WDetailGoods();
                                    goods.setGoodsId(new BigInteger(goodsId));
                                    goods.setStatus("false");

                                    String json = OBJECT_MAPPER.writeValueAsString(goods);
                                    try {

                                        UpdateResponse updateResponse = clients
                                                .prepareUpdate(indexName, typeName, goodsId)
                                                .setDoc(json)
                                                .execute()
                                                .actionGet();

                                        LOG.info("update status is false succeed，type：[ " + typeName + " ]，goodsId：[ " + goodsId + "]，total：[ " + totalHits + " ]，processing：[ " + cnt + " ]， success：[ " + success++ + " ]");
                                    } catch (DocumentAlreadyExistsException d){
                                        LOG.info("update validation document already exist, goodsId:{}", goodsId);
                                    } catch (VersionConflictEngineException e) {
                                        LOG.error(e.getMessage(), e);
                                        LOG.info("update validation version conflict, goodsId:{}", goodsId);
                                    }
                                }
                            }
                        }
                    }
                }
                LOG.info("total update data: {}", totalHits);
            } catch (Exception e) {
                LOG.error(e.getMessage());
            }
            LOG.info("--------------1、更新过期商品结束----------------------" + typeName + "\n");
        }catch (Exception e){
            LOG.error(e.getMessage());
        } finally {
            clients.close();
        }
    }

    public static void main(String[] args) {
        //1、对goods_detail中的商品进行验证，将不符合要求的status设为false
        //validation();

        //更新goods_detail商品的category
        updateCategory();
    }
}
