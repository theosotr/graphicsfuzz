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
import uk.ac.ic.doc.multicore.oglfuzzer.common.util.ExecHelper.RedirectType;
import uk.ac.ic.doc.multicore.oglfuzzer.common.util.ExecResult;
import uk.ac.ic.doc.multicore.oglfuzzer.common.util.ToolHelper;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.FuzzerServiceConstants;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.ImageJobResult;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.ImageJobStatus;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.ResultConstant;

public class LocalImageGenerator implements IImageGenerator {

  private final boolean usingSwiftshader;

  public LocalImageGenerator(boolean usingSwiftshader) {
    this.usingSwiftshader = usingSwiftshader;
  }

  @Override
  public ImageJobResult getImage(
      File shaderFile,
      File referenceFile,
      File tempImageFile,
      boolean skipRender) throws ImageGeneratorException, InterruptedException {
    try {
      ExecResult res = usingSwiftshader
          ? ToolHelper.runSwiftshaderOnShader(RedirectType.TO_BUFFER, shaderFile,
              tempImageFile, skipRender)
          : ToolHelper.runGenerateImageOnShader(RedirectType.TO_BUFFER, shaderFile,
          tempImageFile, skipRender);

      ImageJobResult imageJobResult = new ImageJobResult();

      if (res.res == 0) {
        imageJobResult
            .setStatus(ImageJobStatus.SUCCESS);

        return imageJobResult;
      }

      ResultConstant resultConstant = ResultConstant.ERROR;
      ImageJobStatus status = ImageJobStatus.UNEXPECTED_ERROR;

      if (res.res == FuzzerServiceConstants.COMPILE_ERROR_EXIT_CODE) {
        resultConstant = ResultConstant.COMPILE_ERROR;
        status = ImageJobStatus.COMPILE_ERROR;
      } else if (res.res == FuzzerServiceConstants.LINK_ERROR_EXIT_CODE) {
        resultConstant = ResultConstant.LINK_ERROR;
        status = ImageJobStatus.LINK_ERROR;
      }

      res.stdout.append(res.stderr);
      imageJobResult
          .setStatus(status)
          .setErrorMessage(resultConstant + "\n" + res.stdout.toString());

      return imageJobResult;

    } catch (IOException exception) {
      throw new ImageGeneratorException(exception);
    }
  }
}
