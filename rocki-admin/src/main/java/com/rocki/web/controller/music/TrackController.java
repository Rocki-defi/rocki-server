package com.rocki.web.controller.music;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rocki.music.domain.Track;
import com.rocki.music.service.TrackService;
import com.rocki.common.annotation.Log;
import com.rocki.common.core.controller.BaseController;
import com.rocki.common.core.domain.AjaxResult;
import com.rocki.common.core.page.TableDataInfo;
import com.rocki.common.enums.BusinessType;
import com.rocki.common.utils.poi.ExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/music/track" )
public class TrackController extends BaseController {
    @Autowired
    private TrackService trackService;

    @PreAuthorize("@ss.hasPermi('music:track:list')" )
    @GetMapping("/list" )
    public TableDataInfo list(Track track) {
        Page<Track> page = getPage();
        page = trackService.page(page, new QueryWrapper<>(track));
        return getDataTable(page.getRecords(), page.getTotal());
    }

    @PreAuthorize("@ss.hasPermi('music:track:export')" )
    @GetMapping("/export" )
    public AjaxResult export(Track track) {
        List<Track> list = trackService.list(new QueryWrapper<>(track));
        ExcelUtil<Track> util = new ExcelUtil<Track>(Track. class);
        return util.exportExcel(list, "track" );
    }

    @PreAuthorize("@ss.hasPermi('music:track:query')" )
    @GetMapping(value = "/{id}" )
    public AjaxResult getInfo(@PathVariable("id" ) Long id) {

        return AjaxResult.success(trackService.getById(id));
    }

    @PreAuthorize("@ss.hasPermi('music:track:add')" )
    @PostMapping
    public AjaxResult add(@RequestBody Track track) {

        return toAjax(trackService.save(track));
    }


    @PreAuthorize("@ss.hasPermi('music:track:edit')" )
    @PutMapping
    public AjaxResult edit(@RequestBody Track track) {

        return toAjax(trackService.updateById(track));
    }

    @PreAuthorize("@ss.hasPermi('music:track:remove')" )
    @DeleteMapping("/{ids}" )
    public AjaxResult remove(@PathVariable Long[] ids) {

        return toAjax(trackService.deleteTrackByIds(ids));
    }
}
