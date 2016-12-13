package com.github.rmannibucau.terminal.cdi.scope;

import lombok.RequiredArgsConstructor;

import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

public class CommandContext implements AlterableContext {
    private final ThreadLocal<Delegate> delegate = new ThreadLocal<>();

    @Override
    public Class<? extends Annotation> getScope() {
        return CommandScoped.class;
    }

    @Override
    public <T> T get(final Contextual<T> component, final CreationalContext<T> creationalContext) {
        return delegate.get().get(component, creationalContext);
    }

    @Override
    public <T> T get(final Contextual<T> component) {
        return delegate.get().get(component);
    }

    @Override
    public boolean isActive() {
        final Delegate instance = delegate.get();
        if (instance == null) {
            delegate.remove();
            return false;
        }
        return instance.isActive();
    }

    @Override
    public void destroy(final Contextual<?> contextual) {
        delegate.get().destroy(contextual);
    }

    public Delegate newInstance() {
        return new Delegate();
    }

    public void withContext(final Delegate value, final Runnable task) {
        delegate.set(value);
        try {
            task.run();
        } finally {
            delegate.remove();
        }
    }

    public void destroy(final Delegate delegate) {
        new ArrayList<>(delegate.componentInstanceMap.keySet()).forEach(delegate::destroy);
    }

    public class Delegate implements AlterableContext {
        private final Map<Contextual<?>, BeanInstanceBag<?>> componentInstanceMap = new HashMap<>();

        private Delegate() {
            // no-op
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return CommandScoped.class;
        }

        @Override
        public <T> T get(final Contextual<T> component, final CreationalContext<T> creationalContext) {
            final BeanInstanceBag value = new BeanInstanceBag<>(creationalContext);
            return (T) ofNullable(componentInstanceMap.putIfAbsent(component, value)).orElse(value).create(component);
        }

        @Override
        public <T> T get(final Contextual<T> component) {
            return (T) ofNullable(componentInstanceMap.get(component)).map(b -> b.beanInstance).orElse(null);
        }

        @Override
        public void destroy(final Contextual<?> contextual) {
            ofNullable(componentInstanceMap.remove(contextual)).filter(b -> b.beanInstance != null).ifPresent(b -> {
                final Contextual c = contextual;
                c.destroy(b.beanInstance, b.beanCreationalContext);
                b.beanCreationalContext.release();
            });
        }

        @Override
        public boolean isActive() {
            return true;
        }
    }

    @RequiredArgsConstructor
    private static class BeanInstanceBag<T> {
        private final CreationalContext<T> beanCreationalContext;
        private T beanInstance;

        public T create(final Contextual<T> contextual) {
            if (beanInstance == null) {
                beanInstance = contextual.create(beanCreationalContext);
            }
            return beanInstance;
        }
    }
}
