package cn.ycx.ycxtomcat.test;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.ycx.ycxtomcat.util.MiniBrowser;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestTomcat {
    private static int port=18080;
    private static String ip="127.0.0.1";

    @BeforeClass
    public static void beforeTheClass(){
        if(NetUtil.isUsableLocalPort(port)){
            System.err.println("请先启动位于端口："+port+" 的ycxTomcat");
            System.exit(1);
        }else {
            System.out.println("ycxTomcat已启动");
        }
    }

    @Test
    public void testHelloTomcat() throws IOException {
        String html= getContentString("/");
        Assert.assertEquals(html,"Hello DIY Tomcat from ycx.");
    }

    @Test
    public void testAHtml() throws IOException {
        String html= getContentString("/a.html");
        Assert.assertEquals(html,"Hello DIY Tomcat from a.html");
    }

    @Test
    public void testTimeConsumeHtml() throws InterruptedException {
        CountDownLatch countDownLatch=new CountDownLatch(3);
        TimeInterval timeInterval= DateUtil.timer();
        for (int i = 0; i <3; i++) {
           new Thread(()->{
               try {
                   getContentString("/timeConsume.html");
                   countDownLatch.countDown();
               } catch (IOException e) {
                   e.printStackTrace();
               }
           },"Thread "+i).start();
        }
        countDownLatch.await();
        long duration=timeInterval.intervalMs();
        Assert.assertTrue(duration<3000);
    }

    @Test
    public void testaIndex() throws IOException {
        String html=getContentString("/a");
        Assert.assertEquals(html,"Hello DIY Tomcat from index.html@a");
    }

    @Test
    public void testbIndex() throws IOException {
        String html=getContentString("/b");
        Assert.assertEquals(html,"Hello DIY Tomcat from index.html@b");
    }

    public static String getContentString(String uri) throws IOException {
        String url= StrUtil.format("http://{}:{}{}", ip,port,uri);
        String content=MiniBrowser.getContentString(url);
        return content;
    }

    @Test
    public void test404() throws IOException {
        String response=getHttpString("/not_exist.html");
        containsAssert(response,"HTTP/1.1 404 Not Found");
    }

    @Test
    public void test500() throws IOException {
        String response=getHttpString("/500.html");
        containsAssert(response,"HTTP/1.1 500 Internal Server Error");
    }

    @Test
    public void testATxt() throws IOException {
        String response  = getHttpString("/a.txt");
        containsAssert(response, "Content-Type: text/plain");
    }

    @Test
    public void testPNG() throws IOException {
        byte[]bytes=getContentBytes("/logo.png");
        int pngFileLength=1672;
        Assert.assertEquals(pngFileLength,bytes.length);
    }

    @Test
    public void testPDF() throws IOException {
        byte[]bytes=getContentBytes("/etf.pdf");
        int pdfFileLength = 3590775;
        Assert.assertEquals(pdfFileLength,bytes.length);
    }

    @Test
    public void testHello() throws IOException {
        String html = getContentString("/hello");
        Assert.assertEquals(html,"Hello DIY Tomcat from HelloServlet");
    }

    @Test
    public void testJavawebHello() throws IOException {
        String html1 = getContentString("/webforycxtomcat/hello");
        System.out.println(html1);
        String html2 = getContentString("/webforycxtomcat/hello");
        Assert.assertEquals(html1,html2);
    }

    @Test
    public void testGetParam(){
        String uri="/webforycxtomcat/param";
        String url=StrUtil.format("http://{}:{}{}",ip,port,uri);
        Map<String,Object> params=new HashMap<>();
        params.put("name","meepo");
        String html=MiniBrowser.getContentString(url,params,true);
        Assert.assertEquals(html,"get name:meepo");
    }

    @Test
    public void testPosyParam(){
        String uri="/webforycxtomcat/param";
        String url=StrUtil.format("http://{}:{}{}",ip,port,uri);
        Map<String,Object>params=new HashMap<>();
        params.put("name","meepo");
        String html=MiniBrowser.getContentString(url,params,false);
        Assert.assertEquals(html,"post name:meepo");
    }

    @Test
    public void testHeader() throws IOException {
        String html=getContentString("/webforycxtomcat/header");
        Assert.assertEquals(html,"ycx mini brower / java1.8");
    }

    @Test
    public void testSetCookie() throws IOException {
        String html = getHttpString("/webforycxtomcat/setCookie");
        containsAssert(html,"Set-Cookie: name=Gareen(cookie); Expires=");
    }

    @Test
    public void testSession() throws IOException {
        String jsessionid = getContentString("/webforycxtomcat/setSession");
        if(null!=jsessionid)
            jsessionid = jsessionid.trim();
        String url = StrUtil.format("http://{}:{}{}", ip,port,"/webforycxtomcat/getSession");
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setRequestProperty("Cookie","JSESSIONID="+jsessionid);
        conn.connect();
        InputStream is = conn.getInputStream();
        String html = IoUtil.read(is, "utf-8");
        containsAssert(html,"Gareen(session)");
    }

    @Test
    public void testGetCookie() throws IOException {
        String url=StrUtil.format("http://{}:{}{}",ip,port,"/webforycxtomcat/getCookie");
        URL u=new URL(url);
        HttpURLConnection connection=(HttpURLConnection)u.openConnection();
        connection.setRequestProperty("Cookie","name=Gareen(cookie)");
        connection.connect();
        InputStream is=connection.getInputStream();
        String html= IoUtil.read(is,"utf-8");
        containsAssert(html,"name:Gareen(cookie)");
    }

    @Test
    public void testGzip() throws IOException {
        byte[] gzipContent = getContentBytes("/",true);
        //System.out.println("str: "+new String(gzipContent));
        byte[] unGzipContent = ZipUtil.unGzip(gzipContent);
        String html = new String(unGzipContent);
        Assert.assertEquals(html, "Hello DIY Tomcat from ycx.");
    }

    @Test
    public void testJsp() throws IOException {
        String html=getContentString("/webforycxtomcat/index.jsp");
        Assert.assertEquals(html, "hello jsp@javaweb");
    }

    private String getHttpString(String uri) throws IOException {
        String url=StrUtil.format("http://{}:{}{}", ip,port,uri);
        String http=MiniBrowser.getHttpString(url);
        return http;
    }

    private byte[] getContentBytes(String uri) throws IOException {
        return getContentBytes(uri,false);
    }

    private byte[] getContentBytes(String uri,boolean gzip) throws IOException {
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        return MiniBrowser.getContentBytes(url,gzip);
    }

    private void containsAssert(String html,String string){
        boolean match=StrUtil.containsAny(html,string);
        Assert.assertTrue(match);
    }

}
