package com.seekora.listener;

import com.seekora.config.DatabaseConfig;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/** Starts the connection pool with the web application and closes it on shutdown. */
@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        DatabaseConfig.init();
        event.getServletContext().log("Seekora backend started - MySQL pool ready");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        DatabaseConfig.shutdown();
        event.getServletContext().log("Seekora backend stopped - MySQL pool closed");
    }
}
