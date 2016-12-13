package com.github.rmannibucau.terminal.command.shell;

import com.github.rmannibucau.terminal.command.Command;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.unix4j.Unix4j;
import org.unix4j.unix.echo.EchoOption;
import org.unix4j.unix.echo.EchoOptions;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.Session;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

@ApplicationScoped
@Command(mode = "shell", name = "echo", description = "echo some string(s), system properties can use placeholder syntax")
public class Echo implements BiFunction<Session, String[], String> {
    @Override
    public String apply(final Session session, final String[] strings) {
        final Collection<EchoOption> options = new LinkedList<>();
        final Collection<String> args = new ArrayList<>();
        Stream.of(ofNullable(strings).orElseGet(() -> new String[0])).forEach(next -> {
            if (next.startsWith("-")) {
                final String opt = next.replace("-", "");
                options.add(EchoOption.findByAcronym(opt.charAt(0)));
            } else {
                args.add(next);
            }
        });
        return StrSubstitutor.replaceSystemProperties(
                Unix4j.echo(new EchoOptions.Default(options.toArray(new EchoOption[options.size()])), args.toArray(new String[args.size()]))
                        .toStringResult());
    }
}
