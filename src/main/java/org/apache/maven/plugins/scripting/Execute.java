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

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Execute a script in the appropriate context and return its possibly null result
 * @author Rusi Popov
 */
abstract class Execute {

  /**
   * @param bindings not null bindings to provide to the script to execute
   * @return the possibly null result the script produced
   * @throws IllegalArgumentException when the engine is not configured correctly
   * @throws ScriptException
   */
  public final Object run(Bindings bindings) throws IllegalArgumentException, ScriptException {
    ScriptEngine engine;
    ScriptEngineManager manager;
    ScriptContext context;

    manager = new ScriptEngineManager();
    engine = constructEngine(manager);
    context= engine.getContext();

    context.setBindings( bindings, ScriptContext.GLOBAL_SCOPE );

    return execute( engine, context );
  }

  /**
   * Execute the script
   * @param engine not null
   * @param context not null, initialized
   * @return possibly null result of the script
   * @throws ScriptException
   */
  protected abstract Object execute(ScriptEngine engine, ScriptContext context) throws ScriptException;

  /**
   * @param manager not null
   * @return non-null engine to execute the script
   * @throws IllegalArgumentException when no engine could be identified
   */
  protected abstract ScriptEngine constructEngine(ScriptEngineManager manager) throws IllegalArgumentException;
}
