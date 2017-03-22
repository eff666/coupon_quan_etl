package com.shuyun.data.coupon.transfer.jobs;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shuyun.data.coupon.transfer.App;
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
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.List;

@Component
public class FillAbsentFields implements Runnable {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Value("${limit}")
    private static final Integer LIMIT = 200;
    @Value("${sleepTime}")
    private static final Long SLEEP_TIME = 30000l;


    @Autowired
    private IGoodsDao goodsDao;

    @Override
    public void run() {
        String url = "http://gw.api.taobao.com/router/rest";
        String appkey = "23410415";
        String secret = "11df08dfa118953e8e59d774d4873a19";
        long id =0;
        int count = 0;
        while(true){

            try {
                List<Goods> goodsList = goodsDao.getAbsentGoods(id);
                if(goodsList.size() != 0){
                    for (Goods goods : goodsList) {
                        if(goods.getId() > id){
                            id = goods.getId();
                        }
                        try {
                            TaobaoClient client = new DefaultTaobaoClient(url, appkey, secret);
                            TbkItemInfoGetRequest req = new TbkItemInfoGetRequest();
                            req.setFields("item_url,num_iid,user_type,provcity,nick,seller_id,volume,small_images");
                            req.setPlatform(1L);
                            req.setNumIids(goods.getGoodid().toString());
                            TbkItemInfoGetResponse rsp = null;
                            try {
                                rsp = client.execute(req);
                            } catch (ApiException e) {
                                e.printStackTrace();
                            }
                            LOG.info(rsp.getBody());

                            if(rsp.getBody() != null) {
                                JSONObject json = (JSONObject) JSON.parse(rsp.getBody());
                                JSONObject response = (JSONObject) json.get("tbk_item_info_get_response");
                                JSONObject results = (JSONObject) response.get("results");
                                JSONArray jSONArray = (JSONArray) results.get("n_tbk_item");
                                if(jSONArray != null & jSONArray.size() > 0) {
                                    JSONObject n_tbk_item = jSONArray.getJSONObject(0);
                                    if (n_tbk_item != null) {
                                        goods.setNick(n_tbk_item.getString("nick"));
                                        goods.setProvcity(n_tbk_item.getString("provcity"));
                                        goods.setUsertype(Integer.parseInt(n_tbk_item.getString("user_type")));
                                        goods.setSellerid(new BigInteger(n_tbk_item.getString("seller_id")));
                                        goods.setVolume(Long.parseLong(n_tbk_item.getString("volume")));
                                        goods.setSmallurl(String.valueOf(n_tbk_item.getString("small_images")));
                                        //System.out.println(n_tbk_item.getString("small_images"));
                                        goodsDao.updateSmallUrl(id, String.valueOf(n_tbk_item.getString("small_images")));
                                        count++;
                                        LOG.info("success to update with id {}", goods.getId());
                                    } else {
                                        LOG.warn("n_tbk_item is null");

                                    }

                                }
                            }

                        } catch (Exception e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                }
                if(goodsList.size() != 200){
                    break;
                }

            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        LOG.info("total data: {}", count);
    }

    public void fillAbsentForExcel(Goods goods){
        String url = "http://gw.api.taobao.com/router/rest";
        String appkey = "23410415";
        String secret = "11df08dfa118953e8e59d774d4873a19";

        TaobaoClient client = new DefaultTaobaoClient(url, appkey, secret);
        TbkItemInfoGetRequest req = new TbkItemInfoGetRequest();
        req.setFields("item_url,num_iid,user_type,provcity,nick,seller_id,volume");
        req.setPlatform(1L);
        req.setNumIids(goods.getGoodid().toString());
        TbkItemInfoGetResponse rsp = null;
        try {
            rsp = client.execute(req);
        } catch (ApiException e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.info(rsp.getBody());

        try {
            JSONObject json = (JSONObject) JSON.parse(rsp.getBody());
            JSONObject response = (JSONObject) json.get("tbk_item_info_get_response");
            JSONObject results = (JSONObject) response.get("results");
            JSONArray jSONArray = (JSONArray) results.get("n_tbk_item");
            if(jSONArray == null){
                LOG.warn("jSONArray is null");
                return;
            }
            JSONObject n_tbk_item = jSONArray.getJSONObject(0);
            if(n_tbk_item!= null){
                goods.setProvcity(n_tbk_item.getString("provcity"));
                //goods.setUsertype(Integer.parseInt(n_tbk_item.getString("user_type")));
                //goods.setSellerid(new BigInteger(n_tbk_item.getString("seller_id")));
            }else{
                LOG.warn("n_tbk_item is null ");
            }

        }catch(Exception e){
            LOG.error(e.getMessage(), e);
        }


    }

    public static void main(String[] args) {
        Thread thread = new Thread(App.getAppContext().getBean(FillAbsentFields.class));
        thread.start();

    }
}
