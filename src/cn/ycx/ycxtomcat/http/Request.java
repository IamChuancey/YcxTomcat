package cn.ycx.ycxtomcat.http;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.ycx.ycxtomcat.Bootstrap;
import cn.ycx.ycxtomcat.catalina.*;
import cn.ycx.ycxtomcat.util.MiniBrowser;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.net.Socket;
import java.security.Principal;
import java.util.*;

public class Request extends BaseRequest {
    private String requestString;
    private String uri;
    private Socket socket;
    private Context context;
    private Connector connector;
    private String method;
    private String queryString;
    private Map<String,String[]>parameterMap;
    private Map<String,String>headerMap;
    private Cookie[]cookies;
    private HttpSession session;
    private boolean forwarded;
    private Map<String, Object> attributesMap;
    public Request(Socket socket,Connector connector) throws IOException {
        this.socket=socket;
        this.connector=connector;
        this.parameterMap=new HashMap<>();
        this.headerMap=new HashMap<>();
        this.attributesMap=new HashMap<>();
        parseHttpResquest();
        if(StrUtil.isEmpty(requestString)){
            return;
        }
        parseUri();
        parseContext();
        if(!"/".equals(context.getPath())){
            uri=StrUtil.removePrefix(uri,context.getPath());
            if(StrUtil.isEmpty(uri))
                uri="/";
        }
        parseMethod();
        parseParameters();
        parseHeaders();
        parseCookies();
    }

    public void parseContext(){
        Service service=connector.getService();
        Engine engine = service.getEngine();
        context = engine.getDefaultHost().getContext(uri);
        if(null!=context)
            return;
        String path = StrUtil.subBetween(uri, "/", "/");
        if (null == path)
            path = "/";
        else
            path = "/" + path;
        context = engine.getDefaultHost().getContext(path);
        if (null == context)
            context = engine.getDefaultHost().getContext("/");
    }

    public void parseHttpResquest() throws IOException {
        InputStream is=this.socket.getInputStream();
        byte[]bytes= MiniBrowser.readBytes(is,false);
        requestString=new String(bytes,"utf-8");
    }

    private void parseUri(){
        String temp;
        temp=StrUtil.subBetween(requestString," "," "); //subBetween返回第一个匹配项，所以能得到Uri
        if(!StrUtil.contains(temp,'?')){
            uri=temp;
            return;
        }
        temp=StrUtil.subBefore(temp,'?',false);
        uri=temp;
    }

    private void parseParameters(){
        System.out.println(requestString);
        if("GET".equals(this.getMethod())){
            String url=StrUtil.subBetween(requestString," "," ");
            if(StrUtil.contains(url,'?')){
                queryString=StrUtil.subAfter(url,'?',false);
            }
        }
        if("POST".equals(this.getMethod())){
            queryString=StrUtil.subAfter(requestString,"\r\n\r\n",false);
        }
        if(null==queryString||0==queryString.length()){
            return;
        }
        queryString= URLUtil.decode(queryString);
        String[]parameterValues=queryString.split("&");
        if(parameterValues!=null){
            for (String parameterValue:parameterValues){
                String[]nameValues=parameterValue.split("=");
                String name=nameValues[0];
                String value=nameValues[1];
                String values[]=parameterMap.get(name);
                if(values==null){
                    values=new String[]{value};
                    parameterMap.put(name,values);
                }else {
                    values= ArrayUtil.append(values,value);
                    parameterMap.put(name,values);
                }
            }
        }
    }

    public void parseHeaders() {
        StringReader stringReader=new StringReader(requestString);
        List<String>lines=new ArrayList<>();
        IoUtil.readLines(stringReader,lines);
        for (int i = 1; i < lines.size() ; i++) {
            String line=lines.get(i);
            if(0==line.length()) break;
            String[]segs=line.split(":");
            String headerName=segs[0].toLowerCase();
            String headerValue=segs[1];
            headerMap.put(headerName,headerValue);
        }
    }

    private void parseCookies(){
        List<Cookie>cookieList=new ArrayList<>();
        String cookies=headerMap.get("cookie");
        if(cookies!=null){
            String[]pairs=StrUtil.split(cookies,";");
            for(String pair:pairs){
                if(StrUtil.isBlank(pair))
                   continue;
                String[] segs = StrUtil.split(pair, "=");
                String name = segs[0].trim();
                String value = segs[1].trim();
                Cookie cookie = new Cookie(name, value);
                cookieList.add(cookie);
            }
        }
        this.cookies = ArrayUtil.toArray(cookieList, Cookie.class);
    }

    private void parseMethod(){
        method = StrUtil.subBefore(requestString, " ", false);
    }

    public String getJSessionIdFromCookie() {
        if (null == cookies)
            return null;
        for (Cookie cookie : cookies) {
            if ("JSESSIONID".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public String getUri(){
        return uri;
    }

    public Context getContext() {
        return context;
    }

    public String getRequestString(){
        return requestString;
    }

    @Override
    public String getMethod() {
        return method;
    }

    public ServletContext getServletContext(){
        return context.getServletContext();
    }

    public String getRealPath(String path){
        return getServletContext().getRealPath(path);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameterMap;
    }

    public String getParameter(String name) {
        String values[] = parameterMap.get(name);
        if (null != values && 0 != values.length)
            return values[0];
        return null;
    }
    public Enumeration getParameterName(){
        return Collections.enumeration(parameterMap.keySet());
    }
    public String[]getParametersValues(String name){
        return parameterMap.get(name);
    }

    public String getHeader(String name) {
        if(null==name)
            return null;
        name = name.toLowerCase();
        return headerMap.get(name);
    }
    public Enumeration getHeaderNames() {
        Set keys = headerMap.keySet();
        return Collections.enumeration(keys);
    }
    public int getIntHeader(String name) {
        String value = headerMap.get(name);
        return Convert.toInt(value, 0);
    }

    public Cookie[] getCookies() {
        return cookies;
    }

    public HttpSession getSession() {
        return session;
    }
    public void setSession(HttpSession session) {
        this.session = session;
    }

    public Connector getConnector() {
        return connector;
    }

    public boolean isForwarded() {
        return forwarded;
    }
    public void setForwarded(boolean forwarded) {
        this.forwarded = forwarded;
    }
    public void setUri(String uri) {
        this.uri = uri;
    }
    public Socket getSocket() {
        return socket;
    }
    public RequestDispatcher getRequestDispatcher(String uri) {
        return new ApplicationRequestDispatcher(uri);
    }
    public void removeAttribute(String name) {
        attributesMap.remove(name);
    }
    public void setAttribute(String name, Object value) {
        attributesMap.put(name, value);
    }
    public Object getAttribute(String name) {
        return attributesMap.get(name);
    }
    public Enumeration<String> getAttributeNames() {
        Set<String> keys = attributesMap.keySet();
        return Collections.enumeration(keys);
    }

    @Override
    public String getRequestURI() {
        return this.uri;
    }
}
