/*
 * Copyright (c) 2011, NORDUnet A/S
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *  * Neither the name of the NORDUnet nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.nordu.crowd.shibboleth;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.crowd.embedded.api.Directory;
import com.atlassian.crowd.embedded.api.PasswordCredential;
import com.atlassian.crowd.exception.ApplicationPermissionException;
import com.atlassian.crowd.exception.DirectoryNotFoundException;
import com.atlassian.crowd.exception.GroupNotFoundException;
import com.atlassian.crowd.exception.InactiveAccountException;
import com.atlassian.crowd.exception.InvalidAuthenticationException;
import com.atlassian.crowd.exception.InvalidAuthorizationTokenException;
import com.atlassian.crowd.exception.InvalidCredentialException;
import com.atlassian.crowd.exception.InvalidGroupException;
import com.atlassian.crowd.exception.InvalidTokenException;
import com.atlassian.crowd.exception.InvalidUserException;
import com.atlassian.crowd.exception.ObjectNotFoundException;
import com.atlassian.crowd.exception.OperationFailedException;
import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.crowd.integration.http.HttpAuthenticator;
import com.atlassian.crowd.integration.soap.SOAPAttribute;
import com.atlassian.crowd.integration.soap.SOAPGroup;
import com.atlassian.crowd.integration.soap.SOAPPrincipal;
import com.atlassian.crowd.integration.springsecurity.CrowdSSOAuthenticationDetails;
import com.atlassian.crowd.integration.springsecurity.CrowdSSOAuthenticationToken;
import com.atlassian.crowd.integration.springsecurity.CrowdSSOTokenInvalidException;
import com.atlassian.crowd.integration.springsecurity.RequestToApplicationMapper;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetailsService;
import com.atlassian.crowd.manager.application.ApplicationAccessDeniedException;
import com.atlassian.crowd.manager.authentication.TokenAuthenticationManager;
import com.atlassian.crowd.manager.directory.DirectoryManager;
import com.atlassian.crowd.model.authentication.UserAuthenticationContext;
import com.atlassian.crowd.model.authentication.ValidationFactor;
import com.atlassian.crowd.model.token.Token;
import com.atlassian.crowd.model.user.User;
import com.atlassian.crowd.model.user.UserConstants;
import com.atlassian.crowd.model.user.UserTemplate;
import com.atlassian.crowd.service.UserManager;
import com.atlassian.crowd.service.soap.client.SecurityServerClient;
import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.AbstractAuthenticationToken;
import org.springframework.security.ui.AbstractProcessingFilter;
import org.springframework.security.ui.savedrequest.SavedRequest;
import org.springframework.security.ui.webapp.AuthenticationProcessingFilter;
import org.springframework.security.userdetails.UsernameNotFoundException;

/**
 * Login filter which relies on headers sent by Shibboleth for user information
 *
 * @author Juha-Matti Leppälä <juha@eduix.fi>
 * @version $Id$
 */
public class ShibbolethSSOFilter extends AuthenticationProcessingFilter {

    private static final Logger log = Logger.getLogger(ShibbolethSSOFilter.class);
    private HttpAuthenticator httpAuthenticator;
    private RequestToApplicationMapper requestToApplicationMapper;
    private CrowdUserDetailsService crowdUserDetailsService;
    private SecurityServerClient securityServerClient;
    private UserManager userManager;
    private TokenAuthenticationManager tokenAuthenticationManager;
    private DirectoryManager directoryManager;

  private Mapping mapping;

