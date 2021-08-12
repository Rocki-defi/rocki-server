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
import com.rocki.music.domain.PlaylistFollows;
import com.rocki.music.service.PlaylistFollowsService;
import com.rocki.common.utils.poi.ExcelUtil;
import com.rocki.common.core.page.TableDataInfo;


@RestController
@RequestMapping("/music/playlistFollows" )
public class PlaylistFollowsController extends BaseController {
    @Autowired
    private PlaylistFollowsService playlistFollowsService;


    @PreAuthorize("@ss.hasPermi('music:playlistFollows:list')" )
    @GetMapping("/list" )
    public TableDataInfo list(PlaylistFollows playlistFollows) {
        Page<PlaylistFollows> page = getPage();
        page = playlistFollowsService.page(page, new QueryWrapper<>(playlistFollows));
        return getDataTable(page.getRecords(), page.getTotal());
    }



    @PreAuthorize("@ss.hasPermi('music:playlistFollows:export')" )
    @GetMapping("/export" )
    public AjaxResult export(PlaylistFollows playlistFollows) {
        List<PlaylistFollows> list = playlistFollowsService.list(new QueryWrapper<>(playlistFollows));
        ExcelUtil<PlaylistFollows> util = new ExcelUtil<PlaylistFollows>(PlaylistFollows. class);
        return util.exportExcel(list, "playlistFollows" );
    }


    @PreAuthorize("@ss.hasPermi('music:playlistFollows:query')" )
    @GetMapping(value = "/{id}" )
    public AjaxResult getInfo(@PathVariable("id" ) Long id) {

        return AjaxResult.success(playlistFollowsService.getById(id));
    }


    @PreAuthorize("@ss.hasPermi('music:playlistFollows:add')" )
    @PostMapping
    public AjaxResult add(@RequestBody PlaylistFollows playlistFollows) {

        return toAjax(playlistFollowsService.save(playlistFollows));
    }

    @PreAuthorize("@ss.hasPermi('music:playlistFollows:edit')" )
    @PutMapping
    public AjaxResult edit(@RequestBody PlaylistFollows playlistFollows) {

        return toAjax(playlistFollowsService.updateById(playlistFollows));
    }


    @PreAuthorize("@ss.hasPermi('music:playlistFollows:remove')" )
    @DeleteMapping("/{ids}" )
    public AjaxResult remove(@PathVariable Long[] ids) {

        return toAjax(playlistFollowsService.deletePlaylistFollowsByIds(ids));
    }
}
