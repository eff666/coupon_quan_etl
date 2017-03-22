package com.shuyun.data.coupon.transfer.dao;

import com.shuyun.data.coupon.transfer.pojo.Goods;
import com.shuyun.data.coupon.transfer.pojo.ShopRating;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface IGoodsDao {

    List<Goods> getGoodsList(@Param("offset") int offset, @Param("limit") int limit);

    List<Goods> getGoodsByCtime(@Param("ctime") long ctime);

    List<Goods> getCtimeBatch(@Param("ctime") long ctime, @Param("limit") int limit);

    List<Goods> getIdBatch(@Param("id") long id, @Param("limit") int limit);

    List<Goods> getAbsentGoods(@Param("id") long id);

    void updateAbsentFields(Goods goods);



    void updateSmallUrl(@Param("id") long id, @Param("smallurl") String smallurl);

    List<Goods> getAllGoods();

    //从mysql中查找category不为空的更新到es中
    List<Goods> getUpdateCategoryForEs(@Param("ctime") long ctime);

    //从mysql中查找detailsImgUrls不为空的更新到es中
    List<Goods> getDetailImgUrls(@Param("ctime") long ctime);

    //从mysql中查找所有大于T-1天的数据，更新es中的category和detailsImgUrls
    List<Goods> getUpdateCategoryAndDetailsImgUrlForEs(@Param("ctime") long ctime);

    void insertSellerToMysql(@Param("sellerNick") String sellerNick, @Param("sellerWang") String sellerWang, @Param("insert_time") Date insert_time);

    List<ShopRating> getTkShopRating();



}
