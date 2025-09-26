package co.elastic.service;

import co.elastic.WebVisualizer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Service
public class TokenVisualizerService {

    public byte[] visualize(String text, String language, String mode, MultipartFile userDict) throws Exception {
        String osName = System.getProperty("os.name");
        
        // Create temporary directory for processing
        Path tempDir = Files.createTempDirectory("token-visualizer");
        String outputPath = tempDir.toString() + File.separator;
        
        // Handle user dictionary
        String userDictPath = "";
        if (userDict != null && !userDict.isEmpty()) {
            File tempUserDict = new File(tempDir.toFile(), "userdict.txt");
            userDict.transferTo(tempUserDict);
            userDictPath = tempUserDict.getAbsolutePath();
        }
        
        try {
            // Create modified Visualizer that returns image data
            WebVisualizer visualizer = new WebVisualizer(osName, outputPath, mode, text, language, userDictPath);
            return visualizer.visualizeAndReturnImage();
        } finally {
            // Clean up temporary directory
            deleteDirectory(tempDir.toFile());
        }
    }

    public Map<String, Object> validateInput(String text, String language) {
        Map<String, Object> response = new HashMap<>();
        
        if (text == null || text.trim().isEmpty()) {
            response.put("valid", false);
            response.put("message", "Text should not be empty");
            return response;
        }
        
        if (language != null && !language.equals("ko") && !language.equals("ja")) {
            response.put("valid", false);
            response.put("message", "Language must be 'ko' or 'ja'");
            return response;
        }
        
        response.put("valid", true);
        response.put("message", "Input is valid");
        return response;
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}