package com.yuan.test.test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Map;

public interface ServletRequest {


    public Object getAttribute(String name);

    public Enumeration<String> getAttributeNames();
    public String getCharEncoding();
    public void setCharEncoding(String env) throws UnsupportedEncodingException;
    public int getContentLength();
    public long getContentLengthLong();
    public String getContentType();
    public String getParameter(String name);
    public Enumeration<String> getParameterNames();
    public Map<String,String[]> getParamterMap();
    public String getProtocol();
}
