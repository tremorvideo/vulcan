package com.aol.advertising.vulcan.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.ExceptionHandler;

public class DisruptorExceptionHandler implements ExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(DisruptorExceptionHandler.class);

  @Override
  public void handleEventException(Throwable ex, long sequence, Object event) {
    log.error("Error while handling event\n{}\ndue to", event, ex);
  }

  @Override
  public void handleOnStartException(Throwable ex) {
    log.error("Failed to initialize the events consumer. Avro records will not be written to file", ex);
  }

  @Override
  public void handleOnShutdownException(Throwable ex) {
    log.error("Exception while shutting down events consumer", ex);
  }
}
