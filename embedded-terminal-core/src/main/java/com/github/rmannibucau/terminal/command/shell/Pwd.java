package com.github.rmannibucau.terminal.command.shell;

import com.github.rmannibucau.terminal.command.Command;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.Session;
import java.io.File;
import java.util.function.BiFunction;

@ApplicationScoped
@Command(mode = "shell", name = "pwd", description = "show current directory")
public class Pwd implements BiFunction<Session, String[], String> {
    @Override
    public String apply(final Session session, final String[] strings) {
        return new File(Folders.getDir(session)).getAbsolutePath();
    }
}
