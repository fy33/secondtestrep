package org.springframework.web.servlet.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.ResourceRegionHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.PathExtensionContentNegotiationStrategy;
import org.springframework.web.accept.ServletPathExtensionContentNegotiationStrategy;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.support.WebContentGenerator;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

public class ResourceHttpRequestHandler extends WebContentGenerator implements HttpRequestHandler,
        EmbeddedValueResolverAware, InitializingBean, CorsConfigurationSource {


    private static final Log logger = LogFactory.getLog(ResourceHttpRequestHandler.class);

    private static final String URL_RESOURCE_CHARSET_PREFIX = "[charset=";


    private final List<String> locationValues = new ArrayList<>(4);

    private final List<Resource> locations = new ArrayList<>(4);


    private final Map<Resource, Charset> locationCharsets = new HashMap<>(4);

    private final List<ResourceResolver> resourceResolvers = new ArrayList<>(4);

    private final List<ResourceTransformer> resourceTransformers = new ArrayList<>(4);

    @Nullable
    private ResourceResolverChain resolverChain;


    @Nullable
    private ResourceTransformerChain transformerChain;

    @Nullable
    private ResourceHttpMessageConverter resourceHttpMessageConverter;


    @Nullable
    private ResourceRegionHttpMessageConverter resourceRegionHttpMessageConverter;

    @Nullable
    private ContentNegotiationManager contentNegotiationManager;

    @Nullable
    private PathExtensionContentNegotiationStrategy contentNegotiationStrategy;


    @Nullable
    private CorsConfiguration corsConfiguration;


    @Nullable
    private UrlPathHelper urlPathHelper;


    @Nullable
    private StringValueResolver embeddedValueResolver;

    public ResourceHttpRequestHandler() {
        super(HttpMethod.GET.name(), HttpMethod.HEAD.name());
    }

    public void setLocationValues(List<String> locationValues) {
        Assert.notNull(locationValues, "Location values list must not be null");
        this.locationValues.clear();
        this.locationValues.addAll(locationValues);
    }


    public void setLocations(List<Resource> locations) {
        Assert.notNull(locations, "Locations list must not be null");
        this.locations.clear();
        this.locations.addAll(locations);
    }

    public List<Resource> getLocations() {
        return this.locations;
    }

    public void setResourceResolvers(@Nullable List<ResourceResolver> resourceResolvers) {
        this.resourceResolvers.clear();
        if (resourceResolvers != null) {
            this.resourceResolvers.addAll(resourceResolvers);
        }
    }

    public List<ResourceResolver> getResourceResolvers() {
        return this.resourceResolvers;
    }

    public void setResourceTransformers(@Nullable List<ResourceTransformer> resourceTransformers) {
        this.resourceTransformers.clear();
        if (resourceTransformers != null) {
            this.resourceTransformers.addAll(resourceTransformers);
        }
    }


    public List<ResourceTransformer> getResourceTransformers() {
        return this.resourceTransformers;
    }

    public void setResourceHttpMessageConverter(@Nullable ResourceHttpMessageConverter messageConverter) {
        this.resourceHttpMessageConverter = messageConverter;
    }

    @Nullable
    public ResourceHttpMessageConverter getResourceHttpMessageConverter() {
        return this.resourceHttpMessageConverter;
    }

    public void setResourceRegionHttpMessageConverter(@Nullable ResourceRegionHttpMessageConverter messageConverter) {
        this.resourceRegionHttpMessageConverter = messageConverter;
    }


    @Nullable
    public ResourceRegionHttpMessageConverter getResourceRegionHttpMessageConverter() {
        return this.resourceRegionHttpMessageConverter;
    }

    public void setContentNegotiationManager(@Nullable ContentNegotiationManager contentNegotiationManager) {
        this.contentNegotiationManager = contentNegotiationManager;
    }

    @Nullable
    public ContentNegotiationManager getContentNegotiationManager() {
        return this.contentNegotiationManager;
    }

    public void setCorsConfiguration(CorsConfiguration corsConfiguration) {
        this.corsConfiguration = corsConfiguration;
    }

    @Override
    @Nullable
    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
        return this.corsConfiguration;
    }


    public void setUrlPathHelper(@Nullable UrlPathHelper urlPathHelper) {
        this.urlPathHelper = urlPathHelper;
    }

    @Nullable
    public UrlPathHelper getUrlPathHelper() {
        return this.urlPathHelper;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.embeddedValueResolver = resolver;
    }



    @Override
    public void afterPropertiesSet() throws Exception {
        resolveResourceLocations();

        if (logger.isWarnEnabled() && CollectionUtils.isEmpty(this.locations)) {
            logger.warn("Locations list is empty. No resources will be served unless a " +
                    "custom ResourceResolver is configured as an alternative to PathResourceResolver.");
        }

        if (this.resourceResolvers.isEmpty()) {
            this.resourceResolvers.add(new PathResourceResolver());
        }

        initAllowedLocations();

        // Initialize immutable resolver and transformer chains
        this.resolverChain = new DefaultResourceResolverChain(this.resourceResolvers);
        this.transformerChain = new DefaultResourceTransformerChain(this.resolverChain, this.resourceTransformers);

        if (this.resourceHttpMessageConverter == null) {
            this.resourceHttpMessageConverter = new ResourceHttpMessageConverter();
        }
        if (this.resourceRegionHttpMessageConverter == null) {
            this.resourceRegionHttpMessageConverter = new ResourceRegionHttpMessageConverter();
        }

        this.contentNegotiationStrategy = initContentNegotiationStrategy();
    }


    private void resolveResourceLocations() {
        if (CollectionUtils.isEmpty(this.locationValues)) {
            return;
        }
        else if (!CollectionUtils.isEmpty(this.locations)) {
            throw new IllegalArgumentException("Please set either Resource-based \"locations\" or " +
                    "String-based \"locationValues\", but not both.");
        }

        ApplicationContext applicationContext = obtainApplicationContext();
        for (String location : this.locationValues) {
            if (this.embeddedValueResolver != null) {
                String resolvedLocation = this.embeddedValueResolver.resolveStringValue(location);
                if (resolvedLocation == null) {
                    throw new IllegalArgumentException("Location resolved to null: " + location);
                }
                location = resolvedLocation;
            }
            Charset charset = null;
            location = location.trim();
            if (location.startsWith(URL_RESOURCE_CHARSET_PREFIX)) {
                int endIndex = location.indexOf(']', URL_RESOURCE_CHARSET_PREFIX.length());
                if (endIndex == -1) {
                    throw new IllegalArgumentException("Invalid charset syntax in location: " + location);
                }
                String value = location.substring(URL_RESOURCE_CHARSET_PREFIX.length(), endIndex);
                charset = Charset.forName(value);
                location = location.substring(endIndex + 1);
            }
            Resource resource = applicationContext.getResource(location);
            this.locations.add(resource);
            if (charset != null) {
                if (!(resource instanceof UrlResource)) {
                    throw new IllegalArgumentException("Unexpected charset for non-UrlResource: " + resource);
                }
                this.locationCharsets.put(resource, charset);
            }
        }
    }


    protected void initAllowedLocations() {
        if (CollectionUtils.isEmpty(this.locations)) {
            return;
        }
        for (int i = getResourceResolvers().size() - 1; i >= 0; i--) {
            if (getResourceResolvers().get(i) instanceof PathResourceResolver) {
                PathResourceResolver pathResolver = (PathResourceResolver) getResourceResolvers().get(i);
                if (ObjectUtils.isEmpty(pathResolver.getAllowedLocations())) {
                    pathResolver.setAllowedLocations(getLocations().toArray(new Resource[0]));
                }
                if (this.urlPathHelper != null) {
                    pathResolver.setLocationCharsets(this.locationCharsets);
                    pathResolver.setUrlPathHelper(this.urlPathHelper);
                }
                break;
            }
        }
    }
    protected PathExtensionContentNegotiationStrategy initContentNegotiationStrategy() {
        Map<String, MediaType> mediaTypes = null;
        if (getContentNegotiationManager() != null) {
            PathExtensionContentNegotiationStrategy strategy =
                    getContentNegotiationManager().getStrategy(PathExtensionContentNegotiationStrategy.class);
            if (strategy != null) {
                mediaTypes = new HashMap<>(strategy.getMediaTypes());
            }
        }
        return (getServletContext() != null ?
                new ServletPathExtensionContentNegotiationStrategy(getServletContext(), mediaTypes) :
                new PathExtensionContentNegotiationStrategy(mediaTypes));
    }


    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // For very general mappings (e.g. "/") we need to check 404 first
        Resource resource = getResource(request);
        if (resource == null) {
            logger.debug("Resource not found");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            response.setHeader("Allow", getAllowHeader());
            return;
        }

        // Supported methods and required session
        checkRequest(request);

        // Header phase
        if (new ServletWebRequest(request, response).checkNotModified(resource.lastModified())) {
            logger.trace("Resource not modified");
            return;
        }

        // Apply cache settings, if any
        prepareResponse(response);

        // Check the media type for the resource
        MediaType mediaType = getMediaType(request, resource);

        // Content phase
        if (METHOD_HEAD.equals(request.getMethod())) {
            setHeaders(response, resource, mediaType);
            return;
        }

        ServletServerHttpResponse outputMessage = new ServletServerHttpResponse(response);
        if (request.getHeader(HttpHeaders.RANGE) == null) {
            Assert.state(this.resourceHttpMessageConverter != null, "Not initialized");
            setHeaders(response, resource, mediaType);
            this.resourceHttpMessageConverter.write(resource, mediaType, outputMessage);
        }
        else {
            Assert.state(this.resourceRegionHttpMessageConverter != null, "Not initialized");
            response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
            ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(request);
            try {
                List<HttpRange> httpRanges = inputMessage.getHeaders().getRange();
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                this.resourceRegionHttpMessageConverter.write(
                        HttpRange.toResourceRegions(httpRanges, resource), mediaType, outputMessage);
            }
            catch (IllegalArgumentException ex) {
                response.setHeader("Content-Range", "bytes */" + resource.contentLength());
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            }
        }
    }


    @Nullable
    protected Resource getResource(HttpServletRequest request) throws IOException {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        if (path == null) {
            throw new IllegalStateException("Required request attribute '" +
                    HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE + "' is not set");
        }

        path = processPath(path);
        if (!StringUtils.hasText(path) || isInvalidPath(path)) {
            return null;
        }
        if (isInvalidEncodedPath(path)) {
            return null;
        }

        Assert.notNull(this.resolverChain, "ResourceResolverChain not initialized.");
        Assert.notNull(this.transformerChain, "ResourceTransformerChain not initialized.");

        Resource resource = this.resolverChain.resolveResource(request, path, getLocations());
        if (resource != null) {
            resource = this.transformerChain.transform(request, resource);
        }
        return resource;
    }

    protected String processPath(String path) {
        path = StringUtils.replace(path, "\\", "/");
        path = cleanDuplicateSlashes(path);
        return cleanLeadingSlash(path);
    }

    private String cleanDuplicateSlashes(String path) {
        StringBuilder sb = null;
        char prev = 0;
        for (int i = 0; i < path.length(); i++) {
            char curr = path.charAt(i);
            try {
                if ((curr == '/') && (prev == '/')) {
                    if (sb == null) {
                        sb = new StringBuilder(path.substring(0, i));
                    }
                    continue;
                }
                if (sb != null) {
                    sb.append(path.charAt(i));
                }
            }
            finally {
                prev = curr;
            }
        }
        return sb != null ? sb.toString() : path;
    }



    private String cleanLeadingSlash(String path) {
        boolean slash = false;
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '/') {
                slash = true;
            }
            else if (path.charAt(i) > ' ' && path.charAt(i) != 127) {
                if (i == 0 || (i == 1 && slash)) {
                    return path;
                }
                return (slash ? "/" + path.substring(i) : path.substring(i));
            }
        }
        return (slash ? "/" : "");
    }


    private boolean isInvalidEncodedPath(String path) {
        if (path.contains("%")) {
            try {
                // Use URLDecoder (vs UriUtils) to preserve potentially decoded UTF-8 chars
                String decodedPath = URLDecoder.decode(path, "UTF-8");
                if (isInvalidPath(decodedPath)) {
                    return true;
                }
                decodedPath = processPath(decodedPath);
                if (isInvalidPath(decodedPath)) {
                    return true;
                }
            }
            catch (IllegalArgumentException ex) {
                // May not be possible to decode...
            }
            catch (UnsupportedEncodingException ex) {
                // Should never happen...
            }
        }
        return false;
    }
    protected boolean isInvalidPath(String path) {
        if (path.contains("WEB-INF") || path.contains("META-INF")) {
            if (logger.isWarnEnabled()) {
                logger.warn("Path with \"WEB-INF\" or \"META-INF\": [" + path + "]");
            }
            return true;
        }
        if (path.contains(":/")) {
            String relativePath = (path.charAt(0) == '/' ? path.substring(1) : path);
            if (ResourceUtils.isUrl(relativePath) || relativePath.startsWith("url:")) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Path represents URL or has \"url:\" prefix: [" + path + "]");
                }
                return true;
            }
        }
        if (path.contains("..") && StringUtils.cleanPath(path).contains("../")) {
            if (logger.isWarnEnabled()) {
                logger.warn("Path contains \"../\" after call to StringUtils#cleanPath: [" + path + "]");
            }
            return true;
        }
        return false;
    }


    @Nullable
    protected MediaType getMediaType(HttpServletRequest request, Resource resource) {
        return (this.contentNegotiationStrategy != null ?
                this.contentNegotiationStrategy.getMediaTypeForResource(resource) : null);
    }


    protected void setHeaders(HttpServletResponse response, Resource resource, @Nullable MediaType mediaType)
            throws IOException {

        long length = resource.contentLength();
        if (length > Integer.MAX_VALUE) {
            response.setContentLengthLong(length);
        }
        else {
            response.setContentLength((int) length);
        }

        if (mediaType != null) {
            response.setContentType(mediaType.toString());
        }
        if (resource instanceof HttpResource) {
            HttpHeaders resourceHeaders = ((HttpResource) resource).getResponseHeaders();
            resourceHeaders.forEach((headerName, headerValues) -> {
                boolean first = true;
                for (String headerValue : headerValues) {
                    if (first) {
                        response.setHeader(headerName, headerValue);
                    }
                    else {
                        response.addHeader(headerName, headerValue);
                    }
                    first = false;
                }
            });
        }
        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
    }

    @Override
    public String toString() {
        return "ResourceHttpRequestHandler " + formatLocations();
    }


    private Object formatLocations() {
        if (!this.locationValues.isEmpty()) {
            return this.locationValues.stream().collect(Collectors.joining("\", \"", "[\"", "\"]"));
        }
        else if (!this.locations.isEmpty()) {
            return this.locations;
        }
        return Collections.emptyList();
    }






}
