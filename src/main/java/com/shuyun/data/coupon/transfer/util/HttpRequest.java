package com.shuyun.data.coupon.transfer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class HttpRequest {

    private static final Logger logger = LoggerFactory.getLogger(HttpRequest.class);

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
            connection.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.57 Safari/536.11");
            connection.setRequestProperty("REFERER", "http://www.baidu.com");
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);
            // 建立实际的连接
            connection.connect();

            // 定义 BufferedReader输入流来读取URL的响应
            if(connection.getResponseCode() == 200) {
                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    result += line;
                }
            } else {
                logger.error("验证优惠券请求发生错误，请求是：[ " + urlName + " ]，返回状态码：" + connection.getResponseCode());
            }
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

}
