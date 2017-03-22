package com.shuyun.data.coupon.transfer.launcher;

import com.shuyun.data.coupon.transfer.App;
import com.shuyun.data.coupon.transfer.jobs.MysqlToESJob;

public class MysqlGoodsLauncher {

    public static void main(String[] args) {
        Thread thread = new Thread(App.getAppContext().getBean(MysqlToESJob.class));
        thread.start();
    }
}
