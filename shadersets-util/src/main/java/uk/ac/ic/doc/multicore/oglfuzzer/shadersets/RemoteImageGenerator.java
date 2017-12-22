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

package uk.ac.ic.doc.multicore.oglfuzzer.shadersets;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.io.FileUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ic.doc.multicore.oglfuzzer.common.util.FileHelper;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.FuzzerServiceManager;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.ImageJob;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.ImageJobResult;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.Job;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.JobId;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.ShaderFile;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.Token;

public class RemoteImageGenerator implements IImageGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(RemoteImageGenerator.class);

  private String url;
  private String token;
  private FuzzerServiceManager.Iface fuzzerServiceManager;

  private final AtomicLong jobCounter;
  private final int retryLimit;

  private static final int DEFAULT_RETRY_LIMIT = 2;

  public RemoteImageGenerator(String url, String token,
      FuzzerServiceManager.Iface fuzzerServiceManager, AtomicLong jobCounter) {
    this(url, token, fuzzerServiceManager, jobCounter, DEFAULT_RETRY_LIMIT);
  }

  public RemoteImageGenerator(String url, String token,
      FuzzerServiceManager.Iface fuzzerServiceManager, AtomicLong jobCounter, int retryLimit) {
    this.url = url;
    this.token = token;
    this.fuzzerServiceManager = fuzzerServiceManager;
    this.jobCounter = jobCounter;
    this.retryLimit = retryLimit;
  }

  public RemoteImageGenerator(String url, String token) {
    this(url, token, null, new AtomicLong(), DEFAULT_RETRY_LIMIT);
  }

  @Override
  public ImageJobResult getImage(
      File shaderFile,
      File referenceFile,
      File outputImageFile,
      boolean skipRender) throws ImageGeneratorException {

    LOGGER.info("Get image (via server) {} {}", shaderFile, referenceFile);

    // Optimisation: no need to actually use HTTP if we are on the server.
    if (fuzzerServiceManager != null) {
      try {
        return getImageHelper(shaderFile, referenceFile, fuzzerServiceManager, skipRender);
      } catch (IOException | TException exception) {
        throw new ImageGeneratorException(exception);
      }
    }

    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

      TTransport transport = new THttpClient(url, httpClient);
      transport.open();
      TProtocol protocol = new TBinaryProtocol(transport);
      FuzzerServiceManager.Iface fuzzerServiceManagerProxy = new FuzzerServiceManager.Client(
          protocol);

      return getImageHelper(shaderFile, referenceFile, fuzzerServiceManagerProxy, skipRender);
    } catch (IOException | TException exception) {
      throw new ImageGeneratorException(exception);
    }
  }

  private ImageJobResult getImageHelper(File shaderFile,
      File referenceFile,
      FuzzerServiceManager.Iface fuzzerServiceManagerProxy,
      boolean skipRender)
      throws IOException, TException {

    ImageJob imageJob = new ImageJob();

    if (referenceFile != null) {
      imageJob
          .setReference(new ShaderFile()
              .setName(referenceFile.getName())
              .setContents(FileUtils.readFileToString(referenceFile, Charset.defaultCharset()))
              .setInfo(FileUtils.readFileToString(
                  FileHelper.replaceExtension(referenceFile, ".json"),
                  Charset.defaultCharset())));
    }

    if (skipRender) {
      imageJob
          .setSkipRender(true);
    }

    imageJob
        .setShader(new ShaderFile()
            .setName(shaderFile.getName())
            .setContents(FileUtils.readFileToString(shaderFile, Charset.defaultCharset()))
            .setInfo(FileUtils.readFileToString(
                  FileHelper.replaceExtension(shaderFile, ".json"),
                Charset.defaultCharset())))
        .setMeta(shaderFile.getName());

    Job job = new Job();
    job
        .setJobId(new JobId().setValue(jobCounter.incrementAndGet()))
        .setImageJob(imageJob);

    return fuzzerServiceManagerProxy.submitJob(job, new Token().setValue(token), retryLimit)
        .getImageJob()
        .getResult();
  }
}
