package co.elastic.controller;

import co.elastic.service.TokenVisualizerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Controller
public class TokenVisualizerController {

    @Autowired
    private TokenVisualizerService visualizerService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/api/visualize")
    @ResponseBody
    public ResponseEntity<byte[]> visualize(
            @RequestParam("text") String text,
            @RequestParam(value = "language", defaultValue = "ko") String language,
            @RequestParam(value = "mode", defaultValue = "") String mode,
            @RequestParam(value = "userDict", required = false) MultipartFile userDict) {
        
        try {
            byte[] imageData = visualizerService.visualize(text, language, mode, userDict);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setContentLength(imageData.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(imageData);
                    
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/api/validate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validate(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        String language = request.get("language");
        
        Map<String, Object> response = visualizerService.validateInput(text, language);
        
        if ((Boolean) response.get("valid")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}