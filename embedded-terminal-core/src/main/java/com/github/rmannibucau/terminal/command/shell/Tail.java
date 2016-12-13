package com.github.rmannibucau.terminal.command.shell;

import com.github.rmannibucau.terminal.command.Command;
import org.unix4j.Unix4j;
import org.unix4j.unix.tail.TailOption;
import org.unix4j.unix.tail.TailOptions;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.Session;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

@ApplicationScoped
@Command(mode = "shell", name = "tail", description = "tail some file")
public class Tail implements BiFunction<Session, String[], String> {
    @Override
    public String apply(final Session session, final String[] strings) {
        if (strings == null || strings.length < 2) {
            return "tail needs a count and file paramters";
        }

        final Collection<TailOption> options = new LinkedList<>();
        final List<String> args = new ArrayList<>();
        Stream.of(strings).forEach(next -> {
            if (next.startsWith("-")) {
                final String opt = next.replace("-", "");
                options.add(TailOption.findByAcronym(opt.charAt(0)));
            } else {
                args.add(next);
            }
        });
        final int count = Integer.parseInt(args.remove(0));
        return Unix4j.tail(new TailOptions.Default(options.toArray(new TailOption[options.size()])), count, args.toArray(new String[args.size()])).toStringResult();
    }
}
