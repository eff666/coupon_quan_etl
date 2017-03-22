package com.shuyun.data.coupon.transfer.pojo;

import java.math.BigInteger;
import java.util.Date;

public class WDetailGoods {
    //促销价格
    private String discountPrice;
    //原价格
    private String originalPrice;
    //月销量
    private Long totalSoldQuantity;
    //库存总数
    private Long quantity;
    //天猫积分,4-11
    private String points;

    //不同尺码商品库存数量
    private String skuQuantity;

    //商品售前保证相关信息
    private String beforeGuarantees;

    //商品售后保证相关信息
    private String afterGuarantees;

    //卖家包邮描述
    private String deliveryFeesDesc;
    //邮费
    private Float postagePrice;

    //商品id
    private BigInteger goodsId;
    //商品名称
    private String title;
    //商品主图
    private String goodsImg;
    //商品移动端详情
    private String mobileItemUrl;
    //商品所在地
    private String location;
    /**
     * 卖家类型，0表示淘宝，1表示天猫
     */
    private Integer itemType;
    //卖家类型平台logo
    private String itemTypeLogo;
    //收藏商品人数
    private String favcount;


    /*
     *商品属性详情
     */
    private String goodsPropsDetail;

    /*
    商品详情介绍图片，需要得到fullDescUrl，发送http请求得到
     */
    private String detailImgUrls;

    /*
    商品颜色详情
     */
    private String colourPropsDetail;

    /*
      * 店铺相关信息
      **/
    //店铺昵称
    private String shopNick;
    //店铺信誉等级
    private Long shopCreditLevel;
    //好评率
    private String shopGoodRatePercentage;
    //店铺id
    private BigInteger shopId;
    //店铺粉丝人数
    private String shopFansCount;

    /*
     * 店铺评分信息
     * 负号表示低于同行业
     */
    //店铺描述相符得分
    private Float shopDescScore;
    //铺描述相符与同行相比
    private String shopDescHighGap;
    //店铺服务态度得分
    private Float shopServiceScore;
    //服务态度与同行相比
    private String shopServiceHighGap;
    //店铺物流服务得分
    private Float shopShippingScore;
    //物流服务与同行相比
    private String shopShippingHighGap;
    //店铺logo
    private String shopLogo;
    //店铺类型
    private String shopType;
    //店铺开店时间
    private String shopTime;
    //全部商品数
    private String totalGoods;

    //pc端详情
    private String pcDescUrl;

    //详情图html
    private String fullDescUrlHtml;


   /*
    商品评价详情
     */
    //评价总数
    private BigInteger goodsRateCounts;
    //评价详情
    private String goodsRateDetailInfo;

    //数据插入或者更新时间
    private long ctime;
    //商品是否可用标志，true可用，false不可用
    private String status;
//    //商品版本号
//    private Long version;


    /*
    商品category
     */
    //商品一级类目
    private String category; //商品一级类目
    //商品13个大类目
    private String topCategory; //商品13个大类目
    //商品详情类目
    private String detailCategory; //商品详情类目


    public String getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(String discountPrice) {
        this.discountPrice = discountPrice;
    }

    public String getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(String originalPrice) {
        this.originalPrice = originalPrice;
    }

    public Long getTotalSoldQuantity() {
        return totalSoldQuantity;
    }

