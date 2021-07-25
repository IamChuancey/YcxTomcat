package cn.ycx.ycxtomcat.catalina;

import cn.ycx.ycxtomcat.exception.WebConfigDuplicatedException;
import cn.ycx.ycxtomcat.util.ServerXMLUtil;

import java.util.List;

public class Engine {
    private String defaultHost;
    private List<Host>hosts;
    private Service service;

    public Engine(Service service) throws WebConfigDuplicatedException {
        this.service=service;
        this.defaultHost= ServerXMLUtil.getEngineDefaultHost();
        this.hosts=ServerXMLUtil.getHosts(this);
        checkDefault();
    }

    private void checkDefault(){
        if(getDefaultHost()==null){
            throw new RuntimeException("the defaultHost" + defaultHost + " does not exist!");
        }
    }

    public Host getDefaultHost(){
        for (Host host:hosts) {
            if(host.getName().equals(defaultHost))
                return host;
        }
        return null;
    }

    public Service getService() {
        return service;
    }
}
