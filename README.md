Shibboleth-filter
==========

## Installation

To install the Shibboleth filter to crowd copy the created jar file to %crowd-webapp%/WEB-INF/lib
and modify %crowd-webapp%/WEB-INF/classes/applicationContext-CrowdSecurity.xml.

Add authenticationProcessingShibbolethFilter after authenticationProcessingFilter in the filterInvocationDefinitionSource of the springSecurityFilterChain bean like so:
/**=httpSessionContextIntegrationFilter,logoutFilter,authenticationProcessingFilter,authenticationProcessingShibbolethFilter,securityContextHolderAwareRequestFilter,anonymousProcessingFilter,exceptionTranslationFilter,filterInvocationInterceptor

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
Add a bean definition in the above mentioned applicationContext-CrowdSecurity.xml.

    <bean name="externalGroupMapping" class="net.nordu.crowd.shibboleth.OpenConextGroupMapping">
      <property name="apiClient" ref="openConextApiClient" />
    </bean>
    <bean id="openConextApiClient" class="nl.surfnet.coin.api.client.OpenConextOAuthClientImpl">
      <property name="callbackUrl" value="http://localhost:4990/crowd/plugins/servlet/ssocookie" />
      <property name="consumerKey" value="https://testsp.dev.surfconext.nl/shibboleth" />
      <property name="consumerSecret" value="mysecret" />
      <property name="endpointBaseUrl" value="https://api.dev.surfconext.nl/v1/" />
    </bean>

    <bean id="apiClientAccessTokenFilter" class="net.nordu.crowd.shibboleth.ApiClientAccessTokenFilter">
      <property name="apiClient" ref="openConextApiClient" />
      <property name="userIdResolver">
        <bean class="net.nordu.crowd.shibboleth.ApiClientAccessTokenFilter$ShibbolethUserIdResolver" />
      </property>
    </bean>

Fill in the properties according to your environment.

Add apiClientAccessTokenFilter to the filter chain:
/**=httpSessionContextIntegrationFilter,logoutFilter,apiClientAccessTokenFilter,authenticationProcessingFilter,authenticationProcessingShibbolethFilter,securityContextHolderAwareRequestFilter,anonymousProcessingFilter,exceptionTranslationFilter,filterInvocationInterceptor

### Quirks

* All users in Crowd's internal db should be able to login to Crowd, to let the ShibbolethSSOFilter work. (why?)
* The name of Crowd's internal db is hardcoded in ShibbolethSSOFilter
* All dependencies of crowd-conext-plugin have to be copied to /crowd/webapp/WEB-INF/lib/ (unzip crowd-conext-jar, copy META-INF/lib/*)
* Shibbolethfilter jar has to be copied to classpath manually
* Cookie domain of Crowd has to be set

## Development
To run JIRA using the SDK, you MUST use Sun JDK 6, not 7. Otherwise startup will fail with errors in the log file, and the web interface will redirect to a /jiraLocked url.

Tune up logging: ./target/crowd/webapp/WEB-INF/classes/log4j.properties, add:

    log4j.logger.net.nordu=DEBUG

