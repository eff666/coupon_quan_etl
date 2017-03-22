package com.shuyun.data.coupon.transfer.launcher;

import com.shuyun.data.coupon.transfer.App;
import com.shuyun.data.coupon.transfer.jobs.LoadGoodsFromFile;
import com.shuyun.data.coupon.transfer.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

@Component
public class ExcelGoodsLauncher {
    private static final Logger LOG = LoggerFactory.getLogger(ExcelGoodsLauncher.class);

    @Value("${fileDir}")
    private String fileDir;


    @Value("${filePrefix}")
    private String filePrefix;
    @Value("${filePostfix}")
    private String filePostfix;
    @Autowired
    private LoadGoodsFromFile loadGoodsFromFile;

    public void dailyJob() {
        Date date = new Date();
        String day = Constants.DAY_FORMAT.format(date);
        String fileStr = fileDir + filePrefix + day + filePostfix;
        LOG.info("file name is {}", fileStr);
        LOG.debug("file is begin process, file name is {}", fileStr);

       // String fileStr = "F:\\datafortag\\maimaimai\\quan-2016-11-22.xls";
        while (true) {
            File file = new File(fileStr);
            if (file.exists()) {
                //LOG.info("file is exist and will process");
                try {
                    loadGoodsFromFile.loadGoodsAndInsertInElasicSearch(fileStr);
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
                break;
            } else {
                LOG.info("file is not exist then will sleep and wait one hour, file name : {}\n", fileStr);
                try {
                    Thread.currentThread().sleep(1000 * 60 * 60 * 1);
                } catch (InterruptedException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }

    public static void main(String[] args) {
        ExcelGoodsLauncher excelGoodsLauncher = App.getAppContext().getBean(ExcelGoodsLauncher.class);
        excelGoodsLauncher.dailyJob();
    }
}
