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

/**
 * Execute a script held in a string
 * @author Rusi Popov
 */
class ExecuteString extends Execute {

  /**
   * Not null name of the engine to execute the script
   */
  private final String engineName;

  /**
   * The non-null script itself
   */
  private final String script;

  /**
   * @param engineName
   * @param script
   * @throws IllegalArgumentException
   */
  public ExecuteString(String engineName, String script) throws IllegalArgumentException {
    if (engineName == null || engineName.trim().isEmpty()) {
      throw new IllegalArgumentException("Expected a non-empty engine name provided");
    }
    this.engineName = engineName;

    if (script == null || script.trim().isEmpty()) {
      throw new IllegalArgumentException("Expected a non-empty script provided");
    }
    this.script = script;
  }

  /**
   * @throws IllegalArgumentException
   * @see org.apache.maven.plugins.scripting.Execute#constructEngine(javax.script.ScriptEngineManager)
   */
  protected ScriptEngine constructEngine(ScriptEngineManager manager) throws IllegalArgumentException {
    ScriptEngine result;

    result = manager.getEngineByName( engineName );
    if ( result == null ) {
      throw new IllegalArgumentException( "Unknown engine specified with name \""+engineName+"\"" );
    }
    return result;
  }

  /**
   * @see org.apache.maven.plugins.scripting.Execute#execute(javax.script.ScriptEngine, javax.script.ScriptContext)
   */
  protected Object execute(ScriptEngine engine, ScriptContext context) throws ScriptException {
    return engine.eval( script, context );
  }
}