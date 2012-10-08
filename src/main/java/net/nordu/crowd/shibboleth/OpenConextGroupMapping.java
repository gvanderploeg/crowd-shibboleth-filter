package net.nordu.crowd.shibboleth;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.surfnet.crowd.model.ConextConfig;
import org.surfnet.crowd.model.GroupMapping;

import nl.surfnet.coin.api.client.OpenConextOAuthClient;
import nl.surfnet.coin.api.client.domain.Group20;

import static org.surfnet.crowd.ConfigurationFormServlet.SETTING_MAPPING;

public class OpenConextGroupMapping implements Mapping {

  private final static Logger LOG = LoggerFactory.getLogger(OpenConextGroupMapping.class);

  private OpenConextOAuthClient apiClient;


  public void reloadIfNecessary() {
    // TODO: probably use plugin-settings to inject API-client settings into apiClient?
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
    PluginSettings settings = getSettingsFactory().createGlobalSettings();
    return new HashSet<GroupMapping>(ConextConfig.mappingsFromString((String) settings.get(SETTING_MAPPING)));
  }

  public void setApiClient(OpenConextOAuthClient apiClient) {
    this.apiClient = apiClient;
  }

  public PluginSettingsFactory getSettingsFactory() {
    return PluginSettingsAccessor.getPluginSettingsFactory();
  }
}
