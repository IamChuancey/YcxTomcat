package cn.ycx.ycxtomcat.catalina;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import cn.ycx.ycxtomcat.exception.WebConfigDuplicatedException;
import cn.ycx.ycxtomcat.util.Constant;
import cn.ycx.ycxtomcat.util.ServerXMLUtil;
import cn.ycx.ycxtomcat.watcher.WarFileWatcher;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Host {
    private String name;
    private Map<String,Context>contextMap;
    private Engine engine;
    public Host(String name,Engine engine) throws WebConfigDuplicatedException {
        this.contextMap=new HashMap<>();
        this.name=name;
        this.engine=engine;
        scanContextsOnWebAppsFolder(); //从文件夹中扫描应用
        scanContextsInServerXML(); //从xml文件中扫描应用
        scanWarOnWebAppsFolder(); //war包静态部署
        new WarFileWatcher(this).start();//war包动态部署
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name=name;
    }

    public Context getContext(String path) {
        return contextMap.get(path);
    }

    private  void scanContextsOnWebAppsFolder() throws WebConfigDuplicatedException {
        File[]folders= Constant.webappsFolder.listFiles();
        for (File folder:folders) {
            if(!folder.isDirectory())
                continue;
            loadContext(folder);
        }
    }

    private  void loadContext(File folder) throws WebConfigDuplicatedException {
        String path=folder.getName();
        if("ROOT".equals(path)){
            path="/";
        }else {
            path="/"+path;
        }
        String docBase=folder.getAbsolutePath();
        Context context=new Context(path,docBase,this,true);
        contextMap.put(context.getPath(),context);
    }

    private  void scanContextsInServerXML() throws WebConfigDuplicatedException {
        List<Context> contexts= ServerXMLUtil.getContexts(this);
        for (Context c:contexts
        ) {
            contextMap.put(c.getPath(),c);
        }
    }

    /**
     * 实现热加载的核心代码
     * @param context
     * @throws WebConfigDuplicatedException
     */
    public void reload(Context context) throws WebConfigDuplicatedException {
        LogFactory.get().info("Reloading Context with name [{}] has started", context.getPath());
        String path=context.getPath();
        String docBase=context.getDocBase();
        boolean reloadable=context.isReloadable();
        //当文件发生变化，会发过来很多次事件。 所以我们得一个一个事件的处理，否则搞不好就会让 Context 重载多次。
        //我们只需要取得当前发生变化的文件或者文件夹名称，然后重载一次Context即可，所以这里需要stop();
        context.stop();
        contextMap.remove(path);
        Context newContext=new Context(path,docBase,this,reloadable);
        contextMap.put(newContext.getPath(),newContext);
        LogFactory.get().info("Reloading Context with name [{}] has completed", context.getPath());
    }

    public void scanWarOnWebAppsFolder(){
        File folder = FileUtil.file(Constant.webappsFolder);
        File[] files = folder.listFiles();
        for (File file : files) {
            if(!file.getName().toLowerCase().endsWith(".war"))
                continue;
            loadWar(file);
        }
    }

    public void loadWar(File warFile) {
        String fileName =warFile.getName();
        String folderName = StrUtil.subBefore(fileName,".",true);
        //看看是否已经有对应的 Context了
        Context context= getContext("/"+folderName);
        if(null!=context)
            return;
        //先看是否已经有对应的文件夹
        File folder = new File(Constant.webappsFolder,folderName);
        if(folder.exists())
            return;
        //移动war文件，因为jar 命令只支持解压到当前目录下
        File tempWarFile = FileUtil.file(Constant.webappsFolder, folderName, fileName);
        File contextFolder = tempWarFile.getParentFile();
        contextFolder.mkdir();
        FileUtil.copyFile(warFile, tempWarFile);
        //解压
        String command = "jar xvf " + fileName;
//		System.out.println(command);
        Process p = RuntimeUtil.exec(null, contextFolder, command);
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //解压之后删除临时war
        tempWarFile.delete();
        //然后创建新的 Context
        load(contextFolder);
    }

    public void load(File folder)  {
        String path = folder.getName();
        if ("ROOT".equals(path))
            path = "/";
        else
            path = "/" + path;
        String docBase = folder.getAbsolutePath();
        try{
            Context context = new Context(path, docBase, this, false);
            contextMap.put(context.getPath(), context);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
