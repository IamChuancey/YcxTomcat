package cn.ycx.ycxtomcat.classloader;

import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

//WebappClassLoader加载每个web-app下的WEB-INF文件夹下的classes和lib目录。

/**
 * 所以，至此为止，一个程序里可能使用了不同的类加载器来加载使用到的不同的jar包和类，它们各司其职，但存在双亲委派的关系。
 */
public class WebappClassLoader extends URLClassLoader {
    public WebappClassLoader(String docBase, ClassLoader parent) {
        super(new URL[]{}, parent);
        File webinfFolder = new File(docBase, "WEB-INF");
        File classesFolder = new File(webinfFolder, "classes");
        File libFolder=new File(webinfFolder,"lib");
        try {
            URL url=new URL("file:"+classesFolder.getAbsolutePath()+"/");
            this.addURL(url);
            List<File> jarFiles= FileUtil.loopFiles(libFolder);
            for(File file:jarFiles){
                url=new URL("file:"+file.getAbsolutePath());
                this.addURL(url);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
