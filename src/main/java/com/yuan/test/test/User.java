package com.yuan.test.test;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import lombok.Data;
import org.dom4j.Document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

@Data
@JsonFilter("userFilter")
public class User {
    private String username;
    private String password;
    private Integer age;

    public static void main(String[] args) throws IOException {
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.addFilter("userFilter",   //添加过滤器名称
                SimpleBeanPropertyFilter.serializeAllExcept("username", "password")); //这里指定不序列化的属性
/*        Set exclude = new HashSet();
        exclude.add("username");
        exclude.add("password");
        filterProvider.addFilter("userFilter",
                SimpleBeanPropertyFilter.serializeAllExcept(exclude)); //这里指定不序列化的属性也可以放到Set集合里面
        filterProvider.addFilter("userFilter",
                SimpleBeanPropertyFilter.serializeAll());  // serializeAll()序列化所有属性，
        filterProvider.addFilter("userFilter",
                SimpleBeanPropertyFilter.filterOutAllExcept("age")); //只序列化这里包含的属性*/
        ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(filterProvider);
        User user = new User();
        user.setUsername("小明");
        user.setPassword("123");
        user.setAge(18);
        String s = mapper.writer().withDefaultPrettyPrinter().writeValueAsString(user);
        System.out.println("我是序列化" + s);

        User user1 = mapper.readValue("{\"username\":\"小明\",\"password\":\"123\",\"age\":18}", User.class);
        System.out.println("我是反序列化" + user1);  //这里注意只是在序列化的时候过滤字段，在反序列化的时候是不过滤的
    }
    public void method1(int a,int b)

    {

    }
    public static Document parseXmlByString(String content) throws Exception
    {
        Object[] args={new HashMap<String,String>()};
        System.out.println();
        return null;
    }
    public void method2()
    {
        method1(1,(Integer)null);
    }


    //limit over

}

