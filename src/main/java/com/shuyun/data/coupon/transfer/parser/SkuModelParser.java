package com.shuyun.data.coupon.transfer.parser;

import java.util.List;
import java.util.Map;

public class SkuModelParser {
    private List<SkuPropsParser> skuProps;
    private Object ppathIdmap;
    private boolean installmentEnable;

    private Object skuTitle;
    private Object components;

    public List<SkuPropsParser> getSkuProps() {
        return skuProps;
    }

    public Object getPpathIdmap() {
        return ppathIdmap;
    }


    public boolean isInstallmentEnable() {
        return installmentEnable;
    }

    public void setSkuProps(List<SkuPropsParser> skuProps) {
        this.skuProps = skuProps;
    }

    public void setPpathIdmap(Object ppathIdmap) {
        this.ppathIdmap = ppathIdmap;
    }

    public void setInstallmentEnable(boolean installmentEnable) {
        this.installmentEnable = installmentEnable;
    }

    public Object getSkuTitle() {
        return skuTitle;
    }

    public void setSkuTitle(Object skuTitle) {
        this.skuTitle = skuTitle;
    }

    public Object getComponents() {
        return components;
    }

    public void setComponents(Object components) {
        this.components = components;
    }
}
