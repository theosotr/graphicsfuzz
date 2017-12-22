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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ic.doc.multicore.oglfuzzer.common.util.IShaderSet;
import uk.ac.ic.doc.multicore.oglfuzzer.common.util.IShaderSetExperiment;
import uk.ac.ic.doc.multicore.oglfuzzer.common.util.LocalShaderSet;
import uk.ac.ic.doc.multicore.oglfuzzer.common.util.LocalShaderSetExperiement;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.FuzzerServiceManager;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.ImageJobResult;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.ImageJobStatus;

public class RunShaderSet {

  private static final Logger LOGGER = LoggerFactory.getLogger(RunShaderSet.class);

  public static void main(String[] args)
      throws ImageGeneratorException, InterruptedException, IOException {
    try {
      mainHelper(args, null);
    } catch (ArgumentParserException exception) {
      exception.getParser().handleError(exception);
      System.exit(1);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }

  public static void mainHelper(
      String[] args,
      FuzzerServiceManager.Iface managerOverride)
      throws ImageGeneratorException, InterruptedException, IOException, ArgumentParserException {

    ArgumentParser parser = ArgumentParsers.newArgumentParser("RunShaderSet")
        .defaultHelp(true)
        .description("Get images for all shaders in a shader set.");

    parser.addArgument("--verbose")
        .action(Arguments.storeTrue())
        .help("Verbose output.");

    parser.addArgument("--server")
        .help(
            "URL of server to use for sending get image requests.")
        .type(String.class);

    parser.addArgument("--token")
        .help("The token of the client used for get image requests. Used with --server.")
        .type(String.class);

    parser.addArgument("--output")
        .help("Output directory.")
        .setDefault(new File("."))
        .type(File.class);

    parser.addArgument("shader_set_directory")
        .help("Shader set directory")
        .type(File.class);


    Namespace ns = parser.parseArgs(args);

    final boolean verbose = ns.get("verbose");
    final File shaderSetFile = ns.get("shader_set_directory");
    final String server = ns.get("server");
    final String token = ns.get("token");
    final File outputDir = ns.get("output");

    if (managerOverride != null && (server == null || token == null)) {
      throw new ArgumentParserException(
          "Must supply server (dummy string) and token when executing in server process.",
          parser);
    }

    if (server != null) {
      if (token == null) {
        throw new ArgumentParserException("Must supply token with server.", parser);
      }
    }

    IImageGenerator imageGenerator =
        server == null
            ? new LocalImageGenerator(false)
            : new RemoteImageGenerator(
                server + "/manageAPI",
                token,
                managerOverride,
                new AtomicLong());

    FileUtils.forceMkdir(outputDir);

    if (!shaderSetFile.isDirectory()) {
      if (!shaderSetFile.isFile() || !shaderSetFile.getName().endsWith(".frag")) {
        throw new ArgumentParserException(
            "Shader set must be a directory or a single .frag shader.", parser);
      }
      // Special case: run get image on a single shader.
      runShader(shaderSetFile, null, outputDir, imageGenerator, Optional.empty());
      return;
    }

    IShaderSet shaderSet = new LocalShaderSet(shaderSetFile);

    runShaderSet(shaderSet, outputDir, imageGenerator);
  }

  public static int runShaderSet(IShaderSet shaderSet, File workDir, IImageGenerator imageGenerator)
      throws ImageGeneratorException, InterruptedException, IOException {

    int numShadersRun = 0;

    IShaderSetExperiment experiment = new LocalShaderSetExperiement(workDir.toString(), shaderSet);

    if (experiment.getReferenceImage() == null && experiment.getReferenceTextFile() == null) {
      runShader(shaderSet.getReference(), null, workDir, imageGenerator, Optional.empty());
      ++numShadersRun;
    }

    if (experiment.getReferenceImage() == null) {
      LOGGER.info("Recipient failed to render, so skipping variants.");
      return numShadersRun;
    }

    for (File variant : shaderSet.getVariants()) {

      boolean foundImageOrTextFile =
          experiment
              .getVariantImages()
              .anyMatch(f ->
                  FilenameUtils.removeExtension(f.getName())
                      .equals(FilenameUtils.removeExtension(variant.getName())))
              ||
              experiment
                  .getVariantTextFiles()
                  .anyMatch(f ->
                      FilenameUtils.removeExtension(f.getName())
                          .equals(FilenameUtils.removeExtension(variant.getName())));

      if (foundImageOrTextFile) {
        LOGGER.info("Skipping {} because we already have a result.", variant);
      } else {
        try {
          runShader(variant, shaderSet.getReference(), workDir, imageGenerator,
              Optional.of(new ImageData(experiment.getReferenceImage())));
        } catch (Exception err) {
          LOGGER.error("runShader() raise exception on {}", variant);
          err.printStackTrace();
        }
        ++numShadersRun;
      }
    }
    return numShadersRun;
  }

  private static ImageJobResult runShader(File shader, File referenceShader, File workDir,
      IImageGenerator imageGenerator, Optional<ImageData> referenceImage)
      throws ImageGeneratorException, InterruptedException, IOException {

    String rawname = FilenameUtils.removeExtension(shader.getName());
    File outputImage = new File(workDir, rawname + ".png");
    File outputText = new File(workDir, rawname + ".txt");

    LOGGER.info("Shader set experiment: {} ", shader);
    ImageJobResult res = imageGenerator.getImage(shader, referenceShader, outputImage, false);

    if (res.isSetErrorMessage()) {
      FileUtils.writeStringToFile(outputText, res.getErrorMessage(), Charset.defaultCharset());
    }
    if (res.isSetImageContents()) {
      FileUtils.writeByteArrayToFile(outputImage, res.getImageContents());
    }

    if (res.getStatus() == ImageJobStatus.NONDET) {
      // we can dump both images
      File outputNondet1 = new File(workDir,
          FilenameUtils.removeExtension(shader.getName()) + "_nondet1.png");
      File outputNondet2 = new File(workDir,
          FilenameUtils.removeExtension(shader.getName()) + "_nondet2.png");
      FileUtils.writeByteArrayToFile(outputNondet1, res.getImageFile().getContents());
      FileUtils.writeByteArrayToFile(outputNondet2, res.getImageFile2().getContents());

      // Create gif
      try {
        BufferedImage nondetImg = ImageIO.read(
            Thread.currentThread().getContextClassLoader().getResourceAsStream("nondet.png"));
        BufferedImage img1 = ImageIO.read(
            new ByteArrayInputStream(res.getImageFile().getContents()));
        BufferedImage img2 = ImageIO.read(
            new ByteArrayInputStream(res.getImageFile2().getContents()));
        File gifFile = new File(workDir,
            FilenameUtils.removeExtension(shader.getName()) + ".gif");
        ImageOutputStream gifOutput = new FileImageOutputStream(gifFile);
        GifSequenceWriter gifWriter = new GifSequenceWriter(gifOutput, img1.getType(), 500, true);
        gifWriter.writeToSequence(nondetImg);
        gifWriter.writeToSequence(img1);
        gifWriter.writeToSequence(img2);
        gifWriter.close();
        gifOutput.close();
      } catch (Exception err) {
        LOGGER.error("Error while creating GIF for nondet");
        err.printStackTrace();
      }
    }

    // Dump job info in JSON
    File outputJson = new File(workDir,
        FilenameUtils.removeExtension(shader.getName()) + ".info.json");
    JsonObject infoJson = makeInfoJson(res, outputImage, referenceImage);
    FileUtils.writeStringToFile(outputJson, jsonToString(infoJson), Charset.defaultCharset());

    return res;
  }

  private static String jsonToString(JsonObject infoJson) {
    return new GsonBuilder().setPrettyPrinting().create().toJson(infoJson);
  }

  private static JsonObject makeInfoJson(ImageJobResult res, File outputImage,
      Optional<ImageData> referenceImage) {
    JsonObject infoJson = new JsonObject();
    if (res.isSetTimingInfo()) {
      JsonObject timingInfoJson = new JsonObject();
      timingInfoJson.addProperty("compilationTime", res.timingInfo.compilationTime);
      timingInfoJson.addProperty("linkingTime", res.timingInfo.linkingTime);
      timingInfoJson.addProperty("firstRenderTime", res.timingInfo.firstRenderTime);
      timingInfoJson.addProperty("otherRendersTime", res.timingInfo.otherRendersTime);
      timingInfoJson.addProperty("captureTime", res.timingInfo.captureTime);
      infoJson.add("timingInfo", timingInfoJson);
    }
    if (res.isSetImageContents() && referenceImage.isPresent()) {
      try {
        // Add image data, e.g. histogram distance
        final Map<String, Double> imageStats = referenceImage.get().getImageDiffStats(
            new ImageData(outputImage.getAbsolutePath()));
        final JsonObject metrics = new JsonObject();
        for (String key : imageStats.keySet()) {
          metrics.addProperty(key, imageStats.get(key));
        }
        infoJson.add("metrics", metrics);
      } catch (FileNotFoundException err) {
        err.printStackTrace();
      }
    }
    if (res.isSetStage()) {
      infoJson.addProperty("stage", res.stage.toString());
    }
    if (res.isSetStatus()) {
      infoJson.addProperty("Status", res.getStatus().toString());
    }
    infoJson.addProperty("passSanityCheck", "" + res.passSanityCheck);
    return infoJson;
  }

}
