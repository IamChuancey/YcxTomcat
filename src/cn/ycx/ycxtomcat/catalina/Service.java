package cn.ycx.ycxtomcat.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.LogFactory;
import cn.ycx.ycxtomcat.exception.WebConfigDuplicatedException;
import cn.ycx.ycxtomcat.util.ServerXMLUtil;

import java.util.List;

public class Service {
    private String name;
    private Engine engine;
    private Server server;
    private List<Connector>connectors;
    public Service(Server server) throws WebConfigDuplicatedException {
        this.server=server;
        this.name= ServerXMLUtil.getServiceName();
        this.engine=new Engine(this);
        this.connectors=ServerXMLUtil.getConnectors(this);
    }
    public Engine getEngine(){
        return engine;
    }
    public Server getServer() {
        return server;
    }
    //这里ycxTomcat支持多端口的内部原理即：在每一个端口上都开启相同的服务端服务。
    public void start(){
        init();
    }
    private void init(){
        TimeInterval timeInterval= DateUtil.timer();
        for (Connector c:connectors) {
            c.init();
        }
        LogFactory.get().info("Initialization processed in {} ms",timeInterval.intervalMs());
        for (Connector c:connectors) {
            c.start();
        }
    }
}
