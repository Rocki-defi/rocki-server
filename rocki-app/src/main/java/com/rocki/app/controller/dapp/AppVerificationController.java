package com.rocki.app.controller.dapp;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rocki.common.core.controller.BaseController;
import com.rocki.common.core.domain.AjaxResult;
import com.rocki.common.core.page.TableDataInfo;
import com.rocki.common.utils.MessageUtils;
import com.rocki.user.mapper.UserMapper;
import com.rocki.user.vo.VerifiedArtistVo;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "api/v2/stk/vrfy/")
public class AppVerificationController extends BaseController {
    Logger logger = LoggerFactory.getLogger(AppVerificationController.class);

    @Autowired
    private UserMapper userMapper;

    @GetMapping(path = "pageVerifiedArtist")
    public AjaxResult pageVerifiedAllArtists() {
        Page<VerifiedArtistVo> page = getPage();
        page = userMapper.pageVerifiedArtists(page);
        TableDataInfo tableDataInfo = getDataTable(page.getRecords(), page.getTotal());
        return AjaxResult.success(tableDataInfo);
    }

    @GetMapping(path = "pageVerifiedArtistBaseByName")
    public AjaxResult pageVerifiedArtistBaseByName(
            @RequestParam(name = "artistName") String artistName
    ) {
        if (org.apache.commons.lang3.StringUtils.isBlank(artistName)) {
            return AjaxResult.error(MessageUtils.message("not.null", "Artist Name"));
        }
        Page<VerifiedArtistVo> page = getPage();
        page = userMapper.searchVerifiedArtistBaseByName(page, artistName);
        TableDataInfo tableDataInfo = getDataTable(page.getRecords(), page.getTotal());
        return AjaxResult.success(tableDataInfo);
    }

    @GetMapping(path = "pageVerifiedArtistByName")
    public AjaxResult pageVerifiedArtist(
            @RequestParam(name = "artistName") String artistName
    ) {
        if (org.apache.commons.lang3.StringUtils.isBlank(artistName)) {
            return AjaxResult.error(MessageUtils.message("not.null", "Artist Name"));
        }
        Page<VerifiedArtistVo> page = getPage();
        page = userMapper.searchVerifiedArtistByName(page, artistName);
        TableDataInfo tableDataInfo = getDataTable(page.getRecords(), page.getTotal());
        return AjaxResult.success(tableDataInfo);
    }
}
