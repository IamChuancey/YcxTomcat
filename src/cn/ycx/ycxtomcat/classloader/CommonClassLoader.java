package cn.ycx.ycxtomcat.classloader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class CommonClassLoader extends URLClassLoader {
    public CommonClassLoader() throws IOException {
        super(new URL[]{});
        File workingFolder=new File(System.getProperty("user.dir"));
        File libFolder=new File(workingFolder,"lib");
        File[] jarFiles=libFolder.listFiles();
        for(File file:jarFiles){
            if(file.getName().endsWith("jar")){
                URL url=new URL("file:"+file.getAbsolutePath());
                this.addURL(url);
            }
        }
    }
}
