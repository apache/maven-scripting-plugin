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
package org.apache.maven.plugins.scripting.engine;

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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * A java engine factory to be able to script in plain java in the build.
 */
public class JavaEngineFactory implements ScriptEngineFactory {
    @Override
    public String getEngineName() {
        return "Maven-Scripting-Java-Engine";
    }

    @Override
    public String getEngineVersion() {
        return "1.0";
    }

    @Override
    public List<String> getExtensions() {
        return singletonList("java");
    }

    @Override
    public List<String> getMimeTypes() {
        return singletonList("application/java");
    }

    @Override
    public List<String> getNames() {
        return Stream.concat(
                        Stream.concat(getMimeTypes().stream(), getExtensions().stream()),
                        Stream.of(getEngineName(), getLanguageName()))
                .distinct()
                .collect(toList());
    }

    @Override
    public String getLanguageName() {
        return "java";
    }

    @Override
    public String getLanguageVersion() {
        return System.getProperty("java.version", "8");
    }

    @Override
    public Object getParameter(String key) {
        if (key.equals("javax.script.engine_version")) {
            return getEngineVersion();
        }
        if (key.equals("javax.script.engine")) {
            return getEngineName();
        }
        if (key.equals("javax.script.language")) {
            return getLanguageName();
        }
        if (key.equals("javax.script.language_version")) {
            return getLanguageVersion();
        }
        return null;
    }

    @Override
    public String getMethodCallSyntax(String obj, String method, String... args) {
        return obj + "." + method + '(' + (args == null ? "" : String.join(", ", args)) + ')';
    }

    @Override
    public String getOutputStatement(String toDisplay) {
        return "System.out.println(" + toDisplay + ")";
    }

    @Override
    public String getProgram(String... statements) {
        return Stream.of(statements).collect(joining(";", "", ";"));
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new JavaEngine(this);
    }
}
