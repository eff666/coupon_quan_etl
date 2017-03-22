package com.shuyun.data.coupon.transfer.service;


import com.shuyun.data.coupon.transfer.pojo.Goods;

import java.util.List;

public interface IElasticService {

     boolean upsert(Goods goods, String typeName, boolean flag);

     List<Goods> query(String title, int page, int pageSize);
}
