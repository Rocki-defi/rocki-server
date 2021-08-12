package com.rocki.app.controller.dapp;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rocki.common.core.domain.AjaxResult;
import com.rocki.dapp.domain.Activity;
import com.rocki.dapp.service.ActivityService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author David Wilde
 * @Package com.rocki.app.controller.dapp
 * @ClassName AppActivityController
 * @date 8/12/21 12:04 下午
 */
@RestController
@RequestMapping(path = "api/v2/stk/vrfy/")
public class AppActivityController {
    Logger logger = LoggerFactory.getLogger(AppActivityController.class);

    @Autowired
    private ActivityService activityService;

    @GetMapping(path = "allStakeActivities")
    public AjaxResult allStakeActivities(
            @ApiParam(name = "chainId", required = true)
            @RequestParam(name = "chainId", required = false, defaultValue = "56") Long chainId
    ) {
        List<Activity> activityList = activityService.list(new QueryWrapper<Activity>()
                .eq("chain_id", chainId).eq("type", 1));

        return AjaxResult.success(activityList);
    }

    @GetMapping(path = "allBurnActivities")
    public AjaxResult allBurnActivities(
            @ApiParam(name = "chainId", required = true)
            @RequestParam(name = "chainId", required = false, defaultValue = "56") Long chainId
    ) {
        List<Activity> activityList = activityService.list(new QueryWrapper<Activity>()
                .eq("chain_id", chainId).eq("type", 2));

        return AjaxResult.success(activityList);
    }
}
