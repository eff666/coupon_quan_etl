package com.shuyun.data.coupon.transfer.jobs;


import com.google.common.base.Strings;
import com.shuyun.data.coupon.transfer.cache.RedisCache;
import com.shuyun.data.coupon.transfer.dao.IGoodsDao;
import com.shuyun.data.coupon.transfer.pojo.Goods;
import com.shuyun.data.coupon.transfer.service.IElasticService;
import com.shuyun.data.coupon.transfer.util.CategoryUtil;
import com.shuyun.data.coupon.transfer.util.HttpRequest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Component
public class MysqlToESJob implements Runnable {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private static final String CACHE_KEY = "transfer|lastCtime";
    @Value("${limit}")
    private static final Integer LIMIT = 500;
    @Value("${sleepTime}")
    private static final Long SLEEP_TIME = 30000l;


    @Autowired
    private IGoodsDao goodsDao;
    @Autowired
    private RedisCache cache;
    @Autowired
    private IElasticService elasticService;

    private CategoryUtil categoryUtil = new CategoryUtil();

    @Override
    public void run(){
        {
            while (true) {
                try {
                    //从缓存中读取上次读取的最大ID
                    String cacheValue = cache.getCache(CACHE_KEY, String.class);
                    Long lastCtime;
                    if (cacheValue == null) {
                        LOG.info("get cache with key:{} value is null", CACHE_KEY);
                        lastCtime = 0l;
                    } else {
                        lastCtime = Long.parseLong(cacheValue);
                        LOG.info("get cache with key:{},with value :{}", CACHE_KEY, lastCtime);
                    }

                    //从mysql中读取对应的数据
                    List<Goods> goodsList = goodsDao.getCtimeBatch(lastCtime, LIMIT);

                    if(goodsList.size() != 0){
                        long maxCtime = 0l;
                        for(Goods goods : goodsList){
                            if(goods.getCtime() > maxCtime){
                                maxCtime = goods.getCtime();

                            }
                            if(goods.getCtime() > lastCtime){
                                lastCtime = goods.getCtime();
                            }
                        }
                        List<Goods> goodsList2 = goodsDao.getGoodsByCtime(maxCtime);
                        Set<Goods> set = new HashSet<>();
                        set.addAll(goodsList);
                        set.addAll(goodsList2);
                        for(Goods goods : set){

                            getAbsentGoodsFields(goods);

                            try{
                                if (!elasticService.upsert(goods, "goods_coupon", false)) {
                                    LOG.error("upert failed with goods id :{}", goods.getGoodid());
                                }
                            }catch (Exception e) {
                                LOG.error("goods error with goodsid :{}",goods.getGoodid());
                                LOG.error(e.getMessage(), e);
                            }
                        }
                    }
                    if(cache.setCache(CACHE_KEY, lastCtime.toString())){
                        LOG.info("put cache with key:{} with value :{}", CACHE_KEY, lastCtime);
                    }
                    //如果当前不能读到最大limit数据，sleep 10s
                    if (goodsList.size() < LIMIT) {
                        try {
                            Thread.currentThread().sleep(SLEEP_TIME);
                        } catch (InterruptedException e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }

    //为mysql中导入es的数据缺失字段
    public void getAbsentGoodsFields(Goods goods){
        try{
            //得到discountRatio字段，1个字段
            goods.fillAll();
        }catch (Exception e) {
            LOG.error("goods error with goodsid :{}",goods.getGoodid());
            LOG.error(e.getMessage(), e);
            goods.setStatus(0);
        }

        //根据category更新topCategory和detailCategory字段，2个字段
        try {
            String category = goods.getCategory();
            goods.setDetailCategory(category);
            if (category != null && !"".equals(category.trim())){
                goods.setTopCategory(categoryUtil.getGoodsMysqlGeneralCategory(category));
            }
        }catch (NullPointerException nu){
        }

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
        //return goods;
    }


//    public void run2() {
//        while (true) {
//            try {
//                //从缓存中读取上次读取的最大ID
//                String cacheValue = cache.getCache(CACHE_KEY, String.class);
//                Long lastId;
//                if (cacheValue == null) {
//                    LOG.info("get cache with key:{} value is null", CACHE_KEY);
//                    lastId = 0l;
//                } else {
//                    lastId = Long.parseLong(cacheValue);
//                    LOG.info("get cache with key:{},with value :{}", CACHE_KEY, lastId);
//                }
//
//                //从mysql中读取对应的数据
//                List<Goods> goodsList = goodsDao.getIdBatch(lastId, LIMIT);
//                if(goodsList.size() != 0){
//                    for (Goods goods : goodsList) {
//                        try {
//                            //更新last id
//                            if (goods.getId() > lastId) {
//                                lastId = goods.getId();
//                            }
//                            //把数据写入elasticsearch
//                            if (!elasticService.upsert(goods, "goods")) {
//                                LOG.error("upert failed with goods id :{}", goods.getGoodid());
//                            }
//                        } catch (Exception e) {
//                            LOG.error(e.getMessage(), e);
//                        }
//                    }
//
//                    //把读取过的数据中最大的id写入 redis
//                    if(cache.setCache(CACHE_KEY, lastId.toString())){
//                        LOG.info("put cache with key:{} with value :{}", CACHE_KEY, lastId);
//                    }
//                }
//
//
//                //如果当前不能读到最大limit数据，sleep 10s
//                if (goodsList.size() < LIMIT) {
//                    try {
//                        Thread.currentThread().sleep(SLEEP_TIME);
//                    } catch (InterruptedException e) {
//                        LOG.error(e.getMessage(), e);
//                    }
//                }
//            } catch (Exception e) {
//                LOG.error(e.getMessage(), e);
//            }
//        }
//    }
}
