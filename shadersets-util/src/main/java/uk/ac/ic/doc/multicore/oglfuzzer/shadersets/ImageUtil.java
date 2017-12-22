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
import java.io.FileNotFoundException;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacpp.opencv_imgproc;

public class ImageUtil {

  public static opencv_core.Mat getHistogram(String file) throws FileNotFoundException {
    if (!new File(file).isFile()) {
      throw new FileNotFoundException();
    }

    System.gc();

    opencv_core.Mat mat = opencv_imgcodecs.imread(file);
    opencv_imgproc.cvtColor(mat, mat, opencv_imgproc.COLOR_BGR2HSV);
    opencv_core.Mat hist = new opencv_core.Mat();
    opencv_imgproc.calcHist(
        new opencv_core.MatVector(new opencv_core.Mat[]{mat}),
        new int[]{0, 1},
        new opencv_core.Mat(),
        hist,
        new int[]{50, 60},
        new float[]{0, 256, 0, 256}
    );
    return hist;
  }

  public static double compareHistograms(opencv_core.Mat mat1, opencv_core.Mat mat2) {
    return opencv_imgproc.compareHist(mat1, mat2, opencv_imgproc.HISTCMP_CHISQR);
  }

  public static void main(String[] args) throws FileNotFoundException {
    System.out.println(compareHistograms(getHistogram(args[0]), getHistogram(args[1])));
  }
}
