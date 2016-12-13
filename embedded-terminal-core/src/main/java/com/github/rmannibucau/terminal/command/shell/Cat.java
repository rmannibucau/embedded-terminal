package com.github.rmannibucau.terminal.command.shell;

import com.github.rmannibucau.terminal.command.Command;
import org.unix4j.Unix4j;
import org.unix4j.unix.cat.CatOption;
import org.unix4j.unix.cat.CatOptions;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.Session;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.function.BiFunction;
import java.util.stream.Stream;

@ApplicationScoped
@Command(mode = "shell", name = "cat", description = "cat some file")
public class Cat implements BiFunction<Session, String[], String> {
    @Override
    public String apply(final Session session, final String[] strings) {
        if (strings == null || strings.length == 0) {
            return "cat needs a parameter";
        }

        final Collection<CatOption> options = new LinkedList<>();
        final Collection<String> args = new ArrayList<>();
        Stream.of(strings).forEach(next -> {
            if (next.startsWith("-")) {
                final String opt = next.replace("-", "");
                options.add(CatOption.findByAcronym(opt.charAt(0)));
            } else {
                args.add(next);
            }
        });
        return Unix4j.cat(new CatOptions.Default(options.toArray(new CatOption[options.size()])), args.toArray(new String[args.size()])).toStringResult();
    }
}
