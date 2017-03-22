package com.shuyun.data.coupon.transfer.parser;

import java.util.List;

public class RateInfoParser {
    private String rateCounts;
    //private List<Object> rateDetailList;
    private List<rateDetailListParser> rateDetailList;

    public String getRateCounts() {
        return rateCounts;
    }

    public List<rateDetailListParser> getRateDetailList() {
        return rateDetailList;
    }
}
