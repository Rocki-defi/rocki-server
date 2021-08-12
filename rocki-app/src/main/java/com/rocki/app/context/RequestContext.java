package com.rocki.app.context;

import com.rocki.app.interceptor.FrontLoginUser;
import com.rocki.user.domain.User;

/**
 * @author GZC
 */
public class RequestContext {
    private RequestContext() {
        throw new IllegalStateException();
    }

    private static final ThreadLocal<FrontLoginUser> CONTEXTS = new ThreadLocal<>();

    public static void begin(FrontLoginUser loginUser) {
        CONTEXTS.set(loginUser);
    }

    public static FrontLoginUser get() {
        return CONTEXTS.get();
    }

    public static void end() {
        CONTEXTS.remove();
    }

    public static User getUser() {
        FrontLoginUser frontLoginUser = get();
        return frontLoginUser.getUser();
    }
}
