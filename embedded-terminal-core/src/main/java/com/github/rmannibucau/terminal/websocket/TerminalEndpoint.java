package com.github.rmannibucau.terminal.websocket;

import com.github.rmannibucau.terminal.cdi.TerminalExtension;
import com.github.rmannibucau.terminal.cdi.event.SessionClosed;
import com.github.rmannibucau.terminal.cdi.event.SessionOpened;
import com.github.rmannibucau.terminal.cdi.scope.CommandContext;
import com.github.rmannibucau.terminal.command.CommandExecutor;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.websocket.CloseReason;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.io.IOException;
import java.security.Principal;

import static java.util.Optional.ofNullable;

@Dependent // just to mark it as scanned, not a real cdi bean
public class TerminalEndpoint extends Endpoint {
    @Inject
    private Event<SessionOpened> sessionOpenedEvent;

    @Inject
    private Event<SessionClosed> sessionClosedEvent;

    @Inject
    private CommandExecutor executor;

    @Inject
    private TerminalExtension extension;

    private CommandContext.Delegate context;

    @Override
    public void onOpen(final Session session, final EndpointConfig endpointConfig) {
        context = extension.getContext().newInstance();

        final Principal userPrincipal = session.getUserPrincipal();
        if (userPrincipal != null) {
            try {
                session.getBasicRemote().sendObject(new Value("username", userPrincipal.getName()));
            } catch (final IOException | EncodeException e) {
                throw new IllegalStateException(e);
            }
        }

        extension.getContext().withContext(context, () -> sessionOpenedEvent.fire(new SessionOpened(session)));

        session.addMessageHandler(Request.class, request -> TerminalEndpoint.this.onMessage(session, request));
    }

    private void onMessage(final Session session, final Request request) {
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

    @Override
    public void onClose(final Session session, final CloseReason reason) {
        extension.getContext().withContext(this.context, () -> {
            sessionClosedEvent.fire(new SessionClosed(session));
            extension.getContext().destroy(this.context);
        });
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
