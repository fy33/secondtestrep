package org.springframework.web.servlet.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractFlashMapManager implements FlashMapManager {
    private static final Object DEFAULT_FLASH_MAPS_MUTEX = new Object();

    protected final Log logger = LogFactory.getLog(getClass());

    private int flashMapTimeout = 180;

    private UrlPathHelper urlPathHelper = new UrlPathHelper();


    public void setFlashMapTimeout(int flashMapTimeout) {
        this.flashMapTimeout = flashMapTimeout;
    }


    public int getFlashMapTimeout() {
        return this.flashMapTimeout;
    }


    public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
        Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
        this.urlPathHelper = urlPathHelper;
    }

    public UrlPathHelper getUrlPathHelper() {
        return this.urlPathHelper;
    }

    @Override
    @Nullable
    public final FlashMap retrieveAndUpdate(HttpServletRequest request, HttpServletResponse response) {
        List<FlashMap> allFlashMaps = retrieveFlashMaps(request);
        if (CollectionUtils.isEmpty(allFlashMaps)) {
            return null;
        }

        List<FlashMap> mapsToRemove = getExpiredFlashMaps(allFlashMaps);
        FlashMap match = getMatchingFlashMap(allFlashMaps, request);
        if (match != null) {
            mapsToRemove.add(match);
        }

        if (!mapsToRemove.isEmpty()) {
            Object mutex = getFlashMapsMutex(request);
            if (mutex != null) {
                synchronized (mutex) {
                    allFlashMaps = retrieveFlashMaps(request);
                    if (allFlashMaps != null) {
                        allFlashMaps.removeAll(mapsToRemove);
                        updateFlashMaps(allFlashMaps, request, response);
                    }
                }
            }
            else {
                allFlashMaps.removeAll(mapsToRemove);
                updateFlashMaps(allFlashMaps, request, response);
            }
        }

        return match;
    }


    private List<FlashMap> getExpiredFlashMaps(List<FlashMap> allMaps) {
        List<FlashMap> result = new LinkedList<>();
        for (FlashMap map : allMaps) {
            if (map.isExpired()) {
                result.add(map);
            }
        }
        return result;
    }

    @Nullable
    private FlashMap getMatchingFlashMap(List<FlashMap> allMaps, HttpServletRequest request) {
        List<FlashMap> result = new LinkedList<>();
        for (FlashMap flashMap : allMaps) {
            if (isFlashMapForRequest(flashMap, request)) {
                result.add(flashMap);
            }
        }
        if (!result.isEmpty()) {
            Collections.sort(result);
            if (logger.isTraceEnabled()) {
                logger.trace("Found " + result.get(0));
            }
            return result.get(0);
        }
        return null;
    }

    protected boolean isFlashMapForRequest(FlashMap flashMap, HttpServletRequest request) {
        String expectedPath = flashMap.getTargetRequestPath();
        if (expectedPath != null) {
            String requestUri = getUrlPathHelper().getOriginatingRequestUri(request);
            if (!requestUri.equals(expectedPath) && !requestUri.equals(expectedPath + "/")) {
                return false;
            }
        }
        MultiValueMap<String, String> actualParams = getOriginatingRequestParams(request);
        MultiValueMap<String, String> expectedParams = flashMap.getTargetRequestParams();
        for (Map.Entry<String, List<String>> entry : expectedParams.entrySet()) {
            List<String> actualValues = actualParams.get(entry.getKey());
            if (actualValues == null) {
                return false;
            }
            for (String expectedValue : entry.getValue()) {
                if (!actualValues.contains(expectedValue)) {
                    return false;
                }
            }
        }
        return true;
    }

    private MultiValueMap<String, String> getOriginatingRequestParams(HttpServletRequest request) {
        String query = getUrlPathHelper().getOriginatingQueryString(request);
        return ServletUriComponentsBuilder.fromPath("/").query(query).build().getQueryParams();
    }

    @Override
    public final void saveOutputFlashMap(FlashMap flashMap, HttpServletRequest request, HttpServletResponse response) {
        if (CollectionUtils.isEmpty(flashMap)) {
            return;
        }

        String path = decodeAndNormalizePath(flashMap.getTargetRequestPath(), request);
        flashMap.setTargetRequestPath(path);

        flashMap.startExpirationPeriod(getFlashMapTimeout());

        Object mutex = getFlashMapsMutex(request);
        if (mutex != null) {
            synchronized (mutex) {
                List<FlashMap> allFlashMaps = retrieveFlashMaps(request);
                allFlashMaps = (allFlashMaps != null ? allFlashMaps : new CopyOnWriteArrayList<>());
                allFlashMaps.add(flashMap);
                updateFlashMaps(allFlashMaps, request, response);
            }
        }
        else {
            List<FlashMap> allFlashMaps = retrieveFlashMaps(request);
            allFlashMaps = (allFlashMaps != null ? allFlashMaps : new LinkedList<>());
            allFlashMaps.add(flashMap);
            updateFlashMaps(allFlashMaps, request, response);
        }
    }

    @Nullable
    private String decodeAndNormalizePath(@Nullable String path, HttpServletRequest request) {
        if (path != null && !path.isEmpty()) {
            path = getUrlPathHelper().decodeRequestString(request, path);
            if (path.charAt(0) != '/') {
                String requestUri = getUrlPathHelper().getRequestUri(request);
                path = requestUri.substring(0, requestUri.lastIndexOf('/') + 1) + path;
                path = StringUtils.cleanPath(path);
            }
        }
        return path;
    }

    @Nullable
    protected abstract List<FlashMap> retrieveFlashMaps(HttpServletRequest request);

    protected abstract void updateFlashMaps(
            List<FlashMap> flashMaps, HttpServletRequest request, HttpServletResponse response);

    @Nullable
    protected Object getFlashMapsMutex(HttpServletRequest request) {
        return DEFAULT_FLASH_MAPS_MUTEX;
    }



}
