package net.nordu.crowd.shibboleth;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.surfnet.coin.api.client.OpenConextOAuthClient;

/**
 * Servlet filter that obtains an access token for its ApiClient, by redirecting to the authorization url.
 */
public class ApiClientAccessTokenFilter implements Filter {

  public static final String ORIGINAL_URL_SESSION_ATTR = "ApiClientAccessTokenFilter.ORIGINAL_URL";

  Logger LOG = LoggerFactory.getLogger(ApiClientAccessTokenFilter.class);
  private OpenConextOAuthClient apiClient;

  private UserIdResolver userIdResolver;

  public void init(FilterConfig filterConfig) throws ServletException {
  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    String userId = userIdResolver.resolveUserId(request, response);

    if (req.getParameter("code") != null) {
      LOG.debug("Callback from OAuth conversation for userId {}. Callback URL was: {}?{}",
        new String[] {userId, req.getRequestURI(), req.getQueryString()});
      apiClient.oauthCallback(req, userId);
      String originalUrl = (String) req.getSession().getAttribute(ORIGINAL_URL_SESSION_ATTR);
      LOG.debug("After callback, will redirect to original URL '{}'", originalUrl);
      ((HttpServletResponse) response).sendRedirect(originalUrl);
      return;
    }

    if (!apiClient.isAccessTokenGranted(userId)) {

      // Remember the current URL, for later replaying
      String originalUrl = getCallbackUrl(req);
      req.getSession().setAttribute(ORIGINAL_URL_SESSION_ATTR, originalUrl);
      LOG.debug("Original URL for later replaying, after OAuth: {}", originalUrl);

      String authorizationUrl = apiClient.getAuthorizationUrl();
      LOG.debug("Access token not yet granted for user '{}', will short filter chain and redirect to OAuth authorization url: '{}'", userId, authorizationUrl);
      ((HttpServletResponse) response).sendRedirect(authorizationUrl);
      return;
    }
    LOG.debug("Access token was already granted, will continue chain.");
    chain.doFilter(request, response);
  }

  public void destroy() {
  }

  protected String getCallbackUrl(HttpServletRequest request) {
    return request.getRequestURL()
      .append("?oauthCallback=true&")
      .append(request.getQueryString())
      .toString();
  }
  public void setApiClient(OpenConextOAuthClient apiClient) {
    this.apiClient = apiClient;
  }

  public void setUserIdResolver(UserIdResolver userIdResolver) {
    this.userIdResolver = userIdResolver;
  }

  /**
   * Implementation of UserIdResolver strategy that uses Shibboleths header.
   */
  public static class ShibbolethUserIdResolver implements UserIdResolver {

    public String resolveUserId(ServletRequest request, ServletResponse response) {
      return ((HttpServletRequest) request).getHeader("REMOTE_USER");
    }
  }

  /**
   * Strategy that gets the userId the access token is for.
   */
  public interface UserIdResolver {

    /**
     * Get the userId for whom an access token is to be obtained.
     * @param request the ServletRequest
     * @param response the ServletResponse
     * @return the resolved userId
     */
    String resolveUserId(ServletRequest request, ServletResponse response);
  }
}
