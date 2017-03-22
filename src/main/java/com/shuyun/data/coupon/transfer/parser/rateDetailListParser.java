package com.shuyun.data.coupon.transfer.parser;

import java.util.List;

public class rateDetailListParser {
    private String nick;
    private String headPic;
    private String star;
    private String feedback;

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getHeadPic() {
        return headPic;
    }

    public void setHeadPic(String headPic) {
        this.headPic = headPic;
    }

    public String getStar() {
        return star;
    }

    public void setStar(String star) {
        this.star = star;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getSubInfo() {
        return subInfo;
    }

    public void setSubInfo(String subInfo) {
        this.subInfo = subInfo;
    }

    public List<String> getRatePicList() {
        return ratePicList;
    }

    public void setRatePicList(List<String> ratePicList) {
        this.ratePicList = ratePicList;
    }

    private String subInfo;
    private List<String> ratePicList;


}
