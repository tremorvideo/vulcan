package com.aol.advertising.vulcan.api;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;

import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.aol.advertising.vulcan.ConfiguredUnitTest;
import com.aol.advertising.vulcan.api.AvroWriterBuilder;
import com.aol.advertising.vulcan.api.builder.steps.AvroFilenameStep;
import com.aol.advertising.vulcan.api.builder.steps.OptionalSteps;
import com.aol.advertising.vulcan.api.rolling.RollingPolicy;
import com.aol.advertising.vulcan.exception.DisruptorExceptionHandler;
import com.aol.advertising.vulcan.ringbuffer.AvroEvent;
import com.aol.advertising.vulcan.ringbuffer.AvroEventFactory;
import com.aol.advertising.vulcan.rolling.TimeAndSizeBasedRollingPolicy;
import com.aol.advertising.vulcan.rolling.TimeAndSizeBasedRollingPolicyConfig;
import com.aol.advertising.vulcan.writer.AvroEventConsumer;
import com.aol.advertising.vulcan.writer.AvroEventPublisher;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

// @formatter:off
@RunWith(PowerMockRunner.class)
@PrepareForTest({AvroEventPublisher.class, AvroEventConsumer.class, TimeAndSizeBasedRollingPolicy.class,
                 Disruptor.class, ProducerType.class, Files.class, AvroWriterBuilder.class,
                 Paths.class})
// @formatter:on
public class DisruptorAvroFileWriterBuilderTest extends ConfiguredUnitTest {

  private static final String AVRO_FILE_NAME = "Pizza dough";
  private static final TimeAndSizeBasedRollingPolicyConfig ROLLING_POLICY_CONFIGURATION =
      new TimeAndSizeBasedRollingPolicyConfig().withFileRollingSizeOf(345);

  private AvroFilenameStep disruptorAvroFileWriterBuilderUnderTest;

  @Spy
  private AvroFilenameStep spyOnDisruptorAvroFileWriterBuilder = AvroWriterBuilder.startCreatingANewWriter();
  @Mock
  private AvroEventPublisher avroEventPublisherMock;
  @Mock
  private Path avroFileNameMock;
  @Mock
  private TimeAndSizeBasedRollingPolicy timeAndSizeBasedRollingPolicyMock;
  @Mock
  private Schema avroSchemaMock;
  @Mock
  private Path parentDirMock;
  @Mock
  private DirectoryStream<Path> directoryStreamMock;
  @Mock
  private Iterator<Path> iteratorMock;
  @Mock
  private Disruptor<AvroEvent> disruptorMock;
  @Mock
  private AvroEventConsumer avroEventConsumerMock;
  @Mock
  private ProducerType producerTypeMock;
  @Mock
  private WaitStrategy waitStrategyMock;
  @Mock
  private RollingPolicy rollingPolicyMock;
  @Mock
  private TimeAndSizeBasedRollingPolicy configuredTimeAndSizeBasedRollingPolicyMock;

  @Before
  public void setUp() throws Exception {
    initMocks();
    wireUpMocks();

    disruptorAvroFileWriterBuilderUnderTest = AvroWriterBuilder.startCreatingANewWriter();
  }
  
  private void initMocks() throws Exception {
    mockStatic(Files.class);
    mockPaths();
    mockConstructors();
    mockPermissions();
  }

  private void mockPaths() {
    mockStatic(Paths.class);
    when(Paths.get(AVRO_FILE_NAME)).thenReturn(avroFileNameMock);
  }

  private void mockConstructors() throws Exception {
    whenNew(AvroEventPublisher.class).withAnyArguments().thenReturn(avroEventPublisherMock);
    whenNew(AvroEventConsumer.class).withAnyArguments().thenReturn(avroEventConsumerMock);
    whenNew(TimeAndSizeBasedRollingPolicy.class).withAnyArguments().thenReturn(timeAndSizeBasedRollingPolicyMock);
    whenNew(TimeAndSizeBasedRollingPolicy.class).withArguments(ROLLING_POLICY_CONFIGURATION)
                                                .thenReturn(configuredTimeAndSizeBasedRollingPolicyMock);
    whenNew(Disruptor.class).withAnyArguments().thenReturn(disruptorMock);
  }

