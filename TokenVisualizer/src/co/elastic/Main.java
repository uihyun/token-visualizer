package co.elastic;

import java.io.File;

public class Main {

  private final static String osName = System.getProperty("os.name");

  public static void main(String[] args) {

    String option;
    String outputPath = System.getProperty("user.dir");
    String modeStr = "";
    String text = "";
    String lang = "";
    String userDictPath = "";

    if (args.length < 1) {
      help();
    } else {
      for (int i = 0; i < args.length; i++) {
        option = args[i];

        if (option.equalsIgnoreCase("-o")) {
          i++;
          outputPath = args[i];
          if (!outputPath.endsWith(File.separator))
            outputPath += File.separator;
        } else if (option.equalsIgnoreCase("-h") || option.equalsIgnoreCase("--help")) {
          i++;
          help();
        } else if (option.equalsIgnoreCase("-m")) {
          i++;
          modeStr = args[i];
          if (!modeStr.equalsIgnoreCase("none") && !modeStr.equalsIgnoreCase("discard") &&
              !modeStr.equalsIgnoreCase("mixed") && !modeStr.equalsIgnoreCase("normal") &&
              !modeStr.equalsIgnoreCase("search") && !modeStr.equalsIgnoreCase("extended")) {
            System.out.println("The mode \"" + modeStr + "\" doesn't exist.");
            System.exit(0);
          }
        } else if (option.equalsIgnoreCase("-t")) {
          i++;
          text = args[i];
        } else if (option.equalsIgnoreCase("-l")) {
          i++;
          lang = args[i];
          if (!lang.equalsIgnoreCase("ko") && !lang.equalsIgnoreCase("ja")) {
            System.out
                .println("The language \"" + lang + " doesn't match one of supported language.");
            System.exit(0);
          }
        } else if (option.equalsIgnoreCase("-d")) {
          i++;
          userDictPath = args[i];
        } else {
          System.out.println("Illegal option: " + args[i]);
        }
      }
    }

    if (text.length() == 0) {
      System.out.println("Text should be set by using -t option.");
      System.exit(0);
    }

    Visualizer visualizer = new Visualizer(osName, outputPath, modeStr, text, lang, userDictPath);
    visualizer.vizualize();
  }

  private static void help() {
    System.out.println("Token Visualizer for Korean(nori) and Japanese(kuromoji)");
    System.out.println("Installing Graphviz is needed: https://graphviz.org/download/");
    System.out.println("Usage: java -jar TokenVisualizer.jar [Options]");
    System.out.println("Options:");
    System.out.println("    -h  help");
    System.out.println("    -o  output path (default: current directory)");
    System.out
        .println("    -m  decompound mode: discard|none|mixed for Korean (default: discard), search|normal|extended for Japanese (default: search)");
    System.out.println("    -l  language: ko for Korean, ja for Japapnese (default: ko)");
    System.out.println("    -t  \"text\" you want to analyze");
    System.out.println("    -d  user dictionary path");
    System.out.println();
    System.out.println("Example 1: java -jar TokenVisualizer.jar -t \"뿌리가 깊은 나무\"");
    System.out
        .println("Example 2: java -jar TokenVisualizer.jar -o /Users/elastic/Desktop/ -m mixed -t \"뿌리가 깊은 나무\"");
    System.out
        .println("Example 3: java -jar TokenVisualizer.jar -o /Users/elastic/Desktop/ -t \"세종시는 행정 수도\" -d /Users/elastic/Desktop/userdict.txt");
    System.out.println("Example 4: java -jar TokenVisualizer.jar -l ja -t \"シンプルさは究極の洗練である\"");
    System.out
        .println("Example 5: java -jar TokenVisualizer.jar -o /Users/elastic/Desktop/ -l ja -t \"シンプルさは究極の洗練である\" -d /Users/elastic/Desktop/userdict.txt");
    System.exit(0);
  }

}
