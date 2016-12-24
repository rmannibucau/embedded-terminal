package com.github.rmannibucau.terminal.command;

import com.github.rmannibucau.terminal.cdi.TerminalExtension;
import org.unix4j.Unix4j;
import org.unix4j.builder.Unix4jCommandBuilder;
import org.unix4j.unix.grep.GrepOption;
import org.unix4j.unix.grep.GrepOptions;
import org.unix4j.unix.head.HeadOption;
import org.unix4j.unix.head.HeadOptions;
import org.unix4j.unix.sort.SortOption;
import org.unix4j.unix.sort.SortOptions;
import org.unix4j.unix.tail.TailOption;
import org.unix4j.unix.tail.TailOptions;
import org.unix4j.unix.uniq.UniqOption;
import org.unix4j.unix.uniq.UniqOptions;
import org.unix4j.unix.wc.WcOption;
import org.unix4j.unix.wc.WcOptions;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.websocket.Session;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;

@ApplicationScoped
public class CommandExecutor {
    @Inject
    private BeanManager beanManager;

    @Inject
    private TerminalExtension extension;

    public String execute(final Session session, final String mode, final String command) {
        if (command == null || command.isEmpty()) {
            return "Command is empty";
        }

        final Collection<List<String>> commands = parse(command);
        final Iterator<List<String>> cmdIterator = commands.iterator();

        final List<String> opts = cmdIterator.next();
        final Iterator<String> iterator = opts.iterator();
        final String cmd = iterator.next();
        iterator.remove();
        BiFunction<Session, String[], String> commandHandler = extension.get(mode, cmd);
        if (commandHandler == null) { // try a passthrough handler
            commandHandler = extension.get(mode, "");
            if (commandHandler == null) {
                return "No command matching '" + command + "'";
            } else { // add it back
                opts.add(0, cmd);
            }
        }

        // not a real streaming but good enough for now, if we impl websocket streaming we need to rethink it
        String result;
        try {
            result = commandHandler.apply(session, opts.toArray(new String[opts.size()]));
        } catch (final RuntimeException re) {
            final StringWriter writer = new StringWriter();
            re.printStackTrace(new PrintWriter(writer));
            result = "[ERROR] " + writer.toString();
        }
        if (cmdIterator.hasNext()) { // handle pipes
            return pipes(cmdIterator, result);
        }
        return result;
    }

    private String pipes(final Iterator<List<String>> cmdIterator, final String result) {
        Unix4jCommandBuilder builder = Unix4j.fromString(result);

        while (cmdIterator.hasNext()) {
            final Collection<String> pipeOpts = cmdIterator.next();
            final Iterator<String> pipeIterator = pipeOpts.iterator();
            final String pipeCmd = pipeIterator.next();
            pipeIterator.remove();
            switch (pipeCmd) {
                case "normalize-slashes": { // just allows to have portable commands accross win/lin
                    builder = Unix4j.fromString(builder.toStringResult().replace('\\', '/'));
                    break;
                }
                case "grep": {
                    if (pipeOpts.isEmpty()) {
                        return "grep needs at least a pattern";
                    }

                    String pattern = null;
                    final Collection<GrepOption> options = new LinkedList<>();
                    while (pipeIterator.hasNext()) {
                        final String next = pipeIterator.next();
                        if (next.startsWith("-")) {
                            final String opt = next.replace("-", "");
                            options.add(GrepOption.findByAcronym(opt.charAt(0)));
                        } else if (pattern != null) {
                            return "grep only supports one pattern";
                        } else {
                            pattern = next;
                        }
                    }
                    if (pattern == null) {
                        return "missing grep pattern";
                    }
                    builder.grep(new GrepOptions.Default(options.toArray(new GrepOption[options.size()])), pattern);
                    break;
                }
                case "head": {
                    Integer count = null;
                    final Collection<HeadOption> options = new LinkedList<>();
                    while (pipeIterator.hasNext()) {
                        final String next = pipeIterator.next();
                        if (next.startsWith("-")) {
                            final String opt = next.replace("-", "");
                            options.add(HeadOption.findByAcronym(opt.charAt(0)));
                        } else if (count != null) {
                            return "head only supports one pattern";
                        } else {
                            count = Integer.parseInt(next);
                        }
                    }
                    builder.head(new HeadOptions.Default(options.toArray(new HeadOption[options.size()])), count == null ? 1 : count);
                    break;
                }
                case "tail": {
                    Integer count = null;
                    final Collection<TailOption> options = new LinkedList<>();
                    while (pipeIterator.hasNext()) {
                        final String next = pipeIterator.next();
                        if (next.startsWith("-")) {
                            final String opt = next.replace("-", "");
                            options.add(TailOption.findByAcronym(opt.charAt(0)));
                        } else if (count != null) {
                            return "head only supports one pattern";
                        } else {
                            count = Integer.parseInt(next);
                        }
                    }
                    builder.tail(new TailOptions.Default(options.toArray(new TailOption[options.size()])), count == null ? 1 : count);
                    break;
                }
                case "sed": {
                    String pattern = null;
                    while (pipeIterator.hasNext()) {
                        final String next = pipeIterator.next();
                        if (pattern != null) {
                            return "sed only supports one pattern";
                        } else {
                            pattern = next;
                        }
                    }
                    builder.sed(pattern);
                    break;
                }
                case "sort": {
                    final Collection<SortOption> options = new LinkedList<>();
                    while (pipeIterator.hasNext()) {
                        final String next = pipeIterator.next();
                        if (next.startsWith("-")) {
                            final String opt = next.replace("-", "");
                            options.add(SortOption.findByAcronym(opt.charAt(0)));
                        } else {
                            return "sort doesnt support option " + next;
                        }
                    }
                    builder.sort(new SortOptions.Default(options.toArray(new SortOption[options.size()])));
                    break;
                }
                case "wc": {
                    final Collection<WcOption> options = new LinkedList<>();
                    while (pipeIterator.hasNext()) {
                        final String next = pipeIterator.next();
                        if (next.startsWith("-")) {
                            final String opt = next.replace("-", "");
                            options.add(WcOption.findByAcronym(opt.charAt(0)));
                        } else {
                            return "sort doesnt support option " + next;
                        }
                    }
                    builder.wc(new WcOptions.Default(options.toArray(new WcOption[options.size()])));
                    break;
                }
                case "uniq": {
                    final Collection<UniqOption> options = new LinkedList<>();
                    while (pipeIterator.hasNext()) {
                        final String next = pipeIterator.next();
                        if (next.startsWith("-")) {
                            final String opt = next.replace("-", "");
                            options.add(UniqOption.findByAcronym(opt.charAt(0)));
                        } else {
                            return "sort doesnt support option " + next;
                        }
                    }
                    builder.uniq(new UniqOptions.Default(options.toArray(new UniqOption[options.size()])));
                    break;
                }
                default:
                    return "unsupported " + pipeCmd;
            }
        }

        return builder.toStringResult();
    }

    private Collection<List<String>> parse(final String raw) {
        final Collection<List<String>> results = new LinkedList<>();

        List<String> result = new LinkedList<>();

        Character end = null;
        boolean escaped = false;
        final StringBuilder current = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            final char c = raw.charAt(i);
            if (escaped) {
                escaped = false;
                current.append(c);
            } else if ((end != null && end == c) || (c == ' ' && end == null)) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
                end = null;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"' || c == '\'') {
                end = c;
            } else if (c == '|') {
                results.add(result);
                result = new LinkedList<>();
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        if (!result.isEmpty()) {
            results.add(result);
        }

        return results;
    }
}
