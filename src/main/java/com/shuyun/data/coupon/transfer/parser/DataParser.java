package com.shuyun.data.coupon.transfer.parser;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DataParser {
    private List<ApiStackParser> apiStack;
    private ItemInfoModelParser itemInfoModel;
    private SkuModelParser skuModel;
    private SellerParser seller;
//    private List<Object> props;
    private List<PropsParser> props;
    private DescInfoParser descInfo;
    private RateInfoParser rateInfo;
    private ComboInfoParser comboInfo;
    private LayoutDataParser layoutData;
    private List<WeappListParser> weappList;
    private Object trackParams;
    private ExtrasParser extras;

    private Object guaranteeInfo;
    private Object redirectUrl;
    private Object taobaoPreSaleInfo;
    private Object displayType;
    private Object paramInfo;
    private Object trackAllParams;

    public List<ApiStackParser> getApiStack() {
        return apiStack;
    }

    public ItemInfoModelParser getItemInfoModel() {
        return itemInfoModel;
    }

    public SkuModelParser getSkuModel() {
        return skuModel;
    }

    public SellerParser getSeller() {
        return seller;
    }

    public List<PropsParser> getProps() {
        return props;
    }

    public DescInfoParser getDescInfo() {
        return descInfo;
    }

    public RateInfoParser getRateInfo() {
        return rateInfo;
    }

    public ComboInfoParser getComboInfo() {
        return comboInfo;
    }

    public LayoutDataParser getLayoutData() {
        return layoutData;
    }

    public List<WeappListParser> getWeappList() {
        return weappList;
    }

    public Object getTrackParams() {
        return trackParams;
    }

    public ExtrasParser getExtras() {
        return extras;
    }

    public Object getGuaranteeInfo() {
        return guaranteeInfo;
    }

    public void setGuaranteeInfo(Object guaranteeInfo) {
        this.guaranteeInfo = guaranteeInfo;
    }

    public void setApiStack(List<ApiStackParser> apiStack) {
        this.apiStack = apiStack;
    }

    public void setItemInfoModel(ItemInfoModelParser itemInfoModel) {
        this.itemInfoModel = itemInfoModel;
    }

    public void setSkuModel(SkuModelParser skuModel) {
        this.skuModel = skuModel;
    }

    public void setSeller(SellerParser seller) {
        this.seller = seller;
    }

    public void setProps(List<PropsParser> props) {
        this.props = props;
    }

    public void setDescInfo(DescInfoParser descInfo) {
        this.descInfo = descInfo;
    }

    public void setRateInfo(RateInfoParser rateInfo) {
        this.rateInfo = rateInfo;
    }

    public void setComboInfo(ComboInfoParser comboInfo) {
        this.comboInfo = comboInfo;
    }

    public void setLayoutData(LayoutDataParser layoutData) {
        this.layoutData = layoutData;
    }

    public void setWeappList(List<WeappListParser> weappList) {
        this.weappList = weappList;
    }

    public void setTrackParams(Object trackParams) {
        this.trackParams = trackParams;
    }

    public void setExtras(ExtrasParser extras) {
        this.extras = extras;
    }

    public Object getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(Object redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public Object getTaobaoPreSaleInfo() {
        return taobaoPreSaleInfo;
    }

    public void setTaobaoPreSaleInfo(Object taobaoPreSaleInfo) {
        this.taobaoPreSaleInfo = taobaoPreSaleInfo;
    }

    public Object getDisplayType() {
        return displayType;
    }

    public void setDisplayType(Object displayType) {
        this.displayType = displayType;
    }

    public Object getParamInfo() {
        return paramInfo;
    }

    public void setParamInfo(Object paramInfo) {
        this.paramInfo = paramInfo;
    }

    public Object getTrackAllParams() {
        return trackAllParams;
    }

    public void setTrackAllParams(Object trackAllParams) {
        this.trackAllParams = trackAllParams;
    }
}
