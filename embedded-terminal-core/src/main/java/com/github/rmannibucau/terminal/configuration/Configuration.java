package com.github.rmannibucau.terminal.configuration;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

// no cdi there since we'll use it potentially before the container started
@NoArgsConstructor(access = PRIVATE)
public class Configuration { // TODO: enhance this by not using only system props
    public static boolean isDisabled() {
        return Boolean.getBoolean("terminal.disabled");
    }

    public static String terminalMappingBase() {
        return System.getProperty("terminal.mapping", "/terminal");
    }

    public static String environment() {
        return System.getProperty("terminal.environment", "production");
    }
}
