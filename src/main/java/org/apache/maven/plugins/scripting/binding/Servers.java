/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.scripting.binding;

import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;

import static java.util.stream.Collectors.joining;

/**
 * Binding which enables to work with servers (from settings.xml) and in particular decipher them transparently.
 */
public class Servers {
    private final Settings settings;
    private final SettingsDecrypter settingsDecrypter;

    public Servers(Settings settings, SettingsDecrypter settingsDecrypter) {
        this.settings = settings;
        this.settingsDecrypter = settingsDecrypter;
    }

    public Server find(String id) {
        final Server server = settings.getServer(id);
        if (server == null) {
            return null;
        }
        final SettingsDecryptionResult result = settingsDecrypter.decrypt(new DefaultSettingsDecryptionRequest(server));
        if (!result.getProblems().isEmpty()) {
            throw new IllegalStateException(
                    result.getProblems().stream().map(SettingsProblem::toString).collect(joining("\n")));
        }
        final Server decrypted = result.getServer();
        return decrypted == null ? server : decrypted;
    }
}
