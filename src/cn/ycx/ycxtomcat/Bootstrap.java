package cn.ycx.ycxtomcat;

import cn.ycx.ycxtomcat.catalina.*;
import cn.ycx.ycxtomcat.classloader.CommonClassLoader;
import cn.ycx.ycxtomcat.exception.WebConfigDuplicatedException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

//CommonClassLoader用于加载%tomcat_home%/lib这个目录里的类和jar
public class Bootstrap {
    public static void main(String[] args) throws WebConfigDuplicatedException, IOException, ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        CommonClassLoader commonClassLoader=new CommonClassLoader();
        Thread.currentThread().setContextClassLoader(commonClassLoader); //设置当前线程的类加载器是commonClassLoader
        String serverClassName="cn.ycx.ycxtomcat.catalina.Server";
        Class serverClazz=commonClassLoader.loadClass(serverClassName);
        Object serverObj=serverClazz.newInstance();
        Method m=serverClazz.getMethod("start");
        m.invoke(serverObj);
    }
}
