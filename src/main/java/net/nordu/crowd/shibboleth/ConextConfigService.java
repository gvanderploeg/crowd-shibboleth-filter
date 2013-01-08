package net.nordu.crowd.shibboleth;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.surfnet.crowd.model.ConextConfig;

public class ConextConfigService {

  private static final Logger LOG = LoggerFactory.getLogger(ConextConfigService.class);

  private static final String CONEXT_CONFIG_URL = "http://localhost:4990/crowd/rest/conext-configuration/1.0/configuration.json";
  private static final String CONEXT_CONFIG_USER = "inherlutq8228ojoivhjmknbh";
  private static final String CONEXT_CONFIG_PASSWORD = "noemeruifhpoi8899unhfvi";

  private final Client client;

  public ConextConfigService() {
    client = Client.create();
    client.addFilter(new HTTPBasicAuthFilter(CONEXT_CONFIG_USER, CONEXT_CONFIG_PASSWORD));
  }

  public ConextConfig getConfig() {

    // TODO: cache (1 minute?)

    try {
      ConextConfig conextConfig = client
        .resource(CONEXT_CONFIG_URL).get(ConextConfig.class);
      LOG.debug("Got config from rest service: {}", conextConfig);
      return conextConfig;
    } catch (UniformInterfaceException e) {
      LOG.info("Error fetching conext configuration: " + e.getMessage());
      return null;
    }
  }
}
