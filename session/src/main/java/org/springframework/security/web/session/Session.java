package org.springframework.security.web.session;

import java.io.Serializable;
import java.util.Set;

/**
 * @author Rob Winch
 */
public interface Session extends Serializable {
    void setLastAccessedTime(long lastAccessedTime);

    long getCreationTime();

    String getId();

    long getLastAccessedTime();

    void setMaxInactiveInterval(int interval);

    int getMaxInactiveInterval();

    Object getAttribute(String name);

    Set<String> getAttributeNames();

    void setAttribute(String name, Object value);

    void removeAttribute(String name);
}
