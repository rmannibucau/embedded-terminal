package com.github.rmannibucau.terminal.websocket;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;
import java.util.Collections;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;

public class TerminalEndpointRegistrar implements ServerApplicationConfig {
    @Override
    public Set<ServerEndpointConfig> getEndpointConfigs(final Set<Class<? extends Endpoint>> scanned) {
        if (Boolean.getBoolean("terminal.disabled")) {
            return emptySet();
        }
        return Collections.singleton(
                ServerEndpointConfig.Builder
                        .create(TerminalEndpoint.class, "/terminal/session")
                        .decoders(singletonList(TerminalEndpoint.RequestDecoder.class))
                        .encoders(asList(TerminalEndpoint.ResponseEncoder.class, TerminalEndpoint.ValueEncoder.class))
                        .build());
    }

    @Override
    public Set<Class<?>> getAnnotatedEndpointClasses(final Set<Class<?>> scanned) {
        return emptySet();
    }
}
