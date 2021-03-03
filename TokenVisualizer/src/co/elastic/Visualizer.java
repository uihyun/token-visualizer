package co.elastic;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer.Mode;
import org.apache.lucene.analysis.ko.GraphvizFormatter;
import org.apache.lucene.analysis.ko.KoreanTokenizer;
import org.apache.lucene.analysis.ko.KoreanTokenizer.DecompoundMode;
import org.apache.lucene.analysis.ko.dict.ConnectionCosts;
import org.apache.lucene.analysis.ko.dict.UserDictionary;

public class Visualizer {

  private String exec;
  private String fileName = "graphviz_";
  private String outputPath;
  private String text;
  private String lang;
  private String userDictPath;

  private DecompoundMode modeKo = KoreanTokenizer.DEFAULT_DECOMPOUND;
  private Mode modeJa = JapaneseTokenizer.DEFAULT_MODE;

  public Visualizer(String osname, String outputPath, String modeStr, String text, String lang,
      String userDictPath) {

    if (osname.contains("Windows")) {
      this.exec = "C:/Program Files (x86)/Graphviz2.46.1/bin/dot.exe";
    } else if (osname.contains("Linux")) {
      this.exec = "/usr/bin/dot";
    } else if (osname.contains("Mac")) {
      this.exec = "/usr/local/bin/dot";
    } else {
      this.exec = "dot";
    }

    if (modeStr.equalsIgnoreCase("none")) {
      /** No decomposition for compound. */
      this.modeKo = DecompoundMode.NONE;
    } else if (modeStr.equalsIgnoreCase("discard")) {
      /** Decompose compounds and discards the original form (default). */
      this.modeKo = DecompoundMode.DISCARD;
    } else if (modeStr.equalsIgnoreCase("mixed")) {
      /** Decompose compounds and keeps the original form. */
      this.modeKo = DecompoundMode.MIXED;
    } else if (modeStr.equalsIgnoreCase("normal")) {
      /** Ordinary segmentation: no decomposition for compounds, */
      this.modeJa = Mode.NORMAL;
    } else if (modeStr.equalsIgnoreCase("search")) {
      /**
       * Segmentation geared towards search: this includes a decompounding process for long nouns,
       * also including the full compound token as a synonym.
       */
      this.modeJa = Mode.SEARCH;
    } else if (modeStr.equalsIgnoreCase("extended")) {
      /**
       * Extended mode outputs unigrams for unknown words.
       *
       * @lucene.experimental
       */
      this.modeJa = Mode.EXTENDED;
    }

    this.outputPath = outputPath;
    this.text = text;
    this.lang = lang;
    this.userDictPath = userDictPath;
  }

  public void vizualize() {

    Tokenizer tokenizer = null;
    FileReader fileReader = null;

    if (lang.equalsIgnoreCase("ja")) {
      visualizeJapanese(tokenizer, fileReader);
    } else {
      visualizeKorean(tokenizer, fileReader);
    }

  }

  private void visualizeKorean(Tokenizer tokenizer, FileReader fileReader) {
    UserDictionary userDict = null;
    GraphvizFormatter graphvizFormatter = new GraphvizFormatter(ConnectionCosts.getInstance());

    if (!userDictPath.equals("")) {
      try {
        fileReader = new FileReader(userDictPath);
        userDict = UserDictionary.open(fileReader);
      } catch (IOException e) {
        System.out.println(e.getMessage());
      }
    }

    tokenizer = new KoreanTokenizer(TokenStream.DEFAULT_TOKEN_ATTRIBUTE_FACTORY, userDict, modeKo,
                                    false, true);
    tokenizer.setReader(new StringReader(text));
    ((KoreanTokenizer) tokenizer).setGraphvizFormatter(graphvizFormatter);

    try {
      tokenizer.reset();

      while (tokenizer.incrementToken()) {
      }

      tokenizer.end();
      tokenizer.close();

      System.out.println("------- tokenizing...");
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }

    String dotOut = graphvizFormatter.finish();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(dotOut
        .getBytes(StandardCharsets.UTF_8));

    File tempFile = convertInputStreamToFile(inputStream);

    convertDotToJpg(tempFile);
  }

  private void visualizeJapanese(Tokenizer tokenizer, FileReader fileReader) {
    org.apache.lucene.analysis.ja.dict.UserDictionary userDict = null;
    org.apache.lucene.analysis.ja.GraphvizFormatter graphvizFormatter = new org.apache.lucene.analysis.ja.GraphvizFormatter(org.apache.lucene.analysis.ja.dict.ConnectionCosts
        .getInstance());

    if (!userDictPath.equals("")) {
      try {
        fileReader = new FileReader(userDictPath);
        userDict = org.apache.lucene.analysis.ja.dict.UserDictionary.open(fileReader);
      } catch (IOException e) {
        System.out.println(e.getMessage());
      }
    }

    tokenizer = new JapaneseTokenizer(TokenStream.DEFAULT_TOKEN_ATTRIBUTE_FACTORY, userDict, true,
                                      modeJa);
    tokenizer.setReader(new StringReader(text));
    ((JapaneseTokenizer) tokenizer).setGraphvizFormatter(graphvizFormatter);

    try {
      tokenizer.reset();

      while (tokenizer.incrementToken()) {
      }

      tokenizer.end();
      tokenizer.close();

      System.out.println("------- tokenizing...");
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }

    String dotOut = graphvizFormatter.finish();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(dotOut
        .getBytes(StandardCharsets.UTF_8));

    File tempFile = convertInputStreamToFile(inputStream);

    convertDotToJpg(tempFile);
  }

  private File convertInputStreamToFile(InputStream inputStream) {
    System.out.println("------- generate a graphviz dot file");

    File tempFile = null;
    try {
      tempFile = File.createTempFile(fileName, ".dot", new File(outputPath));
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }

    copyInputStreamToFile(inputStream, tempFile);

    return tempFile;
  }

  private void copyInputStreamToFile(InputStream inputStream, File file) {

    try {
      try (FileOutputStream outputStream = new FileOutputStream(file)) {
        int read;
        byte[] bytes = new byte[1024];

        while ((read = inputStream.read(bytes)) != -1) {
          outputStream.write(bytes, 0, read);
        }
      }
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }

  private void convertDotToJpg(File tempFile) {
    System.out.println("------- convert the graphviz dot file to an image file");

    String outputAbsPath = tempFile.getAbsolutePath();

    try {
      runCommand(outputAbsPath, exec);
    } catch (IOException e) {
      if (e.getMessage().contains("such file")) {
        try {
          runCommand(outputAbsPath, "dot");
        } catch (IOException e1) {
          System.out.println(e1.getMessage());
        }
      } else {
        System.out.println(e.getMessage());
      }
    }
  }

  private void runCommand(String outputAbsPath, String exec) throws IOException {
    String command = exec + " -Tjpg " + outputAbsPath + " -o " +
                     outputAbsPath.substring(0, outputAbsPath.lastIndexOf(".dot")) + ".jpg";
    Runtime runtime = Runtime.getRuntime();

    runtime.exec(command);
  }

}
