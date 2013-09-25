package org.springframework.security.samples.mvc;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class UploadController {

    @RequestMapping("/upload")
    public String uploadForm() {
        return "uploadForm";
    }

    @ResponseBody
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public String handleFormUpload(@RequestParam("name") String name,
            @RequestParam("file") MultipartFile file) throws IOException {

        if (!file.isEmpty()) {
            return IOUtils.toString(file.getInputStream(), "UTF-8");
        } else {
            return "redirect:uploadFailure";
        }
    }
}
