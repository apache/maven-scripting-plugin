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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Execute a script held in a file. Use the engine name to override the engine if the file name does not refer/decode
 * to a valid engine name or not define any at all.
 * @author Rusi Popov
 */
class ExecuteFile extends Execute {

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
   * @throws IllegalArgumentException when the combination of parameters is incorrect
   */
  public ExecuteFile(String engineName, File scriptFile) throws IllegalArgumentException {
    if (scriptFile == null
        || !scriptFile.isFile()
        || !scriptFile.exists()
        || !scriptFile.canRead()) {
      throw new IllegalArgumentException("Expected an existing readable file \""+scriptFile+"\" provided");
    }
    this.scriptFile = scriptFile;

    this.engineName = engineName;
  }

  /**
   * @param engine
   * @param context
   * @return
   * @throws ScriptException
   * @see org.apache.maven.plugins.scripting.Execute#execute(javax.script.ScriptEngine, javax.script.ScriptContext)
   */
  protected Object execute(ScriptEngine engine, ScriptContext context) throws ScriptException {
    FileReader reader;

    try {
      reader = new FileReader(scriptFile);
    } catch (IOException ex) {
      throw new IllegalArgumentException(scriptFile+" caused:", ex);
    }
    return engine.eval( reader, context );
  }

  /**
   * @see org.apache.maven.plugins.scripting.Execute#constructEngine(javax.script.ScriptEngineManager)
   */
  protected ScriptEngine constructEngine(ScriptEngineManager manager) throws IllegalArgumentException {
    ScriptEngine result;
    String extension;
    int position;

    if ( engineName != null && !engineName.trim().isEmpty() ) {
      result = manager.getEngineByName( engineName );

      if ( result == null ) {
        throw new IllegalArgumentException("No engine found by name \""+engineName+"\n");
      }
    } else {
      extension = scriptFile.getName();
      position = extension.indexOf(".");

      if ( position >= 0 ) {
        extension = extension.substring( position+1 );
      }
      result = manager.getEngineByExtension( extension );

      if ( result == null ) {
        throw new IllegalArgumentException("No engine found by extension \""+extension+"\n");
      }
    }
    return result;
  }
}
