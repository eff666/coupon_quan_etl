package com.shuyun.data.coupon.transfer.analysis;

import com.shuyun.data.coupon.transfer.App;
import com.shuyun.data.coupon.transfer.dao.IGoodsDao;
import com.shuyun.data.coupon.transfer.jobs.LoadGoodsFromFile;
import com.shuyun.data.coupon.transfer.pojo.Goods;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

@Component
public class QuanAnalysis {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private IGoodsDao goodsDao;

    @Autowired
    LoadGoodsFromFile loadGoodsFromFile;
    public Map<BigInteger, Goods> getAllGoodsIdFromMysql(){
        Map<BigInteger, Goods> map = new HashMap<>();
        Long lastId = 0l;
        while(true){
            List<Goods> goodsList = goodsDao.getIdBatch(lastId, 200);
            for(Goods goods: goodsList){
                map.put(goods.getGoodid(), goods);
                if(goods.getId() > lastId){
                    lastId = goods.getId();
                }
            }
            if(goodsList.size()!=200){
                break;
            }

        }
        return map;
    }

    public Map<BigInteger, Goods> getGoodsIdFromExcel(String filePath){
        Map<BigInteger, Goods> map =new HashMap<>();
        HSSFWorkbook wb = null;
        POIFSFileSystem fs = null;
        try {
            fs = new POIFSFileSystem(new FileInputStream(new File(filePath)));
            wb = new HSSFWorkbook(fs);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

        HSSFSheet sheet = wb.getSheetAt(0);
        System.out.println(sheet.getLastRowNum());
        for(int rowNum=1; rowNum<=sheet.getLastRowNum();rowNum++){
            Goods goods = loadGoodsFromFile.initGoods(sheet.getRow(rowNum));
            goods.fillAll();
            map.put(new BigInteger(sheet.getRow(rowNum).getCell(0).getStringCellValue()),goods);

        }
        return map;
    }

    public  void mysqlCompareWithExcel(){
        Map<BigInteger, Goods> mysqlMap = getAllGoodsIdFromMysql();

        Map<BigInteger, Goods> excelMap = getGoodsIdFromExcel("F:\\\\file\\\\精选优质商品清单(内含优惠券)-2016-10-11.xls");
        compare(mysqlMap,excelMap);
    }

    public  void excelCompareWithExcel(){
        Map<BigInteger, Goods> mysqlMap = getGoodsIdFromExcel("F:\\\\file\\\\精选优质商品清单(内含优惠券)-2016-10-12.xls");

        Map<BigInteger, Goods> excelMap = getGoodsIdFromExcel("F:\\\\file\\\\精选优质商品清单(内含优惠券)-2016-10-11.xls");
        compare(mysqlMap,excelMap);

    }

    public  void compare(Map<BigInteger, Goods> mysqlMap,  Map<BigInteger, Goods> excelMap){
        int count = 0;
        for(BigInteger b : mysqlMap.keySet()){
            if(excelMap.keySet().contains(b)){
                count++;
                Goods g1 = mysqlMap.get(b);
                Goods g2 = excelMap.get(b);
                compareGoods(g1,g2);
            }
        }
        LOG.info("same goodsid num is :{}", count);
    }

    private void compareGoods(Goods g1, Goods g2) {
        if(g1.getQuanprice()-g2.getQuanprice() > 0.0001 || g1.getQuanprice()-g2.getQuanprice()< -0.001){
            LOG.info("price is different :{},:{}",g1.getQuanprice(),g2.getQuanprice());
        }
    }

    public static void main(String[] args) {
        QuanAnalysis quanAnalysis = App.getAppContext().getBean(QuanAnalysis.class);
        quanAnalysis.mysqlCompareWithExcel();
//        quanAnalysis.excelCompareWithExcel();
    }
}
