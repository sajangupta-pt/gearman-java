package net.johnewart.gearman.server.web;

import com.yammer.metrics.reporting.AdminServlet;
import com.yammer.metrics.reporting.MetricsServlet;
import net.johnewart.gearman.server.config.ServerConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

public class WebListener {
    private final Logger LOG = LoggerFactory.getLogger(WebListener.class);
    private final static String TEMPLATE_PATH = "net/johnewart/gearman/server/web/templates";
    private final ServerConfiguration serverConfiguration;

    public WebListener(ServerConfiguration serverConfiguration) {
        this.serverConfiguration = serverConfiguration;
    }

    public void start() throws Exception {
        LOG.info("Listening on " + ":" + serverConfiguration.httpPort);

        final Server httpServer = new Server(serverConfiguration.httpPort);
        final HandlerList handlerList = new HandlerList();
        final MetricsServlet metricsServlet = new MetricsServlet(true);
        //final HealthCheckRegistry healthChecks = new HealthCheckRegistry();

        final AdminServlet adminServlet = new AdminServlet();
        final GearmanServlet gearmanServlet =
                new GearmanServlet(serverConfiguration.jobQueueMonitor, serverConfiguration.jobManager);
        final DashboardServlet dashboardServlet =
                new DashboardServlet(serverConfiguration.jobQueueMonitor, serverConfiguration.jobManager);

        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final URL templateURL = classLoader.getResource(TEMPLATE_PATH);
        if (templateURL != null) {
            final String webResourceDir = templateURL.toExternalForm();
            final ResourceHandler resourceHandler = new ResourceHandler();
            final ContextHandler resourceContext = new ContextHandler("/static");
            //final ServletContainer container = new ServletContainer();
            //final ServletHolder h = new ServletHolder(container);
            final ServletContextHandler servletHandler = new ServletContextHandler(
                    ServletContextHandler.SESSIONS);
            resourceHandler.setResourceBase(webResourceDir);
            resourceContext.setHandler(resourceHandler);
            servletHandler.setContextPath("/");

            servletHandler.addServlet(new ServletHolder(gearmanServlet), "/gearman/*");
            servletHandler.addServlet(new ServletHolder(metricsServlet), "/metrics/*");
            servletHandler.addServlet(new ServletHolder(adminServlet),   "/admin/*");
            servletHandler.addServlet(new ServletHolder(dashboardServlet),  "/");

            handlerList.addHandler(resourceContext);
            handlerList.addHandler(servletHandler);

            httpServer.setHandler(handlerList);
            httpServer.start();
        } else {
            throw new IllegalArgumentException("Template path inaccessible / does not exist.");
        }
    }

}