/*
 * Copyright (c) 2011-2020, baomidou (jobob@qq.com).
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
package com.baomidou.mybatisplus.generator.engine;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.po.TableEnum;
import com.baomidou.mybatisplus.generator.config.po.TableEnumInfo;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.toolkit.PackageHelper;
import com.baomidou.mybatisplus.generator.InjectionConfig;
import com.baomidou.mybatisplus.generator.config.builder.ConfigBuilder;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.baomidou.mybatisplus.generator.config.rules.FileType;


/**
 * 模板引擎抽象类
 *
 * @author hubin
 * @since 2018-01-10
 */
public abstract class AbstractTemplateEngine {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractTemplateEngine.class);
    /**
     * 配置信息
     */
    private ConfigBuilder configBuilder;


    /**
     * 模板引擎初始化
     */
    public AbstractTemplateEngine init(ConfigBuilder configBuilder) {
        this.configBuilder = configBuilder;
        return this;
    }


    /**
     * 输出 java xml 文件
     */
    public AbstractTemplateEngine batchOutput() {
        try {
            List<TableInfo> tableInfoList = getConfigBuilder().getTableInfoList();
            List<TableEnumInfo> enumFields=new ArrayList<>();
            for (TableInfo tableInfo : tableInfoList) {
                if(null!=tableInfo.getFields()&&!tableInfo.getFields().isEmpty()){
                    tableInfo.getFields().parallelStream().forEach(field ->{
                        String comment=field.getComment();
                        if(field.getPropertyName().toLowerCase().equals("status")){
                            field.setCustomType("SystemStatusEnum");
                            tableInfo.setImportPackages("com.emanor.general.enums.SystemStatusEnum");
                            return;
                        }
                        Pattern pattern = Pattern.compile("(\\d+[^\\d]+)");
                        Matcher matcher = pattern.matcher(comment );
                        Map<String,Object> values=new HashMap<>();
                        TableEnumInfo enums=null;
                        while (matcher.find()) {
                            //String values=matcher.group();
                            String name_value=matcher.group(1);
                            String name=name_value.replaceAll("\\d|[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]", "");
                            String value=name_value.replaceAll("\\D", "");
                            if(StringUtils.isNotEmpty(name)&&StringUtils.isNotEmpty(value)){
                                if(null==enums){
                                    enums=new TableEnumInfo();
                                    String str1=field.getUpName().substring(0,1).toUpperCase();
                                    String str2=field.getUpName().substring(1,field.getUpName().length());
                                    enums.setName(tableInfo.getEntityName()+str1+str2+ConstVal.ENUMS);
                                }
                                TableEnum tableEnum=new TableEnum();
                                tableEnum.setName(ChineseChangeToPinyin.ToPinyin(name).toUpperCase());
                                tableEnum.setValue(Integer.valueOf(value));
                                tableEnum.setDesc(name);
                                enums.getTableEnums().add(tableEnum);
                            }
                            values.put(name,value);
                            //System.out.println(collegeId);//14000
                        }
                        if(null!=enums){
                            field.setCustomType(enums.getName());
                            enumFields.add(enums);
                        }
                    });
                }
                Map<String, Object> objectMap = getObjectMap(tableInfo);
                Map<String, String> pathInfo = getConfigBuilder().getPathInfo();
                TemplateConfig template = getConfigBuilder().getTemplate();
                // 自定义内容
                InjectionConfig injectionConfig = getConfigBuilder().getInjectionConfig();
                if (null != injectionConfig) {
                    injectionConfig.initMap();
                    objectMap.put("cfg", injectionConfig.getMap());
                    List<FileOutConfig> focList = injectionConfig.getFileOutConfigList();
                    if (CollectionUtils.isNotEmpty(focList)) {
                        for (FileOutConfig foc : focList) {
                            if (isCreate(FileType.OTHER, foc.outputFile(tableInfo))) {
                                writer(objectMap, foc.getTemplatePath(), foc.outputFile(tableInfo));
                            }
                        }
                    }
                }
                String entityName = tableInfo.getEntityName();
                // enums.java
                if (!enumFields.isEmpty()&&null != entityName && null != pathInfo.get(ConstVal.ENUMS_PATH)) {
                    for (TableEnumInfo tableEnumInfo:enumFields) {
                        objectMap.put("tableEnumInfo",tableEnumInfo);
                        String enumsFile = String.format((pathInfo.get(ConstVal.ENUMS_PATH) + File.separator + "%s" + suffixJavaOrKt()), tableEnumInfo.getName());
                        String packageUrl=getConfigBuilder().getPackageInfo().get(ConstVal.ENUMS)+"."+tableEnumInfo.getName();
                        tableInfo.setImportPackages(packageUrl);
                        tableInfo.setControllerImportPackages(packageUrl);
                        if (isCreate(FileType.ENUMS, enumsFile)) {
                            writer(objectMap, templateFilePath(template.getEnums()), enumsFile);
                        }

                    }
                }
                // Mp.java
                if (null != entityName && null != pathInfo.get(ConstVal.ENTITY_PATH)) {
                    String fileName=String.format(File.separator + "%s" + suffixJavaOrKt(),entityName);
                    String entityFile =String.format((pathInfo.get(ConstVal.ENTITY_PATH) +File.separator + "%s" + suffixJavaOrKt()),entityName);
                    if (isCreate(FileType.ENTITY, entityFile)) {
                        tableInfo.setControllerImportPackages(getConfigBuilder().getPackageInfo().get(ConstVal.ENTITY)+"."+tableInfo.getEntityName());
                        writer(objectMap, templateFilePath(template.getEntity(getConfigBuilder().getGlobalConfig().isKotlin())), entityFile);
                    }
                }
                //request
                if (null != entityName && null != pathInfo.get(ConstVal.REQUEST_PATH)) {
                    String requestFile =String.format((pathInfo.get(ConstVal.REQUEST_PATH) +File.separator + tableInfo.getRequestName()+suffixJavaOrKt()),entityName);
                    if (isCreate(FileType.REQUEST, requestFile)) {
                        tableInfo.setControllerImportPackages(getConfigBuilder().getPackageInfo().get(ConstVal.REQUEST)+"."+tableInfo.getRequestName());
                        writer(objectMap, templateFilePath(template.getRequest()), requestFile);
                    }
                }
                //response
                if (null != entityName && null != pathInfo.get(ConstVal.RESPONSE_PATH)) {
                    String responseFile =String.format((pathInfo.get(ConstVal.RESPONSE_PATH) +File.separator + tableInfo.getResponseName() +suffixJavaOrKt()),entityName);
                    if (isCreate(FileType.RESPONSE, responseFile)) {
                        tableInfo.setControllerImportPackages(getConfigBuilder().getPackageInfo().get(ConstVal.RESPONSE)+"."+tableInfo.getResponseName());
                        writer(objectMap, templateFilePath(template.getResponse()), responseFile);
                    }
                }
                // MpMapper.java
                if (null != tableInfo.getMapperName() && null != pathInfo.get(ConstVal.MAPPER_PATH)) {
                    String mapperFile = String.format((pathInfo.get(ConstVal.MAPPER_PATH) + File.separator + tableInfo.getMapperName() + suffixJavaOrKt()), entityName);
                    if (isCreate(FileType.MAPPER, mapperFile)) {
                        writer(objectMap, templateFilePath(template.getMapper()), mapperFile);
                    }
                }
                // MpMapper.xml
                if (null != tableInfo.getXmlName() && null != pathInfo.get(ConstVal.XML_PATH)) {
                    String xmlFile = String.format((pathInfo.get(ConstVal.XML_PATH) + File.separator + tableInfo.getXmlName() + ConstVal.XML_SUFFIX), entityName);
                    if (isCreate(FileType.XML, xmlFile)) {
                        writer(objectMap, templateFilePath(template.getXml()), xmlFile);
                    }
                }
                // IMpService.java
                if (null != tableInfo.getServiceName() && null != pathInfo.get(ConstVal.SERVICE_PATH)) {
                    //String serviceFile = String.format((pathInfo.get(ConstVal.SERVICE_PATH) + File.separator + tableInfo.getServiceName() + suffixJavaOrKt()), entityName);
                    String fileName=String.format(File.separator + "%s" + tableInfo.getServiceName()+ suffixJavaOrKt(),entityName);
                    String serviceFile = String.format((pathInfo.get(ConstVal.SERVICE_PATH) + File.separator + tableInfo.getServiceName() + suffixJavaOrKt()), entityName);
                    if (isCreate(FileType.SERVICE, serviceFile)) {
                        tableInfo.setControllerImportPackages(getConfigBuilder().getPackageInfo().get(ConstVal.SERVICE)+"."+tableInfo.getServiceName());
                        writer(objectMap, templateFilePath(template.getService()), serviceFile);
                    }
                }
                // MpServiceImpl.java
                if (null != tableInfo.getServiceImplName() && null != pathInfo.get(ConstVal.SERVICE_IMPL_PATH)) {
                    String implFile = String.format((pathInfo.get(ConstVal.SERVICE_IMPL_PATH) + File.separator + tableInfo.getServiceImplName() + suffixJavaOrKt()), entityName);
                    if (isCreate(FileType.SERVICE_IMPL, implFile)) {
                        writer(objectMap, templateFilePath(template.getServiceImpl()), implFile);
                    }
                }
                // MpController.java
                if (null != tableInfo.getControllerName() && null != pathInfo.get(ConstVal.CONTROLLER_PATH)) {
                    String controllerFile = String.format((pathInfo.get(ConstVal.CONTROLLER_PATH) + File.separator + tableInfo.getControllerName() + suffixJavaOrKt()), entityName);
                    if (isCreate(FileType.CONTROLLER, controllerFile)) {
                        writer(objectMap, templateFilePath(template.getController()), controllerFile);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("无法创建文件，请检查配置信息！", e);
        }
        return this;
    }


    /**
     * 将模板转化成为文件
     *
     * @param objectMap    渲染对象 MAP 信息
     * @param templatePath 模板文件
     * @param outputFile   文件生成的目录
     */
    public abstract void writer(Map<String, Object> objectMap, String templatePath, String outputFile) throws Exception;

    /**
     * 处理输出目录
     */
    public AbstractTemplateEngine mkdirs() {
        ConfigBuilder cb = getConfigBuilder();
        // 自定义判断
        InjectionConfig ic = cb.getInjectionConfig();
        cb.getPathInfo().forEach((key, value) -> {
            if (null != ic && null != ic.getFileCreate()) {
                if(!ic.getFileCreate().isCreate(cb, FileType.FOLDER, value)){
                    return;
                }
            }
            File dir = new File(value);
            if (!dir.exists()) {
                boolean result = dir.mkdirs();
                if (result) {
                    logger.debug("创建目录： [" + value + "]");
                }
            }
        });
        return this;
    }


    /**
     * 打开输出目录
     */
    public void open() {
        String outDir = getConfigBuilder().getGlobalConfig().getOutputDir();
        if (getConfigBuilder().getGlobalConfig().isOpen()
            && StringUtils.isNotEmpty(outDir)) {
            try {
                String osName = System.getProperty("os.name");
                if (osName != null) {
                    if (osName.contains("Mac")) {
                        Runtime.getRuntime().exec("open " + outDir);
                    } else if (osName.contains("Windows")) {
                        Runtime.getRuntime().exec("cmd /c start " + outDir);
                    } else {
                        logger.debug("文件输出目录:" + outDir);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 渲染对象 MAP 信息
     *
     * @param tableInfo 表信息对象
     * @return ignore
     */
    public Map<String, Object> getObjectMap(TableInfo tableInfo) {
        Map<String, Object> objectMap = new HashMap<>(30);
        ConfigBuilder config = getConfigBuilder();
        if (config.getStrategyConfig().isControllerMappingHyphenStyle()) {
            objectMap.put("controllerMappingHyphenStyle", config.getStrategyConfig().isControllerMappingHyphenStyle());
            objectMap.put("controllerMappingHyphen", StringUtils.camelToHyphen(tableInfo.getEntityPath()));
        }
        objectMap.put("restControllerStyle", config.getStrategyConfig().isRestControllerStyle());
        objectMap.put("config", config);
        objectMap.put("package", config.getPackageInfo());
        GlobalConfig globalConfig = config.getGlobalConfig();
        objectMap.put("author", globalConfig.getAuthor());
        objectMap.put("idType", globalConfig.getIdType() == null ? null : globalConfig.getIdType().toString());
        objectMap.put("logicDeleteFieldName", config.getStrategyConfig().getLogicDeleteFieldName());
        objectMap.put("versionFieldName", config.getStrategyConfig().getVersionFieldName());
        objectMap.put("activeRecord", globalConfig.isActiveRecord());
        objectMap.put("kotlin", globalConfig.isKotlin());
        objectMap.put("swagger2", globalConfig.isSwagger2());
        objectMap.put("date", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        objectMap.put("table", tableInfo);
        objectMap.put("enableCache", globalConfig.isEnableCache());
        objectMap.put("baseResultMap", globalConfig.isBaseResultMap());
        objectMap.put("baseColumnList", globalConfig.isBaseColumnList());
        objectMap.put("entity", tableInfo.getEntityName());
        objectMap.put("entitySerialVersionUID", config.getStrategyConfig().isEntitySerialVersionUID());
        objectMap.put("entityColumnConstant", config.getStrategyConfig().isEntityColumnConstant());
        objectMap.put("entityBuilderModel", config.getStrategyConfig().isEntityBuilderModel());
        objectMap.put("entityLombokModel", config.getStrategyConfig().isEntityLombokModel());
        objectMap.put("entityBooleanColumnRemoveIsPrefix", config.getStrategyConfig().isEntityBooleanColumnRemoveIsPrefix());
        objectMap.put("superEntityClass", getSuperClassName(config.getSuperEntityClass()));
        objectMap.put("superMapperClassPackage", config.getSuperMapperClass());
        objectMap.put("superMapperClass", getSuperClassName(config.getSuperMapperClass()));
        objectMap.put("superServiceClassPackage", config.getSuperServiceClass());
        objectMap.put("superServiceClass", getSuperClassName(config.getSuperServiceClass()));
        objectMap.put("superServiceImplClassPackage", config.getSuperServiceImplClass());
        objectMap.put("superServiceImplClass", getSuperClassName(config.getSuperServiceImplClass()));
        objectMap.put("superControllerClassPackage", config.getSuperControllerClass());
        objectMap.put("superControllerClass", getSuperClassName(config.getSuperControllerClass()));
        return objectMap;
    }


    /**
     * 获取类名
     *
     * @param classPath ignore
     * @return ignore
     */
    private String getSuperClassName(String classPath) {
        if (StringUtils.isEmpty(classPath)) {
            return null;
        }
        return classPath.substring(classPath.lastIndexOf(StringPool.DOT) + 1);
    }


    /**
     * 模板真实文件路径
     *
     * @param filePath 文件路径
     * @return ignore
     */
    public abstract String templateFilePath(String filePath);


    /**
     * 检测文件是否存在
     *
     * @return 文件是否存在
     */
    protected boolean isCreate(FileType fileType, String filePath) {
        ConfigBuilder cb = getConfigBuilder();
        // 自定义判断
        InjectionConfig ic = cb.getInjectionConfig();
        if (null != ic && null != ic.getFileCreate()) {
            return ic.getFileCreate().isCreate(cb, fileType, filePath);
        }
        // 全局判断【默认】
        File file = new File(filePath);
        boolean exist = file.exists();
        if (!exist) {
            PackageHelper.mkDir(file.getParentFile());
        }
        return !exist || getConfigBuilder().getGlobalConfig().isFileOverride();
    }

    /**
     * 文件后缀
     */
    protected String suffixJavaOrKt() {
        return getConfigBuilder().getGlobalConfig().isKotlin() ? ConstVal.KT_SUFFIX : ConstVal.JAVA_SUFFIX;
    }


    public ConfigBuilder getConfigBuilder() {
        return configBuilder;
    }

    public AbstractTemplateEngine setConfigBuilder(ConfigBuilder configBuilder) {
        this.configBuilder = configBuilder;
        return this;
    }
}
