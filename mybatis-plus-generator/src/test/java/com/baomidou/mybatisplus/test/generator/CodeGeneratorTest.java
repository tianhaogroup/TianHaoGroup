/*
 * Copyright (c) 2011-2019, hubin (jobob@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.baomidou.mybatisplus.test.generator;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.GlobalConfig;
import com.baomidou.mybatisplus.generator.config.PackageConfig;
import com.baomidou.mybatisplus.generator.config.StrategyConfig;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.mysql.cj.jdbc.Driver;
import org.junit.jupiter.api.Test;

/**
 * 代码生成器 示例
 *
 * @author K神
 * @since 2017/12/29
 */
class CodeGeneratorTest {

    /**
     * 是否强制带上注解
     */
    private boolean enableTableFieldAnnotation = false;
    /**
     * 生成的注解带上IdType类型
     */
    private IdType tableIdType = null;
    /**
     * 是否去掉生成实体的属性名前缀
     */
    private String[] fieldPrefix = null;
    /**
     * 生成的Service 接口类名是否以I开头
     * <p>默认是以I开头</p>
     * <p>user表 -> IUserService, UserServiceImpl</p>
     */
    private boolean serviceClassNameStartWithI = true;

    @Test
    void generateCode() {
        String packageName = "com.lihuiquan.genrate";
        fieldPrefix = new String[]{"test"};
        enableTableFieldAnnotation = true;
        tableIdType = IdType.INPUT;

        serviceClassNameStartWithI = false;
        generateByTables(packageName, "em_item_publish_worth");
    }

    private void generateByTables(String packageName, String... tableNames) {
        GlobalConfig config = new GlobalConfig();
        String dbUrl = "jdbc:mysql://localhost:3306/emanor?rewriteBatchedStatements=true&useUnicode=true&characterEncoding=utf8&autoReconnect=true&zeroDateTimeBehavior=convertToNull&allowMultiQueries=true&useSSL=false&serverTimezone=GMT%2B8";
        DataSourceConfig dataSourceConfig = new DataSourceConfig();
        dataSourceConfig.setDbType(DbType.MYSQL)
            .setUrl(dbUrl)
            .setUsername("root")
            .setPassword("root")
            .setDriverName(Driver.class.getName());
        StrategyConfig strategyConfig = new StrategyConfig();
        strategyConfig
            .setCapitalMode(true)
            .setEntityLombokModel(false)
            // .setDbColumnUnderline(true) 改为如下 2 个配置
            .setNaming(NamingStrategy.underline_to_camel)
            .setColumnNaming(NamingStrategy.underline_to_camel)
            .setEntityTableFieldAnnotationEnable(enableTableFieldAnnotation)
            .setFieldPrefix(fieldPrefix)//test_id -> id, test_type -> type
            .setInclude(tableNames);//修改替换成你需要的表名，多个表名传数组
        config.setActiveRecord(false)
            .setIdType(tableIdType)
            .setAuthor("lihuiquan")
            .setOutputDir("d:\\codeGen")
            .setFileOverride(true);
        if (!serviceClassNameStartWithI) {
            config.setServiceName("%sService");
        }
        config.setMapperName("%sRepository");
        new AutoGenerator().setGlobalConfig(config)
            .setDataSource(dataSourceConfig)
            .setStrategy(strategyConfig)
            .setPackageInfo(
                new PackageConfig()
                    .setParent(packageName)
                    .setController("controller")
                    .setEntity("entity")
            ).execute();
    }
}
