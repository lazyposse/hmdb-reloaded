/*
 * Copyright 2011 Janis Kazakovs <janis.kazakovs@opatopa.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import java.util.Arrays;

public @Aspect abstract class SnijureAspect
{
    public @Pointcut abstract void logging ( );

    @Before ( value = "logging()", argNames = "joinPoint" )
    public void enteringMethod ( JoinPoint joinPoint )
    {
        Signature signature = joinPoint.getSignature ( );
        String className    = signature.getDeclaringType ( ).getSimpleName ( );
        String methodName   = signature.getName ( );
        Object[] args       = joinPoint.getArgs();
        prn("enteringMethod "+className+"::"+methodName+"::args="+Arrays.deepToString(args));
    }

    @AfterReturning ( pointcut = "logging()", returning = "returnValue", argNames = "joinPoint,returnValue" )
    public void leavingMethod ( JoinPoint joinPoint, Object returnValue )
    {
        Signature signature = joinPoint.getSignature ( );
        String className    = signature.getDeclaringType ( ).getSimpleName ( );
        String methodName   = signature.getName ( );
        prn("leavingMethod "+className+"::"+methodName);
    }

    @AfterThrowing ( pointcut = "logging()", throwing = "throwable", argNames = "joinPoint,throwable" )
    public void leavingMethodException ( JoinPoint joinPoint, Throwable throwable )
    {
        Signature signature     = joinPoint.getSignature ( );
        String className        = signature.getDeclaringType ( ).getSimpleName ( );
        String methodName       = signature.getName ( );
        String exceptionMessage = throwable.getMessage ( );
        prn("leavingMethodException "+className+"::"+methodName+". Reason: "+exceptionMessage);
    }

    private static void prn(String msg) {
        System.out.println(msg);
    }
}
