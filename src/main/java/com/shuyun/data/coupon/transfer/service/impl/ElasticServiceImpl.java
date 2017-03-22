package com.shuyun.data.coupon.transfer.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.shuyun.data.coupon.transfer.pojo.Goods;
import com.shuyun.data.coupon.transfer.service.IElasticService;
import com.shuyun.data.coupon.transfer.util.JsonUtil;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.engine.DocumentAlreadyExistsException;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

@Service("elasticService")
public class ElasticServiceImpl implements IElasticService {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Value("${cluster.name}")
    private String clusterName;
    @Value("${cluster.hosts}")
    private String hosts;
    @Value("${cluster.port}")
    private int port;
    @Value("${goods.indexName}")
    private String indexName;

//    @Value("${goods.typeName}")
//    private String typeName;

    private Client client;

    @PostConstruct
    public void initESClient() {
        try {
            // set cluster name
            Settings settings = Settings.settingsBuilder().put("cluster.name", clusterName).build();
            // add transport addresses
            TransportClient transportClient = TransportClient.builder().settings(settings).build();
            for (String ip : hosts.split(";")) {
                transportClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(ip), port));
            }
            client = transportClient;
        }catch (Exception e){
            LOG.error(e.getMessage());
        }
    }

    @Override
    public boolean upsert(Goods goods, String typeName, boolean statusFlag) {
        if (goods == null) {
            LOG.error("goods is null");
            return false;
        }

        try {
            String id = goods.getGoodid().toString();
            LOG.info("start to deal id:{}", id);
            GetResponse getResponse = client.prepareGet(indexName, typeName, id).execute().actionGet();
            if (getResponse == null) {
                LOG.error("response is null  with id :{}", id);
                return false;
            } else if (getResponse.isExists()) {
                // update logic
                try {
                    //为excel更新
//                    if(statusFlag) {
//                        Map<String, Object> map = getResponse.getSourceAsMap();
//                        long status = 0;
//                        try {
//                            if (Boolean.valueOf(map.get("status").toString())) {
//                                status = 1;
//                            } else {
//                                status = 0;
//                            }
//                        } catch (NullPointerException n) {
//                        }
//                        goods.setStatus(status);
//                    }

                    String json = JsonUtil.OBJECT_MAPPER.writeValueAsString(goods);

                    //LOG.info("will update item with id:{}",id);
                    UpdateResponse updateResponse = client
                            .prepareUpdate(indexName, typeName, id)
                            .setVersion(getResponse.getVersion())
                            .setDoc(json).execute()
                            .actionGet();
                    if (updateResponse.getVersion() == getResponse.getVersion() + 1) {
                        LOG.info("update successes with id:{}", id);
                        return true;
                    }
                } catch (VersionConflictEngineException e) {
                    LOG.error(e.getMessage(), e);
                    LOG.info("update version conflict and will try again");
                    // recursion
                    return upsert(goods, typeName, true);
                }
            } else {
                try {
                    // insert logic
                    //LOG.info("id:{} does not exist in elastic and will insert",id);
                    String json = JsonUtil.OBJECT_MAPPER.writeValueAsString(goods);
                    IndexResponse response = client
                            .prepareIndex(indexName, typeName)
                            .setId(id)
                            .setOpType(IndexRequest.OpType.CREATE)//
                            .setSource(json.getBytes("utf-8")).execute().actionGet();
                    if (response != null && response.isCreated()) {
                        LOG.info("insert successes with id:{}", id);
                        return true;
                    } else {
                        LOG.error("index failed for response is null  or created false for id:{}", id);
                        return false;
                    }
                } catch (DocumentAlreadyExistsException e) {
                    LOG.error(e.getMessage(), e);
                    LOG.info("Document Already Exists and will try again");
                    // recursion
                    return upsert(goods, typeName, false);
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            try {
                LOG.error(JsonUtil.OBJECT_MAPPER.writeValueAsString(goods));
            } catch (JsonProcessingException e1) {
                e1.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public List<Goods> query(String title, int page, int pageSize) {
        return null;
    }
}
