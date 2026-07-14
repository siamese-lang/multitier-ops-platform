package io.github.siamese_lang.ops;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpsControllerTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void versionEndpointReturnsReleaseMetadata() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/version", Map.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("service", "ops-sample-service");
        assertThat(response.getBody()).containsKeys("runtime", "build", "artifact", "deployment", "node");
    }

    @Test
    void dbPoolEndpointReturnsPoolSettingsWithoutOpeningDbConnection() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/failure-lab/db-pool", Map.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("scenario", "db_pool");
        assertThat(response.getBody()).containsKeys("db", "pool", "node", "durationMs");
    }

    @Test
    void wasRuntimeEndpointReturnsEmbeddedTomcatEvidence() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/failure-lab/was-runtime", Map.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("scenario", "was_runtime");
        assertThat(response.getBody()).containsKeys("wasRuntime", "dbPool", "node", "durationMs");
    }

    @Test
    void dependenciesEndpointSeparatesAppDbPoolAndStorageState() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/ops/dependencies", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("check", "dependencies");
        assertThat(response.getBody()).containsEntry("status", "degraded");
        assertThat(response.getBody()).containsKeys("summary", "dependencies", "durationMs", "node");

        Map dependencies = (Map) response.getBody().get("dependencies");
        assertThat(dependencies).containsKeys("app", "db", "dbPool", "storage");

        Map app = (Map) dependencies.get("app");
        Map db = (Map) dependencies.get("db");
        Map dbPool = (Map) dependencies.get("dbPool");
        Map storage = (Map) dependencies.get("storage");

        assertThat(app).containsEntry("status", "UP");
        assertThat(db).containsEntry("status", "DOWN");
        assertThat(dbPool).containsKey("settings");
        assertThat(storage).containsKeys("status", "root", "exists", "directory", "readable", "writable", "ready");
    }
}
