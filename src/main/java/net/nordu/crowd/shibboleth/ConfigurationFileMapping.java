package net.nordu.crowd.shibboleth;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class ConfigurationFileMapping implements Mapping {

  private static final Logger log = Logger.getLogger(ConfigurationFileMapping.class);

  private Configuration config;


  public ConfigurationFileMapping() {
    config = ConfigurationLoader.loadConfiguration(null);
  }

  public void reloadIfNecessary() {

    if (config.isReloadConfig() && config.getConfigFile() != null) {
      if (System.currentTimeMillis() < config.getConfigFileLastChecked() + config.getReloadConfigInterval()) {
        return;
      }

      long configFileLastModified = new File(config.getConfigFile()).lastModified();

      if (configFileLastModified != config.getConfigFileLastModified()) {
        log.debug("Config file has been changed, reloading");
        config = ConfigurationLoader.loadConfiguration(config.getConfigFile());
      } else {
        log.debug("Config file has not been changed, not reloading");
        config.setConfigFileLastChecked(System.currentTimeMillis());
      }
    }
  }

  public Set<String> getGroupsForUser(HttpServletRequest request, HttpServletResponse response) {
    Set<String> groupsFromHeaders = new HashSet<String>();
    // Go through group filters
    for (GroupMapper mapper : config.getGroupMappers()) {
      if (mapper.match(request)) {
        groupsFromHeaders.add(mapper.getGroup());
      }
    }
    return groupsFromHeaders;
  }

  public Set<String> getAllGroups() {
    Set<String> mappedGroups = new HashSet<String>();
    for (GroupMapper mapper : config.getGroupMappers()) {
      mappedGroups.add(mapper.getGroup());
    }
    return mappedGroups;
  }
}
