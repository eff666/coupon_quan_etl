package com.shuyun.data.coupon.transfer.pojo;

import java.math.BigInteger;

public class WDetailGoodsSnapshot {

    //商品id
    private BigInteger goodsId;

    //商品完整json信息，为IS_NULL表示商品下架或者调用还失败
    private String data;

    //商品是否可用标志，true可用，false不可用
    private String status;

    //数据插入或者更新时间
    private String ctime;

    //商品版本号
    private Long version;

    public BigInteger getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(BigInteger goodsId) {
        this.goodsId = goodsId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCtime() {
        return ctime;
    }

    public void setCtime(String ctime) {
        this.ctime = ctime;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
