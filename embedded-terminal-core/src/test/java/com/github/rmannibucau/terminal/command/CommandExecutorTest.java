package com.github.rmannibucau.terminal.command;

import org.apache.meecrowave.junit.MonoMeecrowave;
import org.apache.meecrowave.testing.Injector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.websocket.Session;
import java.io.File;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MonoMeecrowave.Runner.class)
public class CommandExecutorTest {
    @Inject
    private CommandExecutor executor;

    private Session session;

    @Before
    public void mockSession() {
        Injector.inject(this);

        final Map<String, Object> props = new HashMap<>();
        session = Session.class.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[]{Session.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUserProperties")) {
                        return props;
                    }
                    return null;
                }));
    }

    @Test
    public void pwd() {
        final String result = executor.execute(session, "shell", "pwd");
        assertEquals(result, new File(System.getProperty("catalina.base")).getAbsolutePath());
    }

    @Test
    public void cd() {
        executor.execute(session, "shell", "cd conf");
        assertEquals(executor.execute(session, "shell", "pwd"), new File(System.getProperty("catalina.base"), "conf").getAbsolutePath());
        executor.execute(session, "shell", "cd ..");
        assertEquals(executor.execute(session, "shell", "pwd"), new File(System.getProperty("catalina.base")).getAbsolutePath());
    }

    @Test
    public void ls() {
        { // default
            final String ls = executor.execute(session, "shell", "ls | sort");
            assertTrue(ls, ls.contains("conf\nlib\nlogs\ntemp\nwebapps\nwork"));
        }
        { // relative
            final String ls = executor.execute(session, "shell", "ls . | sort");
            assertTrue(ls, ls.contains("conf\nlib\nlogs\ntemp\nwebapps\nwork"));
        }
        { // absolute
            final String ls = executor.execute(session, "shell", "ls " + new File("target/classes/com").getAbsolutePath() + " | sort");
            assertTrue(ls, ls.contains("github"));
        }
    }

    @Test
    public void piping() {
        assertEquals("6", executor.execute(session, "shell", "ls | grep -v target | wc -l"));
        assertEquals("meecrowave", executor.execute(session, "shell", "echo -n ${catalina.base} | normalize-slashes | sed 's/.*\\\\/([^\\\\/]*)-\\\\p{Digit}*/$1/'"));
        assertEquals("line", executor.execute(session, "shell", "echo 'line\nline\n' | uniq"));
    }
}
