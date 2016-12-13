package com.github.rmannibucau.terminal.command.jmx;

import com.github.rmannibucau.terminal.command.Command;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.Session;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

@ApplicationScoped
@Command(mode = "jmx", name = "ls", description = "list available mbeans")
public class ListMBeans implements BiFunction<Session, String[], String> {
    @Override
    public String apply(final Session session, final String[] strings) {
        return Stream.of(ManagementFactory.getPlatformMBeanServer().queryMBeans(null, null))
                .flatMap(Collection::stream)
                .map(i -> i.getObjectName().toString())
                .collect(joining("\n"));
    }
}
