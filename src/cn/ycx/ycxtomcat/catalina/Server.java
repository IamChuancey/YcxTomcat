package cn.ycx.ycxtomcat.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import cn.hutool.system.SystemUtil;
import cn.ycx.ycxtomcat.exception.WebConfigDuplicatedException;
import cn.ycx.ycxtomcat.http.Request;
import cn.ycx.ycxtomcat.http.Response;
import cn.ycx.ycxtomcat.util.Constant;
import cn.ycx.ycxtomcat.util.ThreadPoolUtil;
import cn.ycx.ycxtomcat.util.WebXMLUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Server {
    private Service service;
    public Server() throws WebConfigDuplicatedException {
        this.service=new Service(this);
    }
    private void logJVM(){
        Map<String,String> infos=new HashMap<>();
        infos.put("Server version", "Ycx DiyTomcat/1.0.1");
        infos.put("Server built", "2020-04-08 10:20:22");
        infos.put("Server number", "1.0.1");
        infos.put("OS Name\t", SystemUtil.get("os.name"));
        infos.put("OS Version", SystemUtil.get("os.version"));
        infos.put("Architecture", SystemUtil.get("os.arch"));
        infos.put("Java Home", SystemUtil.get("java.home"));
        infos.put("JVM Version", SystemUtil.get("java.runtime.version"));
        infos.put("JVM Vendor", SystemUtil.get("java.vm.specification.vendor"));
        Set<String> keys=infos.keySet();
        for (String key:keys
        ) {
            LogFactory.get().info(key+":\t\t"+infos.get(key));
        }
    }

    private void init(){
        service.start();
    }

    public void start(){
        TimeInterval timeInterval = DateUtil.timer();
        logJVM();
        init();
        LogFactory.get().info("Server startup in {} ms",timeInterval.intervalMs());
    }

}
