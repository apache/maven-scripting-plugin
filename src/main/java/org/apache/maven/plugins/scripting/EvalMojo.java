package org.apache.maven.plugins.scripting;

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

import java.io.File;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Evaluate the specified script
 *
 * @author Robert Scholte
 * @since 3.0.0
 */
@Mojo( name = "eval" )
public class EvalMojo
    extends AbstractMojo
{
    @Parameter( required = true )
    private String engineName;

    /**
     * When used, also specify the engineName
     */
    @Parameter
    private String script;

    /**
     * Provide the script as an external file as an alternative to &lt;script&gt;.
     * When scriptFile provided the script is ignored.
     * The file name extension identifies the script language to use, as of javax.script.ScriptEngineManager
     * and {@linkplain "https://jcp.org/aboutJava/communityprocess/final/jsr223/index.html"}
     */
    @Parameter
    private File scriptFile;

    // script variables
    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
       Execute execute;
       Object result;
       Bindings bindings;

       try
       {
         execute = constructExecute();

         bindings = new SimpleBindings();
         bindings.put( "project", project );
         bindings.put( "log", getLog() );

         result = execute.run( bindings );

         getLog().info( "Result:" );
         if ( result != null )
         {
           getLog().info( result.toString() );
         }
       }
       catch ( IllegalArgumentException e ) // configuring the plugin failed
       {
         throw new MojoExecutionException( e.getMessage(), e );
       }
       catch ( Exception e ) // execution failure
       {
           throw new MojoFailureException( e.getMessage(), e );
       }
    }

    private Execute constructExecute() throws IllegalArgumentException
    {
      Execute execute;

      if ( scriptFile != null )
      {
         execute = new ExecuteFile( engineName, scriptFile );

      }
      else if ( script != null )
      {
         execute = new ExecuteString( engineName, script );

      }
      else
      {
         throw new IllegalArgumentException( "Missing script or scriptFile provided" );
      }
      return execute;
    }
}