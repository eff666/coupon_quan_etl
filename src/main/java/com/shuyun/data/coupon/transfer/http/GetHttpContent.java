package com.shuyun.data.coupon.transfer.http;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.google.common.base.Joiner;
import com.shuyun.data.coupon.transfer.parser.*;
import com.shuyun.data.coupon.transfer.pojo.WDetailGoods;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
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
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class GetHttpContent {

    private static final Logger LOG = LoggerFactory.getLogger(GetHttpContent.class);

    private static Client client;

    static ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd"))
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    //.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);

    static Set<String> goodsIdList = new HashSet<String>();
    static ConcurrentLinkedQueue<SearchHit[]> queuesforconverse = new ConcurrentLinkedQueue<>();
    static AtomicBoolean isInsert = new AtomicBoolean(true);

    static {
        try {
            Settings settings = Settings.settingsBuilder().put("cluster.name", "elasticsearch-cluster").build();
            TransportClient transportClient = TransportClient.builder().settings(settings).build();
            transportClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("10.10.138.183"), 9500));
            transportClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("10.10.128.242"), 9500));
            client = transportClient;
        }catch (Exception e){
            LOG.error("连接client发生错误, 错误原因：{}" + e.getMessage());
        }
    }

    public static boolean isNullOrEmpty(String str){
        if(str != null && !"".equalsIgnoreCase(str) && str.length() > 0){
            return true;
        }
        return false;
    }

    public static boolean insertGoods(WDetailGoods goods) {
        String indexName = "index_goods_detail";
        String typeName = "goods_detail";
        if (goods == null) {
            LOG.info("插入数据失败，goods为空");
            return false;
        }

        try {
            String id = goods.getGoodsId().toString();
            GetResponse getResponse = client.prepareGet(indexName, typeName, id).execute().actionGet();
            if (getResponse == null) {
                LOG.error("response is null, goodsId :{}", id);
                return false;
            } else if (getResponse.isExists()) {
                // update logic
                try {
                    String json = objectMapper.writeValueAsString(goods);

                    UpdateResponse updateResponse = client
                            .prepareUpdate(indexName, typeName, id)
                            .setVersion(getResponse.getVersion())
                            .setDoc(json)
                            .execute()
                            .actionGet();
                    if (updateResponse.getVersion() != getResponse.getVersion()) {
                        //LOG.info("update successes with id:{}", id);
                        return true;
                    }
                } catch (DocumentAlreadyExistsException d){
                    LOG.info("update document already exist, goodsId:{}", id);
                } catch (VersionConflictEngineException e) {
                    LOG.error(e.getMessage(), e);
                    LOG.info("update version conflict and will try again, goodsId:{}", id);
                    // recursion
                    return insertGoods(goods);
                }
            } else {
                try {
                    // insert logic
                    String json = objectMapper.writeValueAsString(goods);
                    IndexResponse response = client
                            .prepareIndex(indexName, typeName)
                            .setId(id)
                            .setOpType(IndexRequest.OpType.CREATE)//
                            .setSource(json.getBytes("utf-8"))
                            .execute()
                            .actionGet();
                    if (response != null && response.isCreated()) {
                        //LOG.info("insert successes with id:{}", id);
                        return true;
                    } else {
                        //LOG.error("insert goods failed, goodsId:{}", id);
                        return false;
                    }
                } catch (DocumentAlreadyExistsException e) {
                    LOG.error(e.getMessage(), e);
                    LOG.info("document already exists, goodsId:{}", id);
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            try {
                LOG.error(objectMapper.writeValueAsString(goods));
            } catch (JsonProcessingException e1) {
                e1.printStackTrace();
            }
        }
        return false;
    }

    private static WDetailGoods getGoods(JsonParser jsonParser, String url){
    //private static WDetailGoods getGoods(DataParser dataParser){
        WDetailGoods goods = new WDetailGoods();
        goods.setStatus("false");
        try {
            String ret = jsonParser.getRet()[0];
            if (isNullOrEmpty(ret) && "SUCCESS::调用成功".equalsIgnoreCase(ret)) {
                DataParser dataParser = jsonParser.getData();
                if (dataParser != null) {
                    /*
                     * 1、apiStack
                     * 产品价格库存等相关信息
                     */
                    List<JsonNode> dataList = null;
                    String valueParser = null;
                    //try {
                        valueParser = dataParser.getApiStack().get(0).getValue();
                        if (isNullOrEmpty(valueParser)) {
                            JsonNode jsonNode = objectMapper.readTree(valueParser);
                            dataList = jsonNode.findValues("data");
                        }
//                    }catch (Exception e){
//                        LOG.error("解析data中的value出错，URL：{}，\n value:{}, \n错误信息：{}", url, valueParser, e.getMessage());
//                    }
                    if (dataList != null && dataList.size() > 0) {
                        JsonNode data = dataList.get(0);
                        if (data != null) {
                            try {
                                JsonNode itemInfoModel = data.get("itemInfoModel");
                                if (itemInfoModel != null) {
                                    JsonNode priceUnits = itemInfoModel.get("priceUnits");
                                    try {
                                        //针对Tmall
                                        //促销价格
                                        goods.setDiscountPrice(priceUnits.get(0).get("rangePrice").asText());
                                        if (priceUnits.size() > 1) {
                                            //原价格
                                            goods.setOriginalPrice(priceUnits.get(1).get("price").asText());
                                        } else if(priceUnits.size() == 1){
                                            //原价格
                                            goods.setOriginalPrice(priceUnits.get(0).get("price").asText());
                                        }

                                    }catch (NullPointerException e){
                                        try {
                                            //针对taobao
                                            if (priceUnits != null && priceUnits.size() > 1) {
                                                //促销价格
                                                goods.setDiscountPrice(priceUnits.get(0).get("price").asText());
                                                //原价格
                                                goods.setOriginalPrice(priceUnits.get(1).get("price").asText());
                                            } else if (priceUnits != null && priceUnits.size() == 1) {
                                                //促销价格
                                                goods.setDiscountPrice(priceUnits.get(0).get("rangePrice").asText());
                                                //原价格
                                                goods.setOriginalPrice(priceUnits.get(0).get("price").asText());
                                            }
                                        }catch (NullPointerException n){

                                         }
                                    }
                                    try {
                                        //月销量
                                        goods.setTotalSoldQuantity(Long.parseLong(itemInfoModel.get("totalSoldQuantity").asText()));
                                    }catch (NullPointerException e){
                                    }

                                    try{
                                        //库存
                                        goods.setQuantity(Long.parseLong(itemInfoModel.get("quantity").asText()));
                                    }catch (NullPointerException e){
                                    }

                                    try{
                                        //天猫积分
                                        goods.setPoints(itemInfoModel.get("points").asText());
                                    }catch (NullPointerException e){

                                    }
                                }

                                //不同尺码商品库存数量
                                try {
                                    Object skuQuantity = data.get("skuModel").get("skus");
                                    if (skuQuantity != null) {
                                        String sku = objectMapper.writeValueAsString(skuQuantity);
                                        goods.setSkuQuantity(sku);
                                    }
                                }catch (Exception e){

                                }

                                //商品售前保证相关信息
                                try {
                                    JsonNode beforeGuarantees = data.get("guaranteeInfo").get("beforeGuarantees");
                                    if(beforeGuarantees != null && beforeGuarantees.size() > 0) {
                                        List<String> beforeGuaranteesList = new ArrayList<>();
                                        for (int i = 0; i < beforeGuarantees.size(); i++) {
                                            beforeGuaranteesList.add(beforeGuarantees.get(i).get("title").asText());
                                        }
                                        if (beforeGuaranteesList.size() > 0) {
                                            goods.setBeforeGuarantees(Joiner.on(",").join(beforeGuaranteesList));
                                        }
                                    }
                                }catch (Exception e){

                                }

                                //商品售后保证相关信息
                                try {
                                    JsonNode afterGuarantees = data.get("guaranteeInfo").get("afterGuarantees");
                                    if(afterGuarantees != null && afterGuarantees.size() > 0) {
                                        List<String> afterGuaranteesList = new ArrayList<>();
                                        for (int i = 0; i < afterGuarantees.size(); i++) {
                                            afterGuaranteesList.add(afterGuarantees.get(i).get("title").asText());
                                        }
                                        if(afterGuaranteesList != null && afterGuaranteesList.size() > 0) {
                                            goods.setAfterGuarantees(Joiner.on(",").join(afterGuaranteesList));
                                        }
                                    }
                                }catch (Exception e){

                                }


                                try {
                                    //卖家包邮描述
                                    String deliveryFeesDesc = data.get("delivery").get("deliveryFees").toString();
                                    deliveryFeesDesc = deliveryFeesDesc.substring(2, deliveryFeesDesc.length() - 2);
                                    goods.setDeliveryFeesDesc(deliveryFeesDesc.replaceAll(" ", ""));
                                }catch (Exception e){

                                }

                                try {
                                    //邮费
                                    String postagePrice = data.get("subInfos").get(0).asText();
                                    //postagePrice = postagePrice.split("快递")[1].replaceAll("元", " ").trim();
                                    Float postPrice = null;
                                    try {
                                        postPrice = Float.parseFloat(postagePrice.split("快递")[1].replaceAll("元", " ").trim());
                                    }catch (Exception e){
                                        if(goods.getDeliveryFeesDesc().contains("包邮")) {
                                            postPrice = 0f;
                                        }
                                    }
                                    goods.setPostagePrice(postPrice);
                                }catch (NullPointerException e){

                                }
                            }catch(Exception e) {
                                LOG.error("解析apiStack发生错误，URL:{} \n错误原因：{} \n错误信息：{}", url, e.getCause(), e.getMessage());
                            }
                        }
                    }

                    /*
                     * 2、itemInfoModel
                     * 商品详情
                     */
                    //商品详情
                    ItemInfoModelParser itemInfoModel = dataParser.getItemInfoModel();
                    if (itemInfoModel != null) {
                        try {
                            //商品名称
                            goods.setTitle(itemInfoModel.getTitle());
                            //商品id
                            goods.setGoodsId(new BigInteger(itemInfoModel.getItemId()));
                            //收藏商品人数
                            goods.setFavcount(itemInfoModel.getFavcount());
                            //商品移动端详情
                            goods.setMobileItemUrl(itemInfoModel.getItemUrl());
                            //店铺所在地
                            goods.setLocation(itemInfoModel.getLocation());
                            //商品主图
                            String[] goodsImgs = itemInfoModel.getPicsPath();
                            String goodsImg = Joiner.on(",").join(goodsImgs);
                            goods.setGoodsImg(goodsImg);
                            //卖家类型平台logo
                            goods.setItemTypeLogo(itemInfoModel.getItemTypeLogo());
                            //卖家类型
                            String itemType = itemInfoModel.getItemTypeName();
                            if ("tmall".equals(itemType)) {
                                goods.setItemType(1);
                            } else {
                                goods.setItemType(0);
                                LOG.warn("平台类型value：{}" + itemType);
                            }
                        }catch(Exception e) {
                            LOG.error("解析itemInfoModel发生错误，URL:{}，\n错误原因：{} \n错误信息：{}", url, e.getCause(), e.getMessage());
                        }
                    }

                    /*
                    * 3、props
                    * 商品属性详情
                     */
                    try{
                        List<PropsParser> propsList = dataParser.getProps();
                        if(propsList != null && propsList.size() > 0) {
                            Map<String, String> map = new LinkedHashMap<String, String>();
                            for (int i = 0; i < propsList.size(); i++) {
                                PropsParser props = propsList.get(i);
                                map.put(props.getName(), props.getValue());
                            }
                            if(map != null && map.size() > 0){
                                //商品属性详情
                                goods.setGoodsPropsDetail(objectMapper.writeValueAsString(map));
                            }
                        }
                    }catch(Exception e) {
                        LOG.error("解析props发生错误，URL:{}， \n错误原因：{} \n错误信息：{}", url, e.getCause(), e.getMessage());
                    }

                    /*
                     * 4、descInfo
                     * 商品详情介绍图片，需要得到fullDescUrl，发送http请求得到
                     */
                    DescInfoParser descInfoParser = dataParser.getDescInfo();

                    if(descInfoParser != null) {
                        try {
                            goods.setPcDescUrl(descInfoParser.getPcDescUrl());

                            String fullDescUrl = descInfoParser.getFullDescUrl();
                            if (isNullOrEmpty(fullDescUrl)) {
                                String htmlResponse = doGet(fullDescUrl);
                                try {
                                    JsonNode jsonNode = objectMapper.readTree(htmlResponse);
                                    dataList = jsonNode.findValues("data");
                                    if (dataList != null && dataList.size() > 0) {
                                        JsonNode data = dataList.get(0);
                                        if (data != null) {
                                            JsonNode html = data.get("desc");
                                            if (html != null) {
                                                String fullDescUrlHtml = objectMapper.writeValueAsString(html);
                                                goods.setFullDescUrlHtml(fullDescUrlHtml);
                                            }
                                        }
                                    }
                                } catch (IOException e) {
                                    LOG.error("解析descInfo1发生错误，URL:{}, \n错误信息：{}", url, e.getMessage());
                                }
                            }

                            String briefDescUrl = descInfoParser.getBriefDescUrl();
                            if (isNullOrEmpty(briefDescUrl)) {
                                //BriefDescUrlParser briefDescUrlParser = null;
                                try {
//                                briefDescUrlParser = new ObjectMapper().readValue(htmlResponse, new TypeReference<BriefDescUrlParser>() {
//                                });
                                    String htmlResponse = doGet(briefDescUrl);
                                    JsonNode jsonNode = objectMapper.readTree(htmlResponse);
                                    dataList = jsonNode.findValues("data");
                                    if (dataList != null && dataList.size() > 0) {
                                        JsonNode data = dataList.get(0);
                                        if (data != null) {
                                            JsonNode images = data.get("images");
                                            if (images != null) {
                                                String detailImgUrls = Joiner.on(",").join(images);
                                                goods.setDetailImgUrls(detailImgUrls);
                                            }
                                        }
                                    }
                                } catch (IOException e) {
                                    LOG.error("解析descInfo2发生错误，URL:{} \n错误信息：{}", url, e.getMessage());
                                }

//                            String rets = briefDescUrlParser.getRet()[0];
//                            if (isNullOrEmpty(rets) && rets.contains("SUCCESS")) {
//                                //使用fullDescUrl
//                                //                        Document searchDoc = Jsoup.parse(htmlResponse);
//                                //                        org.jsoup.select.Elements imgList = searchDoc.getElementsByAttribute("align");
//                                //                        List<String> detailImgUrlList = new ArrayList<>();
//                                //                        for(int i = 0; i < imgList.size(); i++){
//                                //                            List<Attribute> attributeList = imgList.get(i).attributes().asList();
//                                //                            if(attributeList.size() > 1) {
//                                //                                String img = attributeList.get(1).getValue();
//                                //                                detailImgUrlList.add(img.substring(2, img.length() - 2));
//                                //                            }
//                                //                        }
//                                //                        String detailImgUrls = Joiner.on(",").join(detailImgUrlList);
//                                //                        System.out.println(detailImgUrls);
//
//                                List<String> images = briefDescUrlParser.getData().getImages();
//                                if (images != null && images.size() > 0) {
//                                    String detailImgUrls = Joiner.on(",").join(images);
//                                    goods.setDetailImgUrls(detailImgUrls);
//                                }
//                            }
                            }
                        } catch (Exception e) {
                            LOG.error("解析descInfo发生错误，URL:{}，\n错误原因：{} \n错误信息：{}", url, e.getCause(), e.getMessage());
                        }
                    }


                    /*
                     * 5、skuModel
                     * 商品颜色详情
                     */
                    List<SkuPropsParser> skuModelParser = dataParser.getSkuModel().getSkuProps();
                    try{
                        if (skuModelParser != null) {
                            List<Object> values = skuModelParser.get(0).getValues();
                            //商品颜色属性数据
                            if(values != null && values.size() > 0) {
                                goods.setColourPropsDetail(objectMapper.writeValueAsString(values));
                            }
                        }
                    }catch(Exception e) {
                        LOG.error("解析skuModel发生错误，URL:{}，\n错误原因：{} \n错误信息：{}", url, e.getCause(), e.getMessage());
                    }

                    /*
                     * 6、seller
                     * 店铺相关信息
                     */
                    SellerParser sellerParser = dataParser.getSeller();
                    try{
                        if (sellerParser != null) {
                            //店铺昵称
                            goods.setShopNick(sellerParser.getNick());
                            //店铺信誉等级
                            goods.setShopCreditLevel(Long.parseLong(sellerParser.getCreditLevel()));
                            try {
                                //店铺id
                                goods.setShopId(new BigInteger(sellerParser.getShopId()));
                            }catch (NullPointerException n){}

                            try{
                                //好评率
                                goods.setShopGoodRatePercentage(sellerParser.getGoodRatePercentage());
                            }catch (NullPointerException n){}

                            try {
                                //店铺粉丝人数
                                goods.setShopFansCount(sellerParser.getFansCount());
                            }catch (NullPointerException n){}

                            try {
                                //店铺logo
                                goods.setShopLogo(sellerParser.getPicUrl());
                            }catch (NullPointerException n){}

                            try{
                                //店铺类型
                                goods.setShopType(sellerParser.getType());
                            }catch (NullPointerException n){}

                            try {
                                //店铺开店时间
                                goods.setShopTime(sellerParser.getStarts());
                            }catch (NullPointerException n){}

                            try {
                                //全部商品数
                                goods.setTotalGoods(sellerParser.getActionUnits().get(0).getValue());
                            }catch (NullPointerException n){}

                            //店铺评分信息
                            List<EvaluateInfoParser> evaluateInfoList = sellerParser.getEvaluateInfo();
                            if (evaluateInfoList.size() > 0) {
                                for (int i = 0; i < evaluateInfoList.size(); i++) {
                                    EvaluateInfoParser evaluateInfoParser = evaluateInfoList.get(i);
                                    String title = evaluateInfoParser.getTitle();
                                    if (title.contains("描述")) {
                                        String shopDescScore = evaluateInfoParser.getScore();
                                        String shopDescHighGap = evaluateInfoParser.getHighGap();
                                        goods.setShopDescScore(Float.parseFloat(shopDescScore));
                                        goods.setShopDescHighGap(shopDescHighGap);
                                    } else if (title.contains("服务")) {
                                        String shopServiceScore = evaluateInfoParser.getScore();
                                        String shopServiceHighGap = evaluateInfoParser.getHighGap();
                                        goods.setShopServiceScore(Float.parseFloat(shopServiceScore));
                                        goods.setShopServiceHighGap(shopServiceHighGap);
                                    } else if (title.contains("发货")) {
                                        String shopShippingScore = evaluateInfoParser.getScore();
                                        String shopShippingHighGap = evaluateInfoParser.getHighGap();
                                        goods.setShopShippingScore(Float.parseFloat(shopShippingScore));
                                        goods.setShopShippingHighGap(shopShippingHighGap);
                                    } else {

                                    }
                                }
                            }
                        }
                    }catch(Exception e) {
                        LOG.error("解析seller发生错误，URL:{}， \n错误原因：{} \n错误信息：{}",url, e.getCause(), e.getMessage());
                    }


                    /*
                     * 7、rateInfo
                     * 商品评价详情
                     */
                    try {
                        RateInfoParser rateInfoParser = dataParser.getRateInfo();
                        if(rateInfoParser != null) {
                            //评价总数
                            goods.setGoodsRateCounts(new BigInteger(rateInfoParser.getRateCounts()));
                            //评价详情
                            List<rateDetailListParser> rateDetailList = rateInfoParser.getRateDetailList();
                            if(rateDetailList != null && rateDetailList.size() > 0) {
                                Map<String, String> map = new HashMap<>();
                                for (int i = 0; i < rateDetailList.size(); i++) {
                                    rateDetailListParser rateDetail = rateDetailList.get(i);
                                    if(isNullOrEmpty(rateDetail.getNick())){
                                        map.put("nick", rateDetail.getNick());
                                    }
                                    if(isNullOrEmpty(rateDetail.getHeadPic())){
                                        map.put("headPic", rateDetail.getHeadPic());
                                    }
                                    if(isNullOrEmpty(rateDetail.getSubInfo())){
                                        map.put("subInfo", rateDetail.getSubInfo());
                                    }
                                    if(isNullOrEmpty(rateDetail.getStar())){
                                        map.put("star", rateDetail.getStar());
                                    }
                                    if(isNullOrEmpty(rateDetail.getFeedback())){
                                        map.put("feedback", rateDetail.getFeedback());
                                    }

                                    List<String> ratePicList = rateDetail.getRatePicList();
                                    if (ratePicList != null && ratePicList.size() > 0) {
                                        String ratePic = Joiner.on(",").join(ratePicList);
                                        map.put("ratePicList", ratePic);
                                    }
                                }
                                if(map != null && map.size() > 0){
                                    goods.setGoodsRateDetailInfo(objectMapper.writeValueAsString(map));
                                }
                            }
                        }
                    }catch(Exception e) {
                        LOG.error("解析rateInfo发生错误，URL:{}，\n错误原因：{} \n错误信息：{}", url, e.getCause(), e.getMessage());
                    }
                }
                goods.setStatus("true");
           }
        }catch (Exception e){
            LOG.error("解析数据data发生错误，URL:{}，\n错误原因：{} \n错误信息：{}", url, e.getLocalizedMessage(), e.getMessage());
        }
        return goods;
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

    private static void getGoodsId(){
        SearchResponse search = client.prepareSearch().setIndices("index_goods_coupon").setTypes("goods_coupon")
                .setSearchType(SearchType.DEFAULT)
                .setScroll(new TimeValue(60000))
                .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("status", "true")))
                .setFrom(0).setSize(100000)
                .execute().actionGet();

        LOG.info("total hits num:{}", search.getHits().totalHits());

        for (SearchHit hit : search.getHits()) {
            Map<String, Object> map = hit.getSource();
            String goodsId = map.get("goodid").toString();
            if(isNullOrEmpty(goodsId)){
                goodsIdList.add(goodsId);
            }
        }
    }

    private static WDetailGoods getGoodsDetail(DataParser dataParser, String url){
        WDetailGoods goods = new WDetailGoods();
        goods.setStatus("false");
        try {
                if (dataParser != null) {
                    /*
                     * 1、apiStack
                     * 产品价格库存等相关信息
                     */
                    List<JsonNode> dataList = null;
                    String valueParser = null;
                    //try {
                    valueParser = dataParser.getApiStack().get(0).getValue();
                    if (isNullOrEmpty(valueParser)) {
                        JsonNode jsonNode = objectMapper.readTree(valueParser);
                        dataList = jsonNode.findValues("data");
                    }
//                    }catch (Exception e){
//                        LOG.error("解析data中的value出错，URL：{}，\n value:{}, \n错误信息：{}", url, valueParser, e.getMessage());
//                    }
                    if (dataList != null && dataList.size() > 0) {
                        JsonNode data = dataList.get(0);
                        if (data != null) {
                            try {
                                JsonNode itemInfoModel = data.get("itemInfoModel");
                                if (itemInfoModel != null) {
                                    JsonNode priceUnits = itemInfoModel.get("priceUnits");
                                    try {
                                        //针对Tmall
                                        //促销价格
                                        goods.setDiscountPrice(priceUnits.get(0).get("rangePrice").asText());
                                        if (priceUnits.size() > 1) {
//                                            //促销价格
//                                            goods.setDiscountPrice(priceUnits.get(0).get("rangePrice").asText());
                                            //原价格
                                            goods.setOriginalPrice(priceUnits.get(1).get("price").asText());
                                        } else if(priceUnits.size() == 1){
//                                            //促销价格
//                                            goods.setDiscountPrice(priceUnits.get(0).get("rangePrice").asText());
                                            //原价格
                                            goods.setOriginalPrice(priceUnits.get(0).get("price").asText());
                                        }

                                    }catch (NullPointerException e){
                                        try {
                                            //针对taobao
                                            if (priceUnits != null && priceUnits.size() > 1) {
                                                //促销价格
                                                goods.setDiscountPrice(priceUnits.get(0).get("price").asText());
                                                //原价格
                                                goods.setOriginalPrice(priceUnits.get(1).get("price").asText());
                                            } else if (priceUnits != null && priceUnits.size() == 1) {
                                                //促销价格
                                                goods.setDiscountPrice(priceUnits.get(0).get("rangePrice").asText());
                                                //原价格
                                                goods.setOriginalPrice(priceUnits.get(0).get("price").asText());
                                            }
                                        }catch (NullPointerException n){

                                        }
                                    }
                                    try {
                                        //月销量
                                        goods.setTotalSoldQuantity(Long.parseLong(itemInfoModel.get("totalSoldQuantity").asText()));
                                    }catch (NullPointerException e){
                                    }

                                    try{
                                        //库存
                                        goods.setQuantity(Long.parseLong(itemInfoModel.get("quantity").asText()));
                                    }catch (NullPointerException e){
                                    }

                                    try{
                                        //天猫积分
                                        goods.setPoints(itemInfoModel.get("points").asText());
                                    }catch (NullPointerException e){

                                    }
                                }

                                //不同尺码商品库存数量
                                try {
                                    Object skuQuantity = data.get("skuModel").get("skus");
                                    if (skuQuantity != null) {
                                        String sku = objectMapper.writeValueAsString(skuQuantity);
                                        goods.setSkuQuantity(sku);
                                    }
                                }catch (Exception e){

                                }

                                //商品售前保证相关信息
                                try {
                                    JsonNode beforeGuarantees = data.get("guaranteeInfo").get("beforeGuarantees");
                                    if(beforeGuarantees != null && beforeGuarantees.size() > 0) {
                                        List<String> beforeGuaranteesList = new ArrayList<>();
                                        for (int i = 0; i < beforeGuarantees.size(); i++) {
                                            beforeGuaranteesList.add(beforeGuarantees.get(i).get("title").asText());
                                        }
                                        if (beforeGuaranteesList.size() > 0) {
                                            goods.setBeforeGuarantees(Joiner.on(",").join(beforeGuaranteesList));
                                        }
                                    }
                                }catch (Exception e){

                                }

                                //商品售后保证相关信息
                                try {
                                    JsonNode afterGuarantees = data.get("guaranteeInfo").get("afterGuarantees");
                                    if(afterGuarantees != null && afterGuarantees.size() > 0) {
                                        List<String> afterGuaranteesList = new ArrayList<>();
                                        for (int i = 0; i < afterGuarantees.size(); i++) {
                                            afterGuaranteesList.add(afterGuarantees.get(i).get("title").asText());
                                        }
                                        if(afterGuaranteesList != null && afterGuaranteesList.size() > 0) {
                                            goods.setAfterGuarantees(Joiner.on(",").join(afterGuaranteesList));
                                        }
                                    }
                                }catch (Exception e){

                                }


                                try {
                                    //卖家包邮描述
                                    String deliveryFeesDesc = data.get("delivery").get("deliveryFees").toString();
                                    deliveryFeesDesc = deliveryFeesDesc.substring(2, deliveryFeesDesc.length() - 2);
                                    goods.setDeliveryFeesDesc(deliveryFeesDesc.replaceAll(" ", ""));
                                }catch (Exception e){

                                }

                                try {
                                    //邮费
                                    String postagePrice = data.get("subInfos").get(0).asText();
                                    //postagePrice = postagePrice.split("快递")[1].replaceAll("元", " ").trim();
                                    Float postPrice = null;
                                    try {
                                        postPrice = Float.parseFloat(postagePrice.split("快递")[1].replaceAll("元", " ").trim());
                                    }catch (Exception e){
                                        if(goods.getDeliveryFeesDesc().contains("包邮")) {
                                            postPrice = 0f;
                                        }
                                    }
                                    goods.setPostagePrice(postPrice);
                                }catch (NullPointerException e){

                                }
                            }catch(Exception e) {
                                LOG.error("解析apiStack发生错误，URL:{} \n错误原因：{} \n错误信息：{}", url, e.getCause(), e.getMessage());
                            }
                        }
                    }

                    /*
                     * 2、itemInfoModel
                     * 商品详情
                     */
                    //商品详情
                    ItemInfoModelParser itemInfoModel = dataParser.getItemInfoModel();
                    if (itemInfoModel != null) {
                        try {
                            //商品名称
                            goods.setTitle(itemInfoModel.getTitle());
                            //商品id
                            goods.setGoodsId(new BigInteger(itemInfoModel.getItemId()));
                            //收藏商品人数
                            goods.setFavcount(itemInfoModel.getFavcount());
                            //商品移动端详情
                            goods.setMobileItemUrl(itemInfoModel.getItemUrl());
                            //店铺所在地
                            goods.setLocation(itemInfoModel.getLocation());
                            //商品主图
                            String[] goodsImgs = itemInfoModel.getPicsPath();
                            String goodsImg = Joiner.on(",").join(goodsImgs);
                            goods.setGoodsImg(goodsImg);
                            //卖家类型平台logo
                            goods.setItemTypeLogo(itemInfoModel.getItemTypeLogo());
                            //卖家类型
                            String itemType = itemInfoModel.getItemTypeName();
                            if ("tmall".equals(itemType)) {
                                goods.setItemType(1);
                            } else {
                                goods.setItemType(0);
                                LOG.warn("平台类型value：{}" + itemType);
                            }
                        }catch(Exception e) {
                            LOG.error("解析itemInfoModel发生错误，URL:{}，\n错误原因：{} \n错误信息：{}", url, e.getCause(), e.getMessage());
                        }
                    }

                    /*
                    * 3、props
                    * 商品属性详情
                     */
                    try{
                        List<PropsParser> propsList = dataParser.getProps();
                        if(propsList != null && propsList.size() > 0) {
                            Map<String, String> map = new LinkedHashMap<String, String>();
                            for (int i = 0; i < propsList.size(); i++) {
                                PropsParser props = propsList.get(i);
                                map.put(props.getName(), props.getValue());
                            }
                            if(map != null && map.size() > 0){
                                //商品属性详情
                                goods.setGoodsPropsDetail(objectMapper.writeValueAsString(map));
                            }
                        }
                    }catch(Exception e) {
                        LOG.error("解析props发生错误，URL:{}， \n错误原因：{} \n错误信息：{}", url, e.getCause(), e.getMessage());
                    }

                    /*
                     * 4、descInfo
                     * 商品详情介绍图片，需要得到fullDescUrl，发送http请求得到
                     */
                    DescInfoParser descInfoParser = dataParser.getDescInfo();

                    if(descInfoParser != null) {
                        try {
                            goods.setPcDescUrl(descInfoParser.getPcDescUrl());

                            String fullDescUrl = descInfoParser.getFullDescUrl();
                            if (isNullOrEmpty(fullDescUrl)) {
                                String htmlResponse = doGet(fullDescUrl);
                                try {
                                    JsonNode jsonNode = objectMapper.readTree(htmlResponse);
                                    dataList = jsonNode.findValues("data");
                                    if (dataList != null && dataList.size() > 0) {
                                        JsonNode data = dataList.get(0);
                                        if (data != null) {
                                            JsonNode html = data.get("desc");
                                            if (html != null) {
                                                String fullDescUrlHtml = objectMapper.writeValueAsString(html);
                                                goods.setFullDescUrlHtml(fullDescUrlHtml);
                                            }
                                        }
                                    }
                                } catch (IOException e) {
                                    LOG.error("解析descInfo1发生错误，URL:{}, \n错误信息：{}", url, e.getMessage());
                                }
                            }

                            String briefDescUrl = descInfoParser.getBriefDescUrl();
                            if (isNullOrEmpty(briefDescUrl)) {
                                //BriefDescUrlParser briefDescUrlParser = null;
                                try {
//                                briefDescUrlParser = new ObjectMapper().readValue(htmlResponse, new TypeReference<BriefDescUrlParser>() {
//                                });
                                    String htmlResponse = doGet(briefDescUrl);
                                    JsonNode jsonNode = objectMapper.readTree(htmlResponse);
                                    dataList = jsonNode.findValues("data");
                                    if (dataList != null && dataList.size() > 0) {
                                        JsonNode data = dataList.get(0);
                                        if (data != null) {
                                            JsonNode images = data.get("images");
                                            if (images != null) {
                                                String detailImgUrls = Joiner.on(",").join(images);
                                                goods.setDetailImgUrls(detailImgUrls);
                                            }
                                        }
                                    }
                                } catch (IOException e) {
                                    LOG.error("解析descInfo2发生错误，URL:{} \n错误信息：{}", url, e.getMessage());
                                }

//                            String rets = briefDescUrlParser.getRet()[0];
//                            if (isNullOrEmpty(rets) && rets.contains("SUCCESS")) {
//                                //使用fullDescUrl
//                                //                        Document searchDoc = Jsoup.parse(htmlResponse);
//                                //                        org.jsoup.select.Elements imgList = searchDoc.getElementsByAttribute("align");
//                                //                        List<String> detailImgUrlList = new ArrayList<>();
//                                //                        for(int i = 0; i < imgList.size(); i++){
//                                //                            List<Attribute> attributeList = imgList.get(i).attributes().asList();
//                                //                            if(attributeList.size() > 1) {
//                                //                                String img = attributeList.get(1).getValue();
//                                //                                detailImgUrlList.add(img.substring(2, img.length() - 2));
//                                //                            }
//                                //                        }
//                                //                        String detailImgUrls = Joiner.on(",").join(detailImgUrlList);
//                                //                        System.out.println(detailImgUrls);
//
//                                List<String> images = briefDescUrlParser.getData().getImages();
//                                if (images != null && images.size() > 0) {
//                                    String detailImgUrls = Joiner.on(",").join(images);
//                                    goods.setDetailImgUrls(detailImgUrls);
//                                }
//                            }
                            }
                        } catch (Exception e) {
                            LOG.error("解析descInfo发生错误，URL:{}，\n错误原因：{} \n错误信息：{}", url, e.getCause(), e.getMessage());
                        }
                    }


                    /*
                     * 5、skuModel
                     * 商品颜色详情
                     */
                    List<SkuPropsParser> skuModelParser = dataParser.getSkuModel().getSkuProps();
                    try{
                        if (skuModelParser != null) {
                            List<Object> values = skuModelParser.get(0).getValues();
                            //商品颜色属性数据
                            if(values != null && values.size() > 0) {
                                goods.setColourPropsDetail(objectMapper.writeValueAsString(values));
                            }
                        }
                    }catch(Exception e) {
                        LOG.error("解析skuModel发生错误，URL:{}，\n错误原因：{} \n错误信息：{}", url, e.getCause(), e.getMessage());
                    }

                    /*
                     * 6、seller
                     * 店铺相关信息
                     */
                    SellerParser sellerParser = dataParser.getSeller();
                    try{
                        if (sellerParser != null) {
                            try {
                                //店铺昵称
                                goods.setShopNick(sellerParser.getNick());
                            }catch (NullPointerException n){}

                            try {
                                //店铺信誉等级
                                goods.setShopCreditLevel(Long.parseLong(sellerParser.getCreditLevel()));
                            }catch (NullPointerException n){}
                            try {
                                //店铺id
                                goods.setShopId(new BigInteger(sellerParser.getShopId()));
                            }catch (NullPointerException n){}

                            try{
                                //好评率
                                goods.setShopGoodRatePercentage(sellerParser.getGoodRatePercentage());
                            }catch (NullPointerException n){}

                            try {
                                //店铺粉丝人数
                                goods.setShopFansCount(sellerParser.getFansCount());
                            }catch (NullPointerException n){}

                            try {
                                //店铺logo
                                goods.setShopLogo(sellerParser.getPicUrl());
                            }catch (NullPointerException n){}

                            try{
                                //店铺类型
                                goods.setShopType(sellerParser.getType());
                            }catch (NullPointerException n){}

                            try {
                                //店铺开店时间
                                goods.setShopTime(sellerParser.getStarts());
                            }catch (NullPointerException n){}

                            try {
                                //全部商品数
                                goods.setTotalGoods(sellerParser.getActionUnits().get(0).getValue());
                            }catch (NullPointerException n){}

                            //店铺评分信息
                            List<EvaluateInfoParser> evaluateInfoList = sellerParser.getEvaluateInfo();
                            if (evaluateInfoList.size() > 0) {
                                for (int i = 0; i < evaluateInfoList.size(); i++) {
                                    EvaluateInfoParser evaluateInfoParser = evaluateInfoList.get(i);
                                    String title = evaluateInfoParser.getTitle();
                                    if (title.contains("描述")) {
                                        String shopDescScore = evaluateInfoParser.getScore();
                                        String shopDescHighGap = evaluateInfoParser.getHighGap();
                                        goods.setShopDescScore(Float.parseFloat(shopDescScore));
                                        goods.setShopDescHighGap(shopDescHighGap);
                                    } else if (title.contains("服务")) {
                                        String shopServiceScore = evaluateInfoParser.getScore();
                                        String shopServiceHighGap = evaluateInfoParser.getHighGap();
                                        goods.setShopServiceScore(Float.parseFloat(shopServiceScore));
                                        goods.setShopServiceHighGap(shopServiceHighGap);
                                    } else if (title.contains("发货")) {
                                        String shopShippingScore = evaluateInfoParser.getScore();
                                        String shopShippingHighGap = evaluateInfoParser.getHighGap();
                                        goods.setShopShippingScore(Float.parseFloat(shopShippingScore));
                                        goods.setShopShippingHighGap(shopShippingHighGap);
                                    } else {

                                    }
                                }
                            }
                        }
                    }catch(Exception e) {
                        LOG.error("解析seller发生错误，URL:{}， \n错误原因：{} \n错误信息：{}",url, e.getCause(), e.getMessage());
                    }


                    /*
                     * 7、rateInfo
                     * 商品评价详情
                     */
                    try {
                        RateInfoParser rateInfoParser = dataParser.getRateInfo();
                        if(rateInfoParser != null) {
                            //评价总数
                            goods.setGoodsRateCounts(new BigInteger(rateInfoParser.getRateCounts()));
                            //评价详情
                            List<rateDetailListParser> rateDetailList = rateInfoParser.getRateDetailList();
                            if(rateDetailList != null && rateDetailList.size() > 0) {
                                Map<String, String> map = new HashMap<>();
                                for (int i = 0; i < rateDetailList.size(); i++) {
                                    rateDetailListParser rateDetail = rateDetailList.get(i);
                                    if(isNullOrEmpty(rateDetail.getNick())){
                                        map.put("nick", rateDetail.getNick());
                                    }
                                    if(isNullOrEmpty(rateDetail.getHeadPic())){
                                        map.put("headPic", rateDetail.getHeadPic());
                                    }
                                    if(isNullOrEmpty(rateDetail.getSubInfo())){
                                        map.put("subInfo", rateDetail.getSubInfo());
                                    }
                                    if(isNullOrEmpty(rateDetail.getStar())){
                                        map.put("star", rateDetail.getStar());
                                    }
                                    if(isNullOrEmpty(rateDetail.getFeedback())){
                                        map.put("feedback", rateDetail.getFeedback());
                                    }

                                    List<String> ratePicList = rateDetail.getRatePicList();
                                    if (ratePicList != null && ratePicList.size() > 0) {
                                        String ratePic = Joiner.on(",").join(ratePicList);
                                        map.put("ratePicList", ratePic);
                                    }
                                }
                                if(map != null && map.size() > 0){
                                    goods.setGoodsRateDetailInfo(objectMapper.writeValueAsString(map));
                                }
                            }
                        }
                    }catch(Exception e) {
                        LOG.error("解析rateInfo发生错误，URL:{}，\n错误原因：{} \n错误信息：{}", url, e.getCause(), e.getMessage());
                    }
                }
                goods.setStatus("true");

        }catch (Exception e){
            LOG.error("解析数据data发生错误，URL:{}，\n错误原因：{} \n错误信息：{}", url, e.getLocalizedMessage(), e.getMessage());
        }
        return goods;
    }

    private static WDetailGoods getHits(SearchHit hit){
        WDetailGoods goods = new WDetailGoods();
        String goodsId = null;
        try {
            Map<String, Object> map = hit.getSource();
            goodsId = map.get("goodsId").toString();
            String data = map.get("data").toString();
            if (data != null && !"IS_NULL".equalsIgnoreCase(data)) {
                try {
                    String url = "http://hws.m.taobao.com/cache/wdetail/5.0/?id=" + goodsId;
                    //                    String response = doGet(url);
                    DataParser jsonParser = null;
                    try {
                        jsonParser = new ObjectMapper().readValue(data, new TypeReference<DataParser>() {
                        });
                    } catch (IOException e) {
                        LOG.error("解析data发生错误，goodsId：{}， \n错误原因：{} \n错误信息：{}", goodsId, e.getCause(), e.getMessage());
                        e.printStackTrace();
                    }

                    if (jsonParser != null) {
                        goods = getGoodsDetail(jsonParser, url);
                        if (goods.getGoodsId() == null) {
                            goods.setGoodsId(new BigInteger(goodsId));
                        }
                    }
                } catch (Exception e) {
                    LOG.error(e.getMessage());
                }
            } else {
                goods.setStatus("false");
                goods.setGoodsId(new BigInteger(goodsId));
                LOG.info("商品已下架或不存在，goodsId: " + goodsId + "================================================================");
            }
        }catch (Exception e){
            goods.setStatus("false");
            goods.setGoodsId(new BigInteger(goodsId));
            LOG.error("得到hits数据发生错误，goodsId: {}， 错误原因：{}", goodsId, e.getMessage());
        }
        return goods;
    }


    public static void main(String[] args) {

        try {
            //从index_goods_coupon得到status为true的商品
            //得到ctime字段
            String strDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            //String strDate = "2017-03-14";

            SearchResponse searchResponse = client.prepareSearch().setIndices("index_goods_detail_snapshot").setTypes("goods_detail_snapshot")
                    .setSearchType(SearchType.SCAN)
                    .setScroll(new TimeValue(60000))
                    .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery("ctime").gte(strDate)))
                    .setSize(50)
                    .execute().actionGet();

            long total = searchResponse.getHits().totalHits();
            LOG.info("insert goods begin, total:{}", total);
            Integer process = 0;
            Integer success = 0;
            long timeforlive = 800000;
            TimeValue timeValue = new TimeValue(timeforlive);
            while(true) {
                try {
                    for (SearchHit hit : searchResponse.getHits().getHits()) {
                        process++;
                        WDetailGoods goods = getHits(hit);
                        BigInteger goodsId = goods.getGoodsId();
                        if (goodsId != null) {
                            try {
                                Date date = new SimpleDateFormat("yyyy-MM-dd").parse(strDate);
                                goods.setCtime(date.getTime() / 1000);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (insertGoods(goods)) {
                                LOG.info("insert or update goods success for goods_detail, total: {}, processing: {}, success: {}, goodsId: {}", total, process, success++, goodsId);
                            } else {
                                LOG.info("insert or update goods failed for goods_detail, goodsId: {} =====================================================", goodsId);
                            }
                        }
                    }

                    String scrollId = searchResponse.getScrollId();
                    LOG.info("processing: {}, scrollId : {} =====================================================", process, scrollId);
                    LOG.error("processing: {}, scrollId : {} =====================================================", process, scrollId);
                    try {
                        searchResponse = client.prepareSearchScroll(searchResponse.getScrollId()).setScroll(timeValue).execute().actionGet();
                    }catch (Exception n){
                        LOG.error("scrollId是空， scrollId: {}", scrollId);
                    }
                    if (searchResponse.getHits().getHits().length == 0) {
                        break;
                    }

                }catch (Exception e){
                    LOG.error("发生错误，错误原因：" + e.getMessage());
                }



//                for (SearchHit hit : search.getHits().getHits()) {
//                    process++;
//                    Map<String, Object> map = hit.getSource();
//                    String goodsId = map.get("goodsId").toString();
//                    String data = map.get("data").toString();
//                    if(data != null && !"IS_NULL".equalsIgnoreCase(data)){
//                        try {
//                            String url="http://hws.m.taobao.com/cache/wdetail/5.0/?id=" + goodsId;
//        //                    String response = doGet(url);
//                            DataParser jsonParser = null;
//                            try {
//                                jsonParser = new ObjectMapper().readValue(data, new TypeReference<DataParser>() {
//                                });
//                            } catch (IOException e) {
//                                LOG.error("解析data发生错误，goodsId：{}， \n错误原因：{} \n错误信息：{}", goodsId, e.getCause(), e.getMessage());
//                                e.printStackTrace();
//                            }
//
//                            if(jsonParser != null){
//                                WDetailGoods goods = getGoodsDetail(jsonParser, url);
//                                try {
//                                    Date date = new SimpleDateFormat("yyyy-MM-dd").parse(strDate);
//                                    goods.setCtime(date.getTime() / 1000);
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                }
//                                if(goods.getGoodsId() == null){
//                                    goods.setGoodsId(new BigInteger(goodsId));
//                                }
//                                if(insertGoods(goods)){
//                                    LOG.info("insert or update goods success for goods_detail, total: {}, processing: {}, success: {}, goodsId: {}", total, process, success++, goodsId);
//                                }else {
//                                    LOG.info("insert or update goods failed for goods_detail, goodsId: {} =====================================================", goodsId);
//                                }
//                            }
//                        }catch (Exception e){
//                            LOG.error(e.getMessage());
//                        }
//                    } else {
//                        LOG.info("商品已下架或不存在，goodsId: " + goodsId  + "================================================================");
//                    }
//                }
//
//                search = client.prepareSearchScroll(search.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();
//                if (search.getHits().getHits().length == 0) {
//                    isInsert = new AtomicBoolean(false);
//                    break;
//                }
            }
            LOG.info("insert goods end, success:{}", success);
        }catch (Exception e){
            LOG.error(e.getMessage());
        }
    }




