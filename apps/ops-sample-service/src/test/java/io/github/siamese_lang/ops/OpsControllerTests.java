package io.github.siamese_lang.ops;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
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
}
