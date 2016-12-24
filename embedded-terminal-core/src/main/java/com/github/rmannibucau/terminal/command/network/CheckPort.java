package com.github.rmannibucau.terminal.command.network;

import com.github.rmannibucau.terminal.command.Arg;
import com.github.rmannibucau.terminal.command.Command;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;

@ApplicationScoped
public class CheckPort {
    @Command(mode = "network", description = "test if a port is open from this server")
    public String test(@Arg(value = "host", description = "the host to use to connect") final String host,
                       @Arg(value = "port", required = true, description = "the port to use to connect") final int port,
                       @Arg(value = "timeout", description = "the timeout for the connection") final int timeout) {
        final long start = System.nanoTime();
        try (final Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ofNullable(host).orElse("localhost"), port), (int) TimeUnit.SECONDS.toMillis(timeout <= 0 ? 15 : timeout));
            socket.getInputStream().close();
            final long end = System.nanoTime();
            return "Connected in " + TimeUnit.NANOSECONDS.toMillis(end - start) + "ms";
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
