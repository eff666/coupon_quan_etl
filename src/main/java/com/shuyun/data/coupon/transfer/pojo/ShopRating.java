package com.shuyun.data.coupon.transfer.pojo;

import java.util.Date;

public class ShopRating {

    private int pid;
    private String shop_nick;
    private String domain_name;
    private double desc_rate;
    private String desc_flag;
    private String desc_exceed_rate;
    private double serv_rate;
    private String serv_flag;
    private String serv_exceed_rate;
    private double send_rate;
    private String send_flag;
    private String send_exceed_rate;
    private Date insert_time;
    private Date update_time;
    private String shop_logo_url;
    private String shop_promotion_url;

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getShop_promotion_url() {
        return shop_promotion_url;
    }

    public void setShop_promotion_url(String shop_promotion_url) {
        this.shop_promotion_url = shop_promotion_url;
    }

    public String getShop_logo_url() {
        return shop_logo_url;
    }

    public void setShop_logo_url(String shop_logo_url) {
        this.shop_logo_url = shop_logo_url;
    }

    public String getShop_nick() {
        return shop_nick;
    }

    public void setShop_nick(String shop_nick) {
        this.shop_nick = shop_nick;
    }

    public String getDomain_name() {
        return domain_name;
    }

    public void setDomain_name(String domain_name) {
        this.domain_name = domain_name;
    }

    public double getDesc_rate() {
        return desc_rate;
    }

    public void setDesc_rate(double desc_rate) {
        this.desc_rate = desc_rate;
    }

    public String getDesc_flag() {
        return desc_flag;
    }

    public void setDesc_flag(String desc_flag) {
        this.desc_flag = desc_flag;
    }

    public String getDesc_exceed_rate() {
        return desc_exceed_rate;
    }

    public void setDesc_exceed_rate(String desc_exceed_rate) {
        this.desc_exceed_rate = desc_exceed_rate;
    }

    public double getServ_rate() {
        return serv_rate;
    }

    public void setServ_rate(double serv_rate) {
        this.serv_rate = serv_rate;
    }

    public String getServ_flag() {
        return serv_flag;
    }

    public void setServ_flag(String serv_flag) {
        this.serv_flag = serv_flag;
    }

    public String getServ_exceed_rate() {
        return serv_exceed_rate;
    }

    public void setServ_exceed_rate(String serv_exceed_rate) {
        this.serv_exceed_rate = serv_exceed_rate;
    }

    public double getSend_rate() {
        return send_rate;
    }

    public void setSend_rate(double send_rate) {
        this.send_rate = send_rate;
    }

    public String getSend_flag() {
        return send_flag;
    }

    public void setSend_flag(String send_flag) {
        this.send_flag = send_flag;
    }

    public String getSend_exceed_rate() {
        return send_exceed_rate;
    }

    public void setSend_exceed_rate(String send_exceed_rate) {
        this.send_exceed_rate = send_exceed_rate;
    }

    public Date getInsert_time() {
        return insert_time;
    }

    public void setInsert_time(Date insert_time) {
        this.insert_time = insert_time;
    }

    public Date getUpdate_time() {
        return update_time;
    }

    public void setUpdate_time(Date update_time) {
        this.update_time = update_time;
    }
}
