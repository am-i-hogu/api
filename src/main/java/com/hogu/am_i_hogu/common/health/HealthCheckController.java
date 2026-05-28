package com.hogu.am_i_hogu.common.health;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
public class HealthCheckController {

    private final String env;
    private final int serverPort;
    private final String serverAddress;
    private final String serverName;

    public HealthCheckController(
            @Value("${server.env}") String env,
            @Value("${server.port}") int serverPort,
            @Value("${server.serverAddress}") String serverAddress,
            @Value("${serverName}") String serverName
    ) {
        this.env = env;
        this.serverPort = serverPort;
        this.serverAddress = serverAddress;
        this.serverName = serverName;
    }

    @GetMapping("/api/health")
    public HealthCheckResponse healthCheck() {
        return new HealthCheckResponse("UP", env, serverPort, serverAddress, serverName);
    }

    public record HealthCheckResponse(
            String status,
            String env,
            int serverPort,
            String serverAddress,
            String serverName
    ) {
    }
}
