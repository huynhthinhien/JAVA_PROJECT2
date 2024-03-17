package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {
    private final Clock clock;
    private final Object delegate;
    private final ProfilingState state;

    ProfilingMethodInterceptor(Clock clock, Object delegate, ProfilingState state) {
        this.clock = Objects.requireNonNull(clock);
        this.delegate = delegate;
        this.state = state;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //check is annotated with @Profiled or not
        if (method.isAnnotationPresent(Profiled.class)) {
            // start time
            Instant time = clock.instant();
            try {
                return method.invoke(delegate, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } finally {
              //Information about the method's execution time is logged
               Instant endTime = clock.instant();
               Duration elapsedTime = Duration.between(time, endTime);
               state.record(delegate.getClass(), method, elapsedTime);
            }
        }
        return method.invoke(delegate, args);
    }
}