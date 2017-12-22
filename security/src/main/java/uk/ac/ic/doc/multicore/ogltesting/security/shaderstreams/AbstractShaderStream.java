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

package uk.ac.ic.doc.multicore.ogltesting.security.shaderstreams;

import static uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.ImageJobStatus.COMPILE_ERROR;
import static uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.ImageJobStatus.CRASH;
import static uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.ImageJobStatus.LINK_ERROR;
import static uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.ImageJobStatus.SANITY_ERROR;
import static uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.ImageJobStatus.SUCCESS;
import static uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.ImageJobStatus.UNEXPECTED_ERROR;
import static uk.ac.ic.doc.multicore.ogltesting.security.tool.FileGetter.getUniqueFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.ImageJobResult;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.ImageJobStatus;
import uk.ac.ic.doc.multicore.oglfuzzer.shadersets.IImageGenerator;
import uk.ac.ic.doc.multicore.oglfuzzer.shadersets.ImageGeneratorException;

//Abstract implementation of shader stream object, provides basic default interactions with shader
//streams (e.g. setting shader location, running shaders)
//No iterator methods given
public abstract class AbstractShaderStream implements IShaderStream {

  private File shaderLoc;                           //Dir containing shaders/subDirs
  private FileWriter log;                           //Writer for log files
  List<File> shaders = new ArrayList<>();      //List of shader files from shaderLoc
  int curI = 0;                                     //Current location in list

  public AbstractShaderStream(File shaderLoc) {
    this.shaderLoc = shaderLoc;
    addShaders(shaderLoc);
  }

  public AbstractShaderStream(String shaderDir) {
    this(Paths.get(shaderDir).toFile());
  }

  //Loads fragment shaders from path/file supplied to constructor (recursively)
  private void addShaders(File shaderLoc) {
    if (!shaderLoc.exists()) {
      System.err.println("Shader directory not valid: " + shaderLoc.getPath());
      return;
    }

    if (shaderLoc.isFile() && shaderLoc.getPath().endsWith(".frag")) {
      shaders.add(shaderLoc);
      return;
    } else if (shaderLoc.isDirectory()) {

      File[] filesInDir = shaderLoc
          .listFiles();

      if (filesInDir != null) {
        for (File file : filesInDir) {
          addShaders(file);
        }
      }
    }

    Collections.sort(shaders);
  }

  //Creates/opens the log.txt file in current workDir for logging
  private void initLog(File workDir) throws IOException {
    workDir.mkdirs();
    File logFile = new File(workDir, "log.txt");
    if (!logFile.exists()) {
      logFile.createNewFile();
    }
    log = new FileWriter(new File(workDir, "log.txt"), true);
  }

  //Runs the current shader in the stream using imageGenerator, saves results in workDir, returns
  //output image, log messages are added to the log StringBuilder
  //Does not call next()
  private File executeCurrentShader(IImageGenerator imageGenerator, File workDir)
      throws ImageGeneratorException, InterruptedException, IOException {
    File shader = shaders.get(curI);

    //Get path of output (include subDirs of shaderLoc)
    String outPath;
    if (shaderLoc.isDirectory()) {
      Path path = shaderLoc.toPath().relativize(shader.toPath());
      System.out.println(shader.toPath());
      System.out.println(workDir);
      System.out.println(shaderLoc.getPath());
      System.out.println(path);
      System.out.println(new File(workDir, path.toString()).getParentFile());
      new File(workDir, path.toString()).getParentFile().mkdirs();
      outPath = FilenameUtils.removeExtension(path.toString());
    } else {
      outPath = FilenameUtils.removeExtension(shader.getName());
    }
    System.out.println(outPath);
    File outputImage = getUniqueFile(workDir, outPath, ".png");

    ImageJobResult res
        = imageGenerator.getImage(shader, null, outputImage, false);

    if (res.getStatus() == SUCCESS) {
      if (res.isSetImageContents()) {
        FileUtils.writeByteArrayToFile(outputImage, res.getImageContents());
      }
    } else if (isError(res.getStatus())) {
      File err = getUniqueFile(workDir, outPath, ".txt");
      FileWriter errWriter = new FileWriter(err);
      errWriter.write(res.getErrorMessage());
      errWriter.close();
    }

    log.append(FilenameUtils.removeExtension(shader.getName())).append(" result: ")
        .append(res.toString()).append("\n");
    return outputImage;
  }

  private boolean isError(ImageJobStatus status) {
    return Arrays.asList(CRASH, COMPILE_ERROR, LINK_ERROR, SANITY_ERROR, UNEXPECTED_ERROR)
          .contains(status);
  }

  //IShaderStream Methods

  //Runs the current shader and iterates, writes to log file, returns image generated
  public File runShaderAndIterate(IImageGenerator imageGenerator, File workDir)
      throws InterruptedException, IOException, ImageGeneratorException {
    initLog(workDir);
    File image = executeCurrentShader(imageGenerator, workDir);
    next();
    log.close();
    return image;
  }

  //Runs the current shader without iterating
  public File runShader(IImageGenerator imageGenerator, File workDir)
      throws InterruptedException, IOException, ImageGeneratorException {
    initLog(workDir);
    File image = executeCurrentShader(imageGenerator, workDir);
    log.close();
    return image;
  }

  //Runs all shaders in the stream using imageGenerator, saves into workDir, writes a log.txt file
  //containing results of each shader
  public void runStream(IImageGenerator imageGenerator, File workDir)
      throws ImageGeneratorException, InterruptedException, IOException {
    initLog(workDir);
    while (hasNext()) {
      executeCurrentShader(imageGenerator, workDir);
      next();
    }
    log.close();
  }

  //Same as runStream but given a limit, runs until limit is hit or !hasNext()
  public void runStreamUntil(IImageGenerator imageGenerator, File workDir, int maxShadersRun)
      throws ImageGeneratorException, InterruptedException, IOException {
    int count = 0;
    initLog(workDir);
    while (count < maxShadersRun && hasNext()) {
      executeCurrentShader(imageGenerator, workDir);
      next();
      count++;
    }
    log.close();
  }

}
