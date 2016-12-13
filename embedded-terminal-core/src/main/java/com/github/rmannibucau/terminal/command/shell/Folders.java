package com.github.rmannibucau.terminal.command.shell;

import javax.websocket.Session;

public interface Folders {
    String DIR = Folders.class.getName() + ".dir";

    static String getDir(final Session session) {
        String dir = String.class.cast(session.getUserProperties().get(Folders.DIR));
        if (dir == null) {
            dir = System.getProperty("catalina.base", ".");
            session.getUserProperties().put(Folders.DIR, dir);
        }
        return dir;
    }

    static void setDir(final Session session, final String dir) {
        session.getUserProperties().put(Folders.DIR, dir);
    }
}
