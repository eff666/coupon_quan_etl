package com.shuyun.data.coupon.transfer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;

public class PropertiesUtil {

    private static Logger logger =  LoggerFactory.getLogger(PropertiesUtil.class);

    public static final String[] PROPERTIES = System.getProperty("file.separator").equals("/") ?
            new String[]{"/Users/david/test/taobaoke_Id/conf/tbk-spider.properties"}
            :
            new String[]{"D:/JAVA_DEV/bussness/shuyun_spider/taobaoke/conf/tbk-spider.properties"};

    private static Properties properties = new Properties();

    static {
        try {
            for (String file : PROPERTIES) {
//                properties.load(getResourceAsStream(file));
                properties.load(new FileReader(file));
            }
        } catch (IOException e) {
            logger.error("配置文件加载失败！", e);
        }
    }

    private PropertiesUtil() {
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    private static ClassLoader getContextClassLoader() {
        ClassLoader classLoader = null;

        if (classLoader == null) {
            try {
                Method method = Thread.class.getMethod("getContextClassLoader", (Class[]) null);
                try {
                    classLoader = (ClassLoader) method.invoke(Thread.currentThread(), (Class[]) null);
                } catch (IllegalAccessException e) {
                    ; // ignore
                } catch (InvocationTargetException e) {

                    if (e.getTargetException() instanceof SecurityException) {
                        ; // ignore
                    } else {
                        throw new RuntimeException("Unexpected InvocationTargetException",
                                e.getTargetException());
                    }
                }
            } catch (NoSuchMethodException e) {
                // Assume we are running on JDK 1.1
                ; // ignore
            }
        }

        if (classLoader == null) {
            classLoader = PropertiesUtil.class.getClassLoader();
        }

        // Return the selected class loader
        return classLoader;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static InputStream getResourceAsStream(final String name) {
        return (InputStream) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                ClassLoader threadCL = getContextClassLoader();

                if (threadCL != null) {
                    return threadCL.getResourceAsStream(name);
                } else {
                    return ClassLoader.getSystemResourceAsStream(name);
                }
            }
        });
    }

    public static void main(String[] args) {
        System.out.println(PropertiesUtil.getProperty("SPIDER_CONF_PATH"));
    }

}