  private void mockPermissions() {
    mockDestinationFilePermissions();
    mockParentDirPermissions();
  }

  private void mockDestinationFilePermissions() {
    when(Files.isReadable(avroFileNameMock)).thenReturn(true);
    when(Files.isWritable(avroFileNameMock)).thenReturn(true);
  }

  private void mockParentDirPermissions() {
    when(Files.isReadable(parentDirMock)).thenReturn(true);
    when(Files.isWritable(parentDirMock)).thenReturn(true);
    when(Files.isExecutable(parentDirMock)).thenReturn(true);
  }

  private void wireUpMocks() {
    when(avroFileNameMock.getParent()).thenReturn(parentDirMock);
  }

  @Test(expected = NullPointerException.class)
  public void whenANullDestinationFileAsPathIsSpecified_thenANullPointerExceptionIsThrown() {
    disruptorAvroFileWriterBuilderUnderTest.thatWritesTo((Path) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenTheDestinationFileIsADirectory_thenAnIllegalArgumentExceptionIsThrown() {
    givenDestinationFileIsADirectory();

    disruptorAvroFileWriterBuilderUnderTest.thatWritesTo(avroFileNameMock);
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenTheDestinationFileExists_andIsNotReadable_thenAnIllegalArgumentExceptionIsThrown() {
    givenDestinationFileIsNotReadable();

    disruptorAvroFileWriterBuilderUnderTest.thatWritesTo(avroFileNameMock);
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenTheDestinationFileExists_andIsNotWritable_thenAnIllegalArgumentExceptionIsThrown() {
    givenDestinationFileIsNotWritable();

    disruptorAvroFileWriterBuilderUnderTest.thatWritesTo(avroFileNameMock);
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenTheDestinationFileDoesNotExist_andTheContainingDirectoryIsNotReadable_thenAnIllegalArgumentExceptionIsThrown() {
    givenDestinationParentDirIsNotReadable();
    
    disruptorAvroFileWriterBuilderUnderTest.thatWritesTo(avroFileNameMock);
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenTheDestinationFileDoesNotExist_andTheContainingDirectoryIsNotWritable_thenAnIllegalArgumentExceptionIsThrown() {
    givenDestinationParentDirIsNotWritable();

    disruptorAvroFileWriterBuilderUnderTest.thatWritesTo(avroFileNameMock);
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenTheDestinationFileDoesNotExist_andTheContainingDirectoryIsNotExecutable_thenAnIllegalArgumentExceptionIsThrown() {
    givenDestinationParentDirIsNotExecutable();

    disruptorAvroFileWriterBuilderUnderTest.thatWritesTo(avroFileNameMock);
  }

  @Test
  public void whenTheDestinationFileIsValid_andAWriterIsBuilt_thenTheConfiguredDestinationFileIsUsedToBuildTheWriter() throws Exception {
    givenDestinationFileIsValid();
    
    disruptorAvroFileWriterBuilderUnderTest.thatWritesTo(avroFileNameMock).thatWritesRecordsOf(avroSchemaMock).createNewWriter();

    thenConfiguredDestinationFileIsUsedInTheFinalWriterObject();
  }
  
  @Test(expected = NullPointerException.class)
  public void whenANullDestinationFileAsStringIsSpecified_thenANullPointerExceptionIsThrown() {
    disruptorAvroFileWriterBuilderUnderTest.thatWritesTo((String) null);
  }

  // TODO: A data-driven test should be used instead of this delegation check, I couldn't make
  // JunitParams work together with PowerMock though. This needs more investigation to see if they
  // can be made to work together or maybe the test should use the default parameterized JUnit tests
  // (not recommended)
  @Test
  public void whenDestinationFileIsSpecifiedAsString_thenBuildProcessIsDelegatedToThePathInterface() {
    spyOnDisruptorAvroFileWriterBuilder.thatWritesTo(AVRO_FILE_NAME);

    verify(spyOnDisruptorAvroFileWriterBuilder.thatWritesTo(avroFileNameMock));
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenANullSchemaIsSpecified_thenAnIllegalArgumentExceptionIsThrown() {
    disruptorAvroFileWriterBuilderUnderTest.thatWritesTo(avroFileNameMock).thatWritesRecordsOf(null);
  }

  @Test
  public void whenTheSchemaIsValid_andAWriterIsBuilt_thenTheConfiguredIsSchemaUsedToBuildTheWriter() throws Exception {
    givenDestinationFileIsValid();
    
    disruptorAvroFileWriterBuilderUnderTest.thatWritesTo(avroFileNameMock).thatWritesRecordsOf(avroSchemaMock).createNewWriter();

    thenConfiguredSchemaIsUsedInTheFinalWriterObject();
  }
  
  @Test
  public void whenNoOptionalStepsAreCalled_thenDefaultsAreUsedToBuildTheWriter() throws Exception {
    OptionalSteps disruptorAvroFileWriterBuilderUnderTest = givenABuilderWithMandatoryStepsConfigured();

    disruptorAvroFileWriterBuilderUnderTest.createNewWriter();

    thenDefaultsAreUsedInTheFinalWriterObject();
  }
  
  @Test
  public void whenTheRingBufferSizeIsConfigured_thenItIsUsedToBuildTheWriter() throws Exception {
    OptionalSteps disruptorAvroFileWriterBuilderUnderTest = givenABuilderWithMandatoryStepsConfigured();
    
    disruptorAvroFileWriterBuilderUnderTest.withRingBufferSize(Integer.MAX_VALUE).createNewWriter();

    thenConfiguredBufferSizeIsUsedInTheFinalWriterObject();
  }

  @Test
  public void whenTheProducerTypeIsConfigured_thenItIsUsedToBuildTheWriter() throws Exception {
    OptionalSteps disruptorAvroFileWriterBuilderUnderTest = givenABuilderWithMandatoryStepsConfigured();
    
    disruptorAvroFileWriterBuilderUnderTest.withProducerType(producerTypeMock).createNewWriter();

    thenConfiguredProducerTypeIsUsedInTheFinalWriterObject();
  }
  
  @Test
  public void whenTheWaitStrategyIsConfigured_thenItIsUsedToBuildTheWriter() throws Exception {
    OptionalSteps disruptorAvroFileWriterBuilderUnderTest = givenABuilderWithMandatoryStepsConfigured();

    disruptorAvroFileWriterBuilderUnderTest.withWaitStrategy(waitStrategyMock).createNewWriter();

    thenConfiguredWriteStrategyIsUsedInTheFinalWriterObject();
  }

  @Test
  public void whenTheRollingPolicyIsConfigured_thenItIsUsedToBuildTheWriter() throws Exception {
    OptionalSteps disruptorAvroFileWriterBuilderUnderTest = givenABuilderWithMandatoryStepsConfigured();
    
    disruptorAvroFileWriterBuilderUnderTest.withRollingPolicy(rollingPolicyMock).createNewWriter();

    thenConfiguredRollingPolicyIsUsedInTheFinalWriterObject();
  }
  
  @Test
  public void whenAConfigurationForTheDefaultRollingPolicyIsSpecified_thenTheValueIsUsedWhenBuildingTheWriter() throws Exception {
    OptionalSteps disruptorAvroFileWriterBuilderUnderTest = givenABuilderWithMandatoryStepsConfigured();
    
    disruptorAvroFileWriterBuilderUnderTest.withDefaultRollingPolicyConfiguration(
        ROLLING_POLICY_CONFIGURATION).createNewWriter();
    
    thenTheConfigurationForTheDefaultPolicyIsUsedWhenBuildingTheWriterObject();
  }
  
  @Test
  public void whenTheWriterIsBuilt_thenTheSpecifiedDestinationAvroFileIsRegisteredWithTheRollingPolicy() throws Exception {
    OptionalSteps disruptorAvroFileWriterBuilderUnderTest = givenABuilderWithMandatoryStepsConfigured();
    
    disruptorAvroFileWriterBuilderUnderTest.createNewWriter();
    
    verify(timeAndSizeBasedRollingPolicyMock).registerAvroFilename(avroFileNameMock);
  }
  
  @Test
  public void whenTheWriterIsBuilt_thenExceptionsWithinDisruptorAreHandledWithADisruptorExceptionHandler() throws Exception {
    OptionalSteps disruptorAvroFileWriterBuilderUnderTest = givenABuilderWithMandatoryStepsConfigured();
    
    disruptorAvroFileWriterBuilderUnderTest.createNewWriter();
    
    verify(disruptorMock).handleExceptionsWith(isA(DisruptorExceptionHandler.class));
  }
  
  @Test
  public void whenTheWriterIsBuilt_thenTheEventsConsumerExecutorIsRegisteredForShutdown() throws Exception {
    OptionalSteps disruptorAvroFileWriterBuilderUnderTest = givenABuilderWithMandatoryStepsConfigured();
    
    disruptorAvroFileWriterBuilderUnderTest.createNewWriter();
    
    verify(avroEventPublisherMock).registerConsumerExecutorForShutdown(isA(ExecutorService.class));
  }

  private void givenDestinationFileIsADirectory() {
    when(Files.exists(avroFileNameMock)).thenReturn(true);
    when(Files.isDirectory(avroFileNameMock)).thenReturn(true);
  }

  private void givenDestinationFileIsNotReadable() {
    when(Files.exists(avroFileNameMock)).thenReturn(true);
    when(Files.isReadable(avroFileNameMock)).thenReturn(false);
  }

  private void givenDestinationFileIsNotWritable() {
    when(Files.exists(avroFileNameMock)).thenReturn(true);
    when(Files.isWritable(avroFileNameMock)).thenReturn(false);
  }

  private void givenDestinationParentDirIsNotReadable() {
    when(Files.exists(avroFileNameMock)).thenReturn(false);
    when(Files.isReadable(parentDirMock)).thenReturn(false);
  }
  
  private void givenDestinationParentDirIsNotWritable() {
    when(Files.exists(avroFileNameMock)).thenReturn(false);
    when(Files.isWritable(parentDirMock)).thenReturn(false);
  }
  
  private void givenDestinationParentDirIsNotExecutable() {
    when(Files.exists(avroFileNameMock)).thenReturn(false);
    when(Files.isExecutable(parentDirMock)).thenReturn(false);
  }

  private OptionalSteps givenABuilderWithMandatoryStepsConfigured() throws IOException {
    givenDestinationFileIsValid();
    return disruptorAvroFileWriterBuilderUnderTest.thatWritesTo(avroFileNameMock).thatWritesRecordsOf(avroSchemaMock);
  }

  private void givenDestinationFileIsValid() throws IOException {
    when(Files.exists(avroFileNameMock)).thenReturn(true);
    when(Files.isDirectory(avroFileNameMock)).thenReturn(false);
  }
  
  @SuppressWarnings("unchecked")
  private void thenConfiguredDestinationFileIsUsedInTheFinalWriterObject() throws Exception {
    verifyNew(AvroEventConsumer.class).withArguments(eq(avroFileNameMock), any(Schema.class), any(RollingPolicy.class), any(CodecFactory.class));
    verify(disruptorMock).handleEventsWith(avroEventConsumerMock);
    verify(avroEventPublisherMock).startPublisherUsing(disruptorMock);
  }
  
  @SuppressWarnings("unchecked")
  private void thenConfiguredSchemaIsUsedInTheFinalWriterObject() throws Exception {
    verifyNew(AvroEventConsumer.class).withArguments(any(Path.class), eq(avroSchemaMock), any(RollingPolicy.class), any(CodecFactory.class));
    verify(disruptorMock).handleEventsWith(avroEventConsumerMock);
    verify(avroEventPublisherMock).startPublisherUsing(disruptorMock);
  }

  private void thenDefaultsAreUsedInTheFinalWriterObject() throws Exception {
    verifyRollingPolicyDefaults();
    verifyDisruptorDefaults();
    verify(avroEventPublisherMock).startPublisherUsing(disruptorMock);
  }
  
  @SuppressWarnings("unchecked")
  private void verifyRollingPolicyDefaults() throws Exception {
    verifyNew(AvroEventConsumer.class).withArguments(any(Path.class), any(Schema.class), eq(timeAndSizeBasedRollingPolicyMock), any(CodecFactory.class));
    verify(disruptorMock).handleEventsWith(avroEventConsumerMock);
  }
  
  private void verifyDisruptorDefaults() throws Exception {
    verifyNew(Disruptor.class).withArguments(any(AvroEventFactory.class), eq(2048), any(ExecutorService.class),
                                             eq(ProducerType.MULTI), isA(SleepingWaitStrategy.class));
  }
  
  private void thenConfiguredBufferSizeIsUsedInTheFinalWriterObject() throws Exception {
    verifyNew(Disruptor.class).withArguments(any(AvroEventFactory.class), eq(Integer.MAX_VALUE), any(ExecutorService.class),
                                             any(ProducerType.class), any(WaitStrategy.class));
    verify(avroEventPublisherMock).startPublisherUsing(disruptorMock);
  }
  
  private void thenConfiguredProducerTypeIsUsedInTheFinalWriterObject() throws Exception {
    verifyNew(Disruptor.class).withArguments(any(AvroEventFactory.class), anyInt(), any(ExecutorService.class),
                                             eq(producerTypeMock), any(WaitStrategy.class));
    verify(avroEventPublisherMock).startPublisherUsing(disruptorMock);
  }
  
  private void thenConfiguredWriteStrategyIsUsedInTheFinalWriterObject() throws Exception {
    verifyNew(Disruptor.class).withArguments(any(AvroEventFactory.class), anyInt(),
                                             any(ExecutorService.class), any(ProducerType.class), eq(waitStrategyMock));
    verify(avroEventPublisherMock).startPublisherUsing(disruptorMock);
  }

  private void thenConfiguredRollingPolicyIsUsedInTheFinalWriterObject() throws Exception {
    verifyNew(AvroEventConsumer.class).withArguments(any(Path.class), any(Schema.class), eq(rollingPolicyMock), any(CodecFactory.class));
    verifyWriterIsStartedWithConfiguredObjects();
  }

  private void thenTheConfigurationForTheDefaultPolicyIsUsedWhenBuildingTheWriterObject() throws Exception {
    verifyNew(TimeAndSizeBasedRollingPolicy.class).withArguments(ROLLING_POLICY_CONFIGURATION);
    verifyNew(AvroEventConsumer.class).withArguments(any(Path.class), any(Schema.class), eq(configuredTimeAndSizeBasedRollingPolicyMock), any(CodecFactory.class));
    verifyWriterIsStartedWithConfiguredObjects();
  }

  @SuppressWarnings("unchecked")
  private void verifyWriterIsStartedWithConfiguredObjects() {
    verify(disruptorMock).handleEventsWith(avroEventConsumerMock);
    verify(avroEventPublisherMock).startPublisherUsing(disruptorMock);
  }
}
