package com.github.rmannibucau.terminal.command.network;

import com.github.rmannibucau.terminal.command.Arg;
import com.github.rmannibucau.terminal.command.Command;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

@ApplicationScoped
public class ScanPorts {
    @Command(mode = "network", description = "test all ports on the provided host")
    public String scan(@Arg(value = "host", required = true, description = "the host to scan") final String host,
                       @Arg(value = "timeout", description = "the timeout to connect for each port of 1-65535 range") final int timeout) {
        final int ms = (int) TimeUnit.SECONDS.toMillis(timeout <= 0 ? 15 : timeout);
        return IntStream.rangeClosed(1, 65535)
                .map(i -> {
                    try (final Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress(host, i), ms);
                        socket.getInputStream().close();
                        return i;
                    } catch (final IOException e) {
                        return -1;
                    }
                })
                .filter(i -> i > 0)
                .sorted()
                .mapToObj(Integer::toString)
                .collect(joining("\n"));
    }
}
