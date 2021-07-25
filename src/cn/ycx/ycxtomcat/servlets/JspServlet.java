package cn.ycx.ycxtomcat.servlets;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.ycx.ycxtomcat.catalina.Context;
import cn.ycx.ycxtomcat.classloader.JspClassLoader;
import cn.ycx.ycxtomcat.http.Request;
import cn.ycx.ycxtomcat.http.Response;
import cn.ycx.ycxtomcat.util.Constant;
import cn.ycx.ycxtomcat.util.JspUtil;
import cn.ycx.ycxtomcat.util.WebXMLUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

public class JspServlet extends HttpServlet {
    private static final long serialVersionUID=1L;
    private static JspServlet instance=new JspServlet();

    public static synchronized JspServlet getInstance(){
        return instance;
    }

    private JspServlet(){

    }

    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
         try {
             Request request=(Request)httpServletRequest;
             Response response=(Response)httpServletResponse;
             String uri=request.getUri();
             if("/".equals(uri)){
                 uri= WebXMLUtil.getWelcomeFile(request.getContext());
             }
             String fileName= StrUtil.removePrefix(uri,"/");
             File file= FileUtil.file(request.getRealPath(fileName));

             File jspFile = file;
             if(file.exists()){
                 Context context = request.getContext();
                 String path = context.getPath();
                 String subFolder;
                 if ("/".equals(path))
                     subFolder = "_";
                 else
                     subFolder = StrUtil.subAfter(path, '/', false);

                 String servletClassPath = JspUtil.getServletClassPath(uri, subFolder);
                 File jspServletClassFile = new File(servletClassPath);
                 if (!jspServletClassFile.exists()) {
                     JspUtil.compileJsp(context, jspFile);
                 } else if (jspFile.lastModified() > jspServletClassFile.lastModified()) {
                     JspUtil.compileJsp(context, jspFile);
                     JspClassLoader.invalidJspClassLoader(uri,context);
                 }

                 String extName=FileUtil.extName(file);
                 String mimeType=WebXMLUtil.getMimeType(extName);
                 response.setContentType(mimeType);

                 JspClassLoader jspClassLoader = JspClassLoader.getJspClassLoader(uri, context);
                 String jspServletClassName = JspUtil.getJspServletClassName(uri, subFolder);
                 Class jspServletClass = jspClassLoader.loadClass(jspServletClassName);

                 HttpServlet servlet = context.getServlet(jspServletClass);
                 servlet.service(request,response);
                 if(null!=response.getRedirectPath())
                     response.setStatus(Constant.CODE_302);
                 else
                     response.setStatus(Constant.CODE_200);
             }else {
                 response.setStatus(Constant.CODE_404);
             }
         }catch (Exception e){
             throw new RuntimeException(e);
         }
    }
}
