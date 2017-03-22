package com.shuyun.data.coupon.transfer.jobs;

import com.shuyun.data.coupon.transfer.App;
import com.shuyun.data.coupon.transfer.dao.IGoodsDao;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.InetAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class EsToMySql {

    @Value("${cluster.name}")
    private String clusterName;
    @Value("${cluster.hosts}")
    private String hosts;
    @Value("${cluster.port}")
    private int port;
    @Value("${goods.indexName}")
    private String indexName;

    @Autowired
    private IGoodsDao goodsDao;

    private static Client client;

    private final Logger log = LoggerFactory.getLogger(EsToMySql.class);

    List<String> listColumn = Arrays.asList("goodid", "title", "img", "itemDetailUrl", "itemCatId", "goodurl", "price", "volume", "brokerageRatio", "brokerage",
            "sellerWang", "sellerid", "nick", "usertype", "couponId", "totalnum", "restnum", "quanDesc", "datestart", "dateend", "quanurl", "promoteurl");

     ConcurrentLinkedQueue<String> queues = new ConcurrentLinkedQueue<>();
     ConcurrentLinkedQueue<SearchHit[]> queuesforconverse = new ConcurrentLinkedQueue<>();
     AtomicBoolean isInsert = new AtomicBoolean(true);

//    @PostConstruct
//    public void initESClient() {
//        try {
//            // set cluster name
//            Settings settings = Settings.settingsBuilder().put("cluster.name", clusterName).build();
//            // add transport addresses
//            TransportClient transportClient = TransportClient.builder().settings(settings).build();
//            for (String ip : hosts.split(";")) {
//                transportClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(ip), port));
//            }
//            client = transportClient;
//        }catch (Exception e){
//
//        }
//    }

        static  {
            try {
                // set cluster name
                Settings settings = Settings.settingsBuilder().put("cluster.name", "elasticsearch-cluster").build();
                // add transport addresses
                TransportClient transportClient = TransportClient.builder().settings(settings).build();
                //for (String ip : hosts.split(";")) {
                    transportClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("10.10.138.183"), 9500));
                //}
                client = transportClient;
            }catch (Exception e){

            }
        }

    //数据迁移
    //将es1.7中数据迁移到2.3.3
    public void getEsGoodsForFile(){
        log.info("开始时间: " + Calendar.getInstance().getTime());
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        //File file = new File("F:\\datafortag\\maimaimai\\shuyun_id-2016-11-25");
        //String txtFilePath = "/data2/xiuyuan/file/taobao_v3-item" + today;
        String txtFilePath = "F:\\datafortag\\maimaimai\\goods_excel-" + today;
        File txtFile = null;
        FileOutputStream fileOutputStream = null;
        OutputStreamWriter outputStream = null;
        BufferedWriter bufferedWriter = null;
        try {
            txtFile = new File(txtFilePath);
            if (!txtFile.exists()) {
                txtFile.createNewFile();
            }
            fileOutputStream = new FileOutputStream(txtFile);
            outputStream = new OutputStreamWriter(fileOutputStream);
            bufferedWriter = new BufferedWriter(outputStream);
            for(int i = 0; i < 1; i++) {
                final BufferedWriter finalBufferedWriter = bufferedWriter;
                new Thread(new Runnable() {
                    public void run() {
                        Integer count = 0;
                        while (true) {
                            if (!queuesforconverse.isEmpty()) {
                                SearchHit[] hits = queuesforconverse.poll();
                                if(hits == null){
                                    continue;
                                }

                                for(SearchHit hit : hits){
                                    count++;
                                    String source = hit.getSourceAsString();
                                    try {
                                        finalBufferedWriter.write(source);
                                        finalBufferedWriter.newLine();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                if(count % 1000 == 0){
                                   log.info("写入数据到: " + count);
                                }
                            }else if(!isInsert.get()) {
                                System.out.println(Thread.currentThread() + "converse over");
                                try {
                                    finalBufferedWriter.flush();
                                    //finalBufferedWriter.close();
                                } catch (IOException e) {
                                    log.error(e.getMessage());
                                }
                                break;
                            }
                        }
                    }
                }).start();
            }


            SearchResponse scrollResp = client.prepareSearch()
                    .setSearchType(SearchType.SCAN)
                    .setScroll(new TimeValue(60000))
                    .setIndices("coupon")
                    .setTypes("goods_excel")
                    .setSize(400)
                    .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("status", true)))
                    .execute().actionGet();

            int index = 0;
            long timeforlive = 80000;
            TimeValue timeValue = new TimeValue(timeforlive);
            while (true) {
                queuesforconverse.add(scrollResp.getHits().getHits());
                scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(timeValue).execute().actionGet();

                index++;
                if(index == 2){
                    timeforlive = 80000;
                    timeValue = new TimeValue(timeforlive);
                    int number = queues.size() + queuesforconverse.size() * 400 * 36;
                    int sleepTime = 0;
                    int jj = number/80000;
                    while(jj > 0){
                        if(sleepTime > 60 ){
                            timeValue = new TimeValue(timeforlive + 20000);
                            break;
                        }
                        Thread.sleep(1000);
                        sleepTime++;
                        int number2 = queues.size() + queuesforconverse.size() * 400 * 36;
                        jj = number2/80000;
                    }
                    index = 0;
                }
                if (scrollResp.getHits().getHits().length == 0) {
                    isInsert = new AtomicBoolean(false);
                    break;
                }
            }

            //client.close();

        }catch (Exception e){
            log.error(e.getMessage());
        } finally {
            try {
                bufferedWriter.close();
                outputStream.close();
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
           // client.close();
        }
        log.info("结束时间: " + Calendar.getInstance().getTime());
    }


    //从es中得到状态为true的数据写入到文件中
    public  void getGoodFromEs(){
        //String today = "2016-11-20";
        //String txtFilePath = "F:\\datafortag\\maimaimai\\quan-" + today;
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String txtFilePath = "F:\\datafortag\\maimaimai\\quan-" + today;
        File txtFile = null;
        FileOutputStream fileOutputStream = null;
        OutputStreamWriter outputStream = null;
        BufferedWriter bufferedWriter = null;
        try {
            txtFile = new File(txtFilePath);
            if (!txtFile.exists()) {
                txtFile.createNewFile();
            }
            fileOutputStream = new FileOutputStream(txtFile);
            outputStream = new OutputStreamWriter(fileOutputStream);
            bufferedWriter = new BufferedWriter(outputStream);

            SearchResponse search = client.prepareSearch().setIndices(indexName).setTypes("goods_excel").setQuery(
                    QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("status", true)))
                    .setFrom(0).setSize(50000).execute().actionGet();

            int count = 1;
            for (SearchHit hit : search.getHits()) {
                Map<String, Object> map = hit.getSource();
                if(map != null && map.size() > 0) {
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
                            if (lastColumn < 22) {
                                bufferedWriter.write("\u0001");
                            }
                        } else {
                            bufferedWriter.write("\u0001");
                            if (lastColumn < 22) {
                                bufferedWriter.write("\u0001");
                            }
                        }
                    }
                }
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
            System.out.println("共写入数据: " + count);
        }catch (Exception e){
            log.error(e.getMessage());
        } finally {
            try {
                bufferedWriter.close();
                outputStream.close();
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //从es中coupon的type为good_excel中得到所有卖家的昵称和旺旺名写入到mysql中
    public void getSellerToMySql(){//.setFrom(0).setSize(80000)

        SearchResponse searchResponse = client.prepareSearch().setScroll(new TimeValue(60000)).setIndices("coupon").setTypes("goods_excel")
                .setQuery(QueryBuilders.matchAllQuery())
                .setFrom(0).setSize(80000)
                .execute().actionGet();
        int count = 0;
        System.out.println("es共有数据:" + searchResponse.getHits().getTotalHits());
        log.info("es共有数据:" + searchResponse.getHits().getTotalHits());
        try {
            //while (true) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String strDate = simpleDateFormat.format(new Date());
                Date date = null;
                try {
                    date = simpleDateFormat.parse(strDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                for (SearchHit hit : searchResponse.getHits().getHits()) {
                    Map<String, Object> map = hit.getSource();
                    String sellerNick = null;
                    String sellerWang = null;
                    try {
                        sellerNick = map.get("nick").toString();
                    } catch (NullPointerException nu) {
                    }

                    try {
                        sellerWang = map.get("sellerWang").toString();
                    } catch (NullPointerException nu) {
                    }

                    if (sellerNick != null || sellerWang != null) {
                        goodsDao.insertSellerToMysql(sellerNick, sellerWang, date);
                        //log.info("插入数据到:" + count++);
                        count++;
                        if(count % 200 == 0){
                            log.info("插入到:" + count);
                        }
                    }
                }

                //if (searchResponse.getHits().getHits().length == 0) {
                    log.info("插入数据结束，共插入" + count);
            log.info("插入数据结束，共插入:" + count);
                   // break;
               // }
            //}
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args){
        EsToMySql esToMySql = App.getAppContext().getBean(EsToMySql.class);
        try {
            esToMySql.getSellerToMySql();
        }catch (Exception e){

        }finally {

        }
        //得到根据quanurl更新过status的数据
        //esToMySql.getGoodFromEs();
        //esToMySql.getEsGoodsForFile();
    }
}
