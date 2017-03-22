package com.shuyun.data.coupon.transfer.launcher;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.shuyun.data.coupon.transfer.App;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class FileToEsForCoupon{

    private static final Logger LOG = LoggerFactory.getLogger(FileToEsForCoupon.class);

    static ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd"))
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);

    public static final String GOODID = "goodid";
    public static final String LABEL = "label";
    public static final String SCORE = "score";

    public void readFileAndWriteEs() {
        //String today = "2017-01-12";
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String txtFilePath = "/home/elastic/maimaimai/elastic-hdfs/hdfs-download/file/score-" + today;
        while (true) {
            File file = new File(txtFilePath);
            if (file.exists()) {
                LOG.info("file is exist then will insert ES, file name : " + txtFilePath);
                break;
            } else {
                LOG.info("file is not exist then will sleep 30 minutes, file name : " + txtFilePath);
                try {
                    Thread.currentThread().sleep(1000 * 60 * 30);
                } catch (Exception e) {
                    LOG.error(e.getMessage());
                }
            }
        }

        LOG.info("现在时间是："+ Calendar.getInstance().getTime());

        Settings settings = Settings.settingsBuilder().put("client.transport.ping_timeout", "20s").put("cluster.name", "elasticsearch-cluster").build();
        final TransportClient client = TransportClient.builder().settings(settings).build();
        try {
            client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("10.10.138.183"), 9500));
        } catch (UnknownHostException error) {

        }

        int total = 0;
        int count = 0;
        File file = new File(txtFilePath);
        FileInputStream fileInputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileInputStream = new FileInputStream(file);
            inputStreamReader = new InputStreamReader(fileInputStream);
            bufferedReader = new BufferedReader(inputStreamReader);
            String str = null;
            while ((str = bufferedReader.readLine()) != null) {
                total++;
                String[] strs = str.split(",");
                if (strs.length != 3) {
                    continue;
                }

                Map map = new HashMap<String, String>();
                String goodId = strs[0];
                if(!Strings.isNullOrEmpty(goodId)) {
                    //map.put(GOODID, goodId);
                    map.put(SCORE, strs[1]);
                    map.put(LABEL, strs[2]);
                    if (map != null && map.size() > 0) {
                        GetResponse getResponse = client.prepareGet("index_goods_coupon", "goods_coupon", goodId).execute().actionGet();
                        if (getResponse == null) {
                            LOG.error("response is null with id :{}", goodId);
                        } else if (getResponse.isExists()) {
                            // update logic
                            try {
                                String json = objectMapper.writeValueAsString(map);
                                UpdateResponse updateResponse = client
                                        .prepareUpdate("index_goods_coupon", "goods_coupon", goodId)
                                        .setVersion(getResponse.getVersion())
                                        .setDoc(json)
                                        .execute()
                                        .actionGet();
                                if (updateResponse.getVersion() == getResponse.getVersion() + 1) {
                                    LOG.info("update success with goodid :{}, processing data: {}, total data: {}", goodId, count++, total);
                                }
                            } catch (Exception e) {
                                LOG.info("update failed with id:{}", goodId);
                                LOG.error(e.getMessage(), e);
                            }
                        } else {
                            LOG.error("not exist goodid:{}", goodId);
                        }
                    }
                }
            }
            LOG.info("update end, total update : " + count);
        }catch (Exception e){
            LOG.error("解析数据发生错误");
            LOG.error(e.getMessage());
        } finally {
            try {
                bufferedReader.close();
                inputStreamReader.close();
                fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        FileToEsForCoupon fileToEsForCoupon = App.getAppContext().getBean(FileToEsForCoupon.class);
        fileToEsForCoupon.readFileAndWriteEs();
    }
}

