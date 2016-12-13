package com.github.rmannibucau.terminal.command.shell;

import com.github.rmannibucau.terminal.command.Command;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.Session;
import java.io.File;
import java.io.IOException;
import java.util.function.BiFunction;

@ApplicationScoped
@Command(mode = "shell", name = "cd", description = "change current directory")
public class Cd implements BiFunction<Session, String[], String> {
    @Override
    public String apply(final Session session, final String[] strings) {
        if (strings.length != 1) {
            return "only one argument is supported by 'cd'";
        }

        final File file = new File(strings[strings.length - 1]);
        final File target = file.isAbsolute() ? file : new File(Folders.getDir(session), strings[strings.length - 1]);
        if (!target.exists()) {
            return target + " doesn't exist";
        }

        try {
            Folders.setDir(session, target.getCanonicalFile().getAbsolutePath());
        } catch (final IOException e) {
            Folders.setDir(session, target.getAbsolutePath());
        }
        return "";
    }
}
