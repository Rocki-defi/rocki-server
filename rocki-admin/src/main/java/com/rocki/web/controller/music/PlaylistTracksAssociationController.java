package com.rocki.web.controller.music;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rocki.music.domain.PlaylistTracksAssociation;
import com.rocki.music.service.PlaylistTracksAssociationService;
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
@RequestMapping("/music/playlistTracksAssociation" )
public class PlaylistTracksAssociationController extends BaseController {
    @Autowired
    private PlaylistTracksAssociationService playlistTracksAssociationService;

    @PreAuthorize("@ss.hasPermi('music:playlistTracksAssociation:list')" )
    @GetMapping("/list" )
    public TableDataInfo list(PlaylistTracksAssociation playlistTracksAssociation) {
        Page<PlaylistTracksAssociation> page = getPage();
        page = playlistTracksAssociationService.page(page, new QueryWrapper<>(playlistTracksAssociation));
        return getDataTable(page.getRecords(), page.getTotal());
    }

    @PreAuthorize("@ss.hasPermi('music:playlistTracksAssociation:export')" )
    @GetMapping("/export" )
    public AjaxResult export(PlaylistTracksAssociation playlistTracksAssociation) {
        List<PlaylistTracksAssociation> list = playlistTracksAssociationService.list(new QueryWrapper<>(playlistTracksAssociation));
        ExcelUtil<PlaylistTracksAssociation> util = new ExcelUtil<PlaylistTracksAssociation>(PlaylistTracksAssociation. class);
        return util.exportExcel(list, "playlistTracksAssociation" );
    }


    @PreAuthorize("@ss.hasPermi('music:playlistTracksAssociation:query')" )
    @GetMapping(value = "/{id}" )
    public AjaxResult getInfo(@PathVariable("id" ) Long id) {

        return AjaxResult.success(playlistTracksAssociationService.getById(id));
    }
    @PreAuthorize("@ss.hasPermi('music:playlistTracksAssociation:add')" )
    @PostMapping
    public AjaxResult add(@RequestBody PlaylistTracksAssociation playlistTracksAssociation) {

        return toAjax(playlistTracksAssociationService.save(playlistTracksAssociation));
    }

    @PreAuthorize("@ss.hasPermi('music:playlistTracksAssociation:edit')" )
    @PutMapping
    public AjaxResult edit(@RequestBody PlaylistTracksAssociation playlistTracksAssociation) {

        return toAjax(playlistTracksAssociationService.updateById(playlistTracksAssociation));
    }

    @PreAuthorize("@ss.hasPermi('music:playlistTracksAssociation:remove')" )
    @DeleteMapping("/{ids}" )
    public AjaxResult remove(@PathVariable Long[] ids) {

        return toAjax(playlistTracksAssociationService.deletePlaylistTracksAssociationByIds(ids));
    }
}
