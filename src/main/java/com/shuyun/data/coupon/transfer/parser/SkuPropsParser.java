package com.shuyun.data.coupon.transfer.parser;

import java.util.List;

public class SkuPropsParser {
    private String propId;
    private String propName;
    private List<Object> values;

    public String getPropId() {
        return propId;
    }

    public String getPropName() {
        return propName;
    }

    public List<Object> getValues() {
        return values;
    }

    //public List<ValuesParser> getValues() {
//        return values;
//    }
}
