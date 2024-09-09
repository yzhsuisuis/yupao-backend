package com.yupi.yupao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupao.domain.Tag;
import com.yupi.yupao.service.TagService;
import com.yupi.yupao.mapper.TagMapper;
import org.springframework.stereotype.Service;

/**
* @author yangz
* @description 针对表【tag(标签)】的数据库操作Service实现
* @createDate 2024-09-07 17:21:31
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService{

}




