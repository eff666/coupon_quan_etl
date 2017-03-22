package com.shuyun.data.coupon.transfer.pojo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Goods {

    private Long id;
    /**
     * 商品ID
     */
    private BigInteger goodid;
    /**
     * 商品名称标题
     */
    private String title;
    /**
     * 商品主图Url连接地址
     */
    private String img;
    /**
     * 商品价格
     */
    private Float price;
    /**
     * 优惠券价格
     * excel中我从quandesc中抽取
     */
    private Float quanprice;
    /**
     * 剩余优惠券数量
     */
    private Long restnum;
    /**
     * 有效日期开始
     */
    private Long datestart;
    /**
     * 有效日期结束
     */
    private Long dateend;
    /**
     * 优惠券url地址
     */
    private String quanurl;
    /**
     * 商品的淘宝客链接 http://s.click.taobao.com/t?e=m%3D2%26s%3DrrRhzTHATPgcQipKwQzePOeEDrYVVa64K7Vc7tFgwiFRAdhuF14FMVv0iQk6FGIj5x%2BIUlGKNpVGiig5lJ3kbQ6kiG63E0lEiutOHl2eAw1ijQZRFOrv7pl7EJiapk12D4sSHHSJqFOxBr8kfcYFlTOvsxiDn47Jxg5p7bh%2BFbQ%3D
     */
    private String goodurl;

    /**
     * 卖家昵称，店铺名称
     */
    private String nick;
    /**
     * 卖家ID
     */
    private BigInteger sellerid;
    /**
     * 月销量
     */
    private Long volume;
    /**
     * 卖家类型，0表示淘宝，1表示天猫
     */
    private Integer usertype;
    /**
     * 商品优惠券有效性标志位，1可用，0失效
     * excel中默认为true
     */
    private String status = "true";

    //数据插入或者更新时间
    private Long ctime;


    /**
     * 宝贝所在地，省份
     * excel中商品我去从tbk抓取
     */
    private String provcity;
    /**
     * 折扣率
     * mysql我通过计算可得到：price/quanprice
     */
    private Float discountRatio;

    /**
     * 优惠券ID，1c685413be5e4d7b8193aade1c53e0e2
     * mysql我通过quanurl提取出来，activity_id即为优惠券id
     */
    private String couponId;
    /**
     * 商品详情页url  http://item.taobao.com/item.htm?id=537757536191
     * mysql我中通过goodid和固定前缀（http://item.taobao.com/item.htm?id=）拼成
     */
    private String itemDetailUrl;


    /**
     * 优惠券总量
     * mysql中商品需要我去爬取
     */
    private Long totalnum;
    /**
     * 优惠券面额
     * mysql中商品需要我去爬取
     */
    private String quanDesc;


    //注意：以下三个字段（promoteurl，detailImgUrls，smallurl）后面需要伟哥爬取

    /**
     * 商品优惠券推广链接
     * mysql中商品需要后面伟哥爬取
     *https://uland.taobao.com/coupon/edetail?e=51rd6V4CfsEN%2BoQUE6FNzLHOv%2FKoJ4LsM%2FOCBWMCflrRpLchICCDoqC1%2FGlh90mJawrr5tC7ywv6IJYHkiaJoRpywujSvOp2nUIklpPPqYLpLw5%2B9auFQ2B0oPbJUgEmvdk%2Bm1ut%2BoHm1rZB7bc%2FOB5U2gLKVNsV&pid=mm_115940806_14998982_61982315&itemId=539553786496&af=1
     */
    private String promoteurl;

    /**
     * 商品详情图片
     * excel中商品需要后面伟哥去爬取
     */
    private String detailImgUrls;
    /**
     * 商品小图url
     * excel中商品需要后面伟哥去爬取
     */
    private String smallurl;

    //商品一级类目，对应于之前的itemCatId
    private String category; //商品一级类目
    //商品13个大类目，对应于之前的generalCategory
    private String topCategory; //商品13个大类目
    //商品详情类目，对应于之前的thirdCategory
    private String detailCategory; //商品详情类目



    //excel特有字段，mysql中置为空

    /**
     * 卖家旺旺
     */
    private String sellerWang;
    /**
     * 收入比率
     */
    private Float brokerageRatio;
    /**
     * 佣金
     */
    private Float brokerage;


    private Double score;

    private Double label;

//    private Long id;
//    /**
//     * 商品名称标题
//     */
//    private String title;
//    /**
//     * 商品ID
//     */
//    private BigInteger goodid;
//    /**
//     * 商品主图Url连接地址
//     */
//    private String img;
//    /**
//     * 商品价格
//     */
//    private Float price;
//    /**
//     * 优惠券价格
//     */
//    private Float quanprice;
//
//    /**
//     * 优惠券描述
//     */
//    private String quanDesc;
//    /**
//     * 剩余优惠券数量
//     */
//    private Long restnum;
//    /**
//     * 优惠券总量
//     */
//    private Long totalnum;
//    /**
//     * 有效日期开始
//     */
//    private Long datestart;
//    /**
//     * 有效日期结束
//     */
//    private Long dateend;
//
//    /**
//     * 优惠券url地址
//     */
//    private String quanurl;
//    /**
//     * 商品的淘宝客链接 http://s.click.taobao.com/t?e=m%3D2%26s%3DrrRhzTHATPgcQipKwQzePOeEDrYVVa64K7Vc7tFgwiFRAdhuF14FMVv0iQk6FGIj5x%2BIUlGKNpVGiig5lJ3kbQ6kiG63E0lEiutOHl2eAw1ijQZRFOrv7pl7EJiapk12D4sSHHSJqFOxBr8kfcYFlTOvsxiDn47Jxg5p7bh%2BFbQ%3D
//     */
//    private String goodurl;
//    /**
//     * 商品优惠券有效性标志位，1可用，0失效
//     */
//    private Boolean status =true;
//    private Long ctime;
//
//    /**
//     * 卖家昵称
//     */
//    private String nick;
//
//    /**
//     * 卖家ID
//     */
//    private BigInteger sellerid;
//
//    /**
//     * 卖家旺旺
//     */
//    private String sellerWang;
//
//    /**
//     * 30天销量
//     */
//    private Long volume;
//
//    /**
//     * 卖家类型，0表示淘宝，1表示天猫
//     */
//    private Integer usertype;
//
//    /**
//     * 宝贝所在地
//     */
//    private String provcity;
//
////    /**
////     * 优惠券类型，两段式还是二合一
////     *  0 表示二段式
////     *  1 表示二合一
////     */
////    private int couponType;
//
//    /**
//     * 商品类目
//     */
//    private String itemCatId;
//    /**
//     * 商品详情页url  http://item.taobao.com/item.htm?id=537757536191
//     */
//    private String itemDetailUrl;
//
//    /**
//     * 佣金比率
//     */
//    private Float brokerageRatio;
//
//    /**
//     * 佣金
//     */
//    private Float brokerage;
//
//    /**
//     * 折扣率
//     */
//    private Float discountRatio;
//
//    /**
//     * 优惠券ID，1c685413be5e4d7b8193aade1c53e0e2
//     */
//    private String couponId;
//
//    /**
//     * 商品优惠券推广链接
//     * https://uland.taobao.com/coupon/edetail?e=51rd6V4CfsEN%2BoQUE6FNzLHOv%2FKoJ4LsM%2FOCBWMCflrRpLchICCDoqC1%2FGlh90mJawrr5tC7ywv6IJYHkiaJoRpywujSvOp2nUIklpPPqYLpLw5%2B9auFQ2B0oPbJUgEmvdk%2Bm1ut%2BoHm1rZB7bc%2FOB5U2gLKVNsV&pid=mm_115940806_14998982_61982315&itemId=539553786496&af=1
//     */
//    private String promoteurl;
//
//    //小图的url
//    private String smallurl;
//
//    //商品二级类目
//    private String category;
//
//    // 商品一级类目
//    private String generalCategory;
//
//    // 商品三级级类目
//    private String thirdCategory;
//
//    // 商品详情图片
//    private String detailImgUrls;

    public Float getDiscountRatio() {
        return discountRatio;
    }

    public void setDiscountRatio(Float discountRatio) {
        this.discountRatio = discountRatio;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigInteger getGoodid() {
        return goodid;
    }

    public void setGoodid(BigInteger goodid) {
        this.goodid = goodid;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public Float getPrice() {
        return price;
    }

    public void setPrice(Float price) {
        this.price = price;
    }

    public Float getQuanprice() {
        return quanprice;
    }

    public void setQuanprice(Float quanprice) {
        this.quanprice = quanprice;
    }

    public String getQuanDesc() {
        return quanDesc;
    }

    public void setQuanDesc(String quanDesc) {
        this.quanDesc = quanDesc;
    }

    public Long getRestnum() {
        return restnum;
    }

    public void setRestnum(Long restnum) {
        this.restnum = restnum;
    }

    public Long getTotalnum() {
        return totalnum;
    }

    public void setTotalnum(Long totalnum) {
        this.totalnum = totalnum;
    }

    public Long getDatestart() {
        return datestart;
    }

    public void setDatestart(Long datestart) {
        this.datestart = datestart;
    }

    public Long getDateend() {
        return dateend;
    }

    public void setDateend(Long dateend) {
        this.dateend = dateend;
    }

    public String getQuanurl() {
        return quanurl;
    }

    public void setQuanurl(String quanurl) {
        this.quanurl = quanurl;
    }

    public String getGoodurl() {
        return goodurl;
    }

    public void setGoodurl(String goodurl) {
        this.goodurl = goodurl;
    }

    public String getStatus() {
        return status;
    }


    public Long getCtime() {
        return ctime;
    }

    public void setCtime(Long ctime) {
        this.ctime = ctime;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public BigInteger getSellerid() {
        return sellerid;
    }

    public void setSellerid(BigInteger sellerid) {
        this.sellerid = sellerid;
    }

    public String getSellerWang() {
        return sellerWang;
    }

    public void setSellerWang(String sellerWang) {
        this.sellerWang = sellerWang;
    }

    public Long getVolume() {
        return volume;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
    }

    public Integer getUsertype() {
        return usertype;
    }

    public void setUsertype(Integer usertype) {
        this.usertype = usertype;
    }

    public String getProvcity() {
        return provcity;
    }

    public void setProvcity(String provcity) {
        this.provcity = provcity;
    }

//    public String getItemCatId() {
//        return itemCatId;
//    }
//
//    public void setItemCatId(String itemCatId) {
//        this.itemCatId = itemCatId;
//    }

    public String getItemDetailUrl() {
        return itemDetailUrl;
    }

    public void setItemDetailUrl(String itemDetailUrl) {
        this.itemDetailUrl = itemDetailUrl;
    }

    public Float getBrokerageRatio() {
        return brokerageRatio;
    }

    public void setBrokerageRatio(Float brokerageRatio) {
        this.brokerageRatio = brokerageRatio;
    }

    public Float getBrokerage() {
        return brokerage;
    }

    public void setBrokerage(Float brokerage) {
        this.brokerage = brokerage;
    }

    public void setStatus(long status) {
        if(status == 1){
            this.status = "true";
        }else  if(status == 0){
            this.status = "false";
        }

    }


    public String getSmallurl() {
        return smallurl;
    }

    public void setSmallurl(String smallurl) {
        this.smallurl = smallurl;
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

//    public int getCouponType() {
//        return couponType;
//    }
//
//    public void setCouponType(int couponType) {
//        this.couponType = couponType;
//    }


//    public String getGeneralCategory() {
//        return generalCategory;
//    }
//
//    public void setGeneralCategory(String generalCategory) {
//        this.generalCategory = generalCategory;
//    }

    public String getDetailImgUrls() {
        return detailImgUrls;
    }

    public void setDetailImgUrls(String detailImgUrls) {
        this.detailImgUrls = detailImgUrls;
    }

    public void fillAll() {

        if(quanprice == null){
            if(quanDesc != null){
                if(quanDesc.contains("元无条件券")){
                    String priceStr = quanDesc.replaceAll("元无条件券","");
                    quanprice = Float.parseFloat(priceStr);
                }else if (quanDesc.contains("元减")){//满120元减60元
                    String str = quanDesc.replaceAll("元","");
                    String priceStr = quanDesc.split("减")[1].replaceAll("元","");
                    quanprice = Float.parseFloat(priceStr);
                }else{
                    System.out.println("error:"+quanDesc);
                }
            }
        }
        if(discountRatio == null){
            BigDecimal priceDecimal = new BigDecimal(this.price);
            BigDecimal quanpriceDecimal = new BigDecimal(this.quanprice);
            BigDecimal ratio = quanpriceDecimal.divide(priceDecimal,4,BigDecimal.ROUND_HALF_UP);
            discountRatio = ratio.floatValue();
        }
        if( usertype == null){
            status = "false";
        }
    }


    public String getCouponId() {
        return couponId;
    }

    public void setCouponId(String couponId) {
        this.couponId = couponId;
    }

    public String getPromoteurl() {
        return promoteurl;
    }

    public void setPromoteurl(String promoteurl) {
        this.promoteurl = promoteurl;
    }

    public  int hashCode(){
        return goodid.hashCode();
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getLabel() {
        return label;
    }

    public void setLabel(Double label) {
        this.label = label;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof  Goods){
            Goods goods = (Goods) obj;
            if(goodid!= null && goodid.equals(goods.getGoodid())){
                return true;
            }
        }
        return (this == obj);
    }
}


