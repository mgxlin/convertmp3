package com.mgxlin.convertmp3.api;

import com.mgxlin.convertmp3.api.ro.AudioFile;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "mgxlin-audio", fallbackFactory = AudioApiFallback.class)
public interface AudioApi {

    @GetMapping("/download-and-convert")
    String downloadAndConvertAudio(@RequestParam("url") String url);

    @PostMapping("/download-sort-and-merge")
    String downloadSortAndMergeAudios(@RequestBody List<AudioFile> audioFiles);

}
