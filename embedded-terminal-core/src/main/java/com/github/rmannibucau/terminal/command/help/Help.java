package com.github.rmannibucau.terminal.command.help;

import com.github.rmannibucau.terminal.cdi.TerminalExtension;
import com.github.rmannibucau.terminal.command.Command;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.Session;
import java.util.function.BiFunction;

@ApplicationScoped
@Command(name = "help", description = "print this help")
public class Help implements BiFunction<Session, String[], String> {
    @Inject
    private TerminalExtension extension;

    @Override
    public String apply(final Session session, final String[] strings) {
        return extension.help(strings);
    }
}
