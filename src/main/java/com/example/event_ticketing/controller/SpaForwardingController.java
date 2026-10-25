package com.example.event_ticketing.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the single-page React app for client-side routes. In production the built frontend is
 * bundled into the app's static resources and served from the same origin as the API, so there is
 * no CORS boundary. Deep links and refreshes (e.g. /browse, /event/{id}) must return index.html
 * rather than 404, so the browser-side router can take over.
 *
 * Only forwards no-extension GET paths whose first segment is not api/actuator/h2-console, so real
 * API calls, the health endpoint and static files (which contain a '.') are left untouched. In dev
 * the frontend is served by Vite, so this forwarding is a production concern.
 */
@Controller
public class SpaForwardingController {

    @GetMapping(value = {
            "/{path:^(?!api$|actuator$|h2-console$)[^.]*}",
            "/{path:^(?!api$|actuator$|h2-console$)[^.]*}/{sub:[^.]*}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
