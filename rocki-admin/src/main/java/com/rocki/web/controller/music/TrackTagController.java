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
import com.rocki.music.domain.TrackTag;
import com.rocki.music.service.TrackTagService;
import com.rocki.common.utils.poi.ExcelUtil;
import com.rocki.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/music/trackTag" )
public class TrackTagController extends BaseController {
    @Autowired
    private TrackTagService trackTagService;

    @PreAuthorize("@ss.hasPermi('music:trackTag:list')" )
    @GetMapping("/list" )
    public TableDataInfo list(TrackTag trackTag) {
        Page<TrackTag> page = getPage();
        page = trackTagService.page(page, new QueryWrapper<>(trackTag));
        return getDataTable(page.getRecords(), page.getTotal());
    }

    @PreAuthorize("@ss.hasPermi('music:trackTag:export')" )
    @GetMapping("/export" )
    public AjaxResult export(TrackTag trackTag) {
        List<TrackTag> list = trackTagService.list(new QueryWrapper<>(trackTag));
        ExcelUtil<TrackTag> util = new ExcelUtil<TrackTag>(TrackTag. class);
        return util.exportExcel(list, "trackTag" );
    }

    @PreAuthorize("@ss.hasPermi('music:trackTag:query')" )
    @GetMapping(value = "/{trackId}" )
    public AjaxResult getInfo(@PathVariable("trackId" ) Long trackId) {

        return AjaxResult.success(trackTagService.getById(trackId));
    }

    @PreAuthorize("@ss.hasPermi('music:trackTag:add')" )
    @PostMapping
    public AjaxResult add(@RequestBody TrackTag trackTag) {

        return toAjax(trackTagService.save(trackTag));
    }

    @PreAuthorize("@ss.hasPermi('music:trackTag:edit')" )
    @PutMapping
    public AjaxResult edit(@RequestBody TrackTag trackTag) {

        return toAjax(trackTagService.updateById(trackTag));
    }

    @PreAuthorize("@ss.hasPermi('music:trackTag:remove')" )
    @DeleteMapping("/{trackIds}" )
    public AjaxResult remove(@PathVariable Long[] trackIds) {

        return toAjax(trackTagService.deleteTrackTagByIds(trackIds));
    }
}
