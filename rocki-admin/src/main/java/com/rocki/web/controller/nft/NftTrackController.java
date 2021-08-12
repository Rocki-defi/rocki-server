package com.rocki.web.controller.nft;

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
import com.rocki.nft.domain.NftTrack;
import com.rocki.nft.service.NftTrackService;
import com.rocki.common.utils.poi.ExcelUtil;
import com.rocki.common.core.page.TableDataInfo;

/**
 * nft trackController
 *
 * @author GZC
 * @date 2021-03-12
 */
@RestController
@RequestMapping("/nft/nftTrack" )
public class NftTrackController extends BaseController {
    @Autowired
    private NftTrackService nftTrackService;

    @PreAuthorize("@ss.hasPermi('nft:nftTrack:list')" )
    @GetMapping("/list" )
    public TableDataInfo list(NftTrack nftTrack) {
        Page<NftTrack> page = getPage();
        page = nftTrackService.page(page, new QueryWrapper<>(nftTrack));
        return getDataTable(page.getRecords(), page.getTotal());
    }

    @PreAuthorize("@ss.hasPermi('nft:nftTrack:export')" )
    @Log(title = "nft track" , businessType = BusinessType.EXPORT)
    @GetMapping("/export" )
    public AjaxResult export(NftTrack nftTrack) {
        List<NftTrack> list = nftTrackService.list(new QueryWrapper<>(nftTrack));
        ExcelUtil<NftTrack> util = new ExcelUtil<NftTrack>(NftTrack. class);
        return util.exportExcel(list, "nftTrack" );
    }

    @PreAuthorize("@ss.hasPermi('nft:nftTrack:query')" )
    @GetMapping(value = "/{id}" )
    public AjaxResult getInfo(@PathVariable("id" ) Long id) {

        return AjaxResult.success(nftTrackService.getById(id));
    }

    @PreAuthorize("@ss.hasPermi('nft:nftTrack:add')" )
    @Log(title = "nft track" , businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody NftTrack nftTrack) {

        return toAjax(nftTrackService.save(nftTrack));
    }

    @PreAuthorize("@ss.hasPermi('nft:nftTrack:edit')" )
    @Log(title = "nft track" , businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody NftTrack nftTrack) {

        return toAjax(nftTrackService.updateById(nftTrack));
    }

    @PreAuthorize("@ss.hasPermi('nft:nftTrack:remove')" )
    @Log(title = "nft track" , businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}" )
    public AjaxResult remove(@PathVariable Long[] ids) {

        return toAjax(nftTrackService.deleteNftTrackByIds(ids));
    }
}
