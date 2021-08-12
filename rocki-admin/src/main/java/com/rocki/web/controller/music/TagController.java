package com.rocki.web.controller.music;

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
import com.rocki.music.domain.Tag;
import com.rocki.music.service.TagService;
import com.rocki.common.utils.poi.ExcelUtil;
import com.rocki.common.core.page.TableDataInfo;


@RestController
@RequestMapping("/music/tag" )
public class TagController extends BaseController {
    @Autowired
    private TagService tagService;

    @PreAuthorize("@ss.hasPermi('music:tag:list')" )
    @GetMapping("/list" )
    public TableDataInfo list(Tag tag) {
        Page<Tag> page = getPage();
        page = tagService.page(page, new QueryWrapper<>(tag));
        return getDataTable(page.getRecords(), page.getTotal());
    }

    @PreAuthorize("@ss.hasPermi('music:tag:export')" )
    @GetMapping("/export" )
    public AjaxResult export(Tag tag) {
        List<Tag> list = tagService.list(new QueryWrapper<>(tag));
        ExcelUtil<Tag> util = new ExcelUtil<Tag>(Tag. class);
        return util.exportExcel(list, "tag" );
    }

    @PreAuthorize("@ss.hasPermi('music:tag:query')" )
    @GetMapping(value = "/{id}" )
    public AjaxResult getInfo(@PathVariable("id" ) Long id) {

        return AjaxResult.success(tagService.getById(id));
    }


    @PreAuthorize("@ss.hasPermi('music:tag:add')" )
    @PostMapping
    public AjaxResult add(@RequestBody Tag tag) {

        return toAjax(tagService.save(tag));
    }


    @PreAuthorize("@ss.hasPermi('music:tag:edit')" )
    @PutMapping
    public AjaxResult edit(@RequestBody Tag tag) {

        return toAjax(tagService.updateById(tag));
    }

    @PreAuthorize("@ss.hasPermi('music:tag:remove')" )
    @DeleteMapping("/{ids}" )
    public AjaxResult remove(@PathVariable Long[] ids) {

        return toAjax(tagService.deleteTagByIds(ids));
    }
}
