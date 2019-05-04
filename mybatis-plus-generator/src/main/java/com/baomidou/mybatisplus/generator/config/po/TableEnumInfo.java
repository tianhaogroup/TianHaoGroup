package com.baomidou.mybatisplus.generator.config.po;


import com.baomidou.mybatisplus.generator.config.rules.IColumnType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public class TableEnumInfo {
    private String name;
    private IColumnType columnType;
    List<TableEnum> tableEnums=new ArrayList<>();
}