    public void setTotalSoldQuantity(Long totalSoldQuantity) {
        this.totalSoldQuantity = totalSoldQuantity;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public String getPoints() {
        return points;
    }

    public void setPoints(String points) {
        this.points = points;
    }

    public String getSkuQuantity() {
        return skuQuantity;
    }

    public void setSkuQuantity(String skuQuantity) {
        this.skuQuantity = skuQuantity;
    }

    public String getBeforeGuarantees() {
        return beforeGuarantees;
    }

    public void setBeforeGuarantees(String beforeGuarantees) {
        this.beforeGuarantees = beforeGuarantees;
    }

    public String getAfterGuarantees() {
        return afterGuarantees;
    }

    public void setAfterGuarantees(String afterGuarantees) {
        this.afterGuarantees = afterGuarantees;
    }

    public String getDeliveryFeesDesc() {
        return deliveryFeesDesc;
    }

    public void setDeliveryFeesDesc(String deliveryFeesDesc) {
        this.deliveryFeesDesc = deliveryFeesDesc;
    }

    public Float getPostagePrice() {
        return postagePrice;
    }

    public void setPostagePrice(Float postagePrice) {
        this.postagePrice = postagePrice;
    }

    public BigInteger getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(BigInteger goodsId) {
        this.goodsId = goodsId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGoodsImg() {
        return goodsImg;
    }

    public void setGoodsImg(String goodsImg) {
        this.goodsImg = goodsImg;
    }

    public String getMobileItemUrl() {
        return mobileItemUrl;
    }

    public void setMobileItemUrl(String mobileItemUrl) {
        this.mobileItemUrl = mobileItemUrl;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getItemType() {
        return itemType;
    }

    public void setItemType(Integer itemType) {
        this.itemType = itemType;
    }

    public String getItemTypeLogo() {
        return itemTypeLogo;
    }

    public void setItemTypeLogo(String itemTypeLogo) {
        this.itemTypeLogo = itemTypeLogo;
    }

    public String getFavcount() {
        return favcount;
    }

    public void setFavcount(String favcount) {
        this.favcount = favcount;
    }

    public String getGoodsPropsDetail() {
        return goodsPropsDetail;
    }

    public void setGoodsPropsDetail(String goodsPropsDetail) {
        this.goodsPropsDetail = goodsPropsDetail;
    }

    public String getDetailImgUrls() {
        return detailImgUrls;
    }

    public void setDetailImgUrls(String detailImgUrls) {
        this.detailImgUrls = detailImgUrls;
    }

    public String getColourPropsDetail() {
        return colourPropsDetail;
    }

    public void setColourPropsDetail(String colourPropsDetail) {
        this.colourPropsDetail = colourPropsDetail;
    }

    public String getShopNick() {
        return shopNick;
    }

    public void setShopNick(String shopNick) {
        this.shopNick = shopNick;
    }

    public Long getShopCreditLevel() {
        return shopCreditLevel;
    }

    public void setShopCreditLevel(Long shopCreditLevel) {
        this.shopCreditLevel = shopCreditLevel;
    }

    public String getShopGoodRatePercentage() {
        return shopGoodRatePercentage;
    }

    public void setShopGoodRatePercentage(String shopGoodRatePercentage) {
        this.shopGoodRatePercentage = shopGoodRatePercentage;
    }

    public BigInteger getShopId() {
        return shopId;
    }

    public void setShopId(BigInteger shopId) {
        this.shopId = shopId;
    }

    public String getShopFansCount() {
        return shopFansCount;
    }

    public void setShopFansCount(String shopFansCount) {
        this.shopFansCount = shopFansCount;
    }

    public Float getShopDescScore() {
        return shopDescScore;
    }

    public void setShopDescScore(Float shopDescScore) {
        this.shopDescScore = shopDescScore;
    }

    public String getShopDescHighGap() {
        return shopDescHighGap;
    }

    public void setShopDescHighGap(String shopDescHighGap) {
        this.shopDescHighGap = shopDescHighGap;
    }

    public Float getShopServiceScore() {
        return shopServiceScore;
    }

    public void setShopServiceScore(Float shopServiceScore) {
        this.shopServiceScore = shopServiceScore;
    }

    public String getShopServiceHighGap() {
        return shopServiceHighGap;
    }

    public void setShopServiceHighGap(String shopServiceHighGap) {
        this.shopServiceHighGap = shopServiceHighGap;
    }

    public Float getShopShippingScore() {
        return shopShippingScore;
    }

    public void setShopShippingScore(Float shopShippingScore) {
        this.shopShippingScore = shopShippingScore;
    }

    public String getShopShippingHighGap() {
        return shopShippingHighGap;
    }

    public void setShopShippingHighGap(String shopShippingHighGap) {
        this.shopShippingHighGap = shopShippingHighGap;
    }

    public String getShopLogo() {
        return shopLogo;
    }

    public void setShopLogo(String shopLogo) {
        this.shopLogo = shopLogo;
    }

    public String getShopType() {
        return shopType;
    }

    public void setShopType(String shopType) {
        this.shopType = shopType;
    }

    public String getShopTime() {
        return shopTime;
    }

    public void setShopTime(String shopTime) {
        this.shopTime = shopTime;
    }

    public String getTotalGoods() {
        return totalGoods;
    }

    public void setTotalGoods(String totalGoods) {
        this.totalGoods = totalGoods;
    }

    public BigInteger getGoodsRateCounts() {
        return goodsRateCounts;
    }

    public void setGoodsRateCounts(BigInteger goodsRateCounts) {
        this.goodsRateCounts = goodsRateCounts;
    }

    public String getGoodsRateDetailInfo() {
        return goodsRateDetailInfo;
    }

    public void setGoodsRateDetailInfo(String goodsRateDetailInfo) {
        this.goodsRateDetailInfo = goodsRateDetailInfo;
    }

    public long getCtime() {
        return ctime;
    }

    public void setCtime(long ctime) {
        this.ctime = ctime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTopCategory() {
        return topCategory;
    }

    public void setTopCategory(String topCategory) {
        this.topCategory = topCategory;
    }

    public String getDetailCategory() {
        return detailCategory;
    }

    public void setDetailCategory(String detailCategory) {
        this.detailCategory = detailCategory;
    }

    public String getPcDescUrl() {
        return pcDescUrl;
    }

    public void setPcDescUrl(String pcDescUrl) {
        this.pcDescUrl = pcDescUrl;
    }

    public String getFullDescUrlHtml() {
        return fullDescUrlHtml;
    }

    public void setFullDescUrlHtml(String fullDescUrlHtml) {
        this.fullDescUrlHtml = fullDescUrlHtml;
    }
}
