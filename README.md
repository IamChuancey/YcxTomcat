# YcxTomcat
## Tomcat和外界
HTTP请求 --> Tomcat --> HTTP响应

## Tomcat里有什么
- Servlet
- Filter
- Listener

## Tomcat的主运行流程
```java
class Tomcat{
  public static void main(String[] args){
      //0.项目启动，创建ServletContext
      //1.读取xml文件，反射创建Listener
      //2.ServletContext注入ServletContextListener
      //3.调用servletContext的init方法，init()调用listener.contextInitialized方法
      //4.读取web.xml,反射创建Filter
      //5.把所有的Filter放进一个FilterChain
      .....wait for request....
     //6.读取web.xml,反射创建Servlet,解析HTTP请求，塞入Request,并创建空的Response，把它们塞入Servlet
     //7.循环FilterChain,执行每一个Filter的doFilter方法，
     //8. servlet处理请求，输出结果
  }
}

```

## Tomcat的架构体系
从``Tomcat的主运行流程``可以看出，Tomcat最核心的是Servlet,作为一个成熟的框架，Tomcat肯定不是简单的一堆Servlet的排列，肯定会抽象出自己的层次出来。在YcxTomcat里，我们抽象出了以下层次：
- Server
- Service
- Engine
- Host
- Context
```xml
<Server>
    <Service name="Catalina">
        <Engine defaultHost="localhost">
            <Host name="localhost">
                <Context path="/b" docBase="/Users/chuancey/Idea-Workspace/YcxTomcat/webapps/b" />
                <Context path="/webforycxtomcat" docBase="/Users/chuancey/Idea-Workspace/webforycxtomcat/web" reloadable="true"/>
            </Host>
        </Engine>
    </Service>
</Server>
```
## 如何写一个Servlet
Servlet主要由以下三部分组成：
- ServletConfig、 Servlet配置(web.xml)
- ServletRequest、
- ServletResponse

### 那么YcxTomcat主要做了什么
- 重现Tomcat的架构体系
- 重写Servlet、Listener、Filter


