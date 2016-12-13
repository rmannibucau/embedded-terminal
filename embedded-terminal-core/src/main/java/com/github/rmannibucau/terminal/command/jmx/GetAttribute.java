package com.github.rmannibucau.terminal.command.jmx;

import com.github.rmannibucau.terminal.command.Command;

import javax.enterprise.context.ApplicationScoped;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.websocket.Session;
import java.lang.management.ManagementFactory;
import java.util.function.BiFunction;

@ApplicationScoped
@Command(mode = "jmx", name = "get", description = "read a mbean attribute")
public class GetAttribute implements BiFunction<Session, String[], String> {
    @Override
    public String apply(final Session session, final String[] strings) {
        if (strings == null || strings.length != 2) {
            return "get needs an object name and an attribute";
        }
        try {
            return String.valueOf(ManagementFactory.getPlatformMBeanServer().getAttribute(new ObjectName(strings[0]), strings[1]));
        } catch (final MBeanException | AttributeNotFoundException | InstanceNotFoundException | ReflectionException | MalformedObjectNameException e) {
            return e.getMessage();
        }
    }
}
