package cn.ycx.ycxtomcat.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import cn.ycx.ycxtomcat.classloader.WebappClassLoader;
import cn.ycx.ycxtomcat.exception.WebConfigDuplicatedException;
import cn.ycx.ycxtomcat.http.ApplicationContext;
import cn.ycx.ycxtomcat.http.StandardServletConfig;
import cn.ycx.ycxtomcat.util.ContextXmlUtil;
import cn.ycx.ycxtomcat.watcher.ContextFileChangeWatcher;
import org.apache.jasper.JspC;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.jsoup.Jsoup;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.util.*;

/**
 * 代表一个应用
 */
public class Context {
    private String path;
    private String docBase;
    private File contextWebXmlFile;

    private Map<String,String>url_servletClassName;
    private Map<String,String>url_servletName;
    private Map<String,String>servletName_className;
    private Map<String,String>className_servletName;
    private Map<String,Map<String,String>>servlet_className_init_params;

    private Map<String, List<String>> url_filterClassName;
    private Map<String, List<String>> url_FilterNames;
    private Map<String, String> filterName_className;
    private Map<String, String> className_filterName;
    private Map<String, Map<String, String>> filter_className_init_params;

    private WebappClassLoader webappClassLoader;

    private Host host;
    private boolean reloadable;
    private ContextFileChangeWatcher contextFileChangeWatcher;

    private ServletContext servletContext;
    private Map<Class<?>, HttpServlet>servletPool;
    private Map<String, Filter> filterPool;

    private List<String>loadOnStartupServletClassNames;
    private List<ServletContextListener> listeners;

    public Context(String path, String docBase,Host host,boolean reloadable) throws WebConfigDuplicatedException {
        TimeInterval timeInterval=DateUtil.timer();
        this.host=host;
        this.reloadable=reloadable;

        this.path = path;
        this.docBase = docBase;
        this.contextWebXmlFile=new File(docBase, ContextXmlUtil.getWatchedResource());

        this.url_servletClassName=new HashMap<>();
        this.url_servletName=new HashMap<>();
        this.servletName_className=new HashMap<>();
        this.className_servletName=new HashMap<>();
        this.servlet_className_init_params=new HashMap<>();

        this.url_filterClassName = new HashMap<>();
        this.url_FilterNames = new HashMap<>();
        this.filterName_className = new HashMap<>();
        this.className_filterName = new HashMap<>();
        this.filter_className_init_params = new HashMap<>();

        this.servletContext=new ApplicationContext(this);
        this.servletPool=new HashMap<>();
        this.filterPool = new HashMap<>();

        this.loadOnStartupServletClassNames=new ArrayList<>();
        this.listeners=new ArrayList<ServletContextListener>();

        ClassLoader commonClassLoader=Thread.currentThread().getContextClassLoader();;
        this.webappClassLoader=new WebappClassLoader(docBase,commonClassLoader);
        LogFactory.get().info("Deploying web application directory {}", this.docBase);
        deploy();
        LogFactory.get().info("Deployment of web application directory {} has finished in {} ms.",this.getDocBase(),timeInterval.intervalMs());
    }

    public void reload()  {
        try {
            host.reload(this);
        } catch (WebConfigDuplicatedException e) {
            e.printStackTrace();
        }
    }

    private void deploy() throws WebConfigDuplicatedException {
        init();
        loadListeners();
        if(reloadable){
            contextFileChangeWatcher=new ContextFileChangeWatcher(this);
            contextFileChangeWatcher.start();
        }
        JspC c = new JspC();
        new JspRuntimeContext(servletContext, c);
    }

    private void init() throws WebConfigDuplicatedException {
        if(!contextWebXmlFile.exists()){
            return;
        }
        checkDuplicated(); //检查是否有重复声明的Servlet
        String xml= FileUtil.readUtf8String(contextWebXmlFile);
        Document d= Jsoup.parse(xml);
        parseServletMapping(d);
        parseServletInitParams(d);
        parseFilterMapping(d);
        parseFilterInitParams(d);

        parseLoadOnStartup(d);
        handleLoadOnStartUp();
        initFilter();
        fireEvent("init");
    }

    public void addListener(ServletContextListener listener){
        listeners.add(listener);
    }

