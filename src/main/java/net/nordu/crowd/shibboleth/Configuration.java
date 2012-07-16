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

import java.util.Set;

/**
 * Model for configuration
 *
 * @author Juha-Matti Leppälä <juha@eduix.fi>
 * @version $Id$
 */
public class Configuration {

    private Set<GroupMapper> groupMappers;
    private boolean reloadConfig;
    private long reloadConfigInterval;
    private String configFile;
    private long configFileLastModified;
    private long configFileLastChecked;

    public Set<GroupMapper> getGroupMappers() {
        return groupMappers;
    }

    public void setGroupMappers(Set<GroupMapper> groupMappers) {
        this.groupMappers = groupMappers;
    }

    public boolean isReloadConfig() {
        return reloadConfig;
    }

    public void setReloadConfig(boolean reloadConfig) {
        this.reloadConfig = reloadConfig;
    }

    public long getReloadConfigInterval() {
        return reloadConfigInterval;
    }

    public void setReloadConfigInterval(long reloadConfigInterval) {
        this.reloadConfigInterval = reloadConfigInterval;
    }

    public long getConfigFileLastChecked() {
        return configFileLastChecked;
    }

    public void setConfigFileLastChecked(long configFileLastChecked) {
        this.configFileLastChecked = configFileLastChecked;
    }

    public long getConfigFileLastModified() {
        return configFileLastModified;
    }

    public void setConfigFileLastModified(long configFileLastModified) {
        this.configFileLastModified = configFileLastModified;
    }

    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }
}
