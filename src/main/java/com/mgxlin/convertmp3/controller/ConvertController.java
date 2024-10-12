package com.mgxlin.convertmp3.controller;

import com.mgxlin.convertmp3.api.ro.AudioFile;
import com.mgxlin.convertmp3.service.ConvertService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;


@RestController
public class ConvertController {

    @Resource
    private ConvertService convertService;

    @PostMapping("/download-sort-and-merge")
    public String downloadSortAndMergeAudios(@RequestBody List<AudioFile> audioFiles) {
        return convertService.downloadMergeAndConvertAudios(audioFiles);
    }


    @GetMapping("/download-and-convert")
    public String downloadAndConvertAudio(@RequestParam String url) {
        return convertService.downloadAndConvertAudio(url);
    }


    @PostMapping("/download-and-merge")
    public String downloadAndMergeAudios(@RequestBody List<String> urls) {
        return convertService.downloadAndMergeAudios(urls);
    }

}
