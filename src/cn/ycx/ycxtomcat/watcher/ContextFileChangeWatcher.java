package cn.ycx.ycxtomcat.watcher;

import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import cn.hutool.log.LogFactory;
import cn.ycx.ycxtomcat.catalina.Context;

import java.io.Console;
import java.nio.file.Path;
import java.nio.file.WatchEvent;

public class ContextFileChangeWatcher {
    private WatchMonitor monitor;
    private boolean stop=false;
    public ContextFileChangeWatcher(Context context){
        this.monitor=WatchUtil.createAll(context.getDocBase(),Integer.MAX_VALUE,new Watcher(){
            private void dealWith(WatchEvent<?>event){
                synchronized (ContextFileChangeWatcher.class){
                    String fileName=event.context().toString();
                    if(stop) return;
                    if(fileName.endsWith(".jar")||fileName.endsWith(".class")||fileName.endsWith(".xml")){
                        stop=true;
                        LogFactory.get().info(String.valueOf(ContextFileChangeWatcher.this),"检测到了web应用下的重要文件变化 {}",fileName);
                        //热加载的原理就是重新载入context
                        context.reload();
                    }
                }
            }

            @Override
            public void onCreate(WatchEvent<?> watchEvent, Path path) {
                //System.out.println("创建"+path+watchEvent.context());
                dealWith(watchEvent);
            }

            @Override
            public void onModify(WatchEvent<?> watchEvent, Path path) {
                //System.out.println("修改"+path+watchEvent.context());
                 dealWith(watchEvent);
            }

            @Override
            public void onDelete(WatchEvent<?> watchEvent, Path path) {
                //System.out.println("删除"+path+watchEvent.context());
                dealWith(watchEvent);
            }

            @Override
            public void onOverflow(WatchEvent<?> watchEvent, Path path) {
                dealWith(watchEvent);
            }
        });
        this.monitor.setDaemon(true);
    }

    public void start(){
        monitor.start();
    }

    public void stop(){
        monitor.close();
    }

}
