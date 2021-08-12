package com.rocki.app.controller.sys;

import com.rocki.common.core.domain.AjaxResult;
import com.rocki.common.utils.CDNUtils;
import com.rocki.common.utils.StringUtils;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/sys")
public class AppRockiSysController {
    @GetMapping("getOriginalStorageLink")
    @ApiOperation("Get the original storage link")
    public AjaxResult getOriginalStorageLink(
            @ApiParam(name = "cdnUrl", value = "cdn url", required = true)
            @RequestParam(name = "cdnUrl", required = true) String cdnUrl
    ) {
        if (StringUtils.contains(cdnUrl, CDNUtils.OSS_AND_AWS_TF_CDN)) {
            return AjaxResult.success(StringUtils.replace(cdnUrl, CDNUtils.OSS_AND_AWS_TF_CDN, CDNUtils.OSS_ORIGIN));
        }
        if (StringUtils.contains(cdnUrl, CDNUtils.AWS_ROCKI_CDN)){
            return AjaxResult.success(StringUtils.replace(cdnUrl, CDNUtils.AWS_ROCKI_CDN, CDNUtils.AWS_ROCKI));
        }
        if (StringUtils.contains(cdnUrl, CDNUtils.AWS_CHOON_IMAGE_CDN)) {
            return AjaxResult.success(StringUtils.replace(cdnUrl, CDNUtils.AWS_CHOON_IMAGE_CDN,
                    CDNUtils.AWS_CHOON_IMAGE));
        }
        return null;
    }
}
