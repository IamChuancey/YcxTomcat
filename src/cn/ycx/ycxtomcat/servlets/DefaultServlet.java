package cn.ycx.ycxtomcat.servlets;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.ycx.ycxtomcat.catalina.Context;
import cn.ycx.ycxtomcat.http.Request;
import cn.ycx.ycxtomcat.http.Response;
import cn.ycx.ycxtomcat.util.Constant;
import cn.ycx.ycxtomcat.util.WebXMLUtil;
import org.apache.jasper.JasperException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * 处理静态资源的请求，如.png,.txt,等等
 */
public class DefaultServlet extends HttpServlet {
    private static DefaultServlet instance=new DefaultServlet();
    public static synchronized DefaultServlet getInstance(){
        return instance;
    }
    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        Request request=(Request) httpServletRequest;
        Response response=(Response) httpServletResponse;
        Context context=request.getContext();
        String uri=request.getUri();
        //这里做个硬编码，仅供调试使用
        if("/500.html".equals(uri)){
            throw new RuntimeException("this is a deliberately created exception");
        }
        //如果访问默认的http地址,则访问默认的欢迎文件
        if(uri.equals("/"))
        {
            uri = WebXMLUtil.getWelcomeFile(request.getContext());
        }
        if(uri.endsWith(".jsp")){
            JspServlet.getInstance().service(request,response);
            return;
        }
        //如果访问其他文件
        String fileName = StrUtil.removePrefix(uri,"/");
        File file = FileUtil.file(request.getRealPath(fileName));
        if (file.exists()) {
            //根据文件的扩展名匹配出浏览器能识别的mime-type
            String extName=FileUtil.extName(file);
            String mimeType=WebXMLUtil.getMimeType(extName);
            response.setContentType(mimeType);
            //读取文件的内容
            byte body[] = FileUtil.readBytes(file);
            response.setBody(body);
            if (fileName.equals("timeConsume.html")) {
                ThreadUtil.sleep(1000);
            }
            response.setStatus(Constant.CODE_200);
        } else {
            response.setStatus(Constant.CODE_404);
        }
    }
}
