package com.shuyun.data.coupon.transfer.parser;

public class ItemInfoModelParser {
    private String itemId;
    private String title;
    private String[] picsPath;
    private String favcount;
    private String stuffStatus;
    private String itemUrl;
    private String sku;
    private String location;
    private String saleLine;
    private String categoryId;
    private String itemTypeName;
    private String itemTypeLogo;
    private String itemIcon;
    private String isMakeup;

    private String startTime;

    public String getIsMakeup() {
        return isMakeup;
    }

    public String getItemIcon() {
        return itemIcon;
    }

    public String getItemId() {
        return itemId;
    }

    public String getTitle() {
        return title;
    }

    public String[] getPicsPath() {
        return picsPath;
    }

    public String getFavcount() {
        return favcount;
    }

    public String getStuffStatus() {
        return stuffStatus;
    }

    public String getItemUrl() {
        return itemUrl;
    }

    public String getSku() {
        return sku;
    }

    public String getLocation() {
        return location;
    }

    public String getSaleLine() {
        return saleLine;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getItemTypeName() {
        return itemTypeName;
    }

    public String getItemTypeLogo() {
        return itemTypeLogo;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPicsPath(String[] picsPath) {
        this.picsPath = picsPath;
    }

    public void setFavcount(String favcount) {
        this.favcount = favcount;
    }

    public void setStuffStatus(String stuffStatus) {
        this.stuffStatus = stuffStatus;
    }

    public void setItemUrl(String itemUrl) {
        this.itemUrl = itemUrl;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setSaleLine(String saleLine) {
        this.saleLine = saleLine;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public void setItemTypeName(String itemTypeName) {
        this.itemTypeName = itemTypeName;
    }

    public void setItemTypeLogo(String itemTypeLogo) {
        this.itemTypeLogo = itemTypeLogo;
    }

    public void setItemIcon(String itemIcon) {
        this.itemIcon = itemIcon;
    }

    public void setIsMakeup(String isMakeup) {
        this.isMakeup = isMakeup;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
}
