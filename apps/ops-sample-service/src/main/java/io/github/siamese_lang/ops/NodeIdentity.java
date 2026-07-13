package io.github.siamese_lang.ops;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NodeIdentity {

    public Map<String, Object> asMap() {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("hostname", hostname());
        node.put("localAddress", localAddress());
        node.put("role", role());
        node.put("tier", tier());
        node.put("environment", environment());
        node.put("version", version());
        node.put("deploymentId", deploymentId());
        node.put("deploymentSlot", deploymentSlot());
        return node;
    }

    public String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            return "unknown";
        }
    }

    public String localAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            return "unknown";
        }
    }

    public String role() {
        return env("OPS_NODE_ROLE", "app");
    }

    public String tier() {
        return env("OPS_NODE_TIER", "private-was");
    }

    public String environment() {
        return env("OPS_ENVIRONMENT", "lab-full-min");
    }

    public String version() {
        return env("APP_VERSION", "0.1.0-local");
    }

    public String deploymentId() {
        return env("OPS_DEPLOYMENT_ID", "local");
    }

    public String deploymentSlot() {
        return env("OPS_DEPLOYMENT_SLOT", "stable");
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
