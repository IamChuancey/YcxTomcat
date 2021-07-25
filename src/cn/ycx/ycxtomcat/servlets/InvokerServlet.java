package cn.ycx.ycxtomcat.servlets;

import cn.hutool.core.util.ReflectUtil;
import cn.ycx.ycxtomcat.catalina.Context;
import cn.ycx.ycxtomcat.http.Request;
import cn.ycx.ycxtomcat.http.Response;
import cn.ycx.ycxtomcat.util.Constant;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 处理动态资源的请求，如动态Servlet等
 */
public class InvokerServlet extends HttpServlet {
    private static InvokerServlet instance=new InvokerServlet();
    public static synchronized  InvokerServlet getInstance(){
        return instance;
    }
    //InvokerServlet 实现了 HttpServlet,所以一定提供了 service 方法。 这个 service 方法实会根据 request 的 Method ，访问对应的 doGet 或者 doPost,
    //同样地，这也就是为什么我们要在我们的Request类中重写getMethod方法。
    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){
        Request request=(Request) httpServletRequest;
        Response response=(Response)httpServletResponse;

        String uri=request.getUri();
        Context context=request.getContext();
        String servletClassName=context.getServletClassName(uri);

        try {
            Class servletClass=context.getWebClassLoader().loadClass(servletClassName);
            System.out.println("servletClass: "+servletClass);
            System.out.println("servletClass's classLoader: "+servletClass.getClassLoader());
            Object servletObj= context.getServlet(servletClass);
            ReflectUtil.invoke(servletObj,"service",request,response);
            if(null!=response.getRedirectPath())
                response.setStatus(Constant.CODE_302);
            else
                response.setStatus(Constant.CODE_200);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ServletException e) {
            e.printStackTrace();
        }
    }

}
