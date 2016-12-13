package com.github.rmannibucau.terminal.command.shell;

import com.github.rmannibucau.terminal.command.Command;
import org.unix4j.Unix4j;
import org.unix4j.unix.ls.LsOption;
import org.unix4j.unix.ls.LsOptions;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.Session;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.function.BiFunction;
import java.util.stream.Stream;

@ApplicationScoped
@Command(mode = "shell", name = "ls", description = "list files in a directory")
public class Ls implements BiFunction<Session, String[], String> {
    @Override
    public String apply(final Session session, final String[] strings) { // TODO: handle -xxx
        final Collection<String> paths = new LinkedList<>();
        final Collection<LsOption> options = new LinkedList<>();
        if (strings != null && strings.length > 0) {
            Stream.of(strings).forEach(next -> {
                if (next.startsWith("-")) {
                    final String opt = next.replace("-", "");
                    options.add(LsOption.findByAcronym(opt.charAt(0)));
                } else {
                    final File dir = new File(next);
                    final File file = new File(Folders.getDir(session), next);
                    try {
                        paths.add(dir.isAbsolute() ? next : file.getCanonicalFile().getAbsolutePath());
                    } catch (final IOException e) {
                        paths.add(file.getAbsolutePath());
                    }
                }
            });
        } else {
            paths.add(Folders.getDir(session));
        }

        return Unix4j.ls(new LsOptions.Default(options.toArray(new LsOption[options.size()])), paths.toArray(new String[paths.size()]))
                .toStringResult()
                .trim(); // unix4j adds empty lines which are not always welcomed
    }
}
