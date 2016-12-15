package com.github.rmannibucau.terminal.registration;

import com.github.rmannibucau.terminal.configuration.Configuration;
import com.github.rmannibucau.terminal.webresource.WebResource;
import com.github.rmannibucau.terminal.websocket.TerminalEndpoint;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class WebRegistrar implements ServletContainerInitializer {
    @Override
    public void onStartup(final Set<Class<?>> set, final ServletContext servletContext) throws ServletException {
        if (Configuration.isDisabled()) {
            return;
        }

        final ServletRegistration.Dynamic dynamic = servletContext.addServlet("embedded-terminal", WebResource.class);
        final String mapping = Configuration.terminalMappingBase();
        final String wsMapping = mapping + "/session";
        dynamic.setInitParameter("mapping", mapping);
        dynamic.setInitParameter("wsMapping", wsMapping);
        dynamic.setInitParameter("environment", Configuration.environment());
        dynamic.setLoadOnStartup(1);
        dynamic.addMapping(mapping + "/*");

        // register the websocket endpoint after ServletContainerInitializer phase to ensure the attribute is there
        // note: we don't use ServerApplicationConfig to ensure we have the config in a single place (here)
        servletContext.addListener(new ServletContextListener() {
            @Override
            public void contextInitialized(final ServletContextEvent sce) {
                try {
                    ServerContainer.class.cast(sce.getServletContext().getAttribute(ServerContainer.class.getName()))
                            .addEndpoint(ServerEndpointConfig.Builder
                                    .create(TerminalEndpoint.class, wsMapping)
                                    .decoders(singletonList(TerminalEndpoint.RequestDecoder.class))
                                    .encoders(asList(TerminalEndpoint.ResponseEncoder.class, TerminalEndpoint.ValueEncoder.class))
                                    .build());
                    sce.getServletContext().log("Registered terminal on " + sce.getServletContext().getContextPath() + mapping);
                } catch (final DeploymentException e) {
                    throw new IllegalStateException(e);
                }
            }

            @Override
            public void contextDestroyed(final ServletContextEvent sce) {
                // no-op
            }
        });
    }
}
