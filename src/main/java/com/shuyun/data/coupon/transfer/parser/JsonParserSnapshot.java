package com.shuyun.data.coupon.transfer.parser;

public class JsonParserSnapshot {

    private String api;
    private Float v;
    private String[] ret;
    private Object data;

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public Float getV() {
        return v;
    }

    public void setV(Float v) {
        this.v = v;
    }

    public String[] getRet() {
        return ret;
    }

    public void setRet(String[] ret) {
        this.ret = ret;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
