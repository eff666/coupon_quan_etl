package com.shuyun.data.coupon.transfer.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class TestUtil {

    private static final Logger logger = LoggerFactory.getLogger(TestUtil.class);
    private static int httpclientReBuildNumber = 0;

    /**
     * 根据链接去得到返回的html，判断优惠券是否有效
     * @param urlName the request url
     * @return
     */
    public static String getStatusBySendGet(String urlName) {
        String result = "";
        BufferedReader in = null;
        try {
            //urlName = "https://shop.m.taobao.com/shop/coupon.htm?sellerId=696902416&activityId=4aee3e6538d94b518e19b53d3b04f30c";
            URL realUrl = new URL(urlName);
            // 打开和URL之间的连接
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection)realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/602.3.12 (KHTML, like Gecko) Version/10.0.2 Safari/602.3.12");
            connection.setRequestProperty("REFERER", urlName);
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);
            // 建立实际的连接
            connection.connect();

            // 定义 BufferedReader输入流来读取URL的响应
            //if(connection.getResponseCode() == 200) {
                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    result += line;
                }
//            } else {
//                logger.error("验证优惠券请求发生错误，请求是：[ " + urlName + " ]，返回状态码：" + connection.getResponseCode());
//            }
        } catch (Exception e) {
            logger.error("发送GET请求出现异常，请求时：[ " + urlName + " ]，异常原因：\n" + e.getMessage());
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }


    /**
     * 根据url地址, 获取html文本
     * <p>
     * 如果对方服务器拒绝服务, 需考虑重试机制
     *
     * @param url
     * @return
     */
    public static String getHtmlContent(String url) {
        //休眠若干时间, 解决抓取过快, 对方服务器拒绝响应的问题
        //SleepUtil.sleepRandomSecons(1, 1);
        String htmlContent = null;

        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(url);
            HttpResponse response = httpClient.execute(httpGet);
            int resp_status = response.getStatusLine().getStatusCode();

            if (resp_status == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    htmlContent = EntityUtils.toString(entity, "gbk");
                }
            }

            //If it's normal, reset counter to 0.
            httpclientReBuildNumber = 0;
        } catch (Exception e) {
            //If create httpclient object failure, retry 10 times.
            if (httpclientReBuildNumber <= 10) {
                getHtmlContent(url);
                httpclientReBuildNumber++;
                logger.info(">>>>>> 可能由于抓取速度过快, 对方服务器拒绝服务, 第 " + httpclientReBuildNumber + " 次重试中...");
            } else {
                logger.info(">>>>>> 重试 10 次仍失败, 等待下一个执行周期到来 !!!");
            }
        }
        return htmlContent;
    }

    public String grabMobilePost(String m)throws Exception{
        String strUrl = "https://detail.tmall.com/item.htm?id=520945622472";
        URL url = new URL(strUrl) ;
        HttpURLConnection httpUrlCon = (HttpURLConnection)url.openConnection() ;
        InputStreamReader inRead = new InputStreamReader(httpUrlCon.getInputStream(),"GBK") ;
        BufferedReader bufRead = new BufferedReader(inRead) ;
        StringBuffer strBuf = new StringBuffer() ;
        String line = "" ;
        while ((line = bufRead.readLine()) != null) {
            strBuf.append(line);
        }

        String strAll = strBuf.toString() ;


        String result = strAll;
        return result ;
    }


    public static void main(String[] args){
        String url = "https://detail.tmall.com/item.htm?id=520945622472";
        System.out.println(getStatusBySendGet(url));
        //System.out.println(getHtmlContent(url));
    }
}
