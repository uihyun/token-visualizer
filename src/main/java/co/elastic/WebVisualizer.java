package co.elastic;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer.Mode;
import org.apache.lucene.analysis.ko.GraphvizFormatter;
import org.apache.lucene.analysis.ko.KoreanTokenizer;
import org.apache.lucene.analysis.ko.KoreanTokenizer.DecompoundMode;
import org.apache.lucene.analysis.ko.dict.ConnectionCosts;
import org.apache.lucene.analysis.ko.dict.UserDictionary;

public class WebVisualizer {

    private String osname;
    private String exec;
    private String fileName = "graphviz_";
    private String outputPath;
    private String text;
    private String lang;
    private String userDictPath;

    private DecompoundMode modeKo = KoreanTokenizer.DEFAULT_DECOMPOUND;
    private Mode modeJa = JapaneseTokenizer.DEFAULT_MODE;

    public WebVisualizer(String osname, String outputPath, String modeStr, String text, String lang,
                         String userDictPath) {

        this.osname = osname;
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
            this.modeKo = DecompoundMode.NONE;
        } else if (modeStr.equalsIgnoreCase("discard")) {
            this.modeKo = DecompoundMode.DISCARD;
        } else if (modeStr.equalsIgnoreCase("mixed")) {
            this.modeKo = DecompoundMode.MIXED;
        } else if (modeStr.equalsIgnoreCase("normal")) {
            this.modeJa = Mode.NORMAL;
        } else if (modeStr.equalsIgnoreCase("search")) {
            this.modeJa = Mode.SEARCH;
        } else if (modeStr.equalsIgnoreCase("extended")) {
            this.modeJa = Mode.EXTENDED;
        }

        this.outputPath = outputPath;
        this.text = text;
        this.lang = lang;
        this.userDictPath = userDictPath;
    }

    public byte[] visualizeAndReturnImage() throws IOException {
        String dotContent;
        
        if (lang.equalsIgnoreCase("ja")) {
            dotContent = visualizeJapanese();
        } else {
            dotContent = visualizeKorean();
        }
        
        return convertDotToImageBytes(dotContent);
    }

    private String visualizeKorean() throws IOException {
        UserDictionary userDict = null;
        GraphvizFormatter graphvizFormatter = new GraphvizFormatter(ConnectionCosts.getInstance());

        if (!userDictPath.equals("")) {
            try (FileReader fileReader = new FileReader(userDictPath)) {
                userDict = UserDictionary.open(fileReader);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

        try (Tokenizer tokenizer = new KoreanTokenizer(TokenStream.DEFAULT_TOKEN_ATTRIBUTE_FACTORY, userDict, modeKo,
                false, true)) {
            tokenizer.setReader(new StringReader(text));
            ((KoreanTokenizer) tokenizer).setGraphvizFormatter(graphvizFormatter);

            tokenizer.reset();
            while (tokenizer.incrementToken()) {
            }
            tokenizer.end();

            System.out.println("------- tokenizing...");
        } catch (IOException e) {
            throw e;
        }

        return graphvizFormatter.finish();
    }

    private String visualizeJapanese() throws IOException {
        org.apache.lucene.analysis.ja.dict.UserDictionary userDict = null;
        org.apache.lucene.analysis.ja.GraphvizFormatter graphvizFormatter = new org.apache.lucene.analysis.ja.GraphvizFormatter(org.apache.lucene.analysis.ja.dict.ConnectionCosts
                .getInstance());

        if (!userDictPath.equals("")) {
            try (FileReader fileReader = new FileReader(userDictPath)) {
                userDict = org.apache.lucene.analysis.ja.dict.UserDictionary.open(fileReader);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

        try (Tokenizer tokenizer = new JapaneseTokenizer(TokenStream.DEFAULT_TOKEN_ATTRIBUTE_FACTORY, userDict, true,
                modeJa)) {
            tokenizer.setReader(new StringReader(text));
            ((JapaneseTokenizer) tokenizer).setGraphvizFormatter(graphvizFormatter);

            tokenizer.reset();
            while (tokenizer.incrementToken()) {
            }
            tokenizer.end();

            System.out.println("------- tokenizing...");
        } catch (IOException e) {
            throw e;
        }

        return graphvizFormatter.finish();
    }

    private byte[] convertDotToImageBytes(String dotContent) throws IOException {
        // Create temporary dot file
        File tempDotFile = File.createTempFile(fileName, ".dot", new File(outputPath));
        Files.write(tempDotFile.toPath(), dotContent.getBytes(StandardCharsets.UTF_8));

        try {
            return runCommandAndGetBytes(tempDotFile.getAbsolutePath(), exec);
        } catch (IOException e) {
            if (e.getMessage().contains("No such file")) {
                try {
                    if (!osname.equalsIgnoreCase("Windows"))
                        return runCommandAndGetBytes(tempDotFile.getAbsolutePath(), "dot");
                    else
                        return runCommandAndGetBytes(tempDotFile.getAbsolutePath(), "dot.exe");
                } catch (IOException e1) {
                    throw e1;
                }
            } else {
                throw e;
            }
        } finally {
            tempDotFile.delete();
        }
    }

    private byte[] runCommandAndGetBytes(String inputPath, String exec) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(exec, "-Tjpg", inputPath);
        Process process = processBuilder.start();

        try {
            // Read the output directly as bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            
            while ((read = process.getInputStream().read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            
            process.waitFor();
            
            if (process.exitValue() != 0) {
                throw new IOException("Graphviz command failed with exit code: " + process.exitValue());
            }
            
            return outputStream.toByteArray();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Process was interrupted", e);
        }
    }
}