package com.rocki.web.controller.music;

import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.rocki.common.annotation.Log;
import com.rocki.common.core.controller.BaseController;
import com.rocki.common.core.domain.AjaxResult;
import com.rocki.common.enums.BusinessType;
import com.rocki.music.domain.PlaylistTag;
import com.rocki.music.service.PlaylistTagService;
import com.rocki.common.utils.poi.ExcelUtil;
import com.rocki.common.core.page.TableDataInfo;


@RestController
@RequestMapping("/music/playlistTag" )
public class PlaylistTagController extends BaseController {
    @Autowired
    private PlaylistTagService playlistTagService;

    @PreAuthorize("@ss.hasPermi('music:playlistTag:list')" )
    @GetMapping("/list" )
    public TableDataInfo list(PlaylistTag playlistTag) {
        Page<PlaylistTag> page = getPage();
        page = playlistTagService.page(page, new QueryWrapper<>(playlistTag));
        return getDataTable(page.getRecords(), page.getTotal());
    }


    @PreAuthorize("@ss.hasPermi('music:playlistTag:export')" )
    @GetMapping("/export" )
    public AjaxResult export(PlaylistTag playlistTag) {
        List<PlaylistTag> list = playlistTagService.list(new QueryWrapper<>(playlistTag));
        ExcelUtil<PlaylistTag> util = new ExcelUtil<PlaylistTag>(PlaylistTag. class);
        return util.exportExcel(list, "playlistTag" );
    }


    @PreAuthorize("@ss.hasPermi('music:playlistTag:query')" )
    @GetMapping(value = "/{playlistId}" )
    public AjaxResult getInfo(@PathVariable("playlistId" ) Long playlistId) {

        return AjaxResult.success(playlistTagService.getById(playlistId));
    }


    @PreAuthorize("@ss.hasPermi('music:playlistTag:add')" )
    @PostMapping
    public AjaxResult add(@RequestBody PlaylistTag playlistTag) {

        return toAjax(playlistTagService.save(playlistTag));
    }

    @PreAuthorize("@ss.hasPermi('music:playlistTag:edit')" )
    @PutMapping
    public AjaxResult edit(@RequestBody PlaylistTag playlistTag) {

        return toAjax(playlistTagService.updateById(playlistTag));
    }

    @PreAuthorize("@ss.hasPermi('music:playlistTag:remove')" )
    @DeleteMapping("/{playlistIds}" )
    public AjaxResult remove(@PathVariable Long[] playlistIds) {

        return toAjax(playlistTagService.deletePlaylistTagByIds(playlistIds));
    }
}
