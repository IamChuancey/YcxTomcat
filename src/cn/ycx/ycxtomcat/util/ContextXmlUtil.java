package cn.ycx.ycxtomcat.util;

import cn.hutool.core.io.FileUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ContextXmlUtil {
    public static String getWatchedResource(){
        String xml= FileUtil.readUtf8String(Constant.contextXmlFile);
        Document d= Jsoup.parse(xml);
        Element e=d.select("WatchedResource").first();
        return e.text();
    }
}
