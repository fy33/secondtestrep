package org.springframework.web.multipart;

import javax.servlet.http.HttpServletRequest;

public interface MultipartResolver {
    boolean isMultipart(HttpServletRequest request);

    MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException;

    void cleanupMultipart(MultipartHttpServletRequest request);


}
