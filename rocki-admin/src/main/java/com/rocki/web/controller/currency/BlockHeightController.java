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
import com.rocki.currency.domain.BlockHeight;
import com.rocki.currency.service.BlockHeightService;
import com.rocki.common.utils.poi.ExcelUtil;
import com.rocki.common.core.page.TableDataInfo;


@RestController
@RequestMapping("/currency/blockHeight" )
public class BlockHeightController extends BaseController {
    @Autowired
    private BlockHeightService blockHeightService;


    @PreAuthorize("@ss.hasPermi('currency:blockHeight:list')" )
    @GetMapping("/list" )
    public TableDataInfo list(BlockHeight blockHeight) {
        Page<BlockHeight> page = getPage();
        page = blockHeightService.page(page, new QueryWrapper<>(blockHeight));
        return getDataTable(page.getRecords(), page.getTotal());
    }



    @PreAuthorize("@ss.hasPermi('currency:blockHeight:export')" )
    @Log(title = "区块链高度" , businessType = BusinessType.EXPORT)
    @GetMapping("/export" )
    public AjaxResult export(BlockHeight blockHeight) {
        List<BlockHeight> list = blockHeightService.list(new QueryWrapper<>(blockHeight));
        ExcelUtil<BlockHeight> util = new ExcelUtil<BlockHeight>(BlockHeight. class);
        return util.exportExcel(list, "blockHeight" );
    }

    @PreAuthorize("@ss.hasPermi('currency:blockHeight:query')" )
    @GetMapping(value = "/{id}" )
    public AjaxResult getInfo(@PathVariable("id" ) Long id) {

        return AjaxResult.success(blockHeightService.getById(id));
    }

    @PreAuthorize("@ss.hasPermi('currency:blockHeight:add')" )
    @Log(title = "区块链高度" , businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BlockHeight blockHeight) {

        return toAjax(blockHeightService.save(blockHeight));
    }

    @PreAuthorize("@ss.hasPermi('currency:blockHeight:edit')" )
    @Log(title = "区块链高度" , businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BlockHeight blockHeight) {

        return toAjax(blockHeightService.updateById(blockHeight));
    }


    @PreAuthorize("@ss.hasPermi('currency:blockHeight:remove')" )
    @Log(title = "区块链高度" , businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}" )
    public AjaxResult remove(@PathVariable Long[] ids) {

        return toAjax(blockHeightService.deleteBlockHeightByIds(ids));
    }
}
