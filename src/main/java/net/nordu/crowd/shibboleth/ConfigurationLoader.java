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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 * Class for loading filter configuration
 *
 * @author Juha-Matti Leppälä <juha@eduix.fi>
 * @version $Id$
 */
public class ConfigurationLoader {

    private static final Logger log = Logger.getLogger(ConfigurationLoader.class);

    public static Configuration loadConfiguration(String file) {
        Configuration config = new Configuration();
        try {
            Map<String, GroupMapper> mappings = new HashMap<String, GroupMapper>();
            Set<String> attributes = new HashSet<String>();
            Set<String> groupsToPurge = new HashSet<String>();
            InputStream propsIn = null;
            if (file == null) {
                propsIn = ConfigurationLoader.class.getResourceAsStream("/ShibbolethAuthGroupMapping.properties");
            } else {
                propsIn = new FileInputStream(file);
            }
            if(propsIn == null) {
                throw new RuntimeException("Error loading group mapping properties. Configuration file not found");
            }
            Properties props = new Properties();

            props.load(propsIn);

            config.setReloadConfig(Boolean.parseBoolean(props.getProperty(Constants.RELOAD_CONFIG)));
            String reloadInterval = props.getProperty(Constants.RELOAD_CONFIG_INTERVAL);
            if (reloadInterval != null) {
                try {
                    config.setReloadConfigInterval(Long.parseLong(reloadInterval) * 1000);
                } catch (NumberFormatException e) {
                    config.setReloadConfigInterval(3600 * 1000);
                }
            }
            config.setConfigFileLastChecked(System.currentTimeMillis());
            URL confFileURL = ConfigurationLoader.class.getResource("/ShibbolethAuthGroupMapping.properties");
            if (confFileURL != null && confFileURL.getProtocol().equals("file")) {
                String confFile = confFileURL.getFile();
                config.setConfigFile(confFile);
                long configFileLastModified = new File(confFile).lastModified();
                config.setConfigFileLastModified(configFileLastModified);
            }


            // Load group mappings
            Map<String, GroupMapper> mappers = new HashMap<String, GroupMapper>();
            for (Object key : props.keySet()) {
                String keyString = (String) key;
                if (keyString.contains(Constants.DELIMITER)) {
                    String[] parts = keyString.split(Constants.DELIMITER_REGEX, 0);
                    handleGroupMapperParts(parts, mappers, props.getProperty(keyString));
                }
            }
            
            Set<GroupMapper> groupMappers= new HashSet<GroupMapper>();
            for(GroupMapper mapper:mappers.values()) {
                if(!mapper.getHeaderMatches().isEmpty()) {
                    groupMappers.add(mapper);
                }
            }
            config.setGroupMappers(groupMappers);
            log.debug("Group filters: "+groupMappers.size());

        } catch (IOException ex) {
            log.error("Error loading group mapping properties", ex);            
        }
        return config;
    }

    private static void handleGroupMapperParts(String[] parts, Map<String, GroupMapper> mappers, String val) {
        if (parts.length >= 3 && Constants.GROUP.equals(parts[0])) {
            String group = parts[1];            
            GroupMapper filter = mappers.get(group);
            if (filter == null) {
                filter = new GroupMapper(group, new HashMap<String, String>());
                mappers.put(group, filter);
            }
            if (Constants.GROUP_MAPPER_SENSITIVE.equals(parts[2])) {
                filter.setCaseSensitive(Boolean.parseBoolean(val));
            } else if (Constants.GROUP_MAPPER_EXCLUSIVE.equals(parts[2])) {
                filter.setExclusive(Boolean.parseBoolean(val));
            } else if (Constants.GROUP_MAPPER_MATCH.equals(parts[2]) && parts.length == 4) {
                filter.getHeaderMatches().put(parts[3], val);
            }
        }
    }
}
