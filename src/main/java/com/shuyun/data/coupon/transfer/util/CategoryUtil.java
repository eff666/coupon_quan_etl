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

import java.util.Arrays;
import java.util.List;

public class CategoryUtil {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private static int httpclientReBuildNumber = 0;

    private List<String> womenCategory = Arrays.asList("女装/女士精品", "女装", "女士精品");
    private List<String> manCategory = Arrays.asList("男装");
    private List<String> xieBaoCategory = Arrays.asList("箱包皮具/热销女包/男包", "箱包皮具", "热销女包", "男包", "女包", "流行男鞋", "女鞋", "运动包/户外包/配件", "运动包", "户外包");
    private List<String> neiYiCategory = Arrays.asList("女士内衣/男士内衣/家居服", "女士内衣", "男士内衣", "家居服");
    private List<String> meiZhuangCategory = Arrays.asList("彩妆/香水/美妆工具", "美容护肤/美体/精油", "美发护发/假发", "彩妆", "香水", "美妆工具", "美容护肤", "假发");
    private List<String> jiaJuCategory = Arrays.asList("床上用品", "住宅家具", "家居饰品", "家装主材", "居家布艺", "基础建材", "电子/电工", "五金/工具", " 家具", "家饰", "家纺");
    private List<String> jiaDianCategory = Arrays.asList("影音电器", "电子词典/电纸书/文化用品", "个人护理/保健/按摩器材", "厨房电器", "办公设备/耗材/相关服务", "电子词典/电纸书/文化用品", "生活电器", "大家电");
    private List<String> baiHuoCategory = Arrays.asList("居家日用", "餐饮具", "洗护清洁剂/卫生巾/纸/香薰", "家庭/个人清洁工具", "收纳整理", "厨房/烹饪用具", "节庆用品/礼品");
    private List<String> tongZhuangMuYingCategory = Arrays.asList("童鞋/婴儿鞋/亲子鞋", "孕妇装/孕产妇用品/营养", "玩具/童车/益智/积木/模型", "尿片/洗护/喂哺/推车床", "童装/婴儿装/亲子装");
    private List<String> liuXingCategory = Arrays.asList("服饰配件/皮带/帽子/围巾", "饰品/流行首饰/时尚饰品新", "ZIPPO/瑞士军刀/眼镜", "手表");
    private List<String> shiPinCategory = Arrays.asList("零食/坚果/特产", "粮油米面/南北干货/调味品", "水产肉类/新鲜蔬果/熟食", "咖啡/麦片/冲饮", "酒类", "传统滋补营养品", "奶粉/辅食/营养品/零食", "保健食品/膳食营养补充食品");
    private List<String> shuMaCategory = Arrays.asList("3C数码配件", "电脑硬件/显示器/电脑周边", "DIY电脑", "数码相机/单反相机/摄像机", "手机", "电玩/配件/游戏/攻略", "平板电脑/MID", "笔记本电脑", "网络设备/网络相关");
    private List<String> sportsCategory = Arrays.asList("运动/瑜伽/健身/球迷用品", "自行车/骑行装备/零配件", "运动服/休闲服装", "运动鞋new", "户外/登山/野营/旅行用品");

    public CategoryUtil(){}

    //得到商品一级category
    public String getGoodsCategory(String category){
        String goodsCategory = "";
        if(category != null && !"".equals(category)) {
            if(category.indexOf(">") > -1){
                String[] splitCategory = category.split(">");
                goodsCategory = splitCategory[0].trim();
            } else {
                goodsCategory = category.trim();
            }
        }
        return goodsCategory;
    }

    //为mysql数据
    public String getGoodsMysqlGeneralCategory(String category){
        String goodsCategory = "";
        if(category != null && !"".equals(category)) {
            if(category.indexOf(">") > -1){
                String[] splitCategory = category.split(">");
                goodsCategory = getGoodsExcelGeneralCategory(splitCategory[0].trim());
            } else {
                goodsCategory = getGoodsExcelGeneralCategory(category.trim());
            }
        }
        return goodsCategory;
    }

    //为excel数据
    public String getGoodsExcelGeneralCategory(String category){
        String goodsCategory = "";
        if(category != null && !"".equals(category)) {
            if (womenCategory.contains(category)) {
                goodsCategory = "女装";
            } else if (manCategory.contains(category)) {
                goodsCategory = "男装";
            } else if (xieBaoCategory.contains(category)) {
                goodsCategory = "鞋包";
            } else if (neiYiCategory.contains(category)) {
                goodsCategory = "内衣";
            } else if (meiZhuangCategory.contains(category)) {
                goodsCategory = "美妆";
            } else if (jiaJuCategory.contains(category)) {
                goodsCategory = "家居";
            } else if (jiaDianCategory.contains(category)) {
                goodsCategory = "家电";
            } else if (baiHuoCategory.contains(category)) {
                goodsCategory = "百货";
            } else if (tongZhuangMuYingCategory.contains(category)) {
                goodsCategory = "童装母婴";
            } else if (liuXingCategory.contains(category)) {
                goodsCategory = "流行配饰";
            } else if (shiPinCategory.contains(category)) {
                goodsCategory = "食品";
            } else if (shuMaCategory.contains(category)) {
                goodsCategory = "数码";
            } else if (sportsCategory.contains(category)) {
                goodsCategory = "运动";
            } else {
                LOG.debug("其他category:" + category);
                goodsCategory = "其他尖货";
            }
        }
        return goodsCategory;
    }

    /**
     * 根据url地址, 获取html文本
     * <p>
     * 如果对方服务器拒绝服务, 需考虑重试机制
     *
     * @param url
     * @return
     */
    public String getHtmlContent(String url) {
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
                LOG.info(">>>>>> 可能由于抓取速度过快, 对方服务器拒绝服务, 第 " + httpclientReBuildNumber + " 次重试中...");
            } else {
                LOG.info(">>>>>> 重试 10 次仍失败, 等待下一个执行周期到来 !!!");
            }
        }
        return htmlContent;
    }
}
