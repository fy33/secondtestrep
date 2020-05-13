package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;

public interface HttpResource extends Resource {
    HttpHeaders getResponseHeaders();

}