//    public static void main(String[] args) {
//
//        try {
//            //从index_goods_coupon得到status为true的商品
//            getGoodsId();
//            int total = goodsIdList.size();
//            Integer process = 0;
//            Integer success = 0;
//            LOG.info("insert good begin, total:{}", total);
//
//            //得到ctime字段
//            //SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
////            String strDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
////            Date date = null;
////            try {
////                date = new SimpleDateFormat("yyyy-MM-dd").parse(strDate);
////            } catch (ParseException e) {
////                e.printStackTrace();
////            }
////            long today = date.getTime() / 1000;
//
////           goodsIdList.add("545778122566");
////            goodsIdList.add("523187778803");
////            goodsIdList.add("525142933188");
//            for(String goodsId : goodsIdList) {
//                process++;
//                String url = "http://hws.m.taobao.com/cache/wdetail/5.0/?id=" + goodsId;
//                String response = doGet(url);
//
//                JsonParser jsonParser = null;
//                try {
//                    jsonParser = new ObjectMapper().readValue(response, new TypeReference<JsonParser>() {
//                    });
//                } catch (IOException e) {
//                    LOG.error("解析response发生错误，URL：{} \n错误原因：{} \n错误信息：{}", url, e.getCause(), e.getMessage());
//                    e.printStackTrace();
//                }
//
//                if(jsonParser != null){
//                    WDetailGoods goods = getGoods(jsonParser, url);
//                    try {
//                        Date date = new SimpleDateFormat("yyyy-MM-dd").parse(strDate);
//                        goods.setCtime(date.getTime() / 1000);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    //long today = date.getTime() / 1000;
//                    if(goods.getGoodsId() == null) {
//                        goods.setGoodsId(new BigInteger(goodsId));
//                    }
//                    if (insertGoods(goods)) {
//                        LOG.info("insert or update goods success, typeName: {}, total: {}, processing: {}, success: {}, goodsId: {}", "goods_detail", total, process, success++, goodsId);
//                    } else {
//                        LOG.info("insert or update goods failed, typeName: {}, goodsId: {}", "goods_detail", goodsId);
//                    }
//
//                }
//            }
//            LOG.info("insert goods end, total success: {}", success);
//        }catch (Exception e){
//            LOG.error(e.getMessage());
//        }
//
//
//
//       // doGet();
//        //System.out.println(doGet());540459138947;;41441418357;541085439228
////        SearchResponse search = client.prepareSearch().setIndices("index_goods_detail_snapshot").setTypes("goods_detail_snapshot")
////                .setSearchType(SearchType.DEFAULT)
////                .setScroll(new TimeValue(60000))
////                .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("status", "true")))
////                .setFrom(0).setSize(100)
////                .execute().actionGet();
////
////        long total = search.getHits().totalHits();
////        LOG.info("insert goods begin, total:{}", total);
////        Integer process = 0;
////        Integer success = 0;
////        for (SearchHit hit : search.getHits()) {
////            process++;
////            Map<String, Object> map = hit.getSource();
////            String goodsId = map.get("goodsId").toString();
////            String data = map.get("data").toString();
////            if(data != null){
////                try {
//////                    String url="http://hws.m.taobao.com/cache/wdetail/5.0/?id=540459138947";
//////                    String response = doGet(url);
////                    DataParser jsonParser = null;
////                    try {
////                        jsonParser = new ObjectMapper().readValue(data, new TypeReference<DataParser>() {
////                        });
////                    } catch (IOException e) {
////                        LOG.error("解析data发生错误，goodsId：{}， \n错误原因：{} \n错误信息：{}", goodsId, e.getCause(), e.getMessage());
////                        e.printStackTrace();
////                    }
////
////                    if(jsonParser != null){
////                        WDetailGoods goods = getGoods(jsonParser);
////                        if(goods.getGoodsId() == null){
////                            goods.setGoodsId(new BigInteger(goodsId));
////                        }
////                        if(insertGoods(goods)){
////                            LOG.info("insert or update goods success, typeName: {}, total: {}, processing: {}, success: {}, goodsId: {}", "goods_detail", total, process, success++, goodsId);
////                        }else {
////                            LOG.info("insert or update goods failed, typeName: {}, goodsId: {}", "goods_detail", goodsId);
////                        }
////                    }
////                }catch (Exception e){
////                    LOG.error(e.getMessage());
////                }
////            }
////        }
////        LOG.info("insert goods end, success:{}", success);
//
//    }


}
