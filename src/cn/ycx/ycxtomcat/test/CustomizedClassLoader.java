package cn.ycx.ycxtomcat.test;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import sun.plugin2.util.SystemUtil;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CustomizedClassLoader extends ClassLoader {
    private File classFolder=new File(System.getProperty("user.dir"),"classes_4_test");

    //在不打破双亲委派规则的基础上如何自定义classLoader，只需要重写类的findClass方法即可。
    @Override
    protected Class<?>findClass(String QualifiedName) throws ClassNotFoundException {
        byte[]data=loadClassData(QualifiedName);
        return defineClass(QualifiedName,data,0,data.length);
    }

    private byte[] loadClassData(String fullQualifiedName) throws ClassNotFoundException {
        String fileName= StrUtil.replace(fullQualifiedName,".","/")+".class";
        File classFile=new File(classFolder,fileName);
        if(!classFile.exists())
            throw  new ClassNotFoundException(fullQualifiedName);
        return FileUtil.readBytes(classFile);
    }

    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        CustomizedClassLoader loader=new CustomizedClassLoader();
        Class<?>how2jClass=loader.loadClass("cn.how2j.diytomcat.test.HOW2J");
        Object o=how2jClass.newInstance();
        Method m=how2jClass.getMethod("hello");
        m.invoke(o);
        System.out.println(how2jClass.getClassLoader());
    }
}
