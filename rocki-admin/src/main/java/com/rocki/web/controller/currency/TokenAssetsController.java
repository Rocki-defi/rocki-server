package com.rocki.web.controller.currency;

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
import com.rocki.currency.domain.TokenAssets;
import com.rocki.currency.service.TokenAssetsService;
import com.rocki.common.utils.poi.ExcelUtil;
import com.rocki.common.core.page.TableDataInfo;


@RestController
@RequestMapping("/currency/tokenAssets" )
public class TokenAssetsController extends BaseController {
    @Autowired
    private TokenAssetsService tokenAssetsService;

    @PreAuthorize("@ss.hasPermi('currency:tokenAssets:list')" )
    @GetMapping("/list" )
    public TableDataInfo list(TokenAssets tokenAssets) {
        Page<TokenAssets> page = getPage();
        page = tokenAssetsService.page(page, new QueryWrapper<>(tokenAssets));
        return getDataTable(page.getRecords(), page.getTotal());
    }


    @PreAuthorize("@ss.hasPermi('currency:tokenAssets:export')" )
    @Log(title = "token" , businessType = BusinessType.EXPORT)
    @GetMapping("/export" )
    public AjaxResult export(TokenAssets tokenAssets) {
        List<TokenAssets> list = tokenAssetsService.list(new QueryWrapper<>(tokenAssets));
        ExcelUtil<TokenAssets> util = new ExcelUtil<TokenAssets>(TokenAssets. class);
        return util.exportExcel(list, "tokenAssets" );
    }

    @PreAuthorize("@ss.hasPermi('currency:tokenAssets:query')" )
    @GetMapping(value = "/{id}" )
    public AjaxResult getInfo(@PathVariable("id" ) Long id) {

        return AjaxResult.success(tokenAssetsService.getById(id));
    }

    @PreAuthorize("@ss.hasPermi('currency:tokenAssets:add')" )
    @Log(title = "token" , businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TokenAssets tokenAssets) {

        return toAjax(tokenAssetsService.save(tokenAssets));
    }


    @PreAuthorize("@ss.hasPermi('currency:tokenAssets:edit')" )
    @Log(title = "token" , businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TokenAssets tokenAssets) {

        return toAjax(tokenAssetsService.updateById(tokenAssets));
    }

}
