package com.smartcampus;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS Application entry point.
 * Sets the base path for all API endpoints to /api/v1
 *
 * Lifecycle Note: By default JAX-RS creates a new resource class instance
 * per request. This is why all shared state (rooms, sensors) must live in
 * the singleton DataStore, not as instance fields in resource classes.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {
}
