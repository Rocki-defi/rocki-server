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
import com.rocki.nft.domain.NftEmailCollection;
import com.rocki.nft.service.NftEmailCollectionService;
import com.rocki.common.utils.poi.ExcelUtil;
import com.rocki.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/nft/nftEmailCollection" )
public class NftEmailCollectionController extends BaseController {
    @Autowired
    private NftEmailCollectionService nftEmailCollectionService;

    @PreAuthorize("@ss.hasPermi('nft:nftEmailCollection:list')" )
    @GetMapping("/list" )
    public TableDataInfo list(NftEmailCollection nftEmailCollection) {
        Page<NftEmailCollection> page = getPage();
        page = nftEmailCollectionService.page(page, new QueryWrapper<>(nftEmailCollection));
        return getDataTable(page.getRecords(), page.getTotal());
    }

    @PreAuthorize("@ss.hasPermi('nft:nftEmailCollection:export')" )
    @Log(title = "email collection" , businessType = BusinessType.EXPORT)
    @GetMapping("/export" )
    public AjaxResult export(NftEmailCollection nftEmailCollection) {
        List<NftEmailCollection> list = nftEmailCollectionService.list(new QueryWrapper<>(nftEmailCollection));
        ExcelUtil<NftEmailCollection> util = new ExcelUtil<NftEmailCollection>(NftEmailCollection. class);
        return util.exportExcel(list, "nftEmailCollection" );
    }

    @PreAuthorize("@ss.hasPermi('nft:nftEmailCollection:query')" )
    @GetMapping(value = "/{id}" )
    public AjaxResult getInfo(@PathVariable("id" ) Long id) {

        return AjaxResult.success(nftEmailCollectionService.getById(id));
    }

    @PreAuthorize("@ss.hasPermi('nft:nftEmailCollection:add')" )
    @Log(title = "email collection" , businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody NftEmailCollection nftEmailCollection) {

        return toAjax(nftEmailCollectionService.save(nftEmailCollection));
    }

    @PreAuthorize("@ss.hasPermi('nft:nftEmailCollection:edit')" )
    @Log(title = "email collection" , businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody NftEmailCollection nftEmailCollection) {

        return toAjax(nftEmailCollectionService.updateById(nftEmailCollection));
    }

    @PreAuthorize("@ss.hasPermi('nft:nftEmailCollection:remove')" )
    @Log(title = "email collection" , businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}" )
    public AjaxResult remove(@PathVariable Long[] ids) {

        return toAjax(nftEmailCollectionService.deleteNftEmailCollectionByIds(ids));
    }
}
