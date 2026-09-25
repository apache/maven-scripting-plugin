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

import java.util.List;

import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.PlexusContainerException;
import org.junit.jupiter.api.Test;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// sanity check, decrypter impl is mocked for simplicity
public class ServersTest {
    @Test
    public void missing() throws Exception {
        assertNull(new Servers(
                        new Settings(),
                        new DefaultSettingsDecrypter(new DefaultSecDispatcher(new DefaultPlexusCipher())))
                .find("test"));
    }

    @Test
    public void clear() throws PlexusContainerException {
        final Server server = new Server();
        server.setId("test");
        server.setUsername("clear");
        server.setPassword("12345");
        assertEquals(
                "12345",
                new Servers(
                                new Settings() {
                                    @Override
                                    public List<Server> getServers() {
                                        return singletonList(server);
                                    }
                                },
                                new DefaultSettingsDecrypter(new DefaultSecDispatcher(new DefaultPlexusCipher())) {
                                    @Override
                                    public SettingsDecryptionResult decrypt(SettingsDecryptionRequest request) {
                                        return new SettingsDecryptionResult() {
                                            @Override
                                            public Server getServer() {
                                                return null;
                                            }

                                            @Override
                                            public List<SettingsProblem> getProblems() {
                                                return emptyList();
                                            }

                                            @Override
                                            public List<Server> getServers() {
                                                throw new UnsupportedOperationException();
                                            }

                                            @Override
                                            public Proxy getProxy() {
                                                throw new UnsupportedOperationException();
                                            }

                                            @Override
                                            public List<Proxy> getProxies() {
                                                throw new UnsupportedOperationException();
                                            }
                                        };
                                    }
                                })
                        .find("test")
                        .getPassword());
    }

    @Test
    public void ciphered() throws PlexusContainerException {
        final Server server = new Server();
        server.setId("test");
        server.setUsername("not-clear");
        server.setPassword("ciphered");
        assertEquals(
                "12345",
                new Servers(
                                new Settings() {
                                    @Override
                                    public List<Server> getServers() {
                                        return singletonList(server);
                                    }
                                },
                                new DefaultSettingsDecrypter(new DefaultSecDispatcher(new DefaultPlexusCipher())) {
                                    @Override
                                    public SettingsDecryptionResult decrypt(SettingsDecryptionRequest request) {
                                        return new SettingsDecryptionResult() {
                                            @Override
                                            public Server getServer() {
                                                final Server result = new Server();
                                                result.setId(server.getId());
                                                result.setUsername(server.getUsername());
                                                result.setPassword("12345");
                                                return result;
                                            }

                                            @Override
                                            public List<SettingsProblem> getProblems() {
                                                return emptyList();
                                            }

                                            @Override
                                            public List<Server> getServers() {
                                                throw new UnsupportedOperationException();
                                            }

                                            @Override
                                            public Proxy getProxy() {
                                                throw new UnsupportedOperationException();
                                            }

                                            @Override
                                            public List<Proxy> getProxies() {
                                                throw new UnsupportedOperationException();
                                            }
                                        };
                                    }
                                })
                        .find("test")
                        .getPassword());
    }
}
