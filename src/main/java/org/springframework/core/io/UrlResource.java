package org.springframework.core.io;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;

public class UrlResource extends AbstractFileResolvingResource {
    @Nullable
    private final URI uri;

    private final URL url;


    private final URL cleanedUrl;

    public UrlResource(URI uri) throws MalformedURLException {
        Assert.notNull(uri, "URI must not be null");
        this.uri = uri;
        this.url = uri.toURL();
        this.cleanedUrl = getCleanedUrl(this.url, uri.toString());
    }

    public UrlResource(URL url) {
        Assert.notNull(url, "URL must not be null");
        this.url = url;
        this.cleanedUrl = getCleanedUrl(this.url, url.toString());
        this.uri = null;
    }

    public UrlResource(String path) throws MalformedURLException {
        Assert.notNull(path, "Path must not be null");
        this.uri = null;
        this.url = new URL(path);
        this.cleanedUrl = getCleanedUrl(this.url, path);
    }

    public UrlResource(String protocol, String location) throws MalformedURLException  {
        this(protocol, location, null);
    }



    public UrlResource(String protocol, String location, @Nullable String fragment) throws MalformedURLException  {
        try {
            this.uri = new URI(protocol, location, fragment);
            this.url = this.uri.toURL();
            this.cleanedUrl = getCleanedUrl(this.url, this.uri.toString());
        }
        catch (URISyntaxException ex) {
            MalformedURLException exToThrow = new MalformedURLException(ex.getMessage());
            exToThrow.initCause(ex);
            throw exToThrow;
        }
    }

    private URL getCleanedUrl(URL originalUrl, String originalPath) {
        String cleanedPath = StringUtils.cleanPath(originalPath);
        if (!cleanedPath.equals(originalPath)) {
            try {
                return new URL(cleanedPath);
            }
            catch (MalformedURLException ex) {
                // Cleaned URL path cannot be converted to URL -> take original URL.
            }
        }
        return originalUrl;
    }


    @Override
    public InputStream getInputStream() throws IOException {
        URLConnection con = this.url.openConnection();
        ResourceUtils.useCachesIfNecessary(con);
        try {
            return con.getInputStream();
        }
        catch (IOException ex) {
            // Close the HTTP connection (if applicable).
            if (con instanceof HttpURLConnection) {
                ((HttpURLConnection) con).disconnect();
            }
            throw ex;
        }
    }

    @Override
    public URL getURL() {
        return this.url;
    }


    @Override
    public URI getURI() throws IOException {
        if (this.uri != null) {
            return this.uri;
        }
        else {
            return super.getURI();
        }
    }

    @Override
    public boolean isFile() {
        if (this.uri != null) {
            return super.isFile(this.uri);
        }
        else {
            return super.isFile();
        }
    }


    @Override
    public File getFile() throws IOException {
        if (this.uri != null) {
            return super.getFile(this.uri);
        }
        else {
            return super.getFile();
        }
    }


    @Override
    public Resource createRelative(String relativePath) throws MalformedURLException {
        return new UrlResource(createRelativeURL(relativePath));
    }


    protected URL createRelativeURL(String relativePath) throws MalformedURLException {
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        // # can appear in filenames, java.net.URL should not treat it as a fragment
        relativePath = StringUtils.replace(relativePath, "#", "%23");
        // Use the URL constructor for applying the relative path as a URL spec
        return new URL(this.url, relativePath);
    }

    @Override
    public String getFilename() {
        return StringUtils.getFilename(this.cleanedUrl.getPath());
    }

    @Override
    public String getDescription() {
        return "URL [" + this.url + "]";
    }





    @Override
    public boolean equals(@Nullable Object other) {
        return (this == other || (other instanceof UrlResource &&
                this.cleanedUrl.equals(((UrlResource) other).cleanedUrl)));
    }


    @Override
    public int hashCode() {
        return this.cleanedUrl.hashCode();
    }


























}
