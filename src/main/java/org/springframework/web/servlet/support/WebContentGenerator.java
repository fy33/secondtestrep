package org.springframework.web.servlet.support;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.context.support.WebApplicationObjectSupport;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class WebContentGenerator extends WebApplicationObjectSupport {
    public static final String METHOD_GET = "GET";

    /** HTTP method "HEAD". */
    public static final String METHOD_HEAD = "HEAD";

    /** HTTP method "POST". */
    public static final String METHOD_POST = "POST";

    private static final String HEADER_PRAGMA = "Pragma";

    private static final String HEADER_EXPIRES = "Expires";

    protected static final String HEADER_CACHE_CONTROL = "Cache-Control";

    /** Set of supported HTTP methods. */
    @Nullable
    private Set<String> supportedMethods;

    @Nullable
    private String allowHeader;

    private boolean requireSession = false;

    @Nullable
    private CacheControl cacheControl;

    private int cacheSeconds = -1;

    @Nullable
    private String[] varyByRequestHeaders;


    // deprecated fields

    /** Use HTTP 1.0 expires header? */
    private boolean useExpiresHeader = false;

    /** Use HTTP 1.1 cache-control header? */
    private boolean useCacheControlHeader = true;

    /** Use HTTP 1.1 cache-control header value "no-store"? */
    private boolean useCacheControlNoStore = true;

    private boolean alwaysMustRevalidate = false;


    public WebContentGenerator() {
        this(true);
    }


    public WebContentGenerator(boolean restrictDefaultSupportedMethods) {
        if (restrictDefaultSupportedMethods) {
            this.supportedMethods = new LinkedHashSet<>(4);
            this.supportedMethods.add(METHOD_GET);
            this.supportedMethods.add(METHOD_HEAD);
            this.supportedMethods.add(METHOD_POST);
        }
        initAllowHeader();
    }
    public WebContentGenerator(String... supportedMethods) {
        setSupportedMethods(supportedMethods);
    }


    public final void setSupportedMethods(@Nullable String... methods) {
        if (!ObjectUtils.isEmpty(methods)) {
            this.supportedMethods = new LinkedHashSet<>(Arrays.asList(methods));
        }
        else {
            this.supportedMethods = null;
        }
        initAllowHeader();
    }

    @Nullable
    public final String[] getSupportedMethods() {
        return (this.supportedMethods != null ? StringUtils.toStringArray(this.supportedMethods) : null);
    }


    private void initAllowHeader() {
        Collection<String> allowedMethods;
        if (this.supportedMethods == null) {
            allowedMethods = new ArrayList<>(HttpMethod.values().length - 1);
            for (HttpMethod method : HttpMethod.values()) {
                if (method != HttpMethod.TRACE) {
                    allowedMethods.add(method.name());
                }
            }
        }
        else if (this.supportedMethods.contains(HttpMethod.OPTIONS.name())) {
            allowedMethods = this.supportedMethods;
        }
        else {
            allowedMethods = new ArrayList<>(this.supportedMethods);
            allowedMethods.add(HttpMethod.OPTIONS.name());

        }
        this.allowHeader = StringUtils.collectionToCommaDelimitedString(allowedMethods);
    }

    @Nullable
    protected String getAllowHeader() {
        return this.allowHeader;
    }

    public final void setRequireSession(boolean requireSession) {
        this.requireSession = requireSession;
    }

    public final boolean isRequireSession() {
        return this.requireSession;
    }


    public final void setCacheControl(@Nullable CacheControl cacheControl) {
        this.cacheControl = cacheControl;
    }

    @Nullable
    public final CacheControl getCacheControl() {
        return this.cacheControl;
    }

    public final void setCacheSeconds(int seconds) {
        this.cacheSeconds = seconds;
    }


    public final int getCacheSeconds() {
        return this.cacheSeconds;
    }

    public final void setVaryByRequestHeaders(@Nullable String... varyByRequestHeaders) {
        this.varyByRequestHeaders = varyByRequestHeaders;
    }

    @Nullable
    public final String[] getVaryByRequestHeaders() {
        return this.varyByRequestHeaders;
    }


    @Deprecated
    public final void setUseExpiresHeader(boolean useExpiresHeader) {
        this.useExpiresHeader = useExpiresHeader;
    }


    @Deprecated
    public final boolean isUseExpiresHeader() {
        return this.useExpiresHeader;
    }

    @Deprecated
    public final void setUseCacheControlHeader(boolean useCacheControlHeader) {
        this.useCacheControlHeader = useCacheControlHeader;
    }


    @Deprecated
    public final boolean isUseCacheControlHeader() {
        return this.useCacheControlHeader;
    }


    @Deprecated
    public final void setUseCacheControlNoStore(boolean useCacheControlNoStore) {
        this.useCacheControlNoStore = useCacheControlNoStore;
    }

    @Deprecated
    public final boolean isUseCacheControlNoStore() {
        return this.useCacheControlNoStore;
    }

    @Deprecated
    public final void setAlwaysMustRevalidate(boolean mustRevalidate) {
        this.alwaysMustRevalidate = mustRevalidate;
    }

    @Deprecated
    public final boolean isAlwaysMustRevalidate() {
        return this.alwaysMustRevalidate;
    }
    protected final void checkRequest(HttpServletRequest request) throws ServletException {
        // Check whether we should support the request method.
        String method = request.getMethod();
        if (this.supportedMethods != null && !this.supportedMethods.contains(method)) {
            throw new HttpRequestMethodNotSupportedException(method, this.supportedMethods);
        }

        // Check whether a session is required.
        if (this.requireSession && request.getSession(false) == null) {
            throw new HttpSessionRequiredException("Pre-existing session required but none found");
        }
    }

    protected final void prepareResponse(HttpServletResponse response) {
        if (this.cacheControl != null) {
            applyCacheControl(response, this.cacheControl);
        }
        else {
            applyCacheSeconds(response, this.cacheSeconds);
        }
        if (this.varyByRequestHeaders != null) {
            for (String value : getVaryRequestHeadersToAdd(response, this.varyByRequestHeaders)) {
                response.addHeader("Vary", value);
            }
        }
    }

    protected final void applyCacheControl(HttpServletResponse response, CacheControl cacheControl) {
        String ccValue = cacheControl.getHeaderValue();
        if (ccValue != null) {
            // Set computed HTTP 1.1 Cache-Control header
            response.setHeader(HEADER_CACHE_CONTROL, ccValue);

            if (response.containsHeader(HEADER_PRAGMA)) {
                // Reset HTTP 1.0 Pragma header if present
                response.setHeader(HEADER_PRAGMA, "");
            }
            if (response.containsHeader(HEADER_EXPIRES)) {
                // Reset HTTP 1.0 Expires header if present
                response.setHeader(HEADER_EXPIRES, "");
            }
        }
    }


    @SuppressWarnings("deprecation")
    protected final void applyCacheSeconds(HttpServletResponse response, int cacheSeconds) {
        if (this.useExpiresHeader || !this.useCacheControlHeader) {
            // Deprecated HTTP 1.0 cache behavior, as in previous Spring versions
            if (cacheSeconds > 0) {
                cacheForSeconds(response, cacheSeconds);
            }
            else if (cacheSeconds == 0) {
                preventCaching(response);
            }
        }
        else {
            CacheControl cControl;
            if (cacheSeconds > 0) {
                cControl = CacheControl.maxAge(cacheSeconds, TimeUnit.SECONDS);
                if (this.alwaysMustRevalidate) {
                    cControl = cControl.mustRevalidate();
                }
            }
            else if (cacheSeconds == 0) {
                cControl = (this.useCacheControlNoStore ? CacheControl.noStore() : CacheControl.noCache());
            }
            else {
                cControl = CacheControl.empty();
            }
            applyCacheControl(response, cControl);
        }
    }

    @Deprecated
    protected final void checkAndPrepare(
            HttpServletRequest request, HttpServletResponse response, boolean lastModified) throws ServletException {

        checkRequest(request);
        prepareResponse(response);
    }


    @Deprecated
    protected final void checkAndPrepare(
            HttpServletRequest request, HttpServletResponse response, int cacheSeconds, boolean lastModified)
            throws ServletException {

        checkRequest(request);
        applyCacheSeconds(response, cacheSeconds);
    }


    @Deprecated
    protected final void applyCacheSeconds(HttpServletResponse response, int cacheSeconds, boolean mustRevalidate) {
        if (cacheSeconds > 0) {
            cacheForSeconds(response, cacheSeconds, mustRevalidate);
        }
        else if (cacheSeconds == 0) {
            preventCaching(response);
        }
    }

    @Deprecated
    protected final void cacheForSeconds(HttpServletResponse response, int seconds) {
        cacheForSeconds(response, seconds, false);
    }


    @Deprecated
    protected final void cacheForSeconds(HttpServletResponse response, int seconds, boolean mustRevalidate) {
        if (this.useExpiresHeader) {
            // HTTP 1.0 header
            response.setDateHeader(HEADER_EXPIRES, System.currentTimeMillis() + seconds * 1000L);
        }
        else if (response.containsHeader(HEADER_EXPIRES)) {
            // Reset HTTP 1.0 Expires header if present
            response.setHeader(HEADER_EXPIRES, "");
        }

        if (this.useCacheControlHeader) {
            // HTTP 1.1 header
            String headerValue = "max-age=" + seconds;
            if (mustRevalidate || this.alwaysMustRevalidate) {
                headerValue += ", must-revalidate";
            }
            response.setHeader(HEADER_CACHE_CONTROL, headerValue);
        }

        if (response.containsHeader(HEADER_PRAGMA)) {
            // Reset HTTP 1.0 Pragma header if present
            response.setHeader(HEADER_PRAGMA, "");
        }
    }

    @Deprecated
    protected final void preventCaching(HttpServletResponse response) {
        response.setHeader(HEADER_PRAGMA, "no-cache");

        if (this.useExpiresHeader) {
            // HTTP 1.0 Expires header
            response.setDateHeader(HEADER_EXPIRES, 1L);
        }

        if (this.useCacheControlHeader) {
            // HTTP 1.1 Cache-Control header: "no-cache" is the standard value,
            // "no-store" is necessary to prevent caching on Firefox.
            response.setHeader(HEADER_CACHE_CONTROL, "no-cache");
            if (this.useCacheControlNoStore) {
                response.addHeader(HEADER_CACHE_CONTROL, "no-store");
            }
        }
    }

    private Collection<String> getVaryRequestHeadersToAdd(HttpServletResponse response, String[] varyByRequestHeaders) {
        if (!response.containsHeader(HttpHeaders.VARY)) {
            return Arrays.asList(varyByRequestHeaders);
        }
        Collection<String> result = new ArrayList<>(varyByRequestHeaders.length);
        Collections.addAll(result, varyByRequestHeaders);
        for (String header : response.getHeaders(HttpHeaders.VARY)) {
            for (String existing : StringUtils.tokenizeToStringArray(header, ",")) {
                if ("*".equals(existing)) {
                    return Collections.emptyList();
                }
                for (String value : varyByRequestHeaders) {
                    if (value.equalsIgnoreCase(existing)) {
                        result.remove(value);
                    }
                }
            }
        }
        return result;
    }





























}
