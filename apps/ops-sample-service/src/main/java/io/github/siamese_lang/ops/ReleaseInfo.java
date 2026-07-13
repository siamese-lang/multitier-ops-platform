package io.github.siamese_lang.ops;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

@Component
public class ReleaseInfo {

    private final NodeIdentity nodeIdentity;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    public ReleaseInfo(NodeIdentity nodeIdentity, ObjectProvider<BuildProperties> buildPropertiesProvider) {
        this.nodeIdentity = nodeIdentity;
        this.buildPropertiesProvider = buildPropertiesProvider;
    }

    public Map<String, Object> asMap() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "ops-sample-service");
        response.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());
        response.put("runtime", runtimeMap());
        response.put("build", buildMap());
        response.put("artifact", artifactMap());
        response.put("deployment", deploymentMap());
        response.put("node", nodeIdentity.asMap());
        return response;
    }

    private Map<String, Object> runtimeMap() {
        Map<String, Object> runtime = new LinkedHashMap<>();
        runtime.put("appVersion", nodeIdentity.version());
        runtime.put("environment", nodeIdentity.environment());
        runtime.put("role", nodeIdentity.role());
        runtime.put("tier", nodeIdentity.tier());
        return runtime;
    }

    private Map<String, Object> buildMap() {
        Map<String, Object> build = new LinkedHashMap<>();
        BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        if (buildProperties == null) {
            build.put("available", false);
            build.put("name", "unknown");
            build.put("version", "unknown");
            build.put("time", "unknown");
            return build;
        }

        build.put("available", true);
        build.put("name", buildProperties.getName());
        build.put("version", buildProperties.getVersion());
        build.put("time", buildProperties.getTime() == null ? "unknown" : buildProperties.getTime().toString());
        return build;
    }

    private Map<String, Object> artifactMap() {
        Map<String, Object> artifact = new LinkedHashMap<>();
        artifact.put("source", env("OPS_ARTIFACT_SOURCE", "local"));
        artifact.put("sha256", env("OPS_ARTIFACT_SHA256", "unknown"));
        return artifact;
    }

    private Map<String, Object> deploymentMap() {
        Map<String, Object> deployment = new LinkedHashMap<>();
        deployment.put("id", nodeIdentity.deploymentId());
        deployment.put("slot", nodeIdentity.deploymentSlot());
        deployment.put("deployedAt", env("OPS_DEPLOYED_AT", "unknown"));
        return deployment;
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
