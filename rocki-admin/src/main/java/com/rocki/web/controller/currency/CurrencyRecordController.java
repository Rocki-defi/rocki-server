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
import com.rocki.currency.domain.CurrencyRecord;
import com.rocki.currency.service.CurrencyRecordService;
import com.rocki.common.utils.poi.ExcelUtil;
import com.rocki.common.core.page.TableDataInfo;


@RestController
@RequestMapping("/currency/currencyRecord" )
public class CurrencyRecordController extends BaseController {
    @Autowired
    private CurrencyRecordService currencyRecordService;

    @PreAuthorize("@ss.hasPermi('currency:currencyRecord:list')" )
    @GetMapping("/list" )
    public TableDataInfo list(CurrencyRecord currencyRecord) {
        Page<CurrencyRecord> page = getPage();
        page = currencyRecordService.page(page, new QueryWrapper<>(currencyRecord));
        return getDataTable(page.getRecords(), page.getTotal());
    }


    @PreAuthorize("@ss.hasPermi('currency:currencyRecord:export')" )
    @GetMapping("/export" )
    public AjaxResult export(CurrencyRecord currencyRecord) {
        List<CurrencyRecord> list = currencyRecordService.list(new QueryWrapper<>(currencyRecord));
        ExcelUtil<CurrencyRecord> util = new ExcelUtil<CurrencyRecord>(CurrencyRecord. class);
        return util.exportExcel(list, "currencyRecord" );
    }

    @PreAuthorize("@ss.hasPermi('currency:currencyRecord:query')" )
    @GetMapping(value = "/{currencyRecordId}" )
    public AjaxResult getInfo(@PathVariable("currencyRecordId" ) Long currencyRecordId) {

        return AjaxResult.success(currencyRecordService.getById(currencyRecordId));
    }

    @PreAuthorize("@ss.hasPermi('currency:currencyRecord:add')" )
    @PostMapping
    public AjaxResult add(@RequestBody CurrencyRecord currencyRecord) {

        return toAjax(currencyRecordService.save(currencyRecord));
    }


    @PreAuthorize("@ss.hasPermi('currency:currencyRecord:edit')" )
    @PutMapping
    public AjaxResult edit(@RequestBody CurrencyRecord currencyRecord) {

        return toAjax(currencyRecordService.updateById(currencyRecord));
    }


    @PreAuthorize("@ss.hasPermi('currency:currencyRecord:remove')" )
    @DeleteMapping("/{currencyRecordIds}" )
    public AjaxResult remove(@PathVariable Long[] currencyRecordIds) {

        return toAjax(currencyRecordService.deleteCurrencyRecordByIds(currencyRecordIds));
    }
}
