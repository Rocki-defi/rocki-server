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
import com.rocki.nft.domain.NftAlbum;
import com.rocki.nft.service.NftAlbumService;
import com.rocki.common.utils.poi.ExcelUtil;
import com.rocki.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/nft/nftAlbum" )
public class NftAlbumController extends BaseController {
    @Autowired
    private NftAlbumService nftAlbumService;

    @PreAuthorize("@ss.hasPermi('nft:nftAlbum:list')" )
    @GetMapping("/list" )
    public TableDataInfo list(NftAlbum nftAlbum) {
        Page<NftAlbum> page = getPage();
        page = nftAlbumService.page(page, new QueryWrapper<>(nftAlbum));
        return getDataTable(page.getRecords(), page.getTotal());
    }

    @PreAuthorize("@ss.hasPermi('nft:nftAlbum:export')" )
    @Log(title = "nft album" , businessType = BusinessType.EXPORT)
    @GetMapping("/export" )
    public AjaxResult export(NftAlbum nftAlbum) {
        List<NftAlbum> list = nftAlbumService.list(new QueryWrapper<>(nftAlbum));
        ExcelUtil<NftAlbum> util = new ExcelUtil<NftAlbum>(NftAlbum. class);
        return util.exportExcel(list, "nftAlbum" );
    }

    @PreAuthorize("@ss.hasPermi('nft:nftAlbum:query')" )
    @GetMapping(value = "/{id}" )
    public AjaxResult getInfo(@PathVariable("id" ) Long id) {

        return AjaxResult.success(nftAlbumService.getById(id));
    }

    @PreAuthorize("@ss.hasPermi('nft:nftAlbum:add')" )
    @Log(title = "nft album" , businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody NftAlbum nftAlbum) {

        return toAjax(nftAlbumService.save(nftAlbum));
    }


    @PreAuthorize("@ss.hasPermi('nft:nftAlbum:edit')" )
    @Log(title = "nft album" , businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody NftAlbum nftAlbum) {

        return toAjax(nftAlbumService.updateById(nftAlbum));
    }

    @PreAuthorize("@ss.hasPermi('nft:nftAlbum:remove')" )
    @Log(title = "nft album" , businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}" )
    public AjaxResult remove(@PathVariable Long[] ids) {

        return toAjax(nftAlbumService.deleteNftAlbumByIds(ids));
    }
}
