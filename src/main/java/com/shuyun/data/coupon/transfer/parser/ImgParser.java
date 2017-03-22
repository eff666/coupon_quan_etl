package com.shuyun.data.coupon.transfer.parser;

import java.util.List;

public class ImgParser {
    private Object pages;
    private List<String> images;
    private Object summary;

    public Object getPages() {
        return pages;
    }

    public List<String> getImages() {
        return images;
    }

    public void setPages(Object pages) {
        this.pages = pages;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public Object getSummary() {
        return summary;
    }

    public void setSummary(Object summary) {
        this.summary = summary;
    }


}