    private void loadListeners()  {
        try {
            if(!contextWebXmlFile.exists())
                return;
            String xml = FileUtil.readUtf8String(contextWebXmlFile);
            Document d = Jsoup.parse(xml);

            Elements es = d.select("listener listener-class");
            for (Element e : es) {
                String listenerClassName = e.text();

                Class<?> clazz= this.getWebClassLoader().loadClass(listenerClassName);
                ServletContextListener listener = (ServletContextListener) clazz.newInstance();
                addListener(listener);
            }
        } catch (IORuntimeException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void fireEvent(String type) {
        ServletContextEvent event = new ServletContextEvent(servletContext);
        for (ServletContextListener servletContextListener : listeners) {
            if("init".equals(type))
                servletContextListener.contextInitialized(event);
            if("destroy".equals(type))
                servletContextListener.contextDestroyed(event);
        }
    }

    private void parseServletMapping(Document d){
        //url_servletName
        Elements mappingUrlElements=d.select("servlet-mapping url-pattern");
        for (Element mappingUrlElement:mappingUrlElements) {
            String urlPattern=mappingUrlElement.text();
            String servletName=mappingUrlElement.parent().select("servlet-name").first().text();
            url_servletName.put(urlPattern,servletName);
        }
        //servletName_className / className_servletName
        Elements servletNameElements=d.select("servlet servlet-name");
        for (Element servletNameElement:servletNameElements) {
            String servletName=servletNameElement.text();
            String servletClass=servletNameElement.parent().select("servlet-class").first().text();
            servletName_className.put(servletName,servletClass);
            className_servletName.put(servletClass,servletName);
        }
        //url_servletClassName
        Set<String>urls=url_servletName.keySet();
        for (String url: urls) {
            String servletName=url_servletName.get(url);
            String servletClassName=servletName_className.get(servletName);
            url_servletClassName.put(url,servletClassName);
        }
    }

    public void parseFilterMapping(Document d) {
        // filter_url_name
        Elements mappingurlElements = d.select("filter-mapping url-pattern");
        for (Element mappingurlElement : mappingurlElements) {
            String urlPattern = mappingurlElement.text();
            String filterName = mappingurlElement.parent().select("filter-name").first().text();

            List<String> filterNames= url_FilterNames.get(urlPattern);
            if(null==filterNames) {
                filterNames = new ArrayList<>();
                url_FilterNames.put(urlPattern, filterNames);
            }
            filterNames.add(filterName);
        }
        // class_name_filter_name
        Elements filterNameElements = d.select("filter filter-name");
        for (Element filterNameElement : filterNameElements) {
            String filterName = filterNameElement.text();
            String filterClass = filterNameElement.parent().select("filter-class").first().text();
            filterName_className.put(filterName, filterClass);
            className_filterName.put(filterClass, filterName);
        }
        // url_filterClassName

        Set<String> urls = url_FilterNames.keySet();
        for (String url : urls) {
            List<String> filterNames = url_FilterNames.get(url);
            if(null == filterNames) {
                filterNames = new ArrayList<>();
                url_FilterNames.put(url, filterNames);
            }
            for (String filterName : filterNames) {
                String filterClassName = filterName_className.get(filterName);
                List<String> filterClassNames = url_filterClassName.get(url);
                if(null==filterClassNames) {
                    filterClassNames = new ArrayList<>();
                    url_filterClassName.put(url, filterClassNames);
                }
                filterClassNames.add(filterClassName);
            }
        }
    }

    private void parseFilterInitParams(Document d) {
        Elements filterClassNameElements = d.select("filter-class");
        for (Element filterClassNameElement : filterClassNameElements) {
            String filterClassName = filterClassNameElement.text();

            Elements initElements = filterClassNameElement.parent().select("init-param");
            if (initElements.isEmpty())
                continue;

            Map<String, String> initParams = new HashMap<>();

            for (Element element : initElements) {
                String name = element.select("param-name").get(0).text();
                String value = element.select("param-value").get(0).text();
                initParams.put(name, value);
            }

            filter_className_init_params.put(filterClassName, initParams);
        }
    }

    public void checkDuplicated() throws WebConfigDuplicatedException {
        String xml=FileUtil.readUtf8String(contextWebXmlFile);
        Document d=Jsoup.parse(xml);
        checkDuplicated(d,"servlet-mapping url-pattern", "servlet url 重复，请保持其唯一性：{}");
        checkDuplicated(d,"servlet  servlet-name", "servlet 名称重复，请保持其唯一性：{}");
        checkDuplicated(d,"servlet  servlet-class", "servlet 类名重复，请保持其唯一性：{}");
    }

    private void checkDuplicated(Document d,String mapping, String desc) throws WebConfigDuplicatedException {
        Elements elements=d.select(mapping);
        List<String>contents=new ArrayList<>();
        for (Element e:elements) {
            contents.add(e.text());
        }
        Collections.sort(contents);
        for (int i = 0; i <contents.size()-1 ; i++) {
            String contentPre=contents.get(i);
            String contentNext=contents.get(i+1);
            if(contentPre.equals(contentNext)){
                throw new WebConfigDuplicatedException(StrUtil.format(desc,contentPre));
            }
        }
    }

    public synchronized HttpServlet getServlet(Class<?>clazz) throws IllegalAccessException, InstantiationException, ServletException {
        HttpServlet servlet=servletPool.get(clazz);
        if(servlet==null){
            servlet=(HttpServlet)clazz.newInstance();
            //servlet单例放进单例池之前做初始化
            ServletContext servletContext=this.getServletContext();
            String className=clazz.getName();
            String servletName=className_servletName.get(className);
            Map<String,String>initParameters=servlet_className_init_params.get(className);
            ServletConfig servletConfig=new StandardServletConfig(servletContext,servletName,initParameters);
            servlet.init(servletConfig);
            servletPool.put(clazz,servlet);
        }
        return servlet;
    }

    private void parseServletInitParams(Document d){
        Elements servletClassNameElements=d.select("servlet-class");
        for(Element servletClassNameElement: servletClassNameElements){
            String servletClassName=servletClassNameElement.text();
            Elements initElements=servletClassNameElement.parent().select("init-param");
            if(initElements.isEmpty()) continue;
            Map<String,String>initParams=new HashMap<>();
            for(Element element:initElements){
                String name=element.select("param-name").get(0).text();
                String value=element.select("param-value").get(0).text();
                initParams.put(name,value);
            }
            servlet_className_init_params.put(servletClassName,initParams);
        }
    }

    private void initFilter() {
        Set<String> classNames = className_filterName.keySet();
        for (String className : classNames) {
            try {
                Class clazz =  this.getWebClassLoader().loadClass(className);
                Map<String,String> initParameters = filter_className_init_params.get(className);
                String filterName = className_filterName.get(className);

                FilterConfig filterConfig = new StandardFilterConfig(servletContext, filterName, initParameters);

                Filter filter = filterPool.get(clazz);
                if(null==filter) {
                    filter = (Filter) ReflectUtil.newInstance(clazz);
                    filter.init(filterConfig);
                    filterPool.put(className, filter);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean match(String pattern, String uri) {
        // 完全匹配
        if(StrUtil.equals(pattern, uri))
            return true;
        // /* 模式
        if(StrUtil.equals(pattern, "/*"))
            return true;
        // 后缀名 /*.jsp
        if(StrUtil.startWith(pattern, "/*.")) {
            String patternExtName = StrUtil.subAfter(pattern, '.', false);
            String uriExtName = StrUtil.subAfter(uri, '.', false);
            if(StrUtil.equals(patternExtName, uriExtName))
                return true;
        }
        // 其他模式就懒得管了
        return false;
    }

    public List<Filter> getMatchedFilters(String uri) {
        List<Filter> filters = new ArrayList<>();
        Set<String> patterns = url_filterClassName.keySet();
        Set<String> matchedPatterns = new HashSet<>();
        for (String pattern : patterns) {
            if(match(pattern,uri)) {
                matchedPatterns.add(pattern);
            }
        }
        Set<String> matchedFilterClassNames = new HashSet<>();
        for (String pattern : matchedPatterns) {
            List<String> filterClassName = url_filterClassName.get(pattern);
            matchedFilterClassNames.addAll(filterClassName);
        }
        for (String filterClassName : matchedFilterClassNames) {
            Filter filter = filterPool.get(filterClassName);
            filters.add(filter);
        }
        return filters;
    }

    private void parseLoadOnStartup(Document d){
        Elements es=d.select("load-on-startup");
        for(Element e:es){
            String loadOnStartUpServletClassName=e.parent().select("servlet-class").text();
            loadOnStartupServletClassNames.add(loadOnStartUpServletClassName);
        }
    }

    public void handleLoadOnStartUp(){
        for(String loadOnStartUpServletClassName:loadOnStartupServletClassNames){
            try {
                Class<?>clazz=webappClassLoader.loadClass(loadOnStartUpServletClassName);
                getServlet(clazz);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ServletException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

    public void destroyServlets(){
        Collection<HttpServlet> servlets = servletPool.values();
        for (HttpServlet servlet : servlets) {
            servlet.destroy();
        }
    }

    public void stop(){
        webappClassLoader.stop();
        contextFileChangeWatcher.stop();
        destroyServlets();
        fireEvent("destroy");
    }

    public String getPath(){
        return path;
    }
    public void setPath(){
        this.path=path;
    }

    public String getDocBase(){
        return docBase;
    }
    public void setDocBase(String docBase){
        this.docBase=docBase;
    }

    public String getServletClassName(String uri){
        return url_servletClassName.get(uri);
    }

    public WebappClassLoader getWebClassLoader() {
        return webappClassLoader;
    }

    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }
    public boolean isReloadable(){
        return reloadable;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

}
