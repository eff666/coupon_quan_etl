package com.shuyun.data.coupon.transfer.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shuyun.data.coupon.transfer.dao.IGoodsDao;
import com.shuyun.data.coupon.transfer.pojo.Goods;
import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.TbkItemInfoGetRequest;
import com.taobao.api.response.TbkItemInfoGetResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigInteger;

public class TestSmallImage {

    @Value("${limit}")
    private static final Integer LIMIT = 200;
    @Value("${sleepTime}")
    private static final Long SLEEP_TIME = 30000l;


    @Autowired
    private static IGoodsDao goodsDao;

    private static final Logger LOG = LoggerFactory.getLogger(TestSmallImage.class);

    public static void main(String[] args) {


        Goods goods = new Goods();
        String url = "http://gw.api.taobao.com/router/rest";
        String appkey = "23410415";
        String secret = "11df08dfa118953e8e59d774d4873a19";

        TaobaoClient client = new DefaultTaobaoClient(url, appkey, secret);
        TbkItemInfoGetRequest req = new TbkItemInfoGetRequest();
        req.setFields("item_url,num_iid,user_type,provcity,nick,seller_id,volume,small_images,category,itemCatId");
        req.setPlatform(1L);
        req.setNumIids("537341933444");
        TbkItemInfoGetResponse rsp = null;
        try {
            rsp = client.execute(req);
        } catch (ApiException e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.info(rsp.getBody());

        JSONObject json = (JSONObject) JSON.parse(rsp.getBody());
        JSONObject response = (JSONObject)json.get("tbk_item_info_get_response");
        JSONObject results = (JSONObject)response.get("results");
        JSONArray jSONArray = (JSONArray)results.get("n_tbk_item");
        JSONObject n_tbk_item = jSONArray.getJSONObject(0);
        if(n_tbk_item != null) {
            goods.setNick(n_tbk_item.getString("nick"));
            goods.setProvcity(n_tbk_item.getString("provcity"));
            goods.setUsertype(Integer.parseInt(n_tbk_item.getString("user_type")));
            goods.setSellerid(new BigInteger(n_tbk_item.getString("seller_id")));
            goods.setVolume(Long.parseLong(n_tbk_item.getString("volume")));
            goods.setSmallurl(n_tbk_item.getString("small_images"));
            System.out.println(n_tbk_item.getString("provcity"));
            System.out.println(n_tbk_item.getString("small_images"));
            System.out.println(JSON.parse(n_tbk_item.getString("small_images")));
            //goodsDao.updateAbsentFields(goods);
            LOG.info("succed to update with id {}", goods.getId());
        }




//        String url = "http://gw.api.taobao.com/router/rest";
//        String appkey = "23410415";
//        String secret = "11df08dfa118953e8e59d774d4873a19";
//        long id =0;
//
//        while(true){
//
//            try {
//
//
//                List<Goods> goodsList = goodsDao.getAbsentGoods(id);
//                if(goodsList.size() != 0){
//                    for (Goods goods : goodsList) {
//                        if(goods.getId()> id){
//                            id = goods.getId();
//                        }
//                        try {
//
//                            TaobaoClient client = new DefaultTaobaoClient(url, appkey, secret);
//                            TbkItemInfoGetRequest req = new TbkItemInfoGetRequest();
//                            req.setFields("item_url,num_iid,user_type,provcity,nick,seller_id,volume,small_images, category");
//                            req.setPlatform(1L);
//                            req.setNumIids(goods.getGoodid().toString());
//                            TbkItemInfoGetResponse rsp = null;
//                            try {
//                                rsp = client.execute(req);
//                            } catch (ApiException e) {
//                                e.printStackTrace();
//                            }
//                            System.out.println(rsp.getBody());
//
//                            JSONObject json = (JSONObject) JSON.parse(rsp.getBody());
//                            JSONObject response = (JSONObject)json.get("tbk_item_info_get_response");
//                            JSONObject results = (JSONObject)response.get("results");
//                            JSONArray jSONArray = (JSONArray)results.get("n_tbk_item");
//                            JSONObject n_tbk_item = jSONArray.getJSONObject(0);
//                            if(n_tbk_item != null){
//                                goods.setNick(n_tbk_item.getString("nick"));
//                                goods.setProvcity(n_tbk_item.getString("provcity"));
//                                goods.setUsertype(Integer.parseInt(n_tbk_item.getString("user_type")));
//                                goods.setSellerid(new BigInteger(n_tbk_item.getString("seller_id")));
//                                goods.setVolume(Long.parseLong(n_tbk_item.getString("volume")));
//                                goods.setSmallurl(n_tbk_item.getString("small_images"));
//                                System.out.println(n_tbk_item.getString("small_images"));
//                                //goodsDao.updateAbsentFields(goods);
//                                LOG.info("succed to update with id {}", goods.getId());
//                            }else{
//                                LOG.warn("n_tbk_item is null");
//
//                            }
//
//
//
//
//                        } catch (Exception e) {
//                            LOG.error(e.getMessage(), e);
//                        }
//                    }
//
//
//                }
//                if(goodsList.size() != 200){
//                    break;
//                }
//
//            } catch (Exception e) {
//                LOG.error(e.getMessage(), e);
//            }
//        }

    }
}
