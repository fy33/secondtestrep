package org.springframework.web.servlet.resource;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;

@Deprecated
public class GzipResourceResolver extends AbstractResourceResolver {
    @Override
    protected Resource resolveResourceInternal(@Nullable HttpServletRequest request, String requestPath,
                                               List<? extends Resource> locations, ResourceResolverChain chain) {

        Resource resource = chain.resolveResource(request, requestPath, locations);
        if (resource == null || (request != null && !isGzipAccepted(request))) {
            return resource;
        }

        try {
            Resource gzipped = new GzippedResource(resource);
            if (gzipped.exists()) {
                return gzipped;
            }
        }
        catch (IOException ex) {
            logger.trace("No gzip resource for [" + resource.getFilename() + "]", ex);
        }

        return resource;
    }

    private boolean isGzipAccepted(HttpServletRequest request) {
        String value = request.getHeader("Accept-Encoding");
        return (value != null && value.toLowerCase().contains("gzip"));
    }

    @Override
    protected String resolveUrlPathInternal(String resourceUrlPath,
                                            List<? extends Resource> locations, ResourceResolverChain chain) {

        return chain.resolveUrlPath(resourceUrlPath, locations);
    }


    /**
     * A gzipped {@link HttpResource}.
     */
    static final class GzippedResource extends AbstractResource implements HttpResource {

        private final Resource original;

        private final Resource gzipped;

        public GzippedResource(Resource original) throws IOException {
            this.original = original;
            this.gzipped = original.createRelative(original.getFilename() + ".gz");
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return this.gzipped.getInputStream();
        }

        @Override
        public boolean exists() {
            return this.gzipped.exists();
        }

        @Override
        public boolean isReadable() {
            return this.gzipped.isReadable();
        }

        @Override
        public boolean isOpen() {
            return this.gzipped.isOpen();
        }

        @Override
        public boolean isFile() {
            return this.gzipped.isFile();
        }

        @Override
        public URL getURL() throws IOException {
            return this.gzipped.getURL();
        }

        @Override
        public URI getURI() throws IOException {
            return this.gzipped.getURI();
        }

        @Override
        public File getFile() throws IOException {
            return this.gzipped.getFile();
        }

        @Override
        public long contentLength() throws IOException {
            return this.gzipped.contentLength();
        }

        @Override
        public long lastModified() throws IOException {
            return this.gzipped.lastModified();
        }

        @Override
        public Resource createRelative(String relativePath) throws IOException {
            return this.gzipped.createRelative(relativePath);
        }

        @Override
        @Nullable
        public String getFilename() {
            return this.original.getFilename();
        }

        @Override
        public String getDescription() {
            return this.gzipped.getDescription();
        }

        @Override
        public HttpHeaders getResponseHeaders() {
            HttpHeaders headers = (this.original instanceof HttpResource ?
                    ((HttpResource) this.original).getResponseHeaders() : new HttpHeaders());
            headers.add(HttpHeaders.CONTENT_ENCODING, "gzip");
            headers.add(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);
            return headers;
        }
    }
}
