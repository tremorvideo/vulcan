package com.aol.advertising.dmp.disruptor.exception;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LoggerFactory.class, DisruptorExceptionHandler.class})
public class DisruptorExceptionHandlerTest {
  
  private DisruptorExceptionHandler disruptorExceptionHandlerUnderTest;

  private static Logger logMock;
  @Mock
  private Throwable exceptionMock;
  @Mock
  private Object eventMock;

  @BeforeClass
  public static void setUpSuite() {
    logMock = mock(Logger.class);
    mockStatic(LoggerFactory.class);
    when(LoggerFactory.getLogger(DisruptorExceptionHandler.class)).thenReturn(logMock);
  }

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    disruptorExceptionHandlerUnderTest = new DisruptorExceptionHandler();
  }
  
  @Test
  public void whenAnExceptionOccursWhileHandlingAnEvent_thenAnErrorWithTheExceptionIsLogged() {
    disruptorExceptionHandlerUnderTest.handleEventException(exceptionMock, Long.MIN_VALUE, eventMock);
    
    verify(logMock).error(anyString(), same(eventMock), same(exceptionMock));
  }

  @Test
  public void whenAnExceptionOccursDuringStartup_thenAnErrorWithTheExceptionIsLogged() {
    disruptorExceptionHandlerUnderTest.handleOnStartException(exceptionMock);
    
    verify(logMock).error(anyString(), same(exceptionMock));
  }

  @Test
  public void whenAnExceptionOccursOnShutdown_thenAnErrorWithTheExceptionIsLogged() {
    disruptorExceptionHandlerUnderTest.handleOnShutdownException(exceptionMock);

    verify(logMock).error(anyString(), same(exceptionMock));
  }

}
