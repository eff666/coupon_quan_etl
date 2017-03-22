package com.shuyun.data.coupon.transfer.jobs;

import com.google.common.base.Strings;
import com.shuyun.data.coupon.transfer.App;
import com.shuyun.data.coupon.transfer.pojo.Goods;
import com.shuyun.data.coupon.transfer.service.IElasticService;
import com.shuyun.data.coupon.transfer.util.CategoryUtil;
import com.shuyun.data.coupon.transfer.util.Constants;
import com.shuyun.data.coupon.transfer.util.HttpRequest;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class LoadGoodsFromFile  {


    private final Logger LOG = LoggerFactory.getLogger(this.getClass());


//    @Value("${filePath}")
//    private String filePath ;
//        ="F:\\\\file\\\\精选优质商品清单(内含优惠券)-2016-10-11.xls";

    @Autowired
    private IElasticService elasticService;
    @Autowired
    private FillAbsentFields fillAbsentFields;

    private CategoryUtil categoryUtil = new CategoryUtil();

    private Map<Integer, String> keyMap = new HashMap<>();

    public void loadGoodsAndInsertInElasicSearch(String filePath){

        HSSFWorkbook wb = null;
        POIFSFileSystem fs = null;
        try {
            fs = new POIFSFileSystem(new FileInputStream(new File(filePath)));
            wb = new HSSFWorkbook(fs);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

        HSSFSheet sheet = wb.getSheetAt(0);
        initTitle(sheet.getRow(0));
        Integer rowNum = 1;
        LOG.info("file is exist and will process\n");
        for(rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++){
            try{
                //初始化excel，共22个字段
                Goods goods = initGoods(sheet.getRow(rowNum));

                //根据category更新topCategory,1个字段
                try{
                    String topCategory = goods.getCategory();
                    if(topCategory != null && topCategory.trim().length() > 0){
                        goods.setTopCategory(categoryUtil.getGoodsExcelGeneralCategory(topCategory));
                    }
                }catch (NullPointerException nu){
                }

                //得到provcity，1个字段
                fillAbsentFields.fillAbsentForExcel(goods);
                //得到quanprice，discountRatio字段，2个字段
                goods.fillAll();

                //得到ctime字段，1个字段
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String strDate = simpleDateFormat.format(new Date());
                Date date = null;
                try {
                    date = simpleDateFormat.parse(strDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                goods.setCtime(date.getTime() / 1000);

                //根据quanurl更新status，1个字段
                try {
                    long statusFlag = 0;
                    String quanUrl = goods.getQuanurl();
                    if (quanUrl != null && quanUrl.trim().length() > 0) {
                        //判断是否失效
                        if (quanUrl.indexOf("?") > -1) {
                            //quanUrl = quanUrl.substring(quanUrl.indexOf("?") + 1, quanUrl.length());
                            String urlName = "https://shop.m.taobao.com/shop/coupon.htm?" + quanUrl.substring(quanUrl.indexOf("?") + 1, quanUrl.length());
                            String html = HttpRequest.getStatusBySendGet(urlName);
                            if (!Strings.isNullOrEmpty(html)) {
                                if (html.contains("该优惠券不存在或者已经过期") || html.contains("您浏览店铺不存在或已经关闭")) {
                                    statusFlag = 0;
                                } else {
                                    statusFlag = 1;
                                }
                            }
                        }
                    }
                    goods.setStatus(statusFlag);
                }catch (Exception e){
                    LOG.error("解析excel数据到es时根据quanurl更新status出错，goodid：[ " + goods.getGoodid() + " ]，cause: " + e.getMessage());
                }

                elasticService.upsert(goods,"goods_coupon", false);
            }catch(Exception e){
                LOG.error(e.getMessage(),e);
            }
        }
        LOG.info("update end, total data: " + rowNum);
    }

    private void initTitle(HSSFRow row) {
        if(row!=null){
            for (int cellNum = row.getFirstCellNum() ; cellNum< row.getLastCellNum(); cellNum++){
                keyMap.put(cellNum, row.getCell(cellNum).getStringCellValue());
            }
        }
    }

    public Goods initGoods(HSSFRow row) {
        Goods goods = new Goods();
        if(row != null){
            for (int cellNum = row.getFirstCellNum() ; cellNum< row.getLastCellNum(); cellNum++){
                Cell cell = row.getCell(cellNum);
                switch (keyMap.get(cellNum)){
                    case "商品id":
                        goods.setGoodid(new BigInteger(cell.getStringCellValue()));
                        break;
                    case "商品名称":
                        goods.setTitle(cell.getStringCellValue());
                        break;
                    case "商品主图":
                        goods.setImg(cell.getStringCellValue());
                        break;
                    case "商品详情页链接地址":
                        goods.setItemDetailUrl(cell.getStringCellValue());
                        break;
                    case "店铺名称":
                        goods.setNick(cell.getStringCellValue());
                        break;
                    case "商品价格(单位：元)":
                        goods.setPrice(Float.parseFloat(cell.getStringCellValue()));
                        break;
                    case "商品月销量":
                        goods.setVolume(Long.parseLong(cell.getStringCellValue()));
                        break;
                    case "收入比率(%)":
                        goods.setBrokerageRatio(Float.parseFloat(cell.getStringCellValue()));
                        break;
                    case "佣金":
                        goods.setBrokerage(Float.parseFloat(cell.getStringCellValue()));
                        break;
                    case "卖家旺旺":
                        goods.setSellerWang(cell.getStringCellValue());
                        break;
                    case "淘宝客链接":
                        goods.setGoodurl(cell.getStringCellValue());
                        break;
                    case "优惠券总量":
                        goods.setTotalnum(Long.parseLong(cell.getStringCellValue()));
                        break;
                    case "优惠券剩余量":
                        goods.setRestnum(Long.parseLong(cell.getStringCellValue()));
                        break;
                    case "优惠券面额":
                        goods.setQuanDesc(cell.getStringCellValue());
                        break;
                    case "优惠券开始时间":
                        try {
                            Date date = Constants.DAY_FORMAT.parse(cell.getStringCellValue());
                            goods.setDatestart(date.getTime()/1000);
                        } catch (ParseException e) {
                            LOG.error(e.getMessage(),e);
                        }
                        break;
                    case "优惠券结束时间":
                        try {
                            Date date = Constants.DAY_FORMAT.parse(cell.getStringCellValue());
                            goods.setDateend(date.getTime()/1000);
                        } catch (ParseException e) {
                            LOG.error(e.getMessage(),e);
                        }
                        break;
                    case "优惠券链接":
                        goods.setQuanurl(cell.getStringCellValue());
                        break;
                    case "商品一级类目":
                        goods.setCategory(cell.getStringCellValue());
                        break;
                    case "卖家id":
                        goods.setSellerid(new BigInteger(cell.getStringCellValue()));
                        break;
                    case "平台类型":
                        if("天猫".equals(cell.getStringCellValue())){
                            goods.setUsertype(1);
                        }else if("淘宝".equals(cell.getStringCellValue())){
                            goods.setUsertype(0);
                        }else{
                            LOG.error("平台类型value error with：{}",cell.getStringCellValue());
                        }
                        break;
                    case "优惠券id":
                        goods.setCouponId(cell.getStringCellValue());
                        break;
                    case "商品优惠券推广链接":
                        goods.setPromoteurl(cell.getStringCellValue());
                        break;
                    default:
                        LOG.error("excel style changed and title :{}", keyMap.get(cellNum));
                        break;
                }
            }
        }
        return goods;
    }

    private List<String> manClothes = Arrays.asList("女装/女士精品", "女装", "女士精品");

    public static void main(String[] args) {
        LoadGoodsFromFile loadGoodsFromFile = App.getAppContext().getBean(LoadGoodsFromFile.class);
        String filePaths[] = {
//            "F:\\\\file\\\\精选优质商品清单(内含优惠券)-2016-10-13.xls"
//                ,
//                "F:\\\\file\\\\精选优质商品清单(内含优惠券)-2016-10-14.xls",
//                "F:\\\\file\\\\精选优质商品清单(内含优惠券)-2016-10-15.xls",
//                "F:\\\\file\\\\精选优质商品清单(内含优惠券)-2016-10-22.xls",
                "F:\\\\file\\\\精选优质商品清单(内含优惠券)-2016-10-26.xls"
//                "F:\\\\file\\\\quan-2016-10-18.xls"
        };
        for(String file: filePaths) {
            loadGoodsFromFile.loadGoodsAndInsertInElasicSearch(file);
        }
    }
}
