package com.tct.itd.adm.convert;

import com.tct.itd.adm.entity.User;
import com.tct.itd.base.BaseConvert;
import com.tct.itd.common.dto.UserDto;
import org.mapstruct.Mapper;

/**
 * @Description
 * @Author zhaoke
 * @Date 2020-12-01 15:23
 */
@Mapper(componentModel = "spring")
public abstract class UserConvert extends BaseConvert<UserDto, User> {
}
