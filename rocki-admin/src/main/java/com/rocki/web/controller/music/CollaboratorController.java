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
import com.rocki.music.domain.Collaborator;
import com.rocki.music.service.CollaboratorService;
import com.rocki.common.utils.poi.ExcelUtil;
import com.rocki.common.core.page.TableDataInfo;


@RestController
@RequestMapping("/music/collaborator" )
public class CollaboratorController extends BaseController {
    @Autowired
    private CollaboratorService collaboratorService;


    @PreAuthorize("@ss.hasPermi('music:collaborator:list')" )
    @GetMapping("/list" )
    public TableDataInfo list(Collaborator collaborator) {
        Page<Collaborator> page = getPage();
        page = collaboratorService.page(page, new QueryWrapper<>(collaborator));
        return getDataTable(page.getRecords(), page.getTotal());
    }



    @PreAuthorize("@ss.hasPermi('music:collaborator:export')" )
    @Log(title = "" , businessType = BusinessType.EXPORT)
    @GetMapping("/export" )
    public AjaxResult export(Collaborator collaborator) {
        List<Collaborator> list = collaboratorService.list(new QueryWrapper<>(collaborator));
        ExcelUtil<Collaborator> util = new ExcelUtil<Collaborator>(Collaborator. class);
        return util.exportExcel(list, "collaborator" );
    }


    @PreAuthorize("@ss.hasPermi('music:collaborator:query')" )
    @GetMapping(value = "/{id}" )
    public AjaxResult getInfo(@PathVariable("id" ) Long id) {

        return AjaxResult.success(collaboratorService.getById(id));
    }



}
