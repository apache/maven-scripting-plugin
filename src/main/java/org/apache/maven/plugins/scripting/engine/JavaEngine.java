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

import org.apache.maven.plugin.logging.Log;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * The java engine implementation.
 */
public class JavaEngine extends AbstractScriptEngine implements Compilable, ContextAwareEngine
{
    private final ScriptEngineFactory factory;

    private Log log;

    public JavaEngine( ScriptEngineFactory factory )
    {
        this.factory = factory;
    }

    @Override
    public void setLog( Log log )
    {
        this.log = log;
    }

    @Override
    public CompiledScript compile( String script ) throws ScriptException
    {
        // plexus compiler is great but overkill there so don't bring it just for that
        final JavaCompiler compiler = requireNonNull(
                ToolProvider.getSystemJavaCompiler(),
                "you must run on a JDK to have a compiler" );
        Path tmpDir = null;
        try
        {
            tmpDir = Files.createTempDirectory( getClass().getSimpleName() );

            final String packageName = getClass().getPackage().getName() + ".generated";
            final String className = "JavaCompiledScript_" + Math.abs( script.hashCode() );
            final String source = toSource( packageName, className, script );
            final Path src = tmpDir.resolve( "sources" );
            final Path bin = tmpDir.resolve( "bin" );
            final Path srcDir = src.resolve( packageName.replace( '.', '/' ) );
            Files.createDirectories( srcDir );
            Files.createDirectories( bin );
            final Path java = srcDir.resolve( className + ".java" );
            try ( Writer writer = Files.newBufferedWriter( java ) )
            {
                writer.write( source );
            }

            // TODO: make it configurable from the project in subsequent releases
            final String classpath = mavenClasspathPrefix() + System.getProperty( getClass().getName() + ".classpath",
                    System.getProperty( "java.class.path", System.getProperty( "surefire.real.class.path" ) ) );

            // TODO: use a Logger in subsequent releases. Not very important as of now, so using std streams
            final int run = compiler.run( null, System.out, System.err, Stream.of(
                            "-classpath", classpath,
                            "-sourcepath", src.toAbsolutePath().toString(),
                            "-d", bin.toAbsolutePath().toString(),
                            java.toAbsolutePath().toString() )
                    .toArray( String[]::new ) );
            if ( run != 0 )
            {
                throw new IllegalArgumentException(
                        "Can't compile the incoming script, here is the generated code: >\n" + source + "\n<\n" );
            }
            final URLClassLoader loader = new URLClassLoader(
                    new URL[]{ bin.toUri().toURL() },
                    Thread.currentThread().getContextClassLoader() );
            final Class<? extends CompiledScript> loadClass =
                    loader.loadClass( packageName + '.' + className ).asSubclass( CompiledScript.class );
            return loadClass
                    .getConstructor( ScriptEngine.class, URLClassLoader.class )
                    .newInstance( this, loader );
        }
        catch ( Exception e )
        {
            throw new ScriptException( e );
        }
        finally
        {
            if ( tmpDir != null )
            {
                try
                {
                    Files.walkFileTree( tmpDir, new SimpleFileVisitor<Path>()
                    {
                        @Override
                        public FileVisitResult visitFile( Path file, BasicFileAttributes attrs )
                                throws IOException
                        {
                            Files.delete( file );
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory( Path dir, IOException exc )
                                throws IOException
                        {
                            Files.delete( dir );
                            return FileVisitResult.CONTINUE;
                        }

                    } );
                }
                catch ( IOException e )
                {
                    if ( log != null )
                    {
                        log.debug( e );
                    }
                }
            }
        }
    }

    private String mavenClasspathPrefix()
    {
        final String home = System.getProperty( "maven.home" );
        if ( home == null )
        {
            if ( log != null )
            {
                log.debug( "No maven.home set" );
            }
            return "";
        }
        try ( Stream<Path> files = Files.list( Paths.get( home ).resolve( "lib" ) ) )
        {
            return files
                    .filter( it ->
                    {
                        final String name = it.getFileName().toString();
                        return name.startsWith( "maven-" );
                    } )
                    .map( Path::toString )
                    .collect( joining( File.pathSeparator, "", File.pathSeparator ) );
        }
        catch ( IOException e )
        {
            if ( log != null )
            {
                log.debug( e );
            }
            return "";
        }
    }

    private String toSource( String pck, String name, String script )
    {
        final String[] importsAndScript = splitImportsAndScript( script );
        return "package " + pck + ";\n"
                + "\n"
                + "import java.io.*;\n"
                + "import java.net.*;\n"
                + "import java.util.*;\n"
                + "import java.util.stream.*;\n"
                + "import java.nio.file.*;\n"
                + "import org.apache.maven.project.MavenProject;\n"
                + "import org.apache.maven.plugin.logging.Log;\n"
                + "\n"
                + "import javax.script.Bindings;\n"
                + "import javax.script.CompiledScript;\n"
                + "import javax.script.ScriptContext;\n"
                + "import javax.script.ScriptEngine;\n"
                + "import javax.script.ScriptException;\n"
                + "\n"
                + importsAndScript[0] + '\n'
                + "\n"
                + "public class " + name + " extends CompiledScript implements AutoCloseable {\n"
                + "    private final ScriptEngine $engine;\n"
                + "    private final URLClassLoader $loader;\n"
                + "\n"
                + "    public " + name + "( ScriptEngine engine, URLClassLoader loader) {\n"
                + "        this.$engine = engine;\n"
                + "        this.$loader = loader;\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public Object eval( ScriptContext $context) throws ScriptException {\n"
                + "        final Thread $thread = Thread.currentThread();\n"
                + "        final ClassLoader $oldClassLoader = $thread.getContextClassLoader();\n"
                + "        $thread.setContextClassLoader($loader);\n"
                + "        try {\n"
                + "           final Bindings $bindings = $context.getBindings(ScriptContext.GLOBAL_SCOPE);\n"
                + "           final MavenProject $project = MavenProject.class.cast($bindings.get(\"project\"));\n"
                + "           final Log $log = Log.class.cast($bindings.get(\"log\"));\n"
                + "           " + importsAndScript[1] + "\n"
                + "           return null;\n" // assume the script doesn't return anything for now
                + "        } catch ( Exception e) {\n"
                + "            if (RuntimeException.class.isInstance(e)) {\n"
                + "                throw RuntimeException.class.cast(e);\n"
                + "            }\n"
                + "            throw new IllegalStateException(e);\n"
                + "        } finally {\n"
                + "            $thread.setContextClassLoader($oldClassLoader);\n"
                + "        }\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public ScriptEngine getEngine() {\n"
                + "        return $engine;\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public void close() throws Exception {\n"
                + "        $loader.close();\n"
                + "    }\n"
                + "}";
    }

    private String[] splitImportsAndScript( String script )
    {
        final StringBuilder imports = new StringBuilder();
        final StringBuilder content = new StringBuilder();
        boolean useImport = true;
        boolean inComment = false;
        try ( BufferedReader reader = new BufferedReader( new StringReader( script ) ) )
        {
            String line;
            while ( ( line = reader.readLine() ) != null )
            {
                if ( useImport )
                {
                    String trimmed = line.trim();
                    if ( trimmed.isEmpty() )
                    {
                        continue;
                    }
                    if ( trimmed.startsWith( "/*" ) )
                    {
                        inComment = true;
                        continue;
                    }
                    if ( trimmed.endsWith( "*/" ) && inComment )
                    {
                        inComment = false;
                        continue;
                    }
                    if ( inComment )
                    {
                        continue;
                    }
                    if ( trimmed.startsWith( "import " ) && trimmed.endsWith( ";" ) )
                    {
                        imports.append( line ).append( '\n' );
                        continue;
                    }
                    useImport = false;
                }
                content.append( line ).append( '\n' );
            }
        }
        catch ( IOException ioe )
        {
            throw new IllegalStateException( ioe );
        }
        return new String[]
        {
                imports.toString().trim(),
                content.toString().trim()
        };
    }

    @Override
    public Object eval( String script, ScriptContext context ) throws ScriptException
    {
        final CompiledScript compile = compile( script );
        try
        {
            return compile.eval( context );
        }
        finally
        {
            doClose( compile );
        }
    }

    @Override
    public Object eval( Reader reader, ScriptContext context ) throws ScriptException
    {
        return eval( load( reader ), context );
    }

    @Override
    public CompiledScript compile( Reader script ) throws ScriptException
    {
        return compile( load( script ) );
    }

    @Override
    public Bindings createBindings()
    {
        return new SimpleBindings();
    }

    @Override
    public ScriptEngineFactory getFactory()
    {
        return factory;
    }

    private void doClose( final CompiledScript compile )
    {
        if ( !AutoCloseable.class.isInstance( compile ) )
        {
            return;
        }
        try
        {
            AutoCloseable.class.cast( compile ).close();
        }
        catch ( Exception e )
        {
            if ( log != null )
            {
                log.debug( e );
            }
        }
    }

    private String load( Reader reader )
    {
        return new BufferedReader( reader ).lines().collect( joining( "\n" ) );
    }
}
