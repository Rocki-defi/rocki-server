package com.rocki.web.controller.dapp;

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
import com.rocki.dapp.domain.Activity;
import com.rocki.dapp.service.ActivityService;
import com.rocki.common.utils.poi.ExcelUtil;
import com.rocki.common.core.page.TableDataInfo;


@RestController
@RequestMapping("/user/activity" )
public class ActivityController extends BaseController {
    @Autowired
    private ActivityService activityService;


    @PreAuthorize("@ss.hasPermi('user:activity:list')" )
    @GetMapping("/list" )
    public TableDataInfo list(Activity activity) {
        Page<Activity> page = getPage();
        page = activityService.page(page, new QueryWrapper<>(activity));
        return getDataTable(page.getRecords(), page.getTotal());
    }


    @PreAuthorize("@ss.hasPermi('user:activity:export')" )
    @Log(title = "dapp-stake-activity" , businessType = BusinessType.EXPORT)
    @GetMapping("/export" )
    public AjaxResult export(Activity activity) {
        List<Activity> list = activityService.list(new QueryWrapper<>(activity));
        ExcelUtil<Activity> util = new ExcelUtil<Activity>(Activity. class);
        return util.exportExcel(list, "activity" );
    }

    @PreAuthorize("@ss.hasPermi('user:activity:query')" )
    @GetMapping(value = "/{id}" )
    public AjaxResult getInfo(@PathVariable("id" ) Long id) {

        return AjaxResult.success(activityService.getById(id));
    }

    @PreAuthorize("@ss.hasPermi('user:activity:add')" )
    @Log(title = "dapp-stake-activity" , businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Activity activity) {

        return toAjax(activityService.save(activity));
    }

    @PreAuthorize("@ss.hasPermi('user:activity:edit')" )
    @Log(title = "dapp-stake-activity" , businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Activity activity) {

        return toAjax(activityService.updateById(activity));
    }

    @PreAuthorize("@ss.hasPermi('user:activity:remove')" )
    @Log(title = "dapp-stake-activity" , businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}" )
    public AjaxResult remove(@PathVariable Long[] ids) {

        return toAjax(activityService.deleteActivityByIds(ids));
    }
}
