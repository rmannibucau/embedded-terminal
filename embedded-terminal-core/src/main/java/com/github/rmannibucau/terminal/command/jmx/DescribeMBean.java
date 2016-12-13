package com.github.rmannibucau.terminal.command.jmx;

import com.github.rmannibucau.terminal.command.Command;

import javax.enterprise.context.ApplicationScoped;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.websocket.Session;
import java.lang.management.ManagementFactory;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

@ApplicationScoped
@Command(mode = "jmx", name = "describe", description = "describe a mbean")
public class DescribeMBean implements BiFunction<Session, String[], String> {
    @Override
    public String apply(final Session session, final String[] strings) {
        if (strings == null || strings.length != 1) {
            return "describe needs an object name";
        }
        try {
            final MBeanInfo mBeanInfo = ManagementFactory.getPlatformMBeanServer().getMBeanInfo(new ObjectName(strings[0]));
            return strings[0] +
                    "\n\n  Attributes:\n" +
                    Stream.of(mBeanInfo.getAttributes())
                            .map(a -> "- " + a.getName() + '(' + a.getType() + ')')
                            .collect(joining("\n")) +
                    "\n\nOperations:\n" +
                    Stream.of(mBeanInfo.getOperations())
                            .map(o -> "- " + o.getReturnType() + " " + o.getName() + '(' +
                                    Stream.of(o.getSignature()).map(s -> s.getType() + " " + s.getName()).collect(joining(", ")) + ')')
                            .collect(joining("\n"));
        } catch (final InstanceNotFoundException | IntrospectionException | ReflectionException | MalformedObjectNameException e) {
            return e.getMessage();
        }
    }
}
