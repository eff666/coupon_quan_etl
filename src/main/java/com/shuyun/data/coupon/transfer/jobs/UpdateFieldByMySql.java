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
import com.shuyun.data.coupon.transfer.pojo.ShopRating;
import com.shuyun.data.coupon.transfer.service.IElasticService;
import com.shuyun.data.coupon.transfer.util.CategoryUtil;
import com.shuyun.data.coupon.transfer.util.Constants;
import com.shuyun.data.coupon.transfer.util.HttpRequest;
import com.shuyun.data.coupon.transfer.util.JsonUtil;
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
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.InetAddress;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

@Component
public class UpdateFieldByMySql {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateFieldByMySql.class);
    private static Client client;

    @Autowired
    private IGoodsDao goodsDao;

    @Autowired
    private IElasticService elasticService;

    private CategoryUtil categoryUtil = new CategoryUtil();


    private ObjectMapper objectMapper = new ObjectMapper()
	        .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
//            .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    static {
        LOG.info("现在时间是:" + Calendar.getInstance().getTime());
        try {
            Settings settings = Settings.settingsBuilder().put("cluster.name", "elasticsearch-cluster").build();
            TransportClient transportClient = TransportClient.builder().settings(settings).build();
            transportClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("10.10.138.183"), 9500));
            client = transportClient;
        }catch (Exception e){

        }
    }


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

    public void updateEsFiled(){
        try {
            //1、更新es中category和detailsImgUrls
            //去mysql中查找T-1天商品，对应更新es中goods
            LOG.info("--------------1、从MySql更新category和detailsImgUrls商品开始----------------------goods_mysql");
            String todayStrings = Constants.DAY_FORMAT.format(new Date());
            Date dates = Constants.DAY_FORMAT.parse(todayStrings);
            Date dates1 = addDay(dates, -1);
            long yesterday = dates1.getTime() / 1000;
            long count = 0;
            List<Goods> goodsList = goodsDao.getUpdateCategoryAndDetailsImgUrlForEs(yesterday);
            long total = goodsList.size();
            if (goodsList.size() > 0) {
                for (Goods goods : goodsList) {
                    try {
                        goods.fillAll();
                    } catch (Exception e) {
                        LOG.error("goods error with goods id :{}", goods.getGoodid());
                        LOG.error(e.getMessage(), e);
                        goods.setStatus(0);
                    }

                    //根据category，更新generalCategory
                    //goods.setGeneralCategory(categoryUtil.getGoodsMysqlGeneralCategory(goods.getCategory()));

                    try {
                        if (elasticService.upsert(goods, "goods_mysql", false)) {
                            LOG.info("update success with goods id :{}, deal data: {}, total data: {}", goods.getGoodid(), count++, total);
                        } else {
                            LOG.info("update failed with goods id :{}, deal data: {}, total data: {}", goods.getGoodid(), count++, total);
                        }
                    } catch (Exception e) {
                        LOG.error("goods error with goods id :{}", goods.getGoodid());
                        LOG.error(e.getMessage(), e);
                    }
                }
            } else {
                LOG.info("between {} and {} is not data update from mysql", dates, dates1);
            }
            LOG.info("--------------1、从MySql更新category和generalCategory商品结束----------------------goods_mysql \n");
        }catch (Exception e){
            LOG.error(e.getMessage());
        }

    }

    // 增加或减少天数
    public static Date addDay(Date date, int num) {
        Calendar startDT = Calendar.getInstance();
        startDT.setTime(date);
        startDT.add(Calendar.DAY_OF_MONTH, num);
        return startDT.getTime();
    }


    //去mysql中查找tk_shop_rating所有数据，更新到es中
    public void updateIndexShopRatingByMySql(){
            //3、更新index_shop_rating
          //去mysql中查找tk_shop_rating所有数据，更新到es中
            LOG.info("--------------1、更新index_shop_rating商品开始----------------------");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://10.9.56.131:3306/maimaimai?useUnicode=true&characterEncoding=UTF-8";
            conn = DriverManager.getConnection(url, "u_maimm", "hwzxd1gbVop");
            System.out.println("写入数据开始，成功连接MySQL-------------------");

            String sql = "select * from tk_shop_rating order by pid";
            ps = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ps.setFetchSize(Integer.MIN_VALUE);
            rs = ps.executeQuery();

            ResultSetMetaData rsmd = rs.getMetaData();
            int colCount = rsmd.getColumnCount();

            List<String> columnName = Arrays.asList("shop_nick", "domain_name", "desc_rate",
                    "desc_flag", "desc_exceed_rate", "serv_rate", "serv_flag", "serv_exceed_rate",
                    "send_rate", "send_flag", "send_exceed_rate", "insert_time", "update_time", "shop_logo_url", "shop_promotion_url");
            int dealCount = 0;
            int count = 0;
            while (rs.next()) {  //while控制行数
                Map<String, String> map = new LinkedHashMap<>();
                dealCount++;
                for (int i = 1; i <= colCount; i++) {
                    String name = rsmd.getColumnName(i);
                    if (columnName.contains(name)) {
                        String value = rs.getString(i);
                        if (value != null && !"".equals(value.trim()) && value.trim().length() > 0) {
                            map.put(name, value);
                        }
                    }
                }

                if (map != null && map.size() > 0) {
                    String  shopNick = map.get("shop_nick");
                    if(shopNick != null && !"".equals(shopNick)) {
                        GetResponse getResponse = client.prepareGet("index_shop_rating", "shop_rating", shopNick).execute().actionGet();
                        if (getResponse == null) {
                            LOG.error("response is null with shop_nick :{}", shopNick);
                        } else if (getResponse.isExists()) {
                            // update logic
                            try {
                                String json = objectMapper.writeValueAsString(map);

                                //LOG.info("will update item with id:{}",id);
                                UpdateResponse updateResponse = client
                                        .prepareUpdate("index_shop_rating", "shop_rating", shopNick)
                                        .setVersion(getResponse.getVersion())
                                        .setDoc(json)
                                        .execute()
                                        .actionGet();
                                if (updateResponse.getVersion() == getResponse.getVersion() + 1) {
                                    LOG.info("update is succeed，shopNick：[ " + shopNick + " ]，processing：[ " + count++ + " ]，deal：[ " + dealCount + " ]");
                                }
                            } catch (Exception e) {
                                LOG.error(e.getMessage(), e);
                                LOG.info("++++++++++++++++++++++++++++++++++update failed with shop_nick:{}", shopNick);
                                LOG.error("+++++++++++++++++++++++++++++++++update failed with shop_nick:{}", shopNick);
                            }
                        } else {
                            try {
                                // insert logic
                                //LOG.info("id:{} does not exist in elastic and will insert",id);
                                String json = objectMapper.writeValueAsString(map);

                                IndexResponse response = client
                                        .prepareIndex("index_shop_rating", "shop_rating")
                                        .setId(shopNick)
                                        .setOpType(IndexRequest.OpType.CREATE)//
                                        .setSource(json.getBytes("utf-8"))
                                        .execute()
                                        .actionGet();
                                if (response != null && response.isCreated()) {
                                    LOG.info("insert is succeed，shopNick：[ " + shopNick + " ]，processing：[ " + count++ + " ]，deal：[ " + dealCount + " ]");
                                } else {
                                    LOG.info("-------------------------insert failed with shop_nick:{}", shopNick);
                                    LOG.error("------------------------insert failed with shop_nick:{}", shopNick);
                                }
                            } catch (Exception e) {
                                LOG.error(e.getMessage(), e);
                                LOG.info("----------------------------insert failed with shop_nick:{}", shopNick);
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            LOG.error(e.getMessage(), e);
        }
    }


    //全表扫描mysql，更新es中goods_mysql中的detailsImgUrl字段
    public void updateDetailImgUrlsByMySql(){
        try {
            //3、更新detailImgUrls
            //去mysql中查找T-2天商品，对应更新es中goods_mysql中detailImgUrls
            LOG.info("--------------1、更新detailImgUrls商品开始----------------------goods_mysql");
            String todayStrings = Constants.DAY_FORMAT.format(new Date());
            Date dates = Constants.DAY_FORMAT.parse(todayStrings);
            Date dates1 = addDay(dates, -1);
            //long todays = dates.getTime() / 1000;
            //long yesterday = dates1.getTime() / 1000;
            long yesterday = 1471937679;
            long totalHitss = 0;
            List<Goods> goodsList = goodsDao.getDetailImgUrls(yesterday);
            LOG.info("根据MySql共需要更新detailImgUrls商品:" + goodsList.size());
            if (goodsList.size() > 0) {
                for (Goods goods : goodsList) {
                    try {
                        goods.fillAll();
                    } catch (Exception e) {
                        LOG.error("goods error with goodsid :{}", goods.getGoodid());
                        LOG.error(e.getMessage(), e);
                        goods.setStatus(0);
                    }
                    //goods.setGeneralCategory(categoryUtil.getGoodsMysqlGeneralCategory(goods.getCategory()));


                    String id = goods.getGoodid().toString();
                    LOG.info("start to deal id:{}", id);
                    GetResponse getResponse = client.prepareGet("coupon", "goods_mysql", id).execute().actionGet();
                    if (getResponse == null) {
                        LOG.error("response is null with id :{}", id);
                    } else if (getResponse.isExists()) {
                        // update logic
                        try {
                            Map<String, Object> map = getResponse.getSourceAsMap();
                            long status = 0;
                            try {
                                if (Boolean.valueOf(map.get("status").toString())) {
                                    status = 1;
                                } else {
                                    status = 0;
                                }
                            } catch (NullPointerException n) {
                            }
                            goods.setStatus(status);

                            String json = JsonUtil.OBJECT_MAPPER.writeValueAsString(goods);

                            //LOG.info("will update item with id:{}",id);
                            UpdateResponse updateResponse = client
                                    .prepareUpdate("coupon", "goods_mysql", id)
                                    .setVersion(getResponse.getVersion())
                                    .setDoc(json).execute()
                                    .actionGet();
                            if (updateResponse.getVersion() == getResponse.getVersion() + 1) {
                                LOG.info("update success with id:{}", id);
                            }
                        } catch (Exception e) {
                            LOG.error(e.getMessage(), e);
                            LOG.info("++++++++++++++++++++++++++++++++++update failed with id:{}", id);
                        }
                    } else {
                        try {
                            // insert logic
                            //LOG.info("id:{} does not exist in elastic and will insert",id);
                            goods.setStatus(0);
                            String json = JsonUtil.OBJECT_MAPPER.writeValueAsString(goods);

                            IndexResponse response = client
                                    .prepareIndex("coupon", "goods_mysql")
                                    .setId(id)
                                    .setOpType(IndexRequest.OpType.CREATE)//
                                    .setSource(json.getBytes("utf-8")).execute().actionGet();
                            if (response != null && response.isCreated()) {
                                LOG.info("insert success with id:{}", id);
                            } else {
                                LOG.error("+++++++++++++++index failed for response is null  or created false for id:{}", id);
                            }
                        } catch (Exception e) {
                            LOG.error(e.getMessage(), e);
                            LOG.info("++++++++++++++++++++++++++++++++++insert failed with id:{}", id);
                        }
                    }

                }
            } else {
                LOG.info("between {} and {} is not data update from mysql", dates, dates1);
            }
            LOG.info("--------------4、更新category和generalCategory商品结束----------------------goods_mysql \n");
        }catch (Exception e){
            LOG.error(e.getMessage());
        }
    }

    //将es中goods_mysql中所有status为true的数据写入到good_coupon中
    public void insertGoodsMysqlForGoodsCoupon(){
        String typeName = "goods_mysql";
        LOG.info("start to insert type:{}", typeName);
        int count = 0;
        int cnt = 0;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = simpleDateFormat.format(new Date());
        Date date = null;
        try {
            date = Constants.DAY_FORMAT.parse(strDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        long today = date.getTime() / 1000;
        //QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("status", true)).must(QueryBuilders.rangeQuery("dateend").lte(today))
        SearchResponse search = client.prepareSearch().setIndices("coupon").setTypes("goods_mysql")
                .setSearchType(SearchType.DEFAULT)
                .setScroll(new TimeValue(60000))
                .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("status", true)))
                .setFrom(0).setSize(100000)
                .execute().actionGet();

        LOG.info("total hits num:{}", search.getHits().totalHits());
        long total = search.getHits().totalHits();
        System.out.println("-----------------------------------------------------total hits num:" + search.getHits().totalHits());
        for (SearchHit hit : search.getHits()) {
            cnt++;
            Map<String, Object> map = hit.getSource();
            Goods goods = new Goods();
            try {
                goods.setId(Long.parseLong(map.get("id").toString()));
                goods.setGoodid(new BigInteger(map.get("goodid").toString()));
                goods.setTitle(map.get("title").toString());
                goods.setImg(map.get("img").toString());
                goods.setPrice(Float.parseFloat(map.get("price").toString()));
                goods.setQuanprice(Float.parseFloat(map.get("quanprice").toString()));
                goods.setRestnum(Long.parseLong(map.get("restnum").toString()));
                goods.setDatestart(Long.parseLong(map.get("datestart").toString()));
                goods.setDateend(Long.parseLong(map.get("dateend").toString()));
                goods.setQuanurl(map.get("quanurl").toString());
                goods.setGoodurl(map.get("goodurl").toString());
                goods.setNick(map.get("nick").toString());
                goods.setSellerid(new BigInteger(map.get("sellerid").toString()));
                goods.setVolume(Long.parseLong(map.get("volume").toString()));
                goods.setUsertype(Integer.parseInt(map.get("usertype").toString()));
                goods.setStatus(1);
                goods.setCtime(Long.parseLong(map.get("ctime").toString()));
                goods.setProvcity(map.get("provcity").toString());
                goods.setDiscountRatio(Float.parseFloat(map.get("discountRatio").toString()));

                try {
                    goods.setDetailImgUrls(map.get("detailImgUrls").toString());
                } catch (NullPointerException n){
                }

                try {
                    goods.setSmallurl(map.get("smallurl").toString());
                } catch (NullPointerException n){
                }

                try {
                    goods.setCategory(map.get("category").toString());
                } catch (NullPointerException n){
                }

                getAbsentFiled(goods);

                try{
                    if (!elasticService.upsert(goods, "goods_coupon", false)) {
                        LOG.error("insert failed with goods id :{}", goods.getGoodid());
                    }else {
                        LOG.info("insert data succeed，goodid：[ " + goods.getGoodid() + "]，processing：[ " + count++ + " ]，deal：[ " + cnt + " ]， total：[ " + total + " ]");
                    }
                }catch (Exception e) {
                    LOG.error("goods error with goodsid :{}",goods.getGoodid());
                    LOG.error(e.getMessage(), e);
                }
            }catch (Exception e){
                LOG.error("error with goodid :{}, cause: {}",goods.getGoodid(), e.getMessage());
            }
        }

        LOG.info("end to insert data, total insert data: {}", count);
    }

    public void getAbsentFiled(Goods goods){
        /**
         * 优惠券ID，1c685413be5e4d7b8193aade1c53e0e2
         * mysql我通过quanurl提取出来，activity_id即为优惠券id
         * activity_id或者activityId
         * http://shop.m.taobao.com/shop/coupon.htm?seller_id=1637806102&activity_id=46cb90d7ddd94dec9bd8c49b188763cd
         */
        String quanUrl = goods.getQuanurl();
        String couponId = null;
        if(!Strings.isNullOrEmpty(quanUrl)){
            if(quanUrl.contains("activityId")){
                couponId = quanUrl.substring(quanUrl.indexOf("activityId") + 11, quanUrl.length());
            }else if(quanUrl.contains("activity_id")){
                couponId = quanUrl.substring(quanUrl.indexOf("activity_id") + 12, quanUrl.length());
            }
            if(couponId.contains("&sellerId")){
                couponId = couponId.substring(0, couponId.indexOf("&sellerId"));
            }else if(couponId.contains("&seller_id")){
                couponId = couponId.substring(0, couponId.indexOf("&seller_id"));
            }
            goods.setCouponId(couponId);
        }

        /**
         * 商品详情页url  淘宝：http://item.taobao.com/item.htm?id=537757536191
         * 天猫：https://detail.tmall.com/item.htm?id=542758201748
         * mysql我中通过goodid和固定前缀（http://item.taobao.com/item.htm?id=）拼成
         */
        String goodId = goods.getGoodid().toString();
        if(!Strings.isNullOrEmpty(goodId)){
            String itemDetailUrl = "http://item.taobao.com/item.htm?id=" + goodId;
            int userType = goods.getUsertype();
            if(userType == 1){
                itemDetailUrl = "https://detail.tmall.com/item.htm?id=" + goodId;
            }
            goods.setItemDetailUrl(itemDetailUrl);
        }

        /**
         * 优惠券总量
         * mysql中商品需要我去爬取
         */
        /**
         * 优惠券面额
         * mysql中商品需要我去爬取
         */
        if(!Strings.isNullOrEmpty(quanUrl)){
            if (quanUrl.indexOf("?") > -1) {
                //quanUrl = quanUrl.substring(quanUrl.indexOf("?") + 1, quanUrl.length());
                String urlName = "https://shop.m.taobao.com/shop/coupon.htm?" + quanUrl.substring(quanUrl.indexOf("?") + 1, quanUrl.length());
                String html = HttpRequest.getStatusBySendGet(urlName);
                if (!Strings.isNullOrEmpty(html)) {
                    if (!Strings.isNullOrEmpty(html)) {
                        if (html.contains("该优惠券不存在或者已经过期") || html.contains("您浏览店铺不存在或已经关闭")) {

                        } else {
                            Document searchDoc = Jsoup.parse(html);
                            org.jsoup.select.Elements couponDetails = searchDoc.select("#tbh5v0 .coupon dl dd");
                            //10元优惠券 剩13700 张（已领用46300 张） 单笔满37元可用，每人限领3 张 有效期:2016-08-13至2017-01-31

                            //总量优惠券 = 已领取 + 剩余优惠券
                            long totalnum = (Long.parseLong(couponDetails.select("span").get(1).text()) + Long.parseLong(couponDetails.select("span").get(0).text()));
                            String quanName = searchDoc.select("#tbh5v0 .coupon dl dt").text();
                            String quanDesc = couponDetails.get(1).text().replaceAll(" ", "");
                            String price = null;
                            if(quanDesc.contains("元无条件券")){
                                price = quanDesc.replaceAll("元无条件券","");
                            }else if (quanDesc.contains("单笔满")){//满120元减60元
                                //String str = quanDesc.replaceAll("元","");
                                price = quanDesc.split("元")[0].replaceAll("单笔满", "");
                            }else{
                                System.out.println("error:" + quanDesc);
                            }

                            goods.setTotalnum(totalnum);
                            if(price != null) {
                                quanDesc = "满" + price + "元减" + quanName.replaceAll("优惠券", "");
                                goods.setQuanDesc(quanDesc);
                            }
                        }
                    }
                }
            }
        }

        //goods.setCouponId();
        //goods.setItemDetailUrl();
        //goods.setTotalnum();
        //goods.setQuanDesc();

        //goods.setPromoteurl();//需要后面伟哥去爬取

        //根据category更新topCategory和detailCategory字段，2个字段
        try {
            String category = goods.getCategory();
            if (category != null && !"".equals(category.trim())){
                //goods.setCategory(category);
                goods.setDetailCategory(category);
                goods.setTopCategory(categoryUtil.getGoodsMysqlGeneralCategory(category));
            }
        }catch (NullPointerException nu){
        }
    }


    //将es中goods_excel中所有status为true的数据写入到good_coupon中
    public void insertGoodsExcelForGoodsCoupon(){
        String typeName = "goods_excel";
        LOG.info("start to insert type:{}", typeName);
        int count = 0;
        int cnt = 0;
        SearchResponse search = client.prepareSearch().setIndices("coupon").setTypes("goods_excel")
                .setSearchType(SearchType.DEFAULT)
                .setScroll(new TimeValue(60000))
                .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("status", true)))
                .setFrom(0).setSize(100000)
                .execute().actionGet();

        LOG.info("total hits num:{}", search.getHits().totalHits());
        long total = search.getHits().totalHits();
        System.out.println("-----------------------------------------------------total hits num:" + search.getHits().totalHits());
        for (SearchHit hit : search.getHits()) {
            cnt++;
            Map<String, Object> map = hit.getSource();
            Goods goods = new Goods();
            try {
                //goods.setId(Long.parseLong(map.get("id").toString()));
                goods.setGoodid(new BigInteger(map.get("goodid").toString()));
                goods.setTitle(map.get("title").toString());
                goods.setImg(map.get("img").toString());
                goods.setPrice(Float.parseFloat(map.get("price").toString()));
                goods.setQuanprice(Float.parseFloat(map.get("quanprice").toString()));
                goods.setRestnum(Long.parseLong(map.get("restnum").toString()));
                goods.setDatestart(Long.parseLong(map.get("datestart").toString()));
                goods.setDateend(Long.parseLong(map.get("dateend").toString()));
                goods.setQuanurl(map.get("quanurl").toString());
                goods.setGoodurl(map.get("goodurl").toString());
                goods.setNick(map.get("nick").toString());
                goods.setSellerid(new BigInteger(map.get("sellerid").toString()));
                goods.setVolume(Long.parseLong(map.get("volume").toString()));
                goods.setUsertype(Integer.parseInt(map.get("usertype").toString()));
                goods.setStatus(1);
                goods.setProvcity(map.get("provcity").toString());
                goods.setDiscountRatio(Float.parseFloat(map.get("discountRatio").toString()));


                goods.setQuanDesc(map.get("quanDesc").toString());
                goods.setTotalnum(Long.parseLong(map.get("totalnum").toString()));
                goods.setSellerWang(map.get("sellerWang").toString());
                goods.setItemDetailUrl(map.get("itemDetailUrl").toString());
                goods.setBrokerage(Float.parseFloat(map.get("brokerage").toString()));
                goods.setBrokerageRatio(Float.parseFloat(map.get("brokerageRatio").toString()));
                goods.setCouponId(map.get("couponId").toString());
                goods.setPromoteurl(map.get("promoteurl").toString());


                goods.setCategory(map.get("itemCatId").toString());
                goods.setTopCategory(map.get("generalCategory").toString());
                try {
                    goods.setDetailCategory(map.get("thirdCategory").toString());
                }catch (NullPointerException n){}


                //得到ctime字段，1个字段
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String strDate = simpleDateFormat.format(new Date());
                Date date = null;
                try {
                    date = simpleDateFormat.parse(strDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                goods.setCtime(date.getTime() / 1000);

                try{
                    if (!elasticService.upsert(goods, "goods_coupon", false)) {
                        LOG.error("insert failed with goods id :{}", goods.getGoodid());
                    }else {
                        LOG.info("insert data succeed，goodid：[ " + goods.getGoodid() + "]，processing：[ " + count++ + " ]，deal：[ " + cnt + " ]， total：[ " + total + " ]");
                    }
                }catch (Exception e) {
                    LOG.error("goods error with goodsid :{}",goods.getGoodid());
                    LOG.error(e.getMessage(), e);
                }
            }catch (Exception e){
                LOG.error("error with goodid :{}, cause: {}",goods.getGoodid(), e.getMessage());
            }
        }

        LOG.info("end to insert data, total insert data: {}", count);
    }


    public void updateShopRatingFiledForEs(){
        SearchResponse sp = client.prepareSearch().setIndices("index_shop_rating").setTypes("shop_rating")
                .setSearchType(SearchType.DEFAULT)
                .setScroll(new TimeValue(60000))
                .setQuery(QueryBuilders.boolQuery().mustNot(QueryBuilders.missingQuery("desc_flag")))
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
                    shopNick = map.get("shop_nick").toString();
                } catch (NullPointerException e) {
                }

                dealCount++;
                if (shopNick != null) {
                    String json = null;
                    String descFlag = null;
                    String sendFlag = null;
                    String servFlag = null;

                    try {
                        descFlag = map.get("desc_flag").toString();
                    } catch (NullPointerException e) {
                    }

                    try {
                        sendFlag = map.get("send_flag").toString();
                    } catch (NullPointerException e) {
                    }

                    try {
                        servFlag = map.get("serv_flag").toString();
                    } catch (NullPointerException e) {
                    }

                    if (!Strings.isNullOrEmpty(descFlag)) {
                        descFlag = getFlag(descFlag);
                        sendFlag = getFlag(sendFlag);
                        servFlag = getFlag(servFlag);
                        Map<String, String> mapNick = new HashMap<>();
                        mapNick.put("shop_nick", shopNick);
                        mapNick.put("desc_flag", descFlag);
                        mapNick.put("send_flag", sendFlag);
                        mapNick.put("serv_flag", servFlag);

                        try {
                            json = JsonUtil.OBJECT_MAPPER.writeValueAsString(mapNick);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }

                        GetResponse getResponse = client.prepareGet("index_shop_rating", "shop_rating", shopNick).execute().actionGet();
                        if (getResponse == null) {
                            LOG.error("response is null with shop_nick :{}", shopNick);
                        } else if (getResponse.isExists()) {
                            // update logic
                            try {
                                //String json = OBJECT_MAPPER.writeValueAsString(shop);

                                UpdateResponse updateResponse = client
                                        .prepareUpdate("index_shop_rating", "shop_rating", shopNick)
                                        .setVersion(getResponse.getVersion())
                                        .setDoc(json).execute()
                                        .actionGet();
                                if (updateResponse.getVersion() == getResponse.getVersion() + 1) {
                                    LOG.info("update is succeed，type：[shop_rating]，shopNick：[ " + shopNick + " ]，processing：[ " + ++count + " ]，deal：[ " + dealCount + " ]， total：[ " + totalCount + " ]");
                                }
                            } catch (Exception e) {
                                LOG.error(e.getMessage(), e);
                                LOG.info("++++++++++++++++++++++++++++++++++update failed with shop_nick:{}", shopNick);
                            }
                        } else {
                            LOG.info("++++++++++++++++++++++++++++++++++update failed with shop_nick:{}", shopNick);
                        }
                    }
                }
            }
        }catch (Exception e){
            LOG.error(e.getMessage());
        }
    }

    public void updateShopRatingFiled(){
        SearchResponse sp = client.prepareSearch().setIndices("index_shop_rating").setTypes("shop_rating")
                .setSearchType(SearchType.DEFAULT)
                .setScroll(new TimeValue(60000))
                .setQuery(QueryBuilders.boolQuery().mustNot(QueryBuilders.missingQuery("desc_flag")))
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
                    shopNick = map.get("shop_nick").toString();
                } catch (NullPointerException e) {
                }

                dealCount++;
                if (shopNick != null) {
                    String json = null;
                    String descFlag = null;
                    String sendFlag = null;
                    String servFlag = null;

                    try {
                        descFlag = map.get("desc_flag").toString();
                    } catch (NullPointerException e) {
                    }

                    try {
                        sendFlag = map.get("send_flag").toString();
                    } catch (NullPointerException e) {
                    }

                    try {
                        servFlag = map.get("serv_flag").toString();
                    } catch (NullPointerException e) {
                    }



                    if (getBoolean(descFlag) || getBoolean(sendFlag) || getBoolean(servFlag)) {
                        descFlag = getFlag(descFlag);
                        sendFlag = getFlag(sendFlag);
                        servFlag = getFlag(servFlag);
                        Map<String, String> mapNick = new HashMap<>();
                        mapNick.put("shop_nick", shopNick);
                        mapNick.put("desc_flag", descFlag);
                        mapNick.put("send_flag", sendFlag);
                        mapNick.put("serv_flag", servFlag);

                        try {
                            json = JsonUtil.OBJECT_MAPPER.writeValueAsString(mapNick);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }

                        GetResponse getResponse = client.prepareGet("index_shop_rating", "shop_rating", shopNick).execute().actionGet();
                        if (getResponse == null) {
                            LOG.error("response is null with shop_nick :{}", shopNick);
                        } else if (getResponse.isExists()) {
                            // update logic
                            try {
                                //String json = OBJECT_MAPPER.writeValueAsString(shop);

                                UpdateResponse updateResponse = client
                                        .prepareUpdate("index_shop_rating", "shop_rating", shopNick)
                                        .setVersion(getResponse.getVersion())
                                        .setDoc(json).execute()
                                        .actionGet();
                                if (updateResponse.getVersion() == getResponse.getVersion() + 1) {
                                    LOG.info("update is succeed，type：[shop_rating]，shopNick：[ " + shopNick + " ]，processing：[ " + ++count + " ]，deal：[ " + dealCount + " ]， total：[ " + totalCount + " ]");
                                }
                            } catch (Exception e) {
                                LOG.error(e.getMessage(), e);
                                LOG.info("++++++++++++++++++++++++++++++++++update failed with shop_nick:{}", shopNick);
                            }
                        } else {
                            LOG.info("++++++++++++++++++++++++++++++++++update failed with shop_nick:{}", shopNick);
                        }
                    }
                }
            }
        }catch (Exception e){
            LOG.error(e.getMessage());
        }
    }

    private boolean getBoolean(String str){
        boolean flag = false;
        if(!Strings.isNullOrEmpty(str)) {
            if ("持平".equalsIgnoreCase(str)) {
                flag = true;
            }
        }
        return flag;
    }


    private String getFlag(String flag){
        if(!Strings.isNullOrEmpty(flag)) {
            if (flag.equalsIgnoreCase("高于")) {
                flag = "1";
            } else if (flag.equalsIgnoreCase("低于")) {
                flag = "-1";
            } else if (flag.equalsIgnoreCase("等于")) {
                flag = "0";
            }else if (flag.equalsIgnoreCase("持平")) {
                flag = "0";
            } else {
                LOG.error("occur error, flag is: [  " + flag + " ]");
            }
        }
        return flag;
    }



    public static void main(String[] args) {
        UpdateFieldByMySql updateFieldByMySql = App.getAppContext().getBean(UpdateFieldByMySql.class);
        //updateFieldByMySql.updateEsFiled();

        //updateFieldByMySql.updateIndexShopRatingByMySql();

//        //将es中goods_mysql中所有status为true的数据写入到good_coupon中
//        updateFieldByMySql.insertGoodsMysqlForGoodsCoupon();
//
//        //将es中goods_excel中所有status为true的数据写入到good_coupon中
//        updateFieldByMySql.insertGoodsExcelForGoodsCoupon();

        //将es中index_shop_rating中个别字段进行转化
        //updateFieldByMySql.updateShopRatingFiledForEs();

        updateFieldByMySql.insertGoodsCouponForGoodsCoupon();

    }



    //将es中goods_excel中所有status为true的数据写入到good_coupon中
    public void insertGoodsCouponForGoodsCoupon(){
        //String typeName = "goods_excel";

        int count = 0;
        int cnt = 0;

        Client clients = initESClient();

        LOG.info("start to insert type:{}", "goods_coupon");

        SearchResponse search = clients.prepareSearch().setIndices("index_goods_coupon").setTypes("goods_coupon")
                .setSearchType(SearchType.DEFAULT)
                .setScroll(new TimeValue(60000))
                .setQuery(QueryBuilders.matchAllQuery())
                .setFrom(0).setSize(100000)
                .execute().actionGet();

        LOG.info("total hits num:{}", search.getHits().totalHits());
        long total = search.getHits().totalHits();
        System.out.println("-----------------------------------------------------total hits num:" + search.getHits().totalHits());
        for (SearchHit hit : search.getHits()) {
            cnt++;
            Map<String, Object> map = hit.getSource();
            Goods goods = new Goods();
            try {
                try {
                    goods.setId(Long.parseLong(map.get("id").toString()));
                }catch (NullPointerException n){}

                goods.setGoodid(new BigInteger(map.get("goodid").toString()));
                goods.setTitle(map.get("title").toString());
                goods.setImg(map.get("img").toString());
                goods.setPrice(Float.parseFloat(map.get("price").toString()));

                goods.setQuanprice(Float.parseFloat(map.get("quanprice").toString()));
                goods.setRestnum(Long.parseLong(map.get("restnum").toString()));
                goods.setDatestart(Long.parseLong(map.get("datestart").toString()));
                goods.setDateend(Long.parseLong(map.get("dateend").toString()));
                //goods.setQuanurl(map.get("quanurl").toString());
                //goods.setGoodurl(map.get("goodurl").toString());
                //goods.setNick(map.get("nick").toString());
                //goods.setSellerid(new BigInteger(map.get("sellerid").toString()));
                goods.setVolume(Long.parseLong(map.get("volume").toString()));
                //goods.setUsertype(Integer.parseInt(map.get("usertype").toString()));

                try {
                    goods.setQuanurl(map.get("quanurl").toString());
                }catch (NullPointerException n){}

                try {
                    goods.setGoodurl(map.get("goodurl").toString());
                }catch (NullPointerException n){}

                try {
                    goods.setUsertype(Integer.parseInt(map.get("usertype").toString()));
                }catch (NullPointerException n){}

                try {
                    goods.setNick(map.get("nick").toString());
                }catch (NullPointerException n){}

                try {
                    goods.setSellerid(new BigInteger(map.get("sellerid").toString()));
                }catch (NullPointerException n){}

                try {
                    goods.setProvcity(map.get("provcity").toString());
                }catch (NullPointerException n){}


                try {
                    goods.setQuanDesc(map.get("quanDesc").toString());
                }catch (NullPointerException n){}

                try {
                    goods.setTotalnum(Long.parseLong(map.get("totalnum").toString()));
                }catch (NullPointerException n){}

                try {
                    goods.setCouponId(map.get("couponId").toString());
                }catch (NullPointerException n){}

                try {
                    goods.setDiscountRatio(Float.parseFloat(map.get("discountRatio").toString()));
                }catch (NullPointerException n){}


                try {
                    goods.setSellerWang(map.get("sellerWang").toString());
                }catch (NullPointerException n){}

                try {
                    goods.setBrokerage(Float.parseFloat(map.get("brokerage").toString()));
                }catch (NullPointerException n){}

                try {
                    goods.setBrokerageRatio(Float.parseFloat(map.get("brokerageRatio").toString()));
                }catch (NullPointerException n){}

                try {
                    goods.setItemDetailUrl(map.get("itemDetailUrl").toString());
                }catch (NullPointerException n){}

                try {
                    goods.setPromoteurl(map.get("promoteurl").toString());
                }catch (NullPointerException n){}


                try {
                    goods.setDetailImgUrls(map.get("detailImgUrls").toString());
                } catch (NullPointerException n){
                }

                try {
                    goods.setSmallurl(map.get("smallurl").toString());
                } catch (NullPointerException n){
                }

                long status = 0;
                try {
                    if (Boolean.valueOf(map.get("status").toString())) {
                        status = 1;
                    } else {
                        status = 0;
                    }
                } catch (NullPointerException n) {
                }
                goods.setStatus(status);

//                try {
//                    goods.setCategory(map.get("itemCatId").toString());
//                }catch (NullPointerException n){}

                try {
                    goods.setTopCategory(map.get("topCategory").toString());
                }catch (NullPointerException n){}

                try {
                    goods.setCategory(map.get("category").toString());
                } catch (NullPointerException n){
                }

                try {
                    goods.setDetailCategory(map.get("detailCategory").toString());
                }catch (NullPointerException n){}

                goods.setCtime(Long.parseLong(map.get("ctime").toString()));

                try {
                    goods.setScore(Double.parseDouble(map.get("score").toString()));
                } catch (NullPointerException n){
                }

                try {
                    goods.setLabel(Double.parseDouble(map.get("label").toString()));
                }catch (NullPointerException n){}

                try{
                    if (!elasticService.upsert(goods, "goods_coupon", false)) {
                        LOG.error("insert failed with goods id :{}", goods.getGoodid());
                    }else {
                        LOG.info("insert data succeed，goodid：[ " + goods.getGoodid() + "]，processing：[ " + count++ + " ]，deal：[ " + cnt + " ]， total：[ " + total + " ]");
                    }
                }catch (Exception e) {
                    LOG.error("goods error with goodsid :{}",goods.getGoodid());
                    LOG.error(e.getMessage(), e);
                }
            }catch (Exception e){
                LOG.error("error with goodid :{}, cause: {}",goods.getGoodid(), e.getMessage());
            }
        }

        LOG.info("end to insert data, total insert data: {}", count);
    }
}
