// Copyright 2017 Imperial College London
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package uk.ac.ic.doc.multicore.oglfuzzer.server;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.FuzzerService;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.FuzzerServiceManager;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.GetTokenResult;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.ImageJob;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.ImageJobResult;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.ImageJobStatus;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.Job;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.JobId;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.PlatformInfo;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.Token;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.TokenError;

/**
 * Created by david on 06/07/2017.
 */
public class FuzzerServerIntegrationTests {
  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private FuzzerService.Iface fuzzerService;
  private FuzzerServiceManager.Iface fuzzerServiceManager;
  private ExecutorService executorService;

  @Before
  public void setupServices() throws IOException {
    final String processing = testFolder.newFolder("processing").toString();
    final IArtifactManager artifactManager = new LocalArtifactManager(
        testFolder.newFolder("shadersets").toString(), processing);
    this.executorService = Executors.newCachedThreadPool();
    final FuzzerServiceImpl fuzzerServiceImpl = new FuzzerServiceImpl(
        artifactManager, processing, executorService
    );
    this.fuzzerService = fuzzerServiceImpl;
    this.fuzzerServiceManager = new FuzzerServiceManagerImpl(fuzzerServiceImpl,
          (command, manager) -> {
            throw new RuntimeException("TODO: Need to decide what dispatcher to "
                  + "provide for these tests.");
          });
  }

  @Test
  public void willErrorOnAMismatchedJobId() throws Exception {
    final Token token = newToken();

    assertNotNull(token);

    final Job job = new Job().setImageJob(new ImageJob()).setJobId(new JobId().setValue(1));
    final Future<Job> submitting = this.submitJob(token, job, 1);

    try {
      final Job otherJob = new Job().setImageJob(new ImageJob()).setJobId(new JobId().setValue(2));

      final Job got = this.getAJob(token);
      assertEquals(job.getJobId(), got.getJobId());

      thrown.expect(TException.class);

      this.fuzzerService.jobDone(token, otherJob);
    } finally {
      submitting.cancel(true);
    }
  }

  @Test
  public void willGetASubmittedJob() throws Exception {
    final Token token = newToken();

    assertNotNull(token);
    final Job job = new Job().setImageJob(new ImageJob()).setJobId(new JobId().setValue(1));
    assertTrue(job.toString(), job.isSetImageJob());

    Future<Job> submitting = submitJob(token, job, 1);

    this.clientRuns(token, (todo) -> {
      assertTrue(todo.toString(), todo.isSetImageJob());
      assertEquals(1, todo.getJobId().getValue());
      todo.getImageJob().setResult(new ImageJobResult().setStatus(ImageJobStatus.UNEXPECTED_ERROR));
      return todo;
    });

    Job result = submitting.get();
    assertEquals(result.getImageJob().getResult().getStatus(), ImageJobStatus.UNEXPECTED_ERROR);
  }

  @Test
  public void willSetSkippedAfterFailures() throws Exception {

    final Token token = newToken();

    assertNotNull(token);
    final Job job = new Job().setImageJob(new ImageJob()).setJobId(new JobId().setValue(1));
    assertTrue(job.toString(), job.isSetImageJob());

    Future<Job> submitting = submitJob(token, job, 3);
    this.clientRepeatedlyCrashes(token, 3);
    Job result = submitting.get();
    assertFalse(result.isSetSkipJob());
    assertTrue(result.isSetImageJob());
    final ImageJobResult jobResult = result.getImageJob().getResult();
    assertEquals(ImageJobStatus.SKIPPED, jobResult.getStatus());
    assertEquals("SKIPPED\n", jobResult.errorMessage);
  }

  @Test
  public void willSanitizeValueOnOldToken() throws Exception {
    Token oldToken = new Token().setValue("  helloworld ");
    GetTokenResult getTokenResult = this.fuzzerService.getToken(
        new PlatformInfo().setContents("{}"), oldToken);
    assertTrue(getTokenResult.isSetToken());
    assertEquals("helloworld", getTokenResult.getToken().getValue());
  }

