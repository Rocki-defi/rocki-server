package com.rocki.app.controller.nft;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rocki.common.core.domain.AjaxResult;
import com.rocki.common.utils.MessageUtils;
import com.rocki.common.utils.RegexUtils;
import com.rocki.nft.domain.NftEmailCollection;
import com.rocki.nft.service.NftEmailCollectionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * @author David Wilde
 * @Package com.rocki.app.controller.nft
 * @date 3/11/21 2:17 下午
 */
@RestController
@RequestMapping("/api/v2/nft/sys")
@Validated
@ResponseBody
public class AppNftSysController {
    Logger logger = LoggerFactory.getLogger(AppNftSysController.class);

    @Autowired
    private NftEmailCollectionService nftEmailCollectionService;

    @PostMapping("collectEmailAddr")
    public AjaxResult collectEmailAddr(
            @ApiParam(name = "addr", value = "email address", required = true)
            @RequestParam(value = "addr") String addr
    ) {
        if (StringUtils.isBlank(addr) || !RegexUtils.isEmail(addr)) {
            return AjaxResult.error(MessageUtils.message("user.email.not.valid"));
        }

        // 查看邮箱是否存在
        int integer = nftEmailCollectionService.count(
                new QueryWrapper<NftEmailCollection>().eq("email", addr));
        if (integer > 0) {
            return AjaxResult.success();
        }

        NftEmailCollection nftEmailCollection = new NftEmailCollection();
        nftEmailCollection.setEmail(addr);
        nftEmailCollection.setDate(new Date());
        nftEmailCollectionService.save(nftEmailCollection);
        return AjaxResult.success();
    }
}
