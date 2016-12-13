package com.github.rmannibucau.terminal.command.shell;

import com.github.rmannibucau.terminal.command.Command;
import org.unix4j.Unix4j;
import org.unix4j.unix.head.HeadOption;
import org.unix4j.unix.head.HeadOptions;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.Session;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

@ApplicationScoped
@Command(mode = "shell", name = "head", description = "head some file")
public class Head implements BiFunction<Session, String[], String> {
    @Override
    public String apply(final Session session, final String[] strings) {
        if (strings == null || strings.length < 2) {
            return "head needs a count and file paramters";
        }

        final Collection<HeadOption> options = new LinkedList<>();
        final List<String> args = new ArrayList<>();
        Stream.of(strings).forEach(next -> {
            if (next.startsWith("-")) {
                final String opt = next.replace("-", "");
                options.add(HeadOption.findByAcronym(opt.charAt(0)));
            } else {
                args.add(next);
            }
        });
        final int count = Integer.parseInt(args.remove(0));
        return Unix4j.head(new HeadOptions.Default(options.toArray(new HeadOption[options.size()])), count, args.toArray(new String[args.size()])).toStringResult();
    }
}
