package co.elastic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TokenVisualizerApplication {

    public static void main(String[] args) {
        // Check if this is command line mode
        if (args.length > 0 && !isWebMode(args)) {
            // Run in CLI mode using the existing Main class
            Main.main(args);
        } else {
            // Run in web mode
            SpringApplication.run(TokenVisualizerApplication.class, args);
            System.out.println("Token Visualizer Web UI started!");
            System.out.println("Open your browser and go to: http://localhost:8088");
        }
    }

    private static boolean isWebMode(String[] args) {
        // Check if --web flag is present, or if no recognized CLI args
        for (String arg : args) {
            if ("--web".equals(arg)) {
                return true;
            }
        }
        // If no CLI options detected, assume web mode
        return !hasCliOptions(args);
    }

    private static boolean hasCliOptions(String[] args) {
        for (String arg : args) {
            if (arg.equals("-t") || arg.equals("-l") || arg.equals("-m") || 
                arg.equals("-o") || arg.equals("-d") || arg.equals("-h") || 
                arg.equals("--help")) {
                return true;
            }
        }
        return false;
    }
}