package com.github.rmannibucau.terminal.websocket;

import javax.enterprise.context.Dependent;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

@Dependent // just to mark it as scanned, not a real cdi bean
@WebListener
public class TerminalEndpointRegistrar implements ServletContextListener {
    @Override
    public void contextInitialized(final ServletContextEvent servletContextEvent) {
        if (Boolean.getBoolean("terminal.disabled")) {
            return;
        }
        try {
            ServerContainer.class.cast(servletContextEvent.getServletContext().getAttribute("javax.websocket.server.ServerContainer"))
                    .addEndpoint(ServerEndpointConfig.Builder
                            .create(TerminalEndpoint.class, "/terminal/session")
                            .decoders(singletonList(TerminalEndpoint.RequestDecoder.class))
                            .encoders(asList(TerminalEndpoint.ResponseEncoder.class, TerminalEndpoint.ValueEncoder.class))
                            .build());
        } catch (final DeploymentException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent servletContextEvent) {
        // no-op
    }
}
