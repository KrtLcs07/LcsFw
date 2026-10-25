
package lcsfw.fw.http.session;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpSession;

public class LcsSession {

    private String id;
    private long lastAccessTime;

    private final Map<String, Object> attribute = new HashMap<>();
    private final Set<String> initialKeys = new HashSet<>();

    public static LcsSession fromHttpSession(HttpSession session) {
        if (session == null) {
            throw new IllegalArgumentException(
                    "LCSFW : la session HTTP ne peut pas être null."
            );
        }

        LcsSession lcsSession = new LcsSession();

        lcsSession.id = session.getId();
        lcsSession.lastAccessTime = session.getLastAccessedTime();

        session.getAttributeNames().asIterator().forEachRemaining(name -> {
            lcsSession.attribute.put(name, session.getAttribute(name));
            lcsSession.initialKeys.add(name);
        });

        return lcsSession;
    }

    public void flush(HttpSession session) {
        if (session == null) {
            throw new IllegalArgumentException(
                    "LCSFW : la session HTTP ne peut pas être null."
            );
        }

        for (String key : initialKeys) {
            if (!attribute.containsKey(key)) {
                session.removeAttribute(key);
            }
        }

        for (Map.Entry<String, Object> entry : attribute.entrySet()) {
            session.setAttribute(entry.getKey(), entry.getValue());
        }
    }

    public Object getAttribute(String name) {
        return attribute.get(name);
    }

    public void setAttribute(String name, Object value) {
        if (value == null) {
            removeAttribute(name);
        } else {
            attribute.put(name, value);
        }
    }

    public void removeAttribute(String name) {
        attribute.remove(name);
    }

    public String getId() {
        return id;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }
}
