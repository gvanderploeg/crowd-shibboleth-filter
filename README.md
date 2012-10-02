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

    <bean name="mapping" class="net.nordu.crowd.shibboleth.ConfigurationFileMapping" />

Finally you need to create a ShibbolethAuthGroupMapping.properties file in %crowd-webapp%/WEB-INF/classes. There is an example file under src/main/resources/

### Using OpenConext
Add a bean definition in the above mentioned applicationContext-CrowdSecurity.xml.

    <bean name="mapping" class="net.nordu.crowd.shibboleth.OpenConextGroupMapping">
      <property name="apiClient" ref="openConextApiClient" />
      <property name="settingsFactory" ref="pluginSettingsFactory" />
    </bean>
    <bean id="apiClient" class="nl.surfnet.coin.api.client.OpenConextOAuthClientImpl">
    </bean>


Finally you need to create a ShibbolethAuthGroupMapping.properties file in %crowd-webapp%/WEB-INF/classes. There is an example file under src/main/resources/
