package com.rocki.web.controller.music;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rocki.music.domain.Playlist;
import com.rocki.music.service.PlaylistService;
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

/**
 * 用户歌单Controller
 *
 * @author GZC
 * @date 2020-11-07
 */
@RestController
@RequestMapping("/music/playlist" )
public class PlaylistController extends BaseController {
    @Autowired
    private PlaylistService playlistService;

    /**
     * 查询用户歌单列表
     */
    @PreAuthorize("@ss.hasPermi('music:playlist:list')" )
    @GetMapping("/list" )
    public TableDataInfo list(Playlist playlist) {
        Page<Playlist> page = getPage();
        page = playlistService.page(page, new QueryWrapper<>(playlist));
        return getDataTable(page.getRecords(), page.getTotal());
    }


    /**
     * 导出用户歌单列表
     */
    @PreAuthorize("@ss.hasPermi('music:playlist:export')" )
    @GetMapping("/export" )
    public AjaxResult export(Playlist playlist) {
        List<Playlist> list = playlistService.list(new QueryWrapper<>(playlist));
        ExcelUtil<Playlist> util = new ExcelUtil<Playlist>(Playlist. class);
        return util.exportExcel(list, "playlist" );
    }

    /**
     * 获取用户歌单详细信息
     */
    @PreAuthorize("@ss.hasPermi('music:playlist:query')" )
    @GetMapping(value = "/{id}" )
    public AjaxResult getInfo(@PathVariable("id" ) Long id) {

        return AjaxResult.success(playlistService.getById(id));
    }

    /**
     * 新增用户歌单
     */
    @PreAuthorize("@ss.hasPermi('music:playlist:add')" )
    @PostMapping
    public AjaxResult add(@RequestBody Playlist playlist) {

        return toAjax(playlistService.save(playlist));
    }

    /**
     * 修改用户歌单
     */
    @PreAuthorize("@ss.hasPermi('music:playlist:edit')" )
    @PutMapping
    public AjaxResult edit(@RequestBody Playlist playlist) {

        return toAjax(playlistService.updateById(playlist));
    }

    /**
     * 删除用户歌单
     */
    @PreAuthorize("@ss.hasPermi('music:playlist:remove')" )
    @DeleteMapping("/{ids}" )
    public AjaxResult remove(@PathVariable Long[] ids) {

        return toAjax(playlistService.deletePlaylistByIds(ids));
    }
}
