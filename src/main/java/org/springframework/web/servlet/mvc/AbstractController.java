package org.springframework.web.servlet.mvc;

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.WebContentGenerator;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public abstract class AbstractController extends WebContentGenerator implements Controller {
    private boolean synchronizeOnSession = false;

    public AbstractController() {
        this(true);
    }


    public AbstractController(boolean restrictDefaultSupportedMethods) {
        super(restrictDefaultSupportedMethods);
    }

    public final void setSynchronizeOnSession(boolean synchronizeOnSession) {
        this.synchronizeOnSession = synchronizeOnSession;
    }

    public final boolean isSynchronizeOnSession() {
        return this.synchronizeOnSession;
    }


    @Override
    @Nullable
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            response.setHeader("Allow", getAllowHeader());
            return null;
        }

        // Delegate to WebContentGenerator for checking and preparing.
        checkRequest(request);
        prepareResponse(response);

        // Execute handleRequestInternal in synchronized block if required.
        if (this.synchronizeOnSession) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Object mutex = WebUtils.getSessionMutex(session);
                synchronized (mutex) {
                    return handleRequestInternal(request, response);
                }
            }
        }

        return handleRequestInternal(request, response);
    }

    @Nullable
    protected abstract ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
            throws Exception;






}
