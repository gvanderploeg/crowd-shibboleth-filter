package net.nordu.crowd.shibboleth;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interface that defines operations for external group to Crowd group mapping.
 */
public interface Mapping {

  public void reloadIfNecessary();

  Set<String> getGroupsForUser(HttpServletRequest request, HttpServletResponse response);

  Set<String> getAllGroups();

}
