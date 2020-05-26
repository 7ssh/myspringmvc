package com.ssh.core;

import com.ssh.core.annotation.MyAutowired;
import com.ssh.core.annotation.MyComponent;
import com.ssh.core.annotation.MyRequestMapping;
import com.ssh.core.handler.MyRequestHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MyDispatcherServlet extends HttpServlet {

    static String configLocation;//配置文件的路径
    static Properties prop = new Properties();//配置文件信息
    private static Set<String> classes = new HashSet<String>();//所有需要创建对象的类放到HashSet集合中
    private static Map<String, Object> ioc = new HashMap<String, Object>();//IoC容器
    private static Map<String, MyRequestHandler> handlerMapping = new HashMap<>();//映射器

    @Override
    public void init() throws ServletException {
        //1.加载配置文件
        configLocation = this.getInitParameter("configLocation");
        loadConfig(configLocation);
        //2.包扫描
        packageScanner();
        //3.初始化ioc容器
        initIoC();
        //4.自动装配对象
        autowired();
        //5.处理映射器
        initHandlerMapping();
    }

    /**
     * 加载配置文件
     *
     * @param configLocation
     */
    private void loadConfig(String configLocation) {
        InputStream in = MyDispatcherServlet.class.getClassLoader().getResourceAsStream(configLocation);
        try {
            prop.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 扫描包下面的所有类
     */
    private void packageScanner() {
        String packeg = prop.getProperty("basePackage");//获取需要扫描的包
        packeg = packeg.replaceAll("\\.", "\\\\");
        Enumeration<URL> url;
        try {
            url = MyDispatcherServlet.class.getClassLoader().getResources(packeg);
            while (url.hasMoreElements()) {
                URL u = url.nextElement();
                // 找到需要扫描注解的包
                String path = u.getFile();
                // 递归找文件 .class文件
                searchClass(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 递归查找文件
     */
    private void searchClass(String path) {
        File file = new File(path);
        if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            for (File file2 : listFiles) {
                // 如果是个文件 放入 set中
                if (file2.isFile()) {
                    String classPath = file2.getAbsolutePath();
                    String packeg = prop.getProperty("basePackage");
                    packeg = packeg.replaceAll("\\.", "\\\\");
                    String className = classPath.substring(classPath.indexOf(packeg), classPath.lastIndexOf(".class"));
                    className = className.replaceAll("\\\\", "\\.");
                    classes.add(className);
                } else {
                    // 如果是文件夹 则继续循环
                    searchClass(file2.getAbsolutePath());
                }
            }
        } else {
            String name = file.getName();
            if (name.endsWith(".class")) {
                String absolutePath = file.getAbsolutePath();
            }
        }
    }

    /**
     * 初始化IoC容器
     */
    private void initIoC() {
        //1.遍历所有的class
        for (String string : classes) {
            try {
                Class<?> cls = Class.forName(string);
                if (cls.isInterface()) {//如果是一个interface就直接跳过
                    continue;
                }
                //如果是一个class就检查是否存在MyComponent注解
                if (cls.isAnnotationPresent(MyComponent.class)) {
                    //存在就创建这个类的对象
                    Object obj = cls.newInstance();
                    //如果这个类没有接口则直接使用类的完全限定名作为key
                    String name = cls.getName();
                    ioc.put(name, obj);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 自动装配对象
     */
    private void autowired() {
        Set<String> keys = ioc.keySet();//得到ioc容器的所有key
        for (String key : keys) {
            Object object = ioc.get(key);//根据key得到对应对象
            Field[] fields = object.getClass().getDeclaredFields();//获取这个对象的所有属性
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                //判断每个属性是否存在MyAutowired注解
                if (field.isAnnotationPresent(MyAutowired.class)) {
                    Class<?> type = field.getType();//获取属性的类型
                    //从ioc容器中找对应属性
                    Object obj = searchSubClass(type);
                    try {
                        field.setAccessible(true);
                        if (obj != null) {
                            field.set(object, obj);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 判断这个类(接口)是否有子类（实现类）
     */
    private Object searchSubClass(Class<?> cls) {
        //获得ioc容器中的所有对象
        Collection<Object> objects = ioc.values();
        for (Object object : objects) {
            Class<?> obj = object.getClass();
            /**
             * 判定此 cls对象所表示的类或接口与指定的Class参数所表示的类或接口是否相同，
             * 或是否是其超类或超接口。如果是则返回 true；否则返回 false。
             */
            if (cls.isAssignableFrom(obj)) {
                return object;
            }
        }
        return null;
    }

    /**
     * 处理映射器
     */
    private void initHandlerMapping() {
        for (String string : classes) {
            try {
                Class<?> cls = Class.forName(string);
                //判断哪些类上有MyRequestMapping注解
                if (cls.isAnnotationPresent(MyRequestMapping.class)) {
                    // 获取类的全路径
                    String name = cls.getName();
                    // 获取类上面的url
                    String baseUrl = cls.getAnnotation(MyRequestMapping.class).value();
                    Method[] method = cls.getDeclaredMethods();
                    for (Method method2 : method) {
                        //判断哪些方法上有MyRequestMapping注解
                        if (method2.isAnnotationPresent(MyRequestMapping.class)) {
                            // 获取方法上的URL
                            String methodUrl = method2.getAnnotation(MyRequestMapping.class).value();
                            //得到完整的url
                            String uri = baseUrl + methodUrl;
                            MyRequestHandler MyRequestHandler = new MyRequestHandler();
                            MyRequestHandler.setMethod(method2);
                            MyRequestHandler.setObj(ioc.get(name));
                            //成功初始化映射器
                            handlerMapping.put(uri, MyRequestHandler);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 重写service()处理请求
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String contextPath = req.getContextPath();
        String uri = req.getRequestURI();
        uri = uri.replace(contextPath, "");
        // 从映射器中查找处理器
        MyRequestHandler MyRequestHandler = handlerMapping.get(uri);
        if (MyRequestHandler == null) {//如果没有处理器就是404
            resp.getWriter().print("404");
            return;
        }
        Method method = MyRequestHandler.getMethod();
        try {
            method.setAccessible(true);
            Object rs = method.invoke(MyRequestHandler.getObj());//rs是方法执行后的返回值
            //视图解析器
            viewResolver(rs, req, resp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 视图解析器
     */
    private void viewResolver(Object obj, HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        //判断地址是否是String类型
        if (obj instanceof String) {
            String path = (String) obj;
            if (path.startsWith("redirect:")) {//判断是否为重定向
                path = path.replace("redirect:", "");
                sendRedirect(path, req, resp);
                return;
            }
            forwared(path, req, resp);
        }
    }

    //重定向方法
    private void sendRedirect(String path, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(path);
    }

    //内部转发方法
    private void forwared(String path, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher(path).forward(req, resp);
    }

}
