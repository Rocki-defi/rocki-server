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
import com.rocki.currency.domain.Token;
import com.rocki.currency.service.TokenService;
import com.rocki.common.utils.poi.ExcelUtil;
import com.rocki.common.core.page.TableDataInfo;


@RestController
@RequestMapping("/currency/token" )
public class TokenController extends BaseController {
    @Autowired
    private TokenService tokenService;

    @PreAuthorize("@ss.hasPermi('currency:token:list')" )
    @GetMapping("/list" )
    public TableDataInfo list(Token token) {
        Page<Token> page = getPage();
        page = tokenService.page(page, new QueryWrapper<>(token));
        return getDataTable(page.getRecords(), page.getTotal());
    }


    @PreAuthorize("@ss.hasPermi('currency:token:export')" )
    @Log(title = "token" , businessType = BusinessType.EXPORT)
    @GetMapping("/export" )
    public AjaxResult export(Token token) {
        List<Token> list = tokenService.list(new QueryWrapper<>(token));
        ExcelUtil<Token> util = new ExcelUtil<Token>(Token. class);
        return util.exportExcel(list, "token" );
    }

    @PreAuthorize("@ss.hasPermi('currency:token:query')" )
    @GetMapping(value = "/{id}" )
    public AjaxResult getInfo(@PathVariable("id" ) Long id) {

        return AjaxResult.success(tokenService.getById(id));
    }

    @PreAuthorize("@ss.hasPermi('currency:token:add')" )
    @Log(title = "token" , businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Token token) {

        return toAjax(tokenService.save(token));
    }

    @PreAuthorize("@ss.hasPermi('currency:token:edit')" )
    @Log(title = "token" , businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Token token) {

        return toAjax(tokenService.updateById(token));
    }

    @PreAuthorize("@ss.hasPermi('currency:token:remove')" )
    @Log(title = "token" , businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}" )
    public AjaxResult remove(@PathVariable Long[] ids) {

        return toAjax(tokenService.deleteTokenByIds(ids));
    }
}
