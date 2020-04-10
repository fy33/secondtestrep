package org.springframework.boot.system;

import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class ApplicationHome {
    private final File source;
    private final File dir;

    public ApplicationHome() {
        this(null);
    }

    public ApplicationHome(Class<?> sourceClass) {
        this.source = findSource((sourceClass != null) ? sourceClass : getStartClass());
        this.dir = findHomeDir(this.source);
    }

    private File findHomeDir(File source)
    {
        File homeDir=source;
        homeDir=(homeDir!=null)?homeDir:findDefaultHomeDir();
        if(homeDir.isFile())
        {
            homeDir=homeDir.getParentFile();
        }
        homeDir=homeDir.exists()?homeDir:new File(".");
        return homeDir.getAbsoluteFile();
    }
    private File findDefaultHomeDir()
    {
        String userDir=System.getProperty("user.dir");
        return new File(StringUtils.hasLength(userDir)?userDir:".");
    }
    private Class<?> getStartClass() {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            return getStartClass(classLoader.getResources("META-INF/MANIFEST.MF"));
        } catch (Exception ex) {
            return null;
        }
    }

    private Class<?> getStartClass(Enumeration<URL> manifestResources) {
        while (manifestResources.hasMoreElements()) {
            try (InputStream inputStream = manifestResources.nextElement().openStream()) {
                Manifest manifest = new Manifest(inputStream);
                String startClass = manifest.getMainAttributes().getValue("Start-Class");
                if (startClass != null) {
                    return ClassUtils.forName(startClass, getClass().getClassLoader());
                }
            } catch (Exception ex) {

            }
        }
        return null;
    }

    private File findSource(Class<?> sourceClass) {
        try {
            ProtectionDomain domain = (sourceClass != null) ? sourceClass.getProtectionDomain()
                    : null;
            CodeSource codeSource = (domain != null) ? domain.getCodeSource() : null;
            URL location = (codeSource != null) ? codeSource.getLocation() : null;
            File source = (location != null) ? findSource(location) : null;
            if (source != null && source.exists() && !isUnitTest()) {
                return source.getAbsoluteFile();
            }
            return null;
        }catch (Exception ex)
        {
            return null;
        }
    }

    private boolean isUnitTest()
    {
        try{
            StackTraceElement[] stackTrace=Thread.currentThread().getStackTrace();
            for(int i=stackTrace.length-1;i>=0;i--)
            {
                if(stackTrace[i].getClassName().startsWith("org.junit.")){
                    return true;
                }
            }
        }catch (Exception ex)
        {
        }
        return false;
    }
    private File findSource(URL location) throws IOException {
        URLConnection connection = location.openConnection();
        if (connection instanceof JarURLConnection) {
            return getRootJarFile(((JarURLConnection) connection).getJarFile());
        }
        return new File(location.getPath());
    }

    private File getRootJarFile(JarFile jarFile)
    {
        String name=jarFile.getName();
        int separator=name.indexOf("!/");
        if(separator>0)
        {
            name=name.substring(0,separator);
        }
        return new File(name);
    }
    public File getSource()
    {
        return this.source;
    }


}