    /**
     * This filter will process all requests and check for Shibboleth headers
     * to determine if user is logged in. If the user is logged in but a user
     * account does not exist in Crowd one will be made.
     *
     * @param request servlet request containing either username/password paramaters
     * or the Crowd token as a cookie.
     * @param response servlet response to write out cookie.
     * @return <code>true</code> only if the filterProcessesUrl is in the request URI.
     */
    @Override
    protected boolean requiresAuthentication(final HttpServletRequest request, final HttpServletResponse response) {

        boolean newUser = false;

        if (log.isTraceEnabled()) {
            Enumeration headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String h = (String) headerNames.nextElement();
                log.trace(h + " - " + request.getHeader(h));
            }
        }
        String username = request.getHeader("REMOTE_USER");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // If the user is not authenticated we will try authenticating him/her
        if (auth == null || !auth.isAuthenticated()) {
            if (username != null && !username.equals("")) {
                CrowdUserDetails userDetails = null;
                try {
                    userDetails = crowdUserDetailsService.loadUserByUsername(username);
                } catch (UsernameNotFoundException e) {
                    // No need to respond here, the user is created a few lines down
                } catch (DataAccessException e) {
                    // Not sure in which case this can come up while the system is
                    // working correctly so we'll just ignore this
                }

                if (userDetails == null) {
                    log.debug("No user " + username + " found. Creating");
                    String firstName = request.getHeader("givenName");
                    String lastName = request.getHeader("sn");
                    String email = request.getHeader("mail");

                    // Convert first name and last name from latin1 to utf8
                    // TODO: this could be a configuration
                    firstName = StringUtil.latin1ToUTF8(firstName);
                    lastName = StringUtil.latin1ToUTF8(lastName);

                    if (!createUser(username, firstName, lastName, email)) {
                        return super.requiresAuthentication(request, response);
                    } else {
                        newUser = true;
                    }
                }
                UserAuthenticationContext authCtx = new UserAuthenticationContext();
                authCtx.setApplication(httpAuthenticator.getSoapClientProperties().getApplicationName());
                authCtx.setName(username);
                ValidationFactor[] validationFactors = httpAuthenticator.getValidationFactors(request);
                authCtx.setValidationFactors(validationFactors);

                if (log.isTraceEnabled()) {
                    log.trace("Trying to log in as " + username + " without a password");
                }
                try {
                    Token token = tokenAuthenticationManager.authenticateUserWithoutValidatingPassword(authCtx);
                    token = tokenAuthenticationManager.validateUserToken(token.getRandomHash(), validationFactors, httpAuthenticator.getSoapClientProperties().getApplicationName());
                    CrowdSSOAuthenticationToken crowdAuthRequest = new CrowdSSOAuthenticationToken(token.getRandomHash());
                    doSetDetails(request, crowdAuthRequest);

                    Authentication newAuth = getAuthenticationManager().authenticate(crowdAuthRequest);
                    if (newAuth != null) {
                        log.debug("Authentication: principal " + newAuth.getPrincipal() + " credentials " + newAuth.getCredentials() + " isAuthenticated " + newAuth.isAuthenticated());
                    }
                    SecurityContextHolder.getContext().setAuthentication(newAuth);
                } catch (InvalidAuthenticationException e) {
                    log.error(e, e);
                } catch (InactiveAccountException e) {
                    log.error(e, e);
                } catch (NullPointerException e) {
                    log.error(e.getMessage(), e);
                } catch (ApplicationAccessDeniedException e) {
                    log.error(e);
                } catch (OperationFailedException e) {
                    log.error(e);
                } catch (ObjectNotFoundException e) {
                    log.error(e);
                } catch (InvalidTokenException e) {
                    log.error(e);
                } catch (CrowdSSOTokenInvalidException e) {
                    log.error(e, e);
                }

                auth = SecurityContextHolder.getContext().getAuthentication();

                if (auth != null) {
                    log.debug("Trying to set SSO cookie");
                    try {
                        onSuccessfulAuthentication(request, response, auth);
                    } catch (IOException e) {
                        // do nothing
                    }
                } else {
                    log.trace("Authentication is null");
                }
            }
        } else if (!((CrowdUserDetails) auth.getPrincipal()).getUsername().equals(username) && username != null && username.length() > 0) {
            log.debug("User is authenticated but the username from authentication does "
                    + "not match username in request! Logging user out");
            try {
                httpAuthenticator.logoff(request, response);
            } catch (Exception e) {
                logger.error("Could not logout SSO user from Crowd", e);
            }
        } else {
            log.debug("User already authenticated");
        }
        if (newUser) {
            // This session attribute is read by the SSO Cookie Servlet (part of
            // the NORDUnet Crowd SSO Plugin)
            request.getSession().setAttribute("new.user", true);
        }
        return super.requiresAuthentication(request, response);
    }

    private boolean createUser(String username, String firstname, String lastname, String email) {
        SOAPPrincipal newUser = new SOAPPrincipal(username);
        newUser.setActive(Boolean.TRUE);
        SOAPAttribute[] soapAttributes = new SOAPAttribute[4];

        soapAttributes[0] = new SOAPAttribute(UserConstants.EMAIL, email);
        soapAttributes[1] = new SOAPAttribute(UserConstants.FIRSTNAME, firstname);
        soapAttributes[2] = new SOAPAttribute(UserConstants.LASTNAME, lastname);
        soapAttributes[3] = new SOAPAttribute(UserConstants.DISPLAYNAME, firstname + " " + lastname);

        newUser.setAttributes(soapAttributes);
        PasswordCredential credentials = new PasswordCredential(randomPassword(), false);
        try {
            userManager.addUser(newUser, credentials);
            //securityServerClient.addPrincipal(newUser, credentials);
        } catch (RemoteException ex) {
            log.error(ex);
            return false;
        } catch (ApplicationPermissionException ex) {
            log.error(ex);
            return false;
        } catch (InvalidCredentialException ex) {
            log.error(ex);
            return false;
        } catch (InvalidUserException ex) {
            log.error(ex);
            return false;
        } catch (InvalidAuthorizationTokenException ex) {
            log.error(ex);
            return false;
        } catch (InvalidAuthenticationException e) {
            log.error(e);
            return false;
        }

        return true;
    }

    private String randomPassword() {
      return UUID.randomUUID().toString();
    }

    protected void doSetDetails(HttpServletRequest request, AbstractAuthenticationToken authRequest) {
        String application;

        if (requestToApplicationMapper != null) {
            // determine the target path
            String path;

            SavedRequest savedRequest = (SavedRequest) request.getSession().getAttribute(AbstractProcessingFilter.SPRING_SECURITY_SAVED_REQUEST_KEY);
            if (savedRequest != null) {
                path = savedRequest.getRequestURI().substring(savedRequest.getContextPath().length());
            } else {
                path = request.getRequestURI().substring(request.getContextPath().length());
            }

            application = requestToApplicationMapper.getApplication(path);
        } else {
            // default to the "crowd" application
            application = httpAuthenticator.getSoapClientProperties().getApplicationName();
        }

        ValidationFactor[] validationFactors = httpAuthenticator.getValidationFactors(request);

        authRequest.setDetails(new CrowdSSOAuthenticationDetails(application, validationFactors));
    }

    /**
     * Attempts to write out the successful SSO token to a cookie,
     * if an SSO token was generated and stored via the AuthenticationProvider.
     *
     * This effectively establishes SSO when using the CrowdAuthenticationProvider
     * in conjunction with this filter.
     *
     * @param request servlet request.
     * @param response servlet response.
     * @param authResult result of a successful authentication. If it is a CrowdSSOAuthenticationToken
     * then the SSO token will be set to the "credentials" property.
     * @throws java.io.IOException not thrown.
     */
    @Override
    protected void onSuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, Authentication authResult) throws IOException {
        // write successful SSO token if there is one present
        if (authResult instanceof CrowdSSOAuthenticationToken) {
            if (authResult.getCredentials() != null) {
                try {
                    httpAuthenticator.setPrincipalToken(request, response, authResult.getCredentials().toString());
                    updateUserAttributes(authResult.getName(), request);
                    updateUserGroups(authResult.getName(), request, response);
                } catch (Exception e) {
                    // occurs if application's auth token expires while trying to look up the domain property from the Crowd server
                    logger.error("Unable to set Crowd SSO token", e);
                }
            }
        }
    }

    /**
     * Update user attributes from request headers
     * @param username
     * @param request
     */
    private void updateUserAttributes(String username, HttpServletRequest request) {
        try {
            Directory directory = directoryManager.findDirectoryByName("Test internal directory");
            User foundUser = directoryManager.findUserByName(directory.getId(), username);
            UserTemplate mutableUser = new UserTemplate(foundUser);
            String firstName = request.getHeader("givenName");
            String lastName = request.getHeader("sn");
            String email = request.getHeader("mail");

            // Convert first name and last name from latin1 to utf8
            // TODO: this could be a configuration
            firstName = StringUtil.latin1ToUTF8(firstName);
            lastName = StringUtil.latin1ToUTF8(lastName);
            mutableUser.setEmailAddress(email);
            mutableUser.setFirstName(firstName);
            mutableUser.setLastName(lastName);
            mutableUser.setDisplayName(firstName + " " + lastName);
            directoryManager.updateUser(directory.getId(), mutableUser);
        } catch (DirectoryNotFoundException e) {
            log.error("Could not find system users directory");
        } catch (UserNotFoundException e) {
            log.error("Could not find user to update attributes");
        } catch (Exception e) {
            log.error("Could not update user attributes", e);
        }
    }

    /**
     * Update user groups according to the group mappings
     * @param username
     */
    private void updateUserGroups(String username, HttpServletRequest request, HttpServletResponse response) {
      mapping.reloadIfNecessary();
      log.debug("Updating user groups");

      Set<String> groupsUserShouldBeIn;
        Set<String> allConfiguredGroups;

      groupsUserShouldBeIn = mapping.getGroupsForUser(request, response);
      allConfiguredGroups = mapping.getAllGroups();


      // Decide which groups to purge by differencing groups from headers from
      // all the filtered groups and intersecting that with the current groups
      // of the user
      Set<String> groupsToPurge = getCurrentGroupsForUser(username);
      groupsToPurge.removeAll(groupsUserShouldBeIn);
      groupsToPurge.retainAll(allConfiguredGroups);


      for (String group : groupsUserShouldBeIn) {
        addUserToGroup(username, group);
      }

      for (String groupToPurge : groupsToPurge) {
            if (log.isDebugEnabled()) {
                log.debug("Removing user from group " + groupToPurge);
            }
            try {
                securityServerClient.removePrincipalFromGroup(username, groupToPurge);
            } catch (Exception e) {
                log.error("Could not remove user from group " + groupToPurge, e);
            }
        }
    }

    private Set<String> getCurrentGroupsForUser(String username) {
        Set<String> groups = new HashSet<String>();
        try {
            String[] groupNames = securityServerClient.findAllGroupNames();
            for (String groupName : groupNames) {
                if (securityServerClient.isGroupMember(groupName, username)) {
                    groups.add(groupName);
                }
            }
        } catch (Exception e) {
            log.error("Error getting current groups for user", e);
        }
        return groups;
    }

    /**
     * Add user to group. If group does not exist it will be created
     * @param username
     * @param groupName
     */
    private void addUserToGroup(String username, String groupName) {
        SOAPGroup group = null;
        try {
            group = securityServerClient.findGroupByName(groupName);
            if (group != null && !securityServerClient.isGroupMember(groupName, username)) {
                log.debug("Adding user to group " + groupName);
                securityServerClient.addPrincipalToGroup(username, groupName);
            }
        } catch (RemoteException e) {
            log.error("Could not find group " + groupName, e);
        } catch (InvalidAuthorizationTokenException e) {
            log.error("Could not find group " + groupName, e);
        } catch (ApplicationPermissionException e) {
            log.error("Could not add user to group " + groupName, e);
        } catch (GroupNotFoundException e) {
            log.debug("Could not find group " + groupName + ". Will try creating it");
        } catch (InvalidAuthenticationException e) {
            log.error("Could not add user " + username + " to group " + groupName, e);
        } catch (UserNotFoundException e) {
            log.error("Could not add user " + username + " to group " + groupName, e);
        }
        if (group == null) {
            try {
                group = new SOAPGroup(groupName, null);
                securityServerClient.addGroup(group);
                log.debug("Group added");
                securityServerClient.addPrincipalToGroup(username, groupName);
                log.debug("user added to group");
            } catch (RemoteException e) {
                log.error("Could not add group " + groupName, e);
            } catch (InvalidGroupException e) {
                log.error("Could not add group " + groupName, e);
            } catch (InvalidAuthorizationTokenException e) {
                log.error("Could not add group " + groupName, e);
            } catch (ApplicationPermissionException e) {
                log.error("Could not add group " + groupName, e);
            } catch (GroupNotFoundException e) {
                log.error("Could not add user " + username + " to group " + groupName, e);
            } catch (InvalidAuthenticationException e) {
                log.error("Could not add user " + username + " to group " + groupName, e);
            } catch (UserNotFoundException e) {
                log.error("Could not add user " + username + " to group " + groupName, e);
            }
        }
    }

    public void setHttpAuthenticator(HttpAuthenticator httpAuthenticator) {
        this.httpAuthenticator = httpAuthenticator;
    }

    public void setSecurityServerClient(SecurityServerClient securityServerClient) {
        this.securityServerClient = securityServerClient;
    }

    public void setCrowdUserDetailsService(CrowdUserDetailsService crowdUserDetailsService) {
        this.crowdUserDetailsService = crowdUserDetailsService;
    }

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    public void setTokenAuthenticationManager(TokenAuthenticationManager tokenAuthenticationManager) {
        this.tokenAuthenticationManager = tokenAuthenticationManager;
    }

    public void setDirectoryManager(DirectoryManager directoryManager) {
        this.directoryManager = directoryManager;
    }

    /**
     * Optional dependency.
     *
     * @param requestToApplicationMapper only required if multiple Crowd "applications" need to
     * be accessed via the same Spring Security context, eg. when one web-application corresponds to
     * multiple Crowd "applications".
     */
    public void setRequestToApplicationMapper(RequestToApplicationMapper requestToApplicationMapper) {
        this.requestToApplicationMapper = requestToApplicationMapper;
    }

  /**
   * Setter for Mapping implementation
   * @param m the new mapping
   */
  public void setMapping(Mapping m) {
    this.mapping = m;
  }
}
