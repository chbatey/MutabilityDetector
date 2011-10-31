/* 
 * Mutability Detector
 *
 * Copyright 2009 Graham Allan
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.mutabilitydetector;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;

import org.mutabilitydetector.IAnalysisSession.AnalysisError;
import org.mutabilitydetector.checkers.IMutabilityChecker;
import org.mutabilitydetector.locations.Dotted;
import org.objectweb.asm.ClassReader;

import com.google.classpath.ClassPath;
import com.google.classpath.ClassPathFactory;

public class CheckerRunner {

    private ClassReader cr;
    private final ClassPath classpath;

    public CheckerRunner(ClassPath classpath) {
        this.classpath = classpath;
    }

    public static CheckerRunner createWithClasspath(ClassPath classpath) {
        return new CheckerRunner(classpath);
    }

    public static CheckerRunner createWithCurrentClasspath() {
        return new CheckerRunner(new ClassPathFactory().createFromJVM());
    }

    public void run(IAnalysisSession analysisSession, IMutabilityChecker checker, Dotted className) {
        try {
            try {
                cr = new ClassReader(className.asString());
                cr.accept(checker, 0);
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                throw e;
            } catch (Throwable e) {
                // Has to catch NoClassDefFoundError
                analyseAsStream(checker, className.asString());
            }
        } catch (Throwable e) {
            handleException(analysisSession, checker, className.asString(), e);
        }
    }

    private void analyseAsStream(IMutabilityChecker checker, String dottedClassPath) throws IOException {
        String slashedClassPath = dottedClassPath.replace(".", "/").concat(".class");
        InputStream classStream = classpath.getResourceAsStream(slashedClassPath);
        cr = new ClassReader(classStream);
        cr.accept(checker, 0);
    }

    private void handleException(IAnalysisSession analysisSession,
            IMutabilityChecker checker,
            String dottedClassPath,
            Throwable e) {
        checker.visitAnalysisException(e);
        String errorDescription = format("It is likely that the class %s has dependencies outwith the given class path.",
                dottedClassPath);
        AnalysisError error = new AnalysisError(dottedClassPath, getNameOfChecker(checker), errorDescription);
        analysisSession.addAnalysisError(error);
    }

    private String getNameOfChecker(IMutabilityChecker checker) {
        String checkerName = checker.getClass().getName();
        checkerName = checkerName.substring(checkerName.lastIndexOf(".") + 1);
        return checkerName;

    }
}
