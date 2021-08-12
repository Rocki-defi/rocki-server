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
import com.rocki.nft.domain.NftTokenInfo;
import com.rocki.nft.service.NftTokenInfoService;
import com.rocki.common.utils.poi.ExcelUtil;
import com.rocki.common.core.page.TableDataInfo;


@RestController
@RequestMapping("/nft/nftTokenInfo" )
public class NftTokenInfoController extends BaseController {
    @Autowired
    private NftTokenInfoService nftTokenInfoService;
    @PreAuthorize("@ss.hasPermi('nft:nftTokenInfo:list')" )
    @GetMapping("/list" )
    public TableDataInfo list(NftTokenInfo nftTokenInfo) {
        Page<NftTokenInfo> page = getPage();
        page = nftTokenInfoService.page(page, new QueryWrapper<>(nftTokenInfo));
        return getDataTable(page.getRecords(), page.getTotal());
    }

    @PreAuthorize("@ss.hasPermi('nft:nftTokenInfo:export')" )
    @Log(title = "nft token" , businessType = BusinessType.EXPORT)
    @GetMapping("/export" )
    public AjaxResult export(NftTokenInfo nftTokenInfo) {
        List<NftTokenInfo> list = nftTokenInfoService.list(new QueryWrapper<>(nftTokenInfo));
        ExcelUtil<NftTokenInfo> util = new ExcelUtil<NftTokenInfo>(NftTokenInfo. class);
        return util.exportExcel(list, "nftTokenInfo" );
    }

    @PreAuthorize("@ss.hasPermi('nft:nftTokenInfo:query')" )
    @GetMapping(value = "/{id}" )
    public AjaxResult getInfo(@PathVariable("id" ) Long id) {

        return AjaxResult.success(nftTokenInfoService.getById(id));
    }

    @PreAuthorize("@ss.hasPermi('nft:nftTokenInfo:add')" )
    @Log(title = "nft token" , businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody NftTokenInfo nftTokenInfo) {

        return toAjax(nftTokenInfoService.save(nftTokenInfo));
    }

    @PreAuthorize("@ss.hasPermi('nft:nftTokenInfo:edit')" )
    @Log(title = "nft token" , businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody NftTokenInfo nftTokenInfo) {

        return toAjax(nftTokenInfoService.updateById(nftTokenInfo));
    }

    @PreAuthorize("@ss.hasPermi('nft:nftTokenInfo:remove')" )
    @Log(title = "nft token" , businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}" )
    public AjaxResult remove(@PathVariable Long[] ids) {

        return toAjax(nftTokenInfoService.deleteNftTokenInfoByIds(ids));
    }
}
