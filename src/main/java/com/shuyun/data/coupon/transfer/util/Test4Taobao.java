package com.shuyun.data.coupon.transfer.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import javax.swing.*;

public class Test4Taobao {

    public static void main(String[] args) throws ClientProtocolException, IOException {
        HttpClient httpclient = new DefaultHttpClient();
        //String url="http://detail.tmall.com/item.htm?spm=a230r.1.14.44.RoTYht&id=520945622472&ns=1&abbucket=20";
        //String url="https://detail.tmall.com/item.htm?id=520945622472";
        String url="http://hws.m.taobao.com/cache/wdetail/5.0/?id=540459138947";

        //String id=getStrByPrePost(url, "id=", "&");//根据url获取商品的id
        //if(id!=null){
            //组装出获取商品信息的url
           // String detailUrl="http://mdskip.taobao.com/core/initItemDetail.htm?itemId="+id+"×tamp=1429065419751&tgTag=false&cartEnable=true&addressLevel=2&progressiveSupport=false&isUseInventoryCenter=false&household=false¬AllowOriginPrice=false&isRegionLevel=false&sellerUserTag=303632416&sellerUserTag2=18020085046181888&tmallBuySupport=true&isAreaSell=false&queryMemberRight=true&isIFC=false&sellerUserTag3=70368779862144&sellerUserTag4=8800522208643&service3C=true&tryBeforeBuy=false&isSecKill=false&isForbidBuyItem=false&sellerPreview=false&itemTags=843,1163,1478,1483,1547,1611,1803,1867,2049,2059,2443,2507,2635,3787,3974,4166,4555,4811,5323,21762,21826,25282,28802,29122,34178,56130,56194&isApparel=false&offlineShop=false&showShopProm=false&callback=setMdskip";
            HttpGet get = new HttpGet(url);
            get.addHeader("Referer", url);//Referer必须要设置
            HttpResponse response = httpclient.execute(get);
            String content =  getContent(response);
//            content=getStrByPrePost(content, "setMdskip(", ")");
//            //返回的内容是json格式，可以通过gson等来解析，为了方便，此处直接截取。
//            System.out.println("商品信息:"+content);
//            String promotionPrice = getStrByPrePost(content, "promotionList", null);
//            promotionPrice = getStrByPrePost(promotionPrice, "\"price\":", ",");
            System.out.println("商品价格:"+content);
        //}

        BigInteger a = null;



    }

    /**
     * 获取返回内容的string
     * @param response
     * @return
     * @throws IllegalStateException
     * @throws IOException
     */
    private static String getContent(HttpResponse response) throws IllegalStateException, IOException {
        java.io.InputStream is = response.getEntity().getContent();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "GBK"));//response.getEntity().getContentEncoding().getName()
        String line = "";
        String temp= null;
        while ((temp = br.readLine()) != null) {
            line+=temp;
        }
        return line;

    }

    /**
     * 根据前后的文本截取指定的内容。
     * @param str
     * @param pre
     * @param post
     * @return
     */
    public static String getStrByPrePost(String str,String pre,String post){
        if(str!=null){
            if(pre!=null){
                int s = str.indexOf(pre);
                if(s>-1){
                    str = str.substring(s+pre.length(), str.length());
                }else{
                    return null;
                }

            }
            if(post!=null){
                int e =  str.indexOf(post);
                if(e>-1){
                    str = str.substring(0,e);
                }else{
                    return null;
                }

            }
        }

        return str;
    }
}
