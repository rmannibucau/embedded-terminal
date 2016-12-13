package com.github.rmannibucau.terminal.websocket;

import com.github.rmannibucau.terminal.cdi.TerminalExtension;
import com.github.rmannibucau.terminal.cdi.event.SessionClosed;
import com.github.rmannibucau.terminal.cdi.event.SessionOpened;
import com.github.rmannibucau.terminal.cdi.scope.CommandContext;
import com.github.rmannibucau.terminal.command.CommandExecutor;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.security.Principal;

import static java.util.Optional.ofNullable;

@Dependent
@ServerEndpoint(
        value = "/terminal/session",
        decoders = TerminalEndpoint.RequestDecoder.class,
        encoders = {TerminalEndpoint.ResponseEncoder.class, TerminalEndpoint.ValueEncoder.class})
public class TerminalEndpoint {
    private BeanManager beanManager;
    private CommandExecutor executor;
    private TerminalExtension extension;
    private CommandContext.Delegate context;

    @OnOpen
    public void onOpen(final Session session) {
        beanManager = CDI.current().getBeanManager();
        executor = lookup(beanManager, CommandExecutor.class);
        extension = lookup(beanManager, TerminalExtension.class);
        context = extension.getContext().newInstance();

        final Principal userPrincipal = session.getUserPrincipal();
        if (userPrincipal != null) {
            try {
                session.getBasicRemote().sendObject(new Value("username", userPrincipal.getName()));
            } catch (final IOException | EncodeException e) {
                throw new IllegalStateException(e);
            }
        }

        extension.getContext().withContext(context, () -> beanManager.fireEvent(new SessionOpened(session)));
    }

    @OnMessage
    public void onMessage(final Session session, final Request request) {
        extension.getContext().withContext(context, () -> {
            try {
                final String result = executor.execute(session, request.mode, request.command);
                session.getBasicRemote()
                        .sendObject(new Response(result));
            } catch (final IOException | EncodeException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @OnClose
    public void onClose(final Session session) {
        extension.getContext().withContext(this.context, () -> {
            beanManager.fireEvent(new SessionClosed(session));
            extension.getContext().destroy(this.context);
        });
    }

    private <T> T lookup(final BeanManager beanManager, final Class<T> type) {
        return type.cast(beanManager.getReference(beanManager.resolve(beanManager.getBeans(type)), type, beanManager.createCreationalContext(null)));
    }

    @Data
    @AllArgsConstructor
    public static class Response {
        private final String response;
    }

    @Data
    @AllArgsConstructor
    public static class Value {
        private String type;
        private String value;
    }

    @Data
    public static class Request {
        private String mode;
        private String command;
    }

    public static class RequestDecoder implements Decoder.Text<Request> {
        private Jsonb jsonb;

        @Override
        public void init(final EndpointConfig endpointConfig) {
            jsonb = JsonbBuilder.newBuilder().build();
        }

        @Override
        public boolean willDecode(final String s) {
            return s.startsWith("{");
        }

        @Override
        public Request decode(final String s) throws DecodeException {
            return jsonb.fromJson(s, Request.class);
        }

        @Override
        public void destroy() {
            ofNullable(jsonb).ifPresent(j -> {
                try {
                    j.close();
                } catch (final Exception e) {
                    // no-op
                }
            });
        }
    }

    private static abstract class JsonbEncoder<T> implements Encoder.Text<T> {
        protected Jsonb jsonb;

        @Override
        public void init(final EndpointConfig endpointConfig) {
            jsonb = JsonbBuilder.newBuilder().build();
        }

        @Override
        public void destroy() {
            ofNullable(jsonb).ifPresent(j -> {
                try {
                    j.close();
                } catch (final Exception e) {
                    // no-op
                }
            });
        }
    }

    public static class ResponseEncoder extends JsonbEncoder<Response> {
        @Override
        public String encode(final Response response) throws EncodeException {
            return jsonb.toJson(response);
        }
    }

    public static class ValueEncoder extends JsonbEncoder<Value> {
        @Override
        public String encode(final Value value) throws EncodeException {
            return jsonb.toJson(value);
        }
    }
}
