package com.shuyun.data.coupon.transfer;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.math.BigDecimal;

/**
 * Hello world!
 */
public class App {

    private static ApplicationContext appContext;

    public static ApplicationContext getAppContext() {
        if (appContext == null) {
            synchronized (App.class) {
                if (appContext == null) {
                    appContext = new ClassPathXmlApplicationContext("applicationContext.xml");
                }
            }
        }
        return appContext;
    }

    public static void main(String[] args) {
        System.out.println("Hello World!");
        System.out.println("yes");
        BigDecimal priceDecimal = new BigDecimal(30);
        BigDecimal quanpriceDecimal = new BigDecimal(20);
        BigDecimal ratio = quanpriceDecimal.divide(priceDecimal,4,BigDecimal.ROUND_HALF_UP);
        System.out.println(ratio.floatValue());;
    }
}
