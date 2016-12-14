package com.github.rmannibucau.terminal.cdi;

import com.github.rmannibucau.terminal.cdi.scope.CommandContext;
import com.github.rmannibucau.terminal.cdi.scope.CommandScoped;
import com.github.rmannibucau.terminal.command.Command;
import com.github.rmannibucau.terminal.websocket.TerminalEndpoint;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessBean;
import javax.websocket.Session;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

public class TerminalExtension implements Extension {
    private final Map<String, Map<String, CommandMeta>> beansByMode = new HashMap<>();
    private final Collection<Throwable> errors = new ArrayList<>();

    @Getter
    private InjectionTarget<TerminalEndpoint> endpointInjectionTarget;

    @Getter
    private final CommandContext context = new CommandContext();

    private ParameterizedType commandType = new ParameterizedType() {
        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{Session.class, String[].class, String.class};
        }

        @Override
        public Type getRawType() {
            return BiFunction.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    };
    private String help;

    void addScope(@Observes final BeforeBeanDiscovery bbd, final BeanManager bm) {
        bbd.addScope(CommandScoped.class, true, false);
    }

    void addContext(@Observes final AfterBeanDiscovery abd) {
        abd.addContext(context);
    }

    void captureCommands(@Observes final ProcessBean<? extends BiFunction<Session, String[], String>> processBean, final BeanManager bm) {
        final Command cmd = processBean.getAnnotated().getAnnotation(Command.class);
        if (cmd == null) {
            return;
        }

        final Bean<? extends BiFunction<Session, String[], String>> bean = processBean.getBean();
        if (bm.isNormalScope(bean.getScope())) {
            beansByMode.computeIfAbsent(cmd.mode(), k -> new HashMap<>()).put(cmd.name(), new CommandMeta(cmd, bean, null));
        } else {
            errors.add(new IllegalArgumentException(bean + " is not using a normal scope"));
        }
    }

    void afterBoot(@Observes final AfterDeploymentValidation afterDeploymentValidation, final BeanManager bm) {
        if (errors.isEmpty()) {
            beansByMode.values().forEach(m -> m.values().forEach(c -> {
                c.instance = (BiFunction<Session, String[], String>) bm.getReference(c.bean, commandType, bm.createCreationalContext(null));
            }));
            help = "Help:\n\nSelect a mode executing 'mode xxx' then execute this mode's commands.\n\n" +
                    beansByMode.entrySet().stream()
                            .map(e -> "- " + e.getKey() + "\n" + e.getValue().entrySet().stream()
                                    .map(se -> "  - " + se.getValue().command.name() + ": " + se.getValue().command.description())
                                    .collect(joining("\n")))
                            .collect(joining("\n"));
            endpointInjectionTarget = bm.createInjectionTarget(bm.createAnnotatedType(TerminalEndpoint.class));
        } else {
            errors.forEach(afterDeploymentValidation::addDeploymentProblem);
        }
    }

    public BiFunction<Session, String[], String> get(final String mode, final String command) {
        final CommandMeta commandMeta = beansByMode.getOrDefault(ofNullable(mode).orElse("__default__"), Collections.emptyMap()).get(command);
        return ofNullable(commandMeta).map(c -> c.instance).orElseGet(() -> (s, a) -> help);
    }

    public String help(final String[] unusedForNow) {
        return help;
    }

    @AllArgsConstructor
    private static final class CommandMeta {
        private final Command command;
        private final Bean<? extends BiFunction<Session, String[], String>> bean;
        private BiFunction<Session, String[], String> instance;
    }
}
