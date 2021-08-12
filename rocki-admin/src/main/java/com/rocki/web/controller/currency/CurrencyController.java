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
import com.rocki.currency.domain.Currency;
import com.rocki.currency.service.CurrencyService;
import com.rocki.common.utils.poi.ExcelUtil;
import com.rocki.common.core.page.TableDataInfo;


@RestController
@RequestMapping("/currency/currency" )
public class CurrencyController extends BaseController {
    @Autowired
    private CurrencyService currencyService;

    @PreAuthorize("@ss.hasPermi('currency:currency:list')" )
    @GetMapping("/list" )
    public TableDataInfo list(Currency currency) {
        Page<Currency> page = getPage();
        page = currencyService.page(page, new QueryWrapper<>(currency));
        return getDataTable(page.getRecords(), page.getTotal());
    }


    @PreAuthorize("@ss.hasPermi('currency:currency:export')" )
    @GetMapping("/export" )
    public AjaxResult export(Currency currency) {
        List<Currency> list = currencyService.list(new QueryWrapper<>(currency));
        ExcelUtil<Currency> util = new ExcelUtil<Currency>(Currency. class);
        return util.exportExcel(list, "currency" );
    }


    @PreAuthorize("@ss.hasPermi('currency:currency:query')" )
    @GetMapping(value = "/{currencyId}" )
    public AjaxResult getInfo(@PathVariable("currencyId" ) Long currencyId) {

        return AjaxResult.success(currencyService.getById(currencyId));
    }

    @PreAuthorize("@ss.hasPermi('currency:currency:add')" )
    @PostMapping
    public AjaxResult add(@RequestBody Currency currency) {

        return toAjax(currencyService.save(currency));
    }

    @PreAuthorize("@ss.hasPermi('currency:currency:edit')" )
    @PutMapping
    public AjaxResult edit(@RequestBody Currency currency) {

        return toAjax(currencyService.updateById(currency));
    }


    @PreAuthorize("@ss.hasPermi('currency:currency:remove')" )
    @DeleteMapping("/{currencyIds}" )
    public AjaxResult remove(@PathVariable Long[] currencyIds) {

        return toAjax(currencyService.deleteCurrencyByIds(currencyIds));
    }
}
