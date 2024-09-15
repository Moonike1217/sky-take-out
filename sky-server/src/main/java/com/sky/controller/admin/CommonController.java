package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOSSUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {

    @Autowired
    public AliOSSUtils aliOSSUtils;

    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file){
        log.info("上传文件，文件名:{}", file.getOriginalFilename());
        /*
            注意这里的image必须要和接口文档中的要求一致，不能够随意乱写
         */
        String url = null;
        try {
            url = aliOSSUtils.upload(file);
            log.info("文件上传完成，url:{}", url);
            return Result.success(url);

        } catch (IOException e) {
            log.info("文件上传失败:{}", e);
            return Result.error(MessageConstant.UPLOAD_FAILED);
        }
    }
}
