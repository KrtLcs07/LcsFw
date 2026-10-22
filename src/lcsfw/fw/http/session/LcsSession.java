package lcsfw.fw.http.session;

import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpSession;

public class LcsSession {
    private String id;
    private long lastAccessTime;
    private Map<String, Object> attribute;
    private Set<String> initialKeys;

    public void fromHttpSession(HttpSession session) {
        id = session.getId();
        lastAccessTime = session.getLastAccessedTime();
        session.getAttributeNames().asIterator().forEachRemaining(name -> {
            attribute.put(name, session.getAttribute(name));
            initialKeys.add(name);
        });
    }

    public void flush(HttpSession session){
        for (String key : initialKeys) {
            session.removeAttribute(key);
        }
        for (Map.Entry<String,Object> entry : attribute.entrySet()) {
            session.setAttribute(entry.getKey(), entry.getValue());
        }
    }
}
