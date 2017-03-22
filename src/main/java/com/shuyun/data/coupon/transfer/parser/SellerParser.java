package com.shuyun.data.coupon.transfer.parser;

import java.util.List;

public class SellerParser {
    private String userNumId;
    private String type;
    private String nick;
    private String creditLevel;
    private String goodRatePercentage;
    private String shopTitle;
    private String shopId;
    private String weitaoId;
    private String fansCount;
    private String fansCountText;
    private List<EvaluateInfoParser> evaluateInfo;
    private String picUrl;
    private String starts;
    private String shopPromtionType;
    private List<ActionUnitsParser> actionUnits;

    private String bailAmount;
    private String shopIcon;
    private String certificateLogo;
    private String hideDsr;

    public String getUserNumId() {
        return userNumId;
    }

    public String getType() {
        return type;
    }

    public String getNick() {
        return nick;
    }

    public String getCreditLevel() {
        return creditLevel;
    }

    public String getGoodRatePercentage() {
        return goodRatePercentage;
    }

    public String getShopTitle() {
        return shopTitle;
    }

    public String getShopId() {
        return shopId;
    }

    public String getWeitaoId() {
        return weitaoId;
    }

    public String getFansCount() {
        return fansCount;
    }

    public String getFansCountText() {
        return fansCountText;
    }

    public List<EvaluateInfoParser> getEvaluateInfo() {
        return evaluateInfo;
    }

    public String getPicUrl() {
        return picUrl;
    }

    public String getStarts() {
        return starts;
    }

    public String getShopPromtionType() {
        return shopPromtionType;
    }

    public List<ActionUnitsParser> getActionUnits() {
        return actionUnits;
    }

    public String getBailAmount() {
        return bailAmount;
    }

    public void setBailAmount(String bailAmount) {
        this.bailAmount = bailAmount;
    }

    public String getShopIcon() {
        return shopIcon;
    }

    public void setShopIcon(String shopIcon) {
        this.shopIcon = shopIcon;
    }

    public String getCertificateLogo() {
        return certificateLogo;
    }

    public void setCertificateLogo(String certificateLogo) {
        this.certificateLogo = certificateLogo;
    }

    public void setUserNumId(String userNumId) {
        this.userNumId = userNumId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public void setCreditLevel(String creditLevel) {
        this.creditLevel = creditLevel;
    }

    public void setGoodRatePercentage(String goodRatePercentage) {
        this.goodRatePercentage = goodRatePercentage;
    }

    public void setShopTitle(String shopTitle) {
        this.shopTitle = shopTitle;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public void setWeitaoId(String weitaoId) {
        this.weitaoId = weitaoId;
    }

    public void setFansCount(String fansCount) {
        this.fansCount = fansCount;
    }

    public void setFansCountText(String fansCountText) {
        this.fansCountText = fansCountText;
    }

    public void setEvaluateInfo(List<EvaluateInfoParser> evaluateInfo) {
        this.evaluateInfo = evaluateInfo;
    }

    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
    }

    public void setStarts(String starts) {
        this.starts = starts;
    }

    public void setShopPromtionType(String shopPromtionType) {
        this.shopPromtionType = shopPromtionType;
    }

    public void setActionUnits(List<ActionUnitsParser> actionUnits) {
        this.actionUnits = actionUnits;
    }

    public String getHideDsr() {
        return hideDsr;
    }

    public void setHideDsr(String hideDsr) {
        this.hideDsr = hideDsr;
    }
}
