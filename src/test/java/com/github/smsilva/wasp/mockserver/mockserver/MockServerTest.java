package com.github.smsilva.wasp.mockserver.mockserver;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;
import org.springframework.web.reactive.function.client.WebClient;

@TestInstance(Lifecycle.PER_CLASS)
public class MockServerTest {

    private ClientAndServer mockServer;

    @BeforeAll
    public void startMockServer() {
        mockServer = startClientAndServer(4000);
    }

    @AfterAll
    public void stopMockServer() {
        mockServer.stop();
    }

    @Test
    void test() {
        mockServer
            .when(HttpRequest
                    .request()
                    .withMethod("GET")
                    .withPath("/test"))

            .respond(HttpResponse
                    .response()
                    .withStatusCode(200)
                    .withHeaders(
                        new Header("Content-Type", "application/json; charset=utf-8"),
                        new Header("Cache-Control", "public, max-age=86400"))
                    .withBody("{ message: 'incorrect username and password combination' }")
                    .withDelay(TimeUnit.SECONDS,1)
        );

        WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:4000")
            .build();

        String body = webClient.get()
            .uri("/test")
            .retrieve()
            .bodyToMono(String.class)
            .doOnSuccess(response -> {
                System.out.println("doOnSuccess :: " + response);
            })
            .block();

        assertNotNull(body);

        webClient.get()
            .uri("/test")
            .retrieve()
            .toBodilessEntity()
            .block();

        try (MockServerClient mockServerClient = new MockServerClient("localhost", 4000)) {
            mockServerClient.verify(
                request()
                    .withPath("/test"),
                VerificationTimes.atLeast(2)
            );
        }

    }

}
