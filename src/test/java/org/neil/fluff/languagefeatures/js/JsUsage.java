package org.neil.fluff.languagefeatures.js;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Test;

public class JsUsage {
    
    /**
     * This method intentionally fails just to show that Nashorn gives us
     * a stacktrace that crosses the Java/JavaScript boundary. :)
     */
    @Test public void runFailingJavaScript() throws ScriptException, IOException {
        ScriptEngine jsEngine = new ScriptEngineManager().getEngineByExtension("js");
        
        InputStream jsSource = JsUsage.class.getResourceAsStream("fail.js");
        
        try (InputStreamReader r = new InputStreamReader(jsSource)) {
            jsEngine.eval(r);
        }
    }
    
    //    javax.script.ScriptException: failure in JS in <eval> at line number 10 at column number 2
    //    at jdk.nashorn.api.scripting.NashornScriptEngine.throwAsScriptException(NashornScriptEngine.java:564)
    //    at jdk.nashorn.api.scripting.NashornScriptEngine.evalImpl(NashornScriptEngine.java:548)
    //    at jdk.nashorn.api.scripting.NashornScriptEngine.evalImpl(NashornScriptEngine.java:528)
    //    at jdk.nashorn.api.scripting.NashornScriptEngine.evalImpl(NashornScriptEngine.java:524)
    //    at jdk.nashorn.api.scripting.NashornScriptEngine.eval(NashornScriptEngine.java:189)
    //    at javax.script.AbstractScriptEngine.eval(AbstractScriptEngine.java:249)
    //    at org.neil.fluff.languagefeatures.js.JsUsage.runFailingJavaScript(JsUsage.java:24)
    //    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    //    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
    //    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
    //    at java.lang.reflect.Method.invoke(Method.java:483)
    //    at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:45)
    //    at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:15)
    //    at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:42)
    //    at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:20)
    //    at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:263)
    //    at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:68)
    //    at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:47)
    //    at org.junit.runners.ParentRunner$3.run(ParentRunner.java:231)
    //    at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:60)
    //    at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:229)
    //    at org.junit.runners.ParentRunner.access$000(ParentRunner.java:50)
    //    at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:222)
    //    at org.junit.runners.ParentRunner.run(ParentRunner.java:300)
    //    at org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference.run(JUnit4TestReference.java:50)
    //    at org.eclipse.jdt.internal.junit.runner.TestExecution.run(TestExecution.java:38)
    //    at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:467)
    //    at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:683)
    //    at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.run(RemoteTestRunner.java:390)
    //    at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.main(RemoteTestRunner.java:197)
    //Caused by: <eval>:10:2 failure in JS
    //    at jdk.nashorn.internal.scripts.Script$\^eval\_.failingFunction(<eval>:10)
    //    at jdk.nashorn.internal.scripts.Script$\^eval\_.otherFunction(<eval>:6)
    //    at jdk.nashorn.internal.scripts.Script$\^eval\_.main(<eval>:2)
    //    at jdk.nashorn.internal.scripts.Script$\^eval\_.runScript(<eval>:13)
    //    at jdk.nashorn.internal.runtime.ScriptFunctionData.invoke(ScriptFunctionData.java:498)
    //    at jdk.nashorn.internal.runtime.ScriptFunction.invoke(ScriptFunction.java:206)
    //    at jdk.nashorn.internal.runtime.ScriptRuntime.apply(ScriptRuntime.java:378)
    //    at jdk.nashorn.api.scripting.NashornScriptEngine.evalImpl(NashornScriptEngine.java:546)
    //    ... 28 more
}
