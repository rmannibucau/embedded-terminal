package com.github.rmannibucau.terminal.cdi;

import com.github.rmannibucau.terminal.cdi.scope.CommandContext;
import com.github.rmannibucau.terminal.cdi.scope.CommandScoped;
import com.github.rmannibucau.terminal.command.Arg;
import com.github.rmannibucau.terminal.command.Command;
import com.github.rmannibucau.terminal.websocket.TerminalEndpoint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ClassUtils;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.util.TypeLiteral;
import javax.websocket.Session;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class TerminalExtension implements Extension {
    private static final Type TYPE = new TypeLiteral<BiFunction<Session, String[], String>>() {
    }.getType();

    private final Map<String, Map<String, CommandMeta>> beansByMode = new HashMap<>();
    private final Collection<Throwable> errors = new ArrayList<>();

    @Getter
    private InjectionTarget<TerminalEndpoint> endpointInjectionTarget;

    @Getter
    private final CommandContext context = new CommandContext();

    private String help;

    void addScope(@Observes final BeforeBeanDiscovery bbd, final BeanManager bm) {
        bbd.addScope(CommandScoped.class, true, false);
    }

    void addContext(@Observes final AfterBeanDiscovery abd) {
        abd.addContext(context);
    }

    void captureCommands(@Observes final ProcessBean<? extends BiFunction<Session, String[], String>> processBean, final BeanManager bm) {
        final Command cmd = processBean.getAnnotated().getAnnotation(Command.class);
        if (cmd != null) {
            final Bean<? extends BiFunction<Session, String[], String>> bean = processBean.getBean();
            if (bm.isNormalScope(bean.getScope())) {
                final String name = cmd.name();
                if (name.isEmpty()) {
                    errors.add(new IllegalArgumentException(bean + " should set the command name"));
                }
                beansByMode.computeIfAbsent(cmd.mode(), k -> new HashMap<>()).put(name, new CommandMeta(cmd.name(), cmd.description(), null) {
                    @Override
                    public void load(final BeanManager lazyBm) {
                        instance = (BiFunction<Session, String[], String>) lazyBm.getReference(bean, TYPE, lazyBm.createCreationalContext(null));
                    }
                });
            } else {
                errors.add(new IllegalArgumentException(bean + " is not using a normal scope"));
            }
        } else if (AnnotatedType.class.isInstance(processBean.getAnnotated())) { // check all methods
            final AnnotatedType<?> annotatedType = AnnotatedType.class.cast(processBean.getAnnotated());
            annotatedType.getMethods().stream()
                    .filter(m -> m.isAnnotationPresent(Command.class))
                    .forEach(method -> {
                        final Bean<? extends BiFunction<Session, String[], String>> bean = processBean.getBean();
                        if (bm.isNormalScope(bean.getScope())) {
                            final Method m = method.getJavaMember();
                            final Type[] genericParameterTypes = m.getGenericParameterTypes();
                            final Boolean[] required = method.getParameters().stream()
                                    .map(p -> ofNullable(p.getAnnotation(Arg.class)).map(Arg::required).orElse(false))
                                    .toArray(Boolean[]::new);
                            final Map<String, Integer> names = method.getParameters().stream()
                                    .collect(toMap(p -> ofNullable(p.getAnnotation(Arg.class)).map(Arg::value).orElse(""), AnnotatedParameter::getPosition));

                            final Command c = method.getAnnotation(Command.class);
                            String name = c.name();
                            name = name.isEmpty() ? m.getName() : name;
                            beansByMode.computeIfAbsent(c.mode(), k -> new HashMap<>()).put(name, new CommandMeta(name, c.description() +
                                    method.getParameters().stream()
                                            .filter(p -> p.isAnnotationPresent(Arg.class))
                                            .map(p -> {
                                                final Arg arg = p.getAnnotation(Arg.class);
                                                return "    - --" + arg.value() + (arg.required() ? "*" : "") + ": " + arg.description();
                                            })
                                            .collect(joining("\n", "\n", "")), null) {
                                @Override
                                public void load(final BeanManager lazyBm) {
                                    final Object reference = lazyBm.getReference(bean, annotatedType.getJavaClass(), lazyBm.createCreationalContext(null));
                                    instance = (s, args) -> {
                                        final Object[] params;
                                        try {
                                            params = buildArgs(s, args, names, required, genericParameterTypes);
                                        } catch (final IllegalArgumentException iae) {
                                            return iae.getMessage();
                                        }
                                        try {
                                            return String.valueOf(m.invoke(reference, params));
                                        } catch (final IllegalAccessException e) {
                                            throw new IllegalStateException(e);
                                        } catch (final InvocationTargetException e) {
                                            throw new IllegalStateException(e.getCause());
                                        }
                                    };
                                }
                            });
                        } else {
                            errors.add(new IllegalArgumentException(bean + " is not using a normal scope"));
                        }
                    });
        }
    }

    void afterBoot(@Observes final AfterDeploymentValidation afterDeploymentValidation, final BeanManager bm) {
        if (errors.isEmpty()) {
            beansByMode.values().forEach(m -> m.values().forEach(c -> c.load(bm)));
            help = "Help:\n\nSelect a mode executing 'mode xxx' then execute this mode's commands.\n\n" +
                    beansByMode.entrySet().stream()
                            .map(e -> "- " + e.getKey() + "\n" + e.getValue().entrySet().stream()
                                    .map(se -> "  - " + se.getValue().name + ": " + se.getValue().description)
                                    .collect(joining("\n")))
                            .collect(joining("\n"));
            endpointInjectionTarget = bm.createInjectionTarget(bm.createAnnotatedType(TerminalEndpoint.class));
        } else {
            errors.forEach(afterDeploymentValidation::addDeploymentProblem);
        }
    }

    private Object[] buildArgs(final Session session, final String[] args,
                               final Map<String, Integer> names, final Boolean[] required, final Type[] genericParameterTypes) {
        if (args == null) {
            if (Stream.of(required).anyMatch(r -> r)) {
                throw new IllegalArgumentException("Missing parameter, check usage please");
            }
        }

        final Object[] objects = new Object[genericParameterTypes.length];

        // set the session first and default values (primitives)
        for (int i = 0; i < genericParameterTypes.length; i++) {
            if (genericParameterTypes[i] == Session.class) {
                objects[i] = session;
            } else if (genericParameterTypes[i] == int.class) {
                objects[i] = 0;
            } else if (genericParameterTypes[i] == long.class) {
                objects[i] = 0L;
            } else if (genericParameterTypes[i] == boolean.class) {
                objects[i] = false;
            }
        }

        if (args == null) {
            return objects;
        }

        // then check args
        final Collection<Integer> filled = new HashSet<>();
        for (int i = 0; i < args.length; i++) {
            final int idx = i;
            names.keySet().stream().filter(k -> args[idx].startsWith(k) || args[idx].startsWith("--" + k)).forEach(k -> {
                final Integer paramIdx = names.get(k);
                final int eq = args[idx].indexOf('=');
                objects[paramIdx] = eq > 0 ? convert(genericParameterTypes[paramIdx], args[idx].substring(eq + 1)) : true;
                filled.add(paramIdx);
            });
        }

        // validate required params
        for (int i = 0; i < required.length; i++) {
            if (required[i]) {
                if (filled.contains(i)) {
                    continue;
                }
                throw new IllegalArgumentException("Missing parameter, check usage");
            }
        }

        return objects;
    }

    // primitives, basic collections
    private Object convert(final Type type, final String value) {
        if (String.class == type) {
            return value;
        }
        if (Class.class.isInstance(type)) {
            final Class clazz = Class.class.cast(type);
            if (clazz.isEnum()) {
                return Enum.valueOf(clazz, value);
            }
            Class<?> prim = ClassUtils.wrapperToPrimitive(clazz);
            if (prim == null) {
                prim = clazz;
            }
            if (prim.isPrimitive()) {
                try {
                    return ClassUtils.primitiveToWrapper(prim).getMethod("valueOf", String.class).invoke(null, value);
                } catch (final IllegalAccessException e) {
                    throw new IllegalArgumentException(e);
                } catch (final InvocationTargetException e) {
                    throw new IllegalArgumentException(e.getCause());
                } catch (final NoSuchMethodException e) {
                    // no-op: continue coercion strategy
                }
            }
            if (char.class == type) {
                return value.charAt(0);
            }
        }
        if (ParameterizedType.class.isInstance(type)) {
            final ParameterizedType pt = ParameterizedType.class.cast(type);
            if (pt.getRawType() == Collection.class || pt.getRawType() == List.class) {
                return Stream.of(value.split(","))
                        .map(v -> convert(pt.getActualTypeArguments()[0], v))
                        .collect(toList());
            }
        }
        throw new IllegalArgumentException("Can't convert " + value + " to " + type);
    }

    public BiFunction<Session, String[], String> get(final String mode, final String command) {
        final CommandMeta commandMeta = beansByMode.getOrDefault(ofNullable(mode).orElse("__default__"), Collections.emptyMap()).get(command);
        return ofNullable(commandMeta).map(c -> c.instance).orElseGet(() -> (s, a) -> help);
    }

    public String help(final String[] unusedForNow) {
        return help;
    }

    @AllArgsConstructor
    private static abstract class CommandMeta {
        protected final String name;
        protected final String description;
        protected BiFunction<Session, String[], String> instance;

        public abstract void load(BeanManager bm);
    }
}
