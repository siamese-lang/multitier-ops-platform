package io.github.siamese_lang.ops;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FileProbeController {

    private final NodeIdentity nodeIdentity;
    private final Path rootPath;

    public FileProbeController(
        NodeIdentity nodeIdentity,
        @Value("${ops.file-probe.root:/mnt/ops-sample/files}") String rootPath
    ) {
        this.nodeIdentity = nodeIdentity;
        this.rootPath = Path.of(rootPath).normalize();
    }

    @GetMapping("/file-probe/status")
    public ResponseEntity<Map<String, Object>> status() {
        long startedAt = System.nanoTime();
        DirectoryState state = directoryState();
        Map<String, Object> response = response(state.ready() ? "ready" : "not-ready", "file_probe.status", startedAt);
        response.put("rootPath", rootPath.toString());
        response.put("directory", state.asMap());
        return ResponseEntity.status(state.ready() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @PostMapping("/file-probe/roundtrip")
    public ResponseEntity<Map<String, Object>> roundtrip() {
        long startedAt = System.nanoTime();
        DirectoryState state = directoryState();
        if (!state.ready()) {
            Map<String, Object> response = response("not-ready", "file_probe.roundtrip", startedAt);
            response.put("rootPath", rootPath.toString());
            response.put("directory", state.asMap());
            response.put("message", "file probe root is not a writable directory");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }

        String fileName = "probe-" + UUID.randomUUID() + ".txt";
        Path probeFile = rootPath.resolve(fileName).normalize();
        String payload = "file-probe timestamp=" + OffsetDateTime.now(ZoneOffset.UTC) +
            " node=" + nodeIdentity.hostname() + "\n";
        boolean deleted = false;

        try {
            Files.writeString(
                probeFile,
                payload,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
            );
            String readBack = Files.readString(probeFile, StandardCharsets.UTF_8);
            deleted = Files.deleteIfExists(probeFile);

            Map<String, Object> response = response("ok", "file_probe.roundtrip", startedAt);
            response.put("rootPath", rootPath.toString());
            response.put("fileName", fileName);
            response.put("bytesWritten", payload.getBytes(StandardCharsets.UTF_8).length);
            response.put("bytesRead", readBack.getBytes(StandardCharsets.UTF_8).length);
            response.put("readMatchesWrite", payload.equals(readBack));
            response.put("deleted", deleted);
            return ResponseEntity.ok(response);
        } catch (IOException ex) {
            try {
                deleted = Files.deleteIfExists(probeFile);
            } catch (IOException cleanupIgnored) {
                deleted = false;
            }

            Map<String, Object> response = response("file-error", "file_probe.roundtrip", startedAt);
            response.put("rootPath", rootPath.toString());
            response.put("fileName", fileName);
            response.put("deletedAfterFailure", deleted);
            response.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    private DirectoryState directoryState() {
        return new DirectoryState(
            Files.exists(rootPath),
            Files.isDirectory(rootPath),
            Files.isReadable(rootPath),
            Files.isWritable(rootPath)
        );
    }

    private Map<String, Object> response(String status, String operation, long startedAt) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "ops-sample-service");
        response.put("status", status);
        response.put("operation", operation);
        response.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());
        response.put("node", nodeIdentity.asMap());
        response.put("durationMs", durationMs(startedAt));
        return response;
    }

    private static long durationMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private record DirectoryState(boolean exists, boolean directory, boolean readable, boolean writable) {
        boolean ready() {
            return exists && directory && readable && writable;
        }

        Map<String, Object> asMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("exists", exists);
            map.put("directory", directory);
            map.put("readable", readable);
            map.put("writable", writable);
            return map;
        }
    }
}