  @Test
  public void willRejectTokenWithChangedPlatformInfo() throws Exception {
    Token oldToken = this.newToken();
    GetTokenResult getTokenResult = this.fuzzerService.getToken(
        new PlatformInfo().setContents("{\"bogus\": \"key\"}"), oldToken);
    assertFalse(getTokenResult.isSetToken());
    assertEquals(getTokenResult.getError(), TokenError.PLATFORM_INFO_CHANGED);
  }

  @Test
  public void willRejectAnInvalidToken() throws Exception {
    GetTokenResult getTokenResult = this.fuzzerService.getToken(
        new PlatformInfo().setContents("{}"),
        new Token().setValue("hello world")
    );
    assertFalse(getTokenResult.isSetToken());
    assertEquals(getTokenResult.getError(), TokenError.INVALID_PROVIDED_TOKEN);
  }

  @Test
  public void willRejectANonObjectPlatformInfo() throws Exception {
    GetTokenResult getTokenResult = this.fuzzerService.getToken(
        new PlatformInfo().setContents(""),
        new Token()
    );
    assertFalse(getTokenResult.isSetToken());
    assertEquals(getTokenResult.getError(), TokenError.INVALID_PLATFORM_INFO);
  }

  @Test
  public void willPutManufacturerAndModelIntoToken() throws Exception {
    Gson gson = new Gson();
    Map<String, String> platformInfoData = new HashMap<>();
    platformInfoData.put("manufacturer", "ABCD");
    platformInfoData.put("model", "12345");
    String platformInfo = gson.toJson(
        platformInfoData
    );

    GetTokenResult getTokenResult =
        this.fuzzerService.getToken(new PlatformInfo().setContents(platformInfo), new Token());

    assertTrue(getTokenResult.isSetToken());
    assertTrue(getTokenResult.getToken().getValue().contains("ABCD"));
    assertTrue(getTokenResult.getToken().getValue().contains("12345"));
  }

  // Helper methods below here
  private Future<Job> submitJob(Token token, Job job, int retryLimit) {
    return this.submit(() ->
        this.fuzzerServiceManager.submitJob(
            job, token, retryLimit
        )
    );
  }

  private <T> Future<T> submit(ThriftCallable<T> callable) {
    return this.executorService.submit(() -> {
      try {
        return callable.callThrift();
      } catch (TException t) {
        throw new RuntimeException(t);
      }
    });
  }

  private void clientRepeatedlyCrashes(Token token, int maxCrashes) throws Exception {
    for (int i = 0; i <= maxCrashes; i++) {
      boolean gotJob = false;
      for (int j = 0; j < 500; j++) {
        Job todo = this.fuzzerService.getJob(token).deepCopy();
        if (todo.isSetNoJob()) {
          Thread.sleep(1);
          continue;
        }
        if (todo.isSetSkipJob()) {
          this.fuzzerService.jobDone(token, todo.deepCopy());
          return;
        }
        gotJob = true;
        break;
      }
      assertTrue("On try " + i + " did not get a job despite waiting 500ms", gotJob);
    }
  }

  private Job getAJob(Token token) throws Exception {
    while (true) {
      Job todo = this.fuzzerService.getJob(token).deepCopy();
      if (todo.isSetNoJob()) {
        Thread.sleep(1);
        continue;
      }
      return todo.deepCopy();
    }
  }

  private void clientRuns(Token token, ClientAction client) throws Exception {
    Job todo = this.getAJob(token);
    Job result = client.run(todo.deepCopy());
    this.fuzzerService.jobDone(token, result);
  }

  private Token newToken() throws TException {
    return this.fuzzerService.getToken(
        new PlatformInfo().setContents("{}"), new Token()
    ).getToken();
  }

  interface ThriftCallable<T> {
    T callThrift() throws Exception;
  }

  interface ClientAction {
    Job run(Job job) throws Exception;
  }
}