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

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.scripting.engine.ContextAwareEngine;

/**
 * Evaluates a script in the appropriate context and return its possibly null result
 * @author Rusi Popov
 */
abstract class AbstractScriptEvaluator {

    /**
     * @param bindings not null bindings to provide to the script to execute
     * @param log engine logger if context aware.
     * @return the possibly null result the script produced
     * @throws UnsupportedScriptEngineException when the engine is not configured correctly
     * @throws ScriptException  if an error occurs in script.
     */
    public final Object eval(Bindings bindings, Log log) throws ScriptException, UnsupportedScriptEngineException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = getEngine(manager);
        if (ContextAwareEngine.class.isInstance(engine)) {
            ContextAwareEngine.class.cast(engine).setLog(log);
        }
        ScriptContext context = engine.getContext();

        context.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);

        return eval(engine, context);
    }

    /**
     * AbstractScriptEvaluator the script
     * @param engine not null
     * @param context not null, initialized
     * @return possibly null result of the script
     * @throws ScriptException  if an error occurs in script.
     */
    protected abstract Object eval(ScriptEngine engine, ScriptContext context) throws ScriptException;

    /**
     * @param manager not null
     * @return non-null engine to execute the script
     * @throws UnsupportedScriptEngineException when no engine could be identified
     */
    protected abstract ScriptEngine getEngine(ScriptEngineManager manager) throws UnsupportedScriptEngineException;
}
