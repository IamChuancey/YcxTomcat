package cn.ycx.ycxtomcat.http;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Response extends BaseResponse {
    private StringWriter stringWriter;
    private PrintWriter writer;
    private String contentType;
    private byte[] body;
    private int status;
    private List<Cookie>cookies;
    private String redirectPath;

    public Response(){
        this.stringWriter=new StringWriter();
        this.writer=new PrintWriter(stringWriter);
        this.contentType="text/html";
        this.cookies=new ArrayList<>();
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType(){
        return contentType;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public byte[] getBody() throws UnsupportedEncodingException {
        if(null==body) {
            String content = stringWriter.toString();
            body = content.getBytes("utf-8");
        }
        return body;
    }

    @Override
    public void setStatus(int status){
        this.status=status;
    }

    @Override
    public int getStatus(){
        return status;
    }

    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }
    public List<Cookie> getCookies() {
        return this.cookies;
    }

    public PrintWriter getWriter(){
        return writer;
    }

    public String getCookiesHeader(){
        if(cookies==null){
            return "";
        }
        String pattern="EEE, d MMM yyyy HH:mm:ss 'GMT'";
        SimpleDateFormat sdf=new SimpleDateFormat(pattern,Locale.ENGLISH);
        StringBuffer sb=new StringBuffer();
        for(Cookie cookie:getCookies()){
            sb.append("\r\n");
            sb.append("Set-Cookie: ");
            sb.append(cookie.getName()+"="+cookie.getValue()+"; ");
            if(cookie.getMaxAge()!=-1){
                sb.append("Expires=");
                Date now=new Date();
                Date expire= DateUtil.offset(now, DateField.MINUTE,cookie.getMaxAge());
                sb.append(sdf.format(expire));
                sb.append(";");
            }
            if(cookie.getPath()!=null){
                sb.append("Path="+cookie.getPath());
            }
        }
        return sb.toString();
    }

    public String getRedirectPath() {
        return redirectPath;
    }

    public void sendRedirect(String redirect) throws IOException {
        this.redirectPath = redirect;
    }
}
