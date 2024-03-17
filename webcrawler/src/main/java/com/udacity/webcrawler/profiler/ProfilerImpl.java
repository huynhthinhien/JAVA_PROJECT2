package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
//This class will "wrap" the to-be-profiled objects in a dynamic proxy instance.

final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  @Profiled
  public boolean checkKlasseIsProfiled(Class<?> klasse) throws IllegalArgumentException{
    //If at least one method is annotated with @Profiled, will return true else return false.
    return Arrays.stream(klasse.getDeclaredMethods())
            .anyMatch(method -> method.isAnnotationPresent(Profiled.class));
  }


  @Override
  public <T> T wrap(Class<T> klass, T delegate) throws IllegalArgumentException {

    Objects.requireNonNull(klass);

    // Check the klass to see if there is a profile or not
    if (!checkKlasseIsProfiled(klass)) {
      throw new IllegalArgumentException("Has no profiled method");
    }
    // Create an interceptor to record the execution time of the methods
    ProfilingMethodInterceptor proInterceptor = new ProfilingMethodInterceptor(this.clock, delegate, this.state);

    // Create a dynamic proxy using Proxy.newProxyInstance
    @SuppressWarnings("unchecked")
    Object proxy = (T) Proxy.newProxyInstance(ProfilerImpl.class.getClassLoader(), new Class[]{klass}, proInterceptor);
    return (T) proxy;
  }

  @Override
  public void writeData(Path path) throws IOException{
    // TODO: Write the ProfilingState data to the given file path. If a file already exists at that
    //       path, the new data should be appended to the existing file.
    Objects.requireNonNull(path);
    // If file not exist, create new file
    if (Files.notExists(path)){
      Files.createFile(path);
    }
    try (BufferedWriter w = Files.newBufferedWriter(path);){
      // Wirte file
      writeData(w);
      w.flush(); // close file
    }catch(IOException ioe){
      ioe.printStackTrace();
    }

  }


  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }

}