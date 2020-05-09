package org.springframework.core.io;

import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardOpenOption;

public abstract class AbstractFileResolvingResource extends AbstractResource {
    @Override
    public boolean exists() {
        try {
            URL url = getURL();
            if (ResourceUtils.isFileURL(url)) {
                // Proceed with file system resolution
                return getFile().exists();
            }
            else {
                // Try a URL connection content-length header
                URLConnection con = url.openConnection();
                customizeConnection(con);
                HttpURLConnection httpCon =
                        (con instanceof HttpURLConnection ? (HttpURLConnection) con : null);
                if (httpCon != null) {
                    int code = httpCon.getResponseCode();
                    if (code == HttpURLConnection.HTTP_OK) {
                        return true;
                    }
                    else if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                        return false;
                    }
                }
                if (con.getContentLengthLong() > 0) {
                    return true;
                }
                if (httpCon != null) {
                    // No HTTP OK status, and no content-length header: give up
                    httpCon.disconnect();
                    return false;
                }
                else {
                    // Fall back to stream existence: can we open the stream?
                    getInputStream().close();
                    return true;
                }
            }
        }
        catch (IOException ex) {
            return false;
        }
    }

    @Override
    public boolean isReadable() {
        try {
            URL url = getURL();
            if (ResourceUtils.isFileURL(url)) {
                // Proceed with file system resolution
                File file = getFile();
                return (file.canRead() && !file.isDirectory());
            }
            else {
                // Try InputStream resolution for jar resources
                URLConnection con = url.openConnection();
                customizeConnection(con);
                if (con instanceof HttpURLConnection) {
                    HttpURLConnection httpCon = (HttpURLConnection) con;
                    int code = httpCon.getResponseCode();
                    if (code != HttpURLConnection.HTTP_OK) {
                        httpCon.disconnect();
                        return false;
                    }
                }
                long contentLength = con.getContentLengthLong();
                if (contentLength > 0) {
                    return true;
                }
                else if (contentLength == 0) {
                    // Empty file or directory -> not considered readable...
                    return false;
                }
                else {
                    // Fall back to stream existence: can we open the stream?
                    getInputStream().close();
                    return true;
                }
            }
        }
        catch (IOException ex) {
            return false;
        }
    }



    @Override
    public boolean isFile() {
        try {
            URL url = getURL();
            if (url.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
                return VfsResourceDelegate.getResource(url).isFile();
            }
            return ResourceUtils.URL_PROTOCOL_FILE.equals(url.getProtocol());
        }
        catch (IOException ex) {
            return false;
        }
    }


    @Override
    public File getFile() throws IOException {
        URL url = getURL();
        if (url.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
            return VfsResourceDelegate.getResource(url).getFile();
        }
        return ResourceUtils.getFile(url, getDescription());
    }

    @Override
    protected File getFileForLastModifiedCheck() throws IOException {
        URL url = getURL();
        if (ResourceUtils.isJarURL(url)) {
            URL actualUrl = ResourceUtils.extractArchiveURL(url);
            if (actualUrl.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
                return VfsResourceDelegate.getResource(actualUrl).getFile();
            }
            return ResourceUtils.getFile(actualUrl, "Jar URL");
        }
        else {
            return getFile();
        }
    }



    protected boolean isFile(URI uri) {
        try {
            if (uri.getScheme().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
                return VfsResourceDelegate.getResource(uri).isFile();
            }
            return ResourceUtils.URL_PROTOCOL_FILE.equals(uri.getScheme());
        }
        catch (IOException ex) {
            return false;
        }
    }

    protected File getFile(URI uri) throws IOException {
        if (uri.getScheme().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
            return VfsResourceDelegate.getResource(uri).getFile();
        }
        return ResourceUtils.getFile(uri, getDescription());
    }


    @Override
    public ReadableByteChannel readableChannel() throws IOException {
        try {
            // Try file system channel
            return FileChannel.open(getFile().toPath(), StandardOpenOption.READ);
        }
        catch (FileNotFoundException | NoSuchFileException ex) {
            // Fall back to InputStream adaptation in superclass
            return super.readableChannel();
        }
    }












































}
