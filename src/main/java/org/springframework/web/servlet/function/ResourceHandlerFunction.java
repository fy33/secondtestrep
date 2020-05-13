package org.springframework.web.servlet.function;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.EnumSet;
import java.util.Set;

class ResourceHandlerFunction implements HandlerFunction<ServerResponse> {
    private static final Set<HttpMethod> SUPPORTED_METHODS =
            EnumSet.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);

    private final Resource resource;

    public ResourceHandlerFunction(Resource resource) {
        this.resource = resource;
    }



    @Override
    public ServerResponse handle(ServerRequest request) {
        HttpMethod method = request.method();
        if (method != null) {
            switch (method) {
                case GET:
                    return EntityResponse.fromObject(this.resource).build();
                case HEAD:
                    Resource headResource = new HeadMethodResource(this.resource);
                    return EntityResponse.fromObject(headResource).build();
                case OPTIONS:
                    return ServerResponse.ok()
                            .allow(SUPPORTED_METHODS).build();
            }
        }
        return ServerResponse.status(HttpStatus.METHOD_NOT_ALLOWED)
                .allow(SUPPORTED_METHODS).build();
    }


    private static class HeadMethodResource implements Resource {

        private static final byte[] EMPTY = new byte[0];

        private final Resource delegate;

        public HeadMethodResource(Resource delegate) {
            this.delegate = delegate;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(EMPTY);
        }

        // delegation

        @Override
        public boolean exists() {
            return this.delegate.exists();
        }

        @Override
        public URL getURL() throws IOException {
            return this.delegate.getURL();
        }

        @Override
        public URI getURI() throws IOException {
            return this.delegate.getURI();
        }

        @Override
        public File getFile() throws IOException {
            return this.delegate.getFile();
        }

        @Override
        public long contentLength() throws IOException {
            return this.delegate.contentLength();
        }

        @Override
        public long lastModified() throws IOException {
            return this.delegate.lastModified();
        }

        @Override
        public Resource createRelative(String relativePath) throws IOException {
            return this.delegate.createRelative(relativePath);
        }

        @Override
        @Nullable
        public String getFilename() {
            return this.delegate.getFilename();
        }

        @Override
        public String getDescription() {
            return this.delegate.getDescription();
        }
    }

























}
