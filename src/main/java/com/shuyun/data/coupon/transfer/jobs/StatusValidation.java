package com.shuyun.data.coupon.transfer.jobs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Strings;
import com.shuyun.data.coupon.transfer.App;
import com.shuyun.data.coupon.transfer.dao.IGoodsDao;
import com.shuyun.data.coupon.transfer.pojo.Goods;
import com.shuyun.data.coupon.transfer.service.IElasticService;
import com.shuyun.data.coupon.transfer.util.CategoryUtil;
import com.shuyun.data.coupon.transfer.util.Constants;
import com.shuyun.data.coupon.transfer.util.HttpRequest;
import com.shuyun.data.coupon.transfer.util.JsonUtil;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class StatusValidation {

    private final Logger LOG = LoggerFactory.getLogger(StatusValidation.class);

    @Value("${cluster.name}")
    private String clusterName;
    @Value("${cluster.hosts}")
    private String hosts;
    @Value("${cluster.port}")
    private int port;
    @Value("${goods.indexName}")
    private String indexName;
    @Value("${goods.typeName}")
    private String typeName;


    @Value("${validationTypes}")
    private String validationTypes;

    @Autowired
    private IGoodsDao goodsDao;

    @Autowired
    private IElasticService elasticService;

    private Client client;

    private String QUERY_SENSITIVE = "{\"bool\":{\"must\":[{\"query_string\": {\"query\": {\"default_field\": \"%s\",\"query\": \"%s\"}}},{\"term\": {\"status\": \"true\"}}]}}";

    List<String> listColumn = Arrays.asList("goodid", "title", "img", "itemDetailUrl", "category", "goodurl", "price", "volume", "brokerageRatio", "brokerage",
            "sellerWang", "sellerid", "nick", "usertype", "couponId", "totalnum", "restnum", "quanDesc", "datestart", "dateend", "quanurl", "promoteurl", "detailCategory");

//    List<String> listColumn = Arrays.asList("goodid", "title", "img", "price", "quanprice", "restnum", "datestart", "dateend", "quanurl", "goodurl",
//            "nick", "sellerid", "volume", "usertype", "provcity", "discountRatio", "couponId", "itemDetailUrl", "totalnum", "quanDesc", "promoteurl", "detailImgUrls", "smallurl",
//            "category", "topCategory", "detailCategory", "sellerWang", "brokerageRatio", "brokerage");

    private static final String urlPrefix = "http://www.ahatao.com/listname/?lid=";

    private CategoryUtil categoryUtil = new CategoryUtil();

    private ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

//    @PostConstruct
//    public void initESClient() {
//        try {
//            Settings settings = Settings.settingsBuilder().put("cluster.name", clusterName).build();
//            TransportClient transportClient = TransportClient.builder().settings(settings).build();
//            for (String ip : hosts.split(";")) {
//                transportClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(ip), port));
//            }
//            client = transportClient;
//        }catch (Exception e){
//
//        }
//    }


    public Client initESClient() {
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

    /*
     * 1、对es中的商品进行符合要求的各种验证，将不符合要求的status设为false
     */
    public void validation() {
        Client clients = initESClient();
        try {
            LOG.info("--------------1、更新过期商品开始----------------------" + typeName);
            try {
                //1、更新es中优惠券过期商品，将其status置为false
                String todayString = Constants.DAY_FORMAT.format(new Date());
                Date date = Constants.DAY_FORMAT.parse(todayString);
                date = addDay(date, 1);
                long today = date.getTime() / 1000;
                long totalHits = 0;

                SearchResponse searchResponse = clients.prepareSearch().setIndices(indexName).setTypes(typeName)
                        .setSearchType(SearchType.DEFAULT)
                        .setScroll(new TimeValue(60000))
                        .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery("dateend").lte(today)).must(QueryBuilders.termQuery("status", "true")))
                        .setFrom(0).setSize(50000)
                        .execute().actionGet();

                LOG.info("total hits num:{}", searchResponse.getHits().totalHits());
                for (SearchHit hit : searchResponse.getHits()) {
                    Map<String, Object> map = hit.getSource();
                    Goods goods = new Goods();
                    goods.setStatus(0);
                    goods.setGoodid(new BigInteger(map.get("goodid").toString()));
                    elasticService.upsert(goods, typeName, false);
                    totalHits++;
                }
                LOG.info("total update data: {}", totalHits);
            } catch (Exception e) {
                LOG.error(e.getMessage());
            }
            LOG.info("--------------1、更新过期商品结束----------------------" + typeName + "\n");

            LOG.info("--------------2、更新禁用商品开始----------------------" + typeName);
            try {
                //2、更新es中title中有敏感词的goods，将其status置为false
                clients = initESClient();
                int sensitiveCount = 0;
                String querySensitive = String.format(QUERY_SENSITIVE, "title", "避孕套,自慰棒,震动棒,自慰器,阴蒂,自慰,充气娃娃,安全套");
                SearchResponse searchResponses = clients.prepareSearch().setIndices(indexName).setTypes(typeName)
                        .setSearchType(SearchType.DEFAULT)
                        .setScroll(new TimeValue(60000))
                        .setQuery(querySensitive)
                        .setFrom(0).setSize(50000)
                        .execute().actionGet();

                for (SearchHit hit : searchResponses.getHits()) {
                    Map<String, Object> map = hit.getSource();
                    Goods goods = new Goods();
                    goods.setStatus(0);
                    goods.setGoodid(new BigInteger(map.get("goodid").toString()));
                    elasticService.upsert(goods, typeName, false);
                    sensitiveCount++;
                }
                LOG.info("total update data: {}", sensitiveCount);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
            LOG.info("--------------2、更新禁用商品结束----------------------" + typeName + "\n");
        }catch (Exception e){
            LOG.error(e.getMessage());
        } finally {
            clients.close();
        }
    }

    /*
     * 2、从es中得到quanurl，根据quanurl判断优惠券是否有效，如果无效设status为false
     */
    public void updateEsStatusByQuanurl(){
        Client clients = initESClient();
        try {
            LOG.info("--------------3、根据quanurl跟新status开始---------------------- \n");
            LOG.info("----------------------update status begin by quanurl-----------------" + typeName);
            initESClient();
            SearchResponse sp = clients.prepareSearch().setIndices(indexName).setTypes(typeName)
                    .setSearchType(SearchType.DEFAULT)
                    .setScroll(new TimeValue(60000))
                    .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("status", "true")))
                    .setFrom(0).setSize(50000)
                    .execute().actionGet();

            long total = sp.getHits().getTotalHits();
            int falseCount = 1;
            int orderCount = 1;
            for (SearchHit hit : sp.getHits()) {
                Map<String, Object> map = hit.getSource();
                String quanUrl = null;
                try {
                    quanUrl = map.get("quanurl").toString();
                } catch (NullPointerException n) {
                }
                orderCount++;
                boolean statusFlag = false;
                if (quanUrl != null) {
                    //判断是否失效
                    //quanUrl = "https://uland.taobao.com/coupon/edetail?e=kiGA8uSGQ%2BMN%2BoQUE6FNzLHOv%2FKoJ4LsM%2FOCBWMCflrRpLchICCDoqC1%2FGlh90mJlEdKc6OpCBMIstK5O8VvSx0HgBdG%2FDDL%2F1M%2FBw7Sf%2FdophctbWodnnMmEY5JmKtjGXg8TH1ADyFAERRRAXgpOZFprvLPHRAQ&pid=mm_115940806_14998982_61982315&itemId=36711838016&af=1\n";
                    if (quanUrl.indexOf("?") > -1) {
                        String urlName = "https://shop.m.taobao.com/shop/coupon.htm?" + quanUrl.substring(quanUrl.indexOf("?") + 1, quanUrl.length());
                        String html = HttpRequest.getStatusBySendGet(urlName);
                        if (!Strings.isNullOrEmpty(html)) {
                            if (html.contains("该优惠券不存在或者已经过期") || html.contains("您浏览店铺不存在或已经关闭")) {
                                statusFlag = true;
                            }
                        }
                    } else {
                        statusFlag = true;
                    }

                    //如果失效更新设状态为0
                    if (statusFlag) {
                        try {
                            Goods goods = new Goods();
                            goods.setStatus(0);
                            String goodid = map.get("goodid").toString();
                            goods.setGoodid(new BigInteger(goodid));

                            String json = OBJECT_MAPPER.writeValueAsString(goods);

                            //update
                            UpdateResponse updateResponse = clients.prepareUpdate(indexName, typeName, goodid)
                                    .setDoc(json)
                                    .execute()
                                    .actionGet();

                            if (updateResponse.getVersion() != hit.getVersion()) {
                                LOG.info("update status is false succeed，type：[ " + typeName + " ]，goodid：[ " + goodid + "]，processing：[ " + falseCount++ + " ]，deal：[ " + orderCount + " ]， total：[ " + total + " ]");
                            }
                        } catch (Exception e) {
                            LOG.error(e.getMessage());
                        }
                    }
                }
            }
            LOG.info("----------------------update status end by quanurl-----------------" + typeName + "\n");
            LOG.info("--------------3、根据quanurl跟新status结束---------------------- \n");
        }catch (Exception e){
            LOG.error(e.getMessage());
        } finally {
            clients.close();
        }
    }

    /*
     * 3、将验证和更新过后es中goods_excel的数据写入到文件中
     */
    public  void getGoodFromEs(){
        Client clients = null;
        try {
//            String today = "2017-01-09";
//            String txtFilePath = "F:\\datafortag\\maimaimai\\quan-" + today;
            String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String txtFilePath = "/home/elastic/maimaimai/elastic-validation/file/quan-" + today;
            LOG.info("--------------4、向 " + txtFilePath + " 写入数据开始---------------------- \n");
            File txtFile = null;
            FileOutputStream fileOutputStream = null;
            OutputStreamWriter outputStream = null;
            BufferedWriter bufferedWriter = null;
            int count = 1;
            try {
                txtFile = new File(txtFilePath);
                if (!txtFile.exists()) {
                    txtFile.createNewFile();
                }
                fileOutputStream = new FileOutputStream(txtFile);
                outputStream = new OutputStreamWriter(fileOutputStream);
                bufferedWriter = new BufferedWriter(outputStream);

                clients = initESClient();
                SearchResponse search = clients.prepareSearch().setIndices("index_goods_coupon").setTypes("goods_coupon")
                        .setSearchType(SearchType.DEFAULT)
                        .setScroll(new TimeValue(600000))
                        .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("status", "true")))
                        .setFrom(0).setSize(50000)
                        .execute().actionGet();

                for (SearchHit hit : search.getHits()) {
                    Map<String, Object> map = hit.getSource();
                    if (map != null && map.size() > 0) {
                        count++;
                        int lastColumn = 0;
                        for (String column : listColumn) {
                            String value = null;
                            try {
                                value = map.get(column).toString();
                            } catch (NullPointerException n) {

                            }
                            lastColumn++;
                            if (value != null && !"".equals(value) && value.length() > 0) {
                                bufferedWriter.write(value);
                                if (lastColumn < 23) {//23
                                    bufferedWriter.write("\u0001");
                                }
                            } else {
                                bufferedWriter.write("\u0001");
                                if (lastColumn < 23) {
                                    bufferedWriter.write("\u0001");
                                }
                            }
                        }
                    }
                    bufferedWriter.newLine();
                }
                bufferedWriter.flush();
                LOG.info("来自于es状态为true的数据共有：[ " + search.getHits().getTotalHits() + " ],共写入文件数据: [ " + count + " ]");
            } catch (Exception e) {
                LOG.error(e.getMessage());
            } finally {
                try {
                    bufferedWriter.close();
                    outputStream.close();
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            LOG.info("--------------4、向 " + txtFilePath + " 写入数据结束---------------------- \n");
        }catch (Exception e){
            LOG.error(e.getMessage());
        }finally {
            clients.close();
        }
    }

    /*
     * 4、从es中取得goodid，根据goodid去网站爬取这个商品的详细类目，更新到es中detailCategory
     */
    public void updateThirdCategory() {
        Client clients = initESClient();
        try {
            LOG.info("---------------------5、get detailCategory for goods_coupon begin-----------------");
            String todayString = Constants.DAY_FORMAT.format(new Date());
            Date date = Constants.DAY_FORMAT.parse(todayString);
            //date = addDay(date, -1);
            long today = date.getTime() / 1000;

            SearchResponse sp = clients.prepareSearch().setIndices(indexName).setTypes(typeName)
                    .setSearchType(SearchType.DEFAULT)
                    .setScroll(new TimeValue(60000))
                    .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("status", "true")).must(QueryBuilders.missingQuery("detailCategory")).must(QueryBuilders.rangeQuery("ctime").gte(today)))
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
                try {
                    goodId = map.get("goodid").toString();
                } catch (NullPointerException n) {
                }

                try {
                    topCategory = map.get("topCategory").toString();
                } catch (NullPointerException n) {
                }

                if (goodId != null) {
                    //根据goodId抓取商品三级目录
                    String detailCategory = null;
                    try {
                        LOG.info("Now processing : {}, goodid [ {} ], total: {}", cnt, goodId, total);
                        String html = categoryUtil.getHtmlContent(urlPrefix + goodId);
                        if (StringUtils.isEmpty(html))
                            continue;

                        // Parse search list
                        Document searchDoc = Jsoup.parse(html);
                        Elements searchEles = searchDoc.select("table.gridtable tbody tr td font");
                        if (null != searchEles && 0 < searchEles.size()) {
                            for (int index = 0; index < searchEles.size(); index++) {
                                Element eleCat = searchEles.get(index);
                                if (eleCat.html().contains("暂无数据")) {
                                    LOG.info("goodid : [ " + goodId + " ], Category未查询到, 网页显示为 \"暂无数据\"");
                                    continue;
                                } else {
//                            thirdCategory = StringEscapeUtils.unescapeHtml(eleCat.html().split("(\\s\\S)")[0]);
                                    detailCategory = StringEscapeUtils.unescapeHtml4(eleCat.html().split("(\\s\\S)")[0]);
                                    LOG.info("goodid : [ " + goodId + " ], Category is : [ " + detailCategory + " ]");

                                    if (detailCategory != null) {
                                        Goods goods = new Goods();
                                        goods.setStatus(1);
                                        goods.setGoodid(new BigInteger(goodId));
                                        goods.setDetailCategory(detailCategory);
                                        if (topCategory == null) {
                                            goods.setTopCategory(categoryUtil.getGoodsMysqlGeneralCategory(detailCategory));
                                            goods.setCategory(categoryUtil.getGoodsCategory(detailCategory));
                                        }

                                        String json = OBJECT_MAPPER.writeValueAsString(goods);

                                        //update
                                        UpdateResponse updateResponse = clients.prepareUpdate(indexName, typeName, goodId)
                                                .setDoc(json)
                                                .execute()
                                                .actionGet();

                                        if (updateResponse.getVersion() != hit.getVersion()) {
                                            LOG.info("update detailCategory is succeed，goodid：[ " + goodId + "]，processing：[ " + count++ + " ]，deal：[ " + cnt + " ]， total：[ " + total + " ]");
                                        } else {
                                            LOG.error("update detailCategory is failed，goodid：[ " + goodId + "]，processing：[ " + count++ + " ]，deal：[ " + cnt + " ]， total：[ " + total + " ]");
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOG.error(e.getMessage());
                    }
                }
            }
            LOG.info("----------------------5、get detailCategory for goods_coupon end-----------------\n");
        }catch (Exception e){
            LOG.error(e.getMessage());
        } finally {
            clients.close();
        }

    }

    //检查优惠券不能用的情况
    public void priceValidation(){
        LOG.info("---------------------6、get thirdCategory for goods_excel begin-----------------");

        SearchResponse sp = client.prepareSearch().setIndices("coupon").setTypes("goods_excel")
                .setSearchType(SearchType.DEFAULT)
                .setScroll(new TimeValue(60000))
                .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("status", true)))
                .setFrom(0).setSize(50000)
                .execute().actionGet();

        long totalCount = sp.getHits().getTotalHits();
        int dealCount = 0;
        int count = 0;
        for (SearchHit hit : sp.getHits()) {
            Map<String, Object> map = hit.getSource();
            String quanDesc = null;
            try {
                quanDesc = map.get("quanDesc").toString();
            } catch (NullPointerException n) {
            }
            dealCount++;
            if (quanDesc != null) {
                double quanPrice = 0;
                String goodId = map.get("goodid").toString();
                if(quanDesc.contains("元无条件券")){
                    String priceStr = quanDesc.replaceAll("元无条件券","");
                    quanPrice = Double.parseDouble(priceStr);
                }else if (quanDesc.contains("元减")){//满120元减60元
                    //String str = quanDesc.replaceAll("元","");
                    String priceStr = quanDesc.split("元")[0].replaceAll("满","");
                    quanPrice = Double.parseDouble(priceStr);
                }else{
                    System.out.println("error:"+quanDesc);

                }
                double price = Double.valueOf(map.get("price").toString());
                //如果quanDesc描述的满多少的价格大于这个商品本身价格，即购买当前商品优惠券无法使用的情况
                if(quanPrice > price){
                    LOG.info("update price is succeed，type：[goods_excel]，goodid：[ " + goodId + " ]，processing：[ " + count++ + " ]，deal：[ " + dealCount + " ]， total：[ " + totalCount + " ]");
                }
            }
        }
    }

    public void insertShopNickForEs(){
        SearchResponse sp = client.prepareSearch().setIndices("coupon").setTypes("goods_excel")
                .setSearchType(SearchType.DEFAULT)
                .setScroll(new TimeValue(60000))
                .setQuery(QueryBuilders.matchAllQuery())
                .setFrom(0).setSize(100000)
                .execute().actionGet();
        try {
            long totalCount = sp.getHits().getTotalHits();
            int dealCount = 0;
            int count = 0;
            for (SearchHit hit : sp.getHits()) {
                Map<String, Object> map = hit.getSource();
                String shopNick = null;
                try {
                    shopNick = map.get("nick").toString();
                } catch (NullPointerException e) {

                }
                dealCount++;
                if (shopNick != null) {
                    String json = null;
                    Map<String, String> mapNick = new HashMap<>();
                    mapNick.put("shop_nick", shopNick);
                    try {
                        json = JsonUtil.OBJECT_MAPPER.writeValueAsString(mapNick);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }

//                    IndexResponse response = client
//                            .prepareIndex("index_shop_rating", "shop_rating")
//                            .setId(shopNick)
//                            .setOpType(IndexRequest.OpType.CREATE)
//                            .setSource(json.getBytes("utf-8"))
//                            .execute().actionGet();
//
//                    if (response != null && response.isCreated()) {
//                        LOG.info("insert is succeed，type：[shop_rating]，shopNick：[ " + shopNick + " ]，processing：[ " + ++count + " ]，deal：[ " + dealCount + " ]， total：[ " + totalCount + " ]");
//                    } else {
//                        LOG.error("insert failed, shopNick : {}", shopNick);
//                    }
                    GetResponse getResponse = client.prepareGet("index_shop_rating", "shop_rating", shopNick).execute().actionGet();
                    if (getResponse == null) {
                        LOG.error("response is null with shop_nick :{}", shopNick);
                    } else if (getResponse.isExists()) {
                        // update logic
//                        try {
//                            //String json = OBJECT_MAPPER.writeValueAsString(shop);
//
//                            //LOG.info("will update item with id:{}",id);
//                            UpdateResponse updateResponse = client
//                                    .prepareUpdate("index_shop_rating", "shop_rating", shopNick)
//                                    .setVersion(getResponse.getVersion())
//                                    .setDoc(json).execute()
//                                    .actionGet();
//                            if (updateResponse.getVersion() == getResponse.getVersion() + 1) {
//                                LOG.info("update success with shop_nick: {}", shopNick);
//                            }
//                        } catch (Exception e) {
//                            LOG.error(e.getMessage(), e);
//                            LOG.info("++++++++++++++++++++++++++++++++++update failed with shop_nick:{}", shopNick);
//                        }
                        LOG.info("++++++++++++++++++++++++++++++++++have benn exist, shop_nick:{}", shopNick);
                    } else {
                        try {
                            // insert logic
                            IndexResponse response = client
                                    .prepareIndex("index_shop_rating", "shop_rating")
                                    .setId(shopNick)
                                    .setOpType(IndexRequest.OpType.CREATE)//
                                    .setSource(json.getBytes("utf-8"))
                                    .execute().actionGet();
                            if (response != null && response.isCreated()) {
                                LOG.info("insert is succeed，type：[shop_rating]，shopNick：[ " + shopNick + " ]，processing：[ " + ++count + " ]，deal：[ " + dealCount + " ]， total：[ " + totalCount + " ]");

                            } else {
                                LOG.error("+++++++++++++++index failed for response is null  or created false for shop_nick:{}", shopNick);
                            }
                        } catch (Exception e) {
                            LOG.error(e.getMessage(), e);
                            LOG.info("++++++++++++++++++++++++++++++++++insert failed with shop_nick:{}", shopNick);
                        }
                    }
                }
            }
        }catch (Exception e){
            LOG.error(e.getMessage());
        }
    }





    public static void main(String[] args) {
        try {
            StatusValidation statusValidation = App.getAppContext().getBean(StatusValidation.class);

            //2、各种验证，判断优惠券是否有效
            statusValidation.validation();

            //3、根据quanurl，判断优惠券是否有效
            statusValidation.updateEsStatusByQuanurl();

            //4、将es中status为true的所有数据写入文件
            statusValidation.getGoodFromEs();

            //1、根据goodid，去网站爬取thirdCategory，更新到es中goods_excel
            statusValidation.updateThirdCategory();

            //statusValidation.priceValidation();

            //statusValidation.insertShopNickForEs();

        } catch (Exception e){

        } finally {
            //client.close();
        }
    }


    // 增加或减少天数
    public static Date addDay(Date date, int num) {
        Calendar startDT = Calendar.getInstance();
        startDT.setTime(date);
        startDT.add(Calendar.DAY_OF_MONTH, num);
        return startDT.getTime();
    }

}
