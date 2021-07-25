package cn.ycx.ycxtomcat.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

//URLCLassLoader继承于ClassLoader,相比于ClassLoader,URLClassLoader可以加载指定jar
public class CustomizedURLClassLoader extends URLClassLoader {
    public CustomizedURLClassLoader(URL[] urls) {
        super(urls);
    }

    public static void main(String[] args) throws MalformedURLException, ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        URL url=new URL("file:/Users/chuancey/Idea-Workspace/YcxTomcat/jar_4_test/test.jar");
        URL[] urls=new URL[]{url};
        CustomizedURLClassLoader loader=new CustomizedURLClassLoader(urls);
        Class<?>how2jClass=loader.loadClass("cn.how2j.diytomcat.test.HOW2J");
        Object o=how2jClass.newInstance();
        Method m=how2jClass.getMethod("hello");
        m.invoke(o);
        System.out.println(how2jClass.getClassLoader());
    }
}
