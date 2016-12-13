package com.github.rmannibucau.terminal.cdi.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.websocket.Session;

@RequiredArgsConstructor
public class SessionClosed {
    @Getter
    private final Session session;
}
