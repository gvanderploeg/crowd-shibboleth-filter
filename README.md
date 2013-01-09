Shibboleth-filter
==========

## Installation

To install the Shibboleth filter to crowd copy the created jar file to %crowd-webapp%/WEB-INF/lib
and modify %crowd-webapp%/WEB-INF/classes/applicationContext-CrowdSecurity.xml.

Insert this line into the filterInvocationDefinitionSource of the springSecurityFilterChain bean, before the catchall:

    /plugins/servlet/ssocookie=httpSessionContextIntegrationFilter,logoutFilter,authenticationProcessingFilter,authenticationProcessingShibbolethFilter,securityContextHolderAwareRequestFilter,anonymousProcessingFilter,exceptionTranslationFilter,filterInvocationInterceptor

It then looks like this:

                    CONVERT_URL_TO_LOWERCASE_BEFORE_COMPARISON
                    PATTERN_TYPE_APACHE_ANT
                    /services/**=#NONE#
                    /console/decorator/**=#NONE#
                    /console/images/**=#NONE#
                    /console/style/**=#NONE#
                    /template/**=#NONE#
                    /rest/syncfeedback/**=httpSessionContextIntegrationFilter
                    /rest/**=#NONE#
                    /plugins/servlet/ssocookie=httpSessionContextIntegrationFilter,logoutFilter,authenticationProcessingFilter,authenticationProcessingShibbolethFilter,securityContextHolderAwareRequestFilter,anonymousProcessingFilter,exceptionTranslationFilter,filterInvocationInterceptor
                    /**=httpSessionContextIntegrationFilter,logoutFilter,authenticationProcessingFilter,securityContextHolderAwareRequestFilter,anonymousProcessingFilter,exceptionTranslationFilter,filterInvocationInterceptor

(this basically adds a filter to requests to the ssocookie-servlet)

Then add this bean definition after the authenticationProcessingFilter bean:

    <bean id="authenticationProcessingShibbolethFilter" class="net.nordu.crowd.shibboleth.ShibbolethSSOFilter">
        <security:custom-filter position="AUTHENTICATION_PROCESSING_FILTER"/>
        <property name="httpAuthenticator" ref="httpAuthenticator"/>
        <property name="tokenAuthenticationManager" ref="tokenAuthenticationManager"/>
        <property name="authenticationManager" ref="authenticationManager"/>
        <property name="crowdUserDetailsService" ref="crowdUserDetailsService"/>
        <property name="filterProcessesUrl" value="/console/j_security_check"/>
        <property name="securityServerClient" ref="securityServerClient"/>
        <property name="userManager" ref="crowdUserManager"/>
        <property name="directoryManager" ref="directoryManager"/>
        <property name="authenticationFailureUrl" value="/console/login.action?error=true"/>
        <property name="defaultTargetUrl" value="/console/defaultstartpage.action"/>
        <property name="requestToApplicationMapper" ref="requestToApplicationMapper"/>
        <property name="mapping" ref="externalGroupMapping" />
    </bean>


## Group mapping
Currently there are two implementations  for mapping external groups to Crowd groups:

* A configuration file
* OpenConext API (using VOOT/OpenSocial queries)

### Using a configuration file
Add a bean definition in the above mentioned applicationContext-CrowdSecurity.xml.

    <bean name="externalGroupMapping" class="net.nordu.crowd.shibboleth.ConfigurationFileMapping" />

Finally you need to create a ShibbolethAuthGroupMapping.properties file in %crowd-webapp%/WEB-INF/classes. There is an example file under src/main/resources/

### Using OpenConext
Add these bean definitions in the above mentioned applicationContext-CrowdSecurity.xml.

    <bean name="externalGroupMapping" class="net.nordu.crowd.shibboleth.OpenConextGroupMapping">
      <property name="apiClient" ref="openConextApiClient" />
      <property name="conextConfigService" ref="conextConfigService" />
    </bean>
    <bean id="openConextApiClient" class="nl.surfnet.coin.api.client.OpenConextOAuthClientImpl">
      <!-- these properties will be overwritten by configuration through the user interface. However, the component needs some sensible defaults. -->
      <property name="callbackUrl" value="http://localhost:4990/crowd/plugins/servlet/ssocookie" />
      <property name="consumerKey" value="consumerKey" />
      <property name="consumerSecret" value="secret" />
      <property name="endpointBaseUrl" value="https://api.surfconext.nl/v1/" />
    </bean>

    <bean id="apiClientAccessTokenFilter" class="net.nordu.crowd.shibboleth.ApiClientAccessTokenFilter">
      <property name="apiClient" ref="openConextApiClient" />
      <property name="userIdResolver">
        <bean class="net.nordu.crowd.shibboleth.ApiClientAccessTokenFilter$ShibbolethUserIdResolver" />
      </property>
      <property name="conextConfigService" ref="conextConfigService" />
    </bean>

    <bean id="conextConfigService" class="net.nordu.crowd.shibboleth.ConextConfigService">
      <property name="conextConfigUser" value="inherlutq8228ojoivhjmknbh" />
      <property name="conextConfigPassword" value="noemeruifhpoi8899unhfvi" />
      <property name="conextConfigUrl" value="http://localhost:4990/crowd/rest/conext-configuration/1.0/configuration.xml" />
    </bean>

Fill in the properties according to your environment.

Add apiClientAccessTokenFilter to the filter chain line that was added before:

    /plugins/servlet/ssocookie=httpSessionContextIntegrationFilter,logoutFilter,apiClientAccessTokenFilter,authenticationProcessingFilter,authenticationProcessingShibbolethFilter,securityContextHolderAwareRequestFilter,anonymousProcessingFilter,exceptionTranslationFilter,filterInvocationInterceptor

### Quirks

* All users in Crowd's internal db should be able to login to Crowd, to let the ShibbolethSSOFilter work. (why?)
* The name of Crowd's internal db is hardcoded in ShibbolethSSOFilter
* Shibbolethfilter jar (and all deps, by using the jar-with-filtered-dependencies from target/) has to be copied to classpath manually
* Cookie domain of Crowd has to be set (Console -> Administration -> Cookie domain)

## Development
To run JIRA using the SDK, you MUST use Sun JDK 6, not 7. Otherwise startup will fail with errors in the log file, and the web interface will redirect to a /jiraLocked url.

Tune up logging: ./target/crowd/webapp/WEB-INF/classes/log4j.properties, add:

    log4j.logger.net.nordu=DEBUG

