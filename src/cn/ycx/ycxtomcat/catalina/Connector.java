package cn.ycx.ycxtomcat.catalina;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import cn.ycx.ycxtomcat.http.Request;
import cn.ycx.ycxtomcat.http.Response;
import cn.ycx.ycxtomcat.util.Constant;
import cn.ycx.ycxtomcat.util.ThreadPoolUtil;
import cn.ycx.ycxtomcat.util.WebXMLUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Connector implements Runnable {
    int port;
    private Service service;
    private String compression;
    private int compressionMinSize;
    private String noCompressionUserAgents;
    private String compressableMimeType;
    public Connector(Service service){
        this.service=service;
    }
    public Service getService(){
        return service;
    }
    public void setPort(int port){
        this.port=port;
    }
    @Override
    public void run() {
        try {
            //传统的bio写法
            ServerSocket ss=new ServerSocket(port);
            while (true){
                Socket s=ss.accept();
                Runnable r = () -> {
                    try {
                        Request request = new Request(s,Connector.this);
                        Response response = new Response();
                        HttpProcessor processor=new HttpProcessor();
                        processor.execute(s,request,response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                };
                ThreadPoolUtil.run(r);
            }
        }catch (IOException e){
            LogFactory.get().error(e);
            e.printStackTrace();
        }
    }


    public void init() {
        LogFactory.get().info("Initializing ProtocolHandler [http-bio-{}]",port);
    }

    public void start(){
        LogFactory.get().info("Starting ProtocolHandler [http-bio-{}]",port);
        new Thread(this).start();
    }

    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public int getCompressionMinSize() {
        return compressionMinSize;
    }

    public void setCompressionMinSize(int compressionMinSize) {
        this.compressionMinSize = compressionMinSize;
    }

    public String getNoCompressionUserAgents() {
        return noCompressionUserAgents;
    }

    public void setNoCompressionUserAgents(String noCompressionUserAgents) {
        this.noCompressionUserAgents = noCompressionUserAgents;
    }

    public String getCompressableMimeType() {
        return compressableMimeType;
    }

    public void setCompressableMimeType(String compressableMimeType) {
        this.compressableMimeType = compressableMimeType;
    }
}
