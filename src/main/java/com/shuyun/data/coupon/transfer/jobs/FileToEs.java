package com.shuyun.data.coupon.transfer.jobs;




import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;


public class FileToEs {
    private static final Logger log = LoggerFactory.getLogger(FileToEs.class);

    static ConcurrentLinkedQueue<String> queues = new ConcurrentLinkedQueue<String>();
    static AtomicBoolean isInsert = new AtomicBoolean(true);
    static Integer count = 0;

    private static Client client;

    public static void main(String[] agrs) throws Exception{
        log.info("现在时间是:" + Calendar.getInstance().getTime());
        try {
            // set cluster name
            Settings settings = Settings.settingsBuilder().put("cluster.name", "elasticsearch-cluster").build();
            // add transport addresses
            TransportClient transportClient = TransportClient.builder().settings(settings).build();
            String hosts = "10.10.138.183";
            for (String ip : hosts.split(";")) {
                transportClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(ip), 9500));
            }
            client = transportClient;
        }catch (Exception e){

        }

        final long aa = System.currentTimeMillis();

        final ConcurrentHashMap<String, Boolean> hashMap = new ConcurrentHashMap();
        for(int i = 0; i < 5; i++) {
            new Thread(new Runnable() {

                public void run() {
                    //Add transport addresses and do something with the client...
                    hashMap.put(Thread.currentThread().getName(), Boolean.FALSE);
                    BulkProcessor bulkProcessor = BulkProcessor.builder(
                            client,
                            new BulkProcessor.Listener() {

                                public void afterBulk(long l, BulkRequest bulkRequest, BulkResponse bulkResponse) {
                                    if(bulkResponse.hasFailures()) log.error("fail.11......");
                                }

                                public void beforeBulk(long executionId,
                                                       BulkRequest request) {
                                }

                                public void afterBulk(long executionId,
                                                      BulkRequest request,
                                                      Throwable failure) {
                                    log.error("fail..22..........");
                                }
                            })
                            .setBulkActions(5000)
                            .setBulkSize(new ByteSizeValue(5, ByteSizeUnit.MB))
                            .setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueMillis(100), 3))
                            .setConcurrentRequests(1)
                            .build();
                    while (true) {
                        if (!queues.isEmpty()) {
                            try {
                                String json = queues.poll();
                                if (json == null) continue;
                               // System.out.println(json);
                                String buyNick = "";
                                try {
                                    int index5 = json.indexOf("goodid");
                                    int index6 = json.indexOf(",", index5);
                                    index5 += 8;
                                    buyNick = json.substring(index5, index6);
                                } catch (NullPointerException nu){

                                }
                                System.out.println(buyNick);
                                System.out.println(json);
                                bulkProcessor.add(new IndexRequest("coupon", "goods_mysql").id(buyNick).source(json));

                            } catch (Exception e) {
                                log.error(e.getMessage());
                            }
                        }
                        if (queues.isEmpty() && !isInsert.get()) {
                            bulkProcessor.flush();
                            long jjj = System.currentTimeMillis() - aa;
                            log.info("   " + Thread.currentThread().getName() + ":" + jjj + "   ");
                            hashMap.put(Thread.currentThread().getName(), Boolean.TRUE);
                            while(hashMap.values().contains(Boolean.FALSE)){
                                try {
                                    Thread.currentThread().sleep(1 * 1000);
                                } catch(Exception e){
                                    e.printStackTrace(System.out);
                                }
                            }
                            bulkProcessor.close();
                            break;
                        }
                    }
                }
            }).start();
        }

        System.out.println("主线程for循环执行完毕..");

        //File file = new File("/data2/xiuyuan/file/shuyun_id-2016-11-25");
        File file = new File("F:\\datafortag\\maimaimai\\goods_mysql-2016-11-30");
        FileInputStream fileInputStream = new FileInputStream(file);
        InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String json = null;
        Integer index = 0;
        while((json = bufferedReader.readLine()) != null){
            queues.add(json);
            index++;
            if(index % 20000 == 0){
                int number = queues.size();
                int jj = number/20000;
                log.info("index: " + index + ", jj: " + jj + ", number: " + number);
                while(jj > 0){
                    Thread.sleep(1000*jj);
                    int number2 = queues.size();
                    jj = number2/20000;
                    log.info("index: " + index + ", jj: " + jj + ", number2: " + number2);
                }
            }
        }
        isInsert = new AtomicBoolean(false);
        bufferedReader.close();
        inputStreamReader.close();
        fileInputStream.close();
        log.info("现在时间是:" + Calendar.getInstance().getTime());
        //System.out.println("未加密数据：" + count);
        log.info("共有数据：" + index);
    }

}

