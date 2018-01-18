package uk.ac.ic.doc.multicore.ogltesting.common.tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.TranslationUnit;
import uk.ac.ic.doc.multicore.ogltesting.common.util.ParseHelper;


public class RemoveX {
  private static Namespace parse(String[] args) {
    ArgumentParser parser = ArgumentParsers.newArgumentParser("RemoveX")
        .defaultHelp(true)
        .description("Remove declarations which contains a variable named X.");

    // Required arguments
    parser.addArgument("shader")
        .help("Path of shader.")
        .type(File.class);

    parser.addArgument("output")
        .help("Target file name.")
        .type(String.class);

    // Optional arguments
    parser.addArgument("--glsl_version")
        .help("Version of GLSL to target.")
        .type(String.class);

    try {
      return parser.parseArgs(args);
    } catch (ArgumentParserException exception) {
      exception.getParser().handleError(exception);
      System.exit(1);
      return null;
    }

  }

  public static void main(String[] args) {

    Namespace ns = parse(args);

    try {

      long startTime = System.currentTimeMillis();
      TranslationUnit tu = ParseHelper.parse(
          new File(ns.getString("shader")), false);
      long endTime = System.currentTimeMillis();

      prettyPrintShader(ns, tu);

      System.err.println("Time for parsing: " + (endTime - startTime));
    } catch (Throwable exception) {
      exception.printStackTrace();
      System.exit(1);
    }

  }

  private static void prettyPrintShader(Namespace ns, TranslationUnit tu)
      throws FileNotFoundException {
    PrintStream stream = new PrintStream(new FileOutputStream(
        new File(ns.getString("output"))));
    if (getGlslVersion(ns) != null) {
      throw new RuntimeException();
    }
    TranslationUnit ctu = tu.clone();
    RemoveXVisitor rmXv = new RemoveXVisitor();
    rmXv.visit(ctu);

    PrettyPrinterVisitor ppv = new PrettyPrinterVisitor(stream);
    ppv.visit(ctu);
    stream.close();
  }

  private static String getGlslVersion(Namespace ns) {
    return ns.getString("glsl_version");
  }
}
