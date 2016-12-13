package com.github.rmannibucau.terminal.websocket;

import org.apache.meecrowave.Meecrowave;
import org.apache.meecrowave.junit.MonoMeecrowave;
import org.apache.meecrowave.testing.ConfigurationInject;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@RunWith(MonoMeecrowave.Runner.class)
public class TerminalEndpointTest {
    @ConfigurationInject
    private Meecrowave.Builder config;

    @Test
    public void command() throws IOException, DeploymentException {
        final WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        final Client client = new Client();
        try (final Session session = container.connectToServer(client, URI.create("ws://localhost:" + config.getHttpPort() + "/terminal/session"))) {
            // no security so no onOpen message
            assertNull(client.msg);

            // send a command
            session.getBasicRemote().sendText("{\"mode\":\"shell\",\"command\":\"echo test\"}");
            for (int i = 0; i < 60; i++) { // wait a bit the message is received
                try {
                    assertEquals("{\"response\":\"test\"}", client.msg);
                    return;
                } catch (final AssertionError ae) {
                    try {
                        sleep(1000);
                    } catch (final InterruptedException e) {
                        Thread.interrupted();
                        fail();
                    }
                }
            }
        }
        fail("should have exited before");
    }

    @ClientEndpoint
    public static class Client {
        private String msg;

        @OnMessage
        public void onMessage(final String msg) {
            this.msg = msg;
        }
    }
}
