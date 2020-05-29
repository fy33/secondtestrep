package org.springframework.web.servlet.handler;

import org.springframework.lang.Nullable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class UserRoleAuthorizationInterceptor extends HandlerInterceptorAdapter{
    @Nullable
    private String[] authorizedRoles;


    public final void setAuthorizedRoles(String... authorizedRoles) {
        this.authorizedRoles = authorizedRoles;
    }

    @Override
    public final boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws ServletException, IOException {

        if (this.authorizedRoles != null) {
            for (String role : this.authorizedRoles) {
                if (request.isUserInRole(role)) {
                    return true;
                }
            }
        }
        handleNotAuthorized(request, response, handler);
        return false;
    }



    protected void handleNotAuthorized(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws ServletException, IOException {

        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }


}
