package net.nordu.crowd.shibboleth;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.surfnet.coin.api.client.OpenConextOAuthClientImpl;
import nl.surfnet.coin.api.client.domain.Group20;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.surfnet.crowd.model.ConextConfig;
import org.surfnet.crowd.model.GroupMapping;

public class OpenConextGroupMapping implements Mapping {

  private final static Logger LOG = LoggerFactory.getLogger(OpenConextGroupMapping.class);

  private ConextConfigService conextConfigService;

  private OpenConextOAuthClientImpl apiClient;

  public void reloadIfNecessary() {
    ConextConfig conextConfig = conextConfigService.getConfig();

    LOG.debug("Reloaded conext config. New config: " + conextConfig);
    apiClient.setCallbackUrl(conextConfig.getCallbackUrl());
    apiClient.setConsumerKey(conextConfig.getApiKey());
    apiClient.setConsumerSecret(conextConfig.getApiSecret());
    apiClient.setEndpointBaseUrl(conextConfig.getApiUrl());
  }

  public Set<String> getGroupsForUser(HttpServletRequest request, HttpServletResponse response) {
    String user = request.getHeader("REMOTE_USER");

    List<Group20> externalGroup20s = apiClient.getGroups20(user, user);
    LOG.info("User {} has groups in conext: {}", user, externalGroup20s);
    Set<String> groupNames = getGroupNames(externalGroup20s);

    Set<String> crowdGroups = new HashSet<String>();

    for (GroupMapping gm : getGroupMappings()) {
      if (groupNames.contains(gm.getExternalGroupName())) {
        crowdGroups.add(gm.getCrowdGroupName());
      }
    }
    return crowdGroups;
  }

  /**
   * Transform List of Group20 to Set of String, using their id.
   * Effectively removes duplicates.
   *
   */
  public Set<String> getGroupNames(Collection<Group20> conextGroups) {
    Set<String> groupNames = new HashSet<String>();
    for (Group20 g : conextGroups) {
      if (!groupNames.contains(g.getId())) {
        groupNames.add(g.getId());
      }
    }
    return groupNames;
  }

  public Set<String> getAllGroups() {
    Set<String> groupNames = new HashSet<String>();
    for (GroupMapping gm : getGroupMappings()) {
      groupNames.add(gm.getCrowdGroupName());
    }
    return groupNames;
  }

  protected Set<GroupMapping> getGroupMappings() {
    return new HashSet(conextConfigService.getConfig().getGroupmappings());
  }

  public void setConextConfigService(ConextConfigService conextConfigService) {
    this.conextConfigService = conextConfigService;
  }

  public void setApiClient(OpenConextOAuthClientImpl apiClient) {
    this.apiClient = apiClient;
  }
}
