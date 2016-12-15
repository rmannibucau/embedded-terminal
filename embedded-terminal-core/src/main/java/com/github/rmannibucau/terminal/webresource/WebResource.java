package com.github.rmannibucau.terminal.webresource;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class WebResource extends HttpServlet { // actually a kind of proxy resource rewriting needed parts
    private static final byte[] NULL = new byte[0];

    private boolean dev;
    private String mapping;
    private String wsMapping;

    private final ConcurrentMap<String, byte[]> cache = new ConcurrentHashMap<>();

    @Override
    public void init() throws ServletException {
        super.init();
        dev = "dev".equals(getServletConfig().getInitParameter("environment"));
        mapping = getServletConfig().getInitParameter("mapping");
        wsMapping = getServletConfig().getInitParameter("wsMapping");
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        final String relative = req.getRequestURI().substring(req.getContextPath().length() + mapping.length());
        final String resource = "/embedded-terminal/web-resources" + (relative.isEmpty() || relative.equals("/") ? "/index.html" : relative);
        final byte[] content = dev ? doLoad(req, resource) : cache.computeIfAbsent(resource, r -> {
            try {
                return doLoad(req, r);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        });

        setContentType(req, resp, relative);

        if (content != NULL) {
            resp.getOutputStream().write(content);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // could be cached too but not that important
    private void setContentType(final HttpServletRequest req, final HttpServletResponse resp, final String resource) {
        final int lastDot = resource.lastIndexOf('.');
        if (lastDot < 0) {
            resp.setContentType("text/html");
        } else if (lastDot > 0) {
            final String ext = resource.substring(lastDot + 1);
            final String mime = req.getServletContext().getMimeType(ext);
            if (mime != null) {
                resp.setContentType(mime);
            } else {
                switch (ext) {
                    case "css":
                        resp.setContentType("text/css");
                        break;
                    case "js":
                        resp.setContentType("application/javascript");
                        break;
                    default:
                        resp.setContentType("text/html");
                }
            }
        }
    }

    private byte[] doLoad(final HttpServletRequest req, final String resource) throws IOException {
        try (final InputStream is = req.getServletContext().getClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                // depends the container loader, so try without prefixing with "/" too
                return resource.startsWith("/") ? doLoad(req, resource.substring(1)) : NULL;
            }

            final ByteArrayOutputStream mem = new ByteArrayOutputStream();
            final byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) >= 0) {
                mem.write(buffer, 0, read);
            }

            // some post processing to switch base on some well known resources, easier than guessing it on client side
            // if needed we can make it another config or use a rest endpoint to provide it
            if (resource.endsWith("index.html")) { // absolute path for resources
                return new String(mem.toByteArray(), StandardCharsets.UTF_8)
                        .replace("href=\"", "href=\"" + req.getContextPath() + mapping + '/')
                        .replace("src=\"js/", "src=\"" + req.getContextPath() + mapping + "/js/")
                        .getBytes(StandardCharsets.UTF_8);
            }
            if (resource.endsWith(".js") && resource.contains("embedded-terminal/web-resources/js/app.")) { // switch ws endpoint
                return new String(mem.toByteArray(), StandardCharsets.UTF_8)
                        .replace("\"/terminal/session\"", '"' + req.getContextPath() + wsMapping + '"')
                        .getBytes(StandardCharsets.UTF_8);
            }
            // else polyfill.js and vendor.js are fine OOTB
            return mem.toByteArray();
        }
    }
}
