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
package org.apache.maven.plugins.scripting;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Evaluates a script held in a file. Use the engine name to override the engine if the file name does not refer/decode
 * to a valid engine name or not define any at all.
 * @author Rusi Popov
 */
public class FileScriptEvaluator extends AbstractScriptEvaluator {

    /**
     * Not null, existing readable file with the script
     */
    private final File scriptFile;

    /**
     * Possibly null engine name
     */
    private final String engineName;

    /**
     * @param engineName optional engine name, used to override the engine selection from the file extension
     * @param scriptFile not null
     */
    public FileScriptEvaluator(String engineName, File scriptFile) {
        this.scriptFile = scriptFile;

        this.engineName = engineName;
    }

    /**
     * @param engine the script engine.
     * @param context the script context.
     * @return the result of the scriptFile.
     * @throws ScriptException if an error occurs in script.
     * @see org.apache.maven.plugins.scripting.AbstractScriptEvaluator#eval(javax.script.ScriptEngine, javax.script.ScriptContext)
     */
    protected Object eval(ScriptEngine engine, ScriptContext context) throws ScriptException {
        try (FileReader reader = new FileReader(scriptFile)) {
            return engine.eval(reader, context);
        } catch (IOException ex) {
            throw new UncheckedIOException(scriptFile + " caused:", ex);
        }
    }

    /**
     * Gets the script engine by engineName, otherwise by extension of the sciptFile
     *
     * @param manager the script engine manager
     * @throws UnsupportedScriptEngineException if specified engine is not available
     * @see org.apache.maven.plugins.scripting.AbstractScriptEvaluator#getEngine(javax.script.ScriptEngineManager)
     */
    protected ScriptEngine getEngine(ScriptEngineManager manager) throws UnsupportedScriptEngineException {
        ScriptEngine result;

        if (engineName != null && !engineName.isEmpty()) {
            result = manager.getEngineByName(engineName);

            if (result == null) {
                throw new UnsupportedScriptEngineException("No engine found by name \"" + engineName + "\n");
            }
        } else {
            String extension = scriptFile.getName();
            int position = extension.indexOf(".");

            if (position >= 0) {
                extension = extension.substring(position + 1);
            }
            result = manager.getEngineByExtension(extension);

            if (result == null) {
                throw new UnsupportedScriptEngineException("No engine found by extension \"" + extension + "\n");
            }
        }
        return result;
    }
}
