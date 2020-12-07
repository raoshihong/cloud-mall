/**
 * Copyright 2016 Welab, Inc. All rights reserved. WELAB PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.rao.cloud.mall.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.welab.common.utils.StringUtil;
import com.welab.dust.common.cost.ApplicationId;
import com.welab.dust.common.excel.*;
import com.welab.dust.common.exception.DustBusinessException;
import com.welab.dust.dto.ExcelReadParamDTO;
import com.welab.dust.enums.ExcelFieldTypeEnum;
import com.welab.dust.enums.TypeEnum;
import com.welab.dust.parse.ResultParse;
import com.welab.dust.parse.impl.ModelHandheldImageVoParse;
import com.welab.dust.parse.impl.ModelLiaisonCheckVoParse;
import com.welab.dust.thread.ExcelReadContentExecuteUnit;
import com.welab.dust.utils.*;
import com.welab.dust.web.vo.VerifyReportVo;
import jxl.Cell;
import jxl.CellView;
import jxl.Sheet;
import jxl.Workbook;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:Jason@wolaidai.com">Jason</a>
 */

public class ExcelUtils {

    private static final Logger logger = LoggerFactory.getLogger(ExcelUtils.class);

    private static String[] parsePatterns = {"yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy/MM/dd",
        "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm"};

    /**
     * 分页读取Excel时,每页的大小
     */
    private static final Integer PAGE_SIZE = 50;



    /**
     * @param list
     *            数据源
     * @param fieldMap
     *            类的英文属性和Excel中的中文列名的对应关系 如果需要的是引用对象的属性，则英文属性使用类似于EL表达式的格式
     *            如：list中存放的都是student，student中又有college属性，而我们需要学院名称，则可以这样写 fieldMap.put("college.collegeName","学院名称")
     * @param sheetName
     *            工作表的名称
     * @param sheetSize
     *            每个工作表中记录的最大个数
     * @param out
     *            导出流
     * @throws Exception
     * @MethodName : listToExcel
     * @Description : 导出Excel（可以导出到本地文件系统，也可以导出到浏览器，可自定义工作表大小）
     */
    public static <T> void listToExcel(List<T> list, LinkedHashMap<String, Integer> mergeFieldMap,
        LinkedHashMap<String, String> fieldMap, String sheetName, int sheetSize, OutputStream out) throws Exception {

        if (list == null || list.size() == 0) {
            throw new Exception("数据源中没有任何数据");
        }

        if (sheetSize > 65535 || sheetSize < 1) {
            sheetSize = 65535;
        }

        // 创建工作簿并发送到OutputStream指定的地方
        XSSFWorkbook xwb = null;
        try {
            xwb = new XSSFWorkbook();

            // 因为2003的Excel一个工作表最多可以有65536条记录，除去列头剩下65535条
            // 所以如果记录太多，需要放到多个工作表中，其实就是个分页的过程
            // 1.计算一共有多少个工作表
            double sheetNum = Math.ceil(list.size() / new Integer(sheetSize).doubleValue());

            // 2.创建相应的工作表，并向其中填充数据
            for (int i = 0; i < sheetNum; i++) {
                // 如果只有一个工作表的情况
                if (1 == sheetNum) {
                    XSSFSheet sheet = xwb.createSheet(sheetName);
                    ExcelMeta excelMeta = fillSheetHeader(sheet, null, mergeFieldMap, fieldMap);
                    fillSheetContents(sheet, list, 0, list.size() - 1, excelMeta);

                    // 有多个工作表的情况
                } else {
                    XSSFSheet sheet = xwb.createSheet(sheetName + (i + 1));

                    // 获取开始索引和结束索引
                    int firstIndex = i * sheetSize;
                    int lastIndex =
                        (i + 1) * sheetSize - 1 > list.size() - 1 ? list.size() - 1 : (i + 1) * sheetSize - 1;
                    // 填充工作表
                    ExcelMeta excelMeta = fillSheetHeader(sheet, null, mergeFieldMap, fieldMap);
                    fillSheetContents(sheet, list, firstIndex, lastIndex, excelMeta);
                }
            }
            xwb.write(out);

        } catch (Exception e) {
            logger.info("导出Excel失败:{}", e);
            throw new Exception("导出Excel失败", e);
        } finally {
            if (null != out) {
                out.close();
            }
        }

    }

    /**
     * 自定义多个sheet的excel
     * 
     * @param excelDataList excel导出数据
     * @param out 输出流
     * @throws Exception
     */
    public static void multiSheetExcel(List<ExcelData> excelDataList, OutputStream out) throws Exception {
        // 创建工作簿并发送到OutputStream指定的地方
        XSSFWorkbook xwb = null;
        try {
            xwb = new XSSFWorkbook();

            // 根据数据,自定义多个sheet
            for (int i = 0; i < excelDataList.size(); i++) {
                ExcelData excelData = excelDataList.get(i);
                XSSFSheet sheet = xwb.createSheet(excelData.getSheetName());

                ExcelMeta excelMeta = fillSheetHeader(sheet, excelData.getColumnMap(), excelData.getMergeFieldMap(),
                    excelData.getFieldMap());
                fillSheetContents(sheet, excelData.getData(), 0, excelData.getData().size() - 1, excelMeta);
            }

            xwb.write(out);
        } catch (Exception e) {
            logger.info("导出Excel失败:{}", e);
            throw new Exception("导出Excel失败", e);
        } finally {
            if (null != out) {
                out.close();
            }
        }
    }

    public static <T> void headToExcel(LinkedHashMap<String, Integer> mergeFieldMap,
        LinkedHashMap<String, String> fieldMap, String sheetName, int sheetSize, OutputStream out) throws Exception {
        if (sheetSize > 65535 || sheetSize < 1) {
            sheetSize = 65535;
        }

        // 创建工作簿并发送到OutputStream指定的地方
        XSSFWorkbook xwb = null;
        try {
            xwb = new XSSFWorkbook();

            XSSFSheet sheet = xwb.createSheet(sheetName);
            fillSheetHeader(sheet, null, mergeFieldMap, fieldMap);

            xwb.write(out);

        } catch (Exception e) {
            throw new Exception("导出Excel失败", e);
        } finally {
            if (null != out) {
                out.close();
            }
        }

    }

    /**
     * @param list
     *            数据源
     * @param fieldMap
     *            类的英文属性和Excel中的中文列名的对应关系
     * @param out
     *            导出流
     * @throws Exception
     * @MethodName : listToExcel
     * @Description : 导出Excel（可以导出到本地文件系统，也可以导出到浏览器，工作表大小为2003支持的最大值）
     */
    public static <T> void listToExcel(List<T> list, LinkedHashMap<String, Integer> mergeFieldMap,
        LinkedHashMap<String, String> fieldMap, String sheetName, OutputStream out) throws Exception {

        listToExcel(list, mergeFieldMap, fieldMap, sheetName, 65535, out);

    }

    /**
     * 通过注解映射excel到list
     * 
     * @param result
     *            list结果
     * @param wb
     *            待映射的excel
     * @param clz
     *            映射对象
     * @param generateApplicationId
     * @param prefix
     */
    public static void excelToList(ExcelList result, org.apache.poi.ss.usermodel.Workbook wb, Class clz,
        boolean generateApplicationId, TypeEnum.submitType prefix) throws Exception {
        List<Field> fields = Arrays.asList(clz.getDeclaredFields());
        org.apache.poi.ss.usermodel.Sheet sheet = wb.getSheetAt(0);
        Row first = sheet.getRow(sheet.getFirstRowNum());
        int start = first.getFirstCellNum();
        int end = first.getLastCellNum();
        Map<String, Integer> filedIndex = Maps.newHashMap();
        // 处理标题对应的位置
        for (int i = start; i < end; i++) {
            first.getCell(i).setCellType(HSSFCell.CELL_TYPE_STRING);
            filedIndex.put(first.getCell(i).getStringCellValue(), i);
        }

        for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            Object o = clz.newInstance();
            String applicationId = prefix.getValue() + ApplicationIdGenerator.generate(new Date());
            fields.forEach(field -> {
                ExcelTitleMap excelTitleMap = field.getAnnotation(ExcelTitleMap.class);
                field.setAccessible(true);
                ApplicationId appId = field.getAnnotation(ApplicationId.class);
                if (appId != null && generateApplicationId) {
                    try {
                        field.set(o, applicationId);
                    } catch (IllegalAccessException e) {
                        logger.error("{}", e);
                    }
                }
                if (excelTitleMap != null && filedIndex.get(excelTitleMap.title()) == null) {
                    throw new DustBusinessException("上传模板与验证模板不一致");
                }
                if (excelTitleMap != null && filedIndex.get(excelTitleMap.title()) != null
                    && (row.getCell(filedIndex.get(excelTitleMap.title())) == null
                        || row.getCell(filedIndex.get(excelTitleMap.title())).toString().trim().equals(""))) {
                    throw new DustBusinessException("errors.validate.is.blank");
                }
                if (excelTitleMap != null && filedIndex.get(excelTitleMap.title()) != null
                    && row.getCell(filedIndex.get(excelTitleMap.title())) != null) {
                    try {
                        if (Number.class.isAssignableFrom(field.getType())) {
                            row.getCell(filedIndex.get(excelTitleMap.title())).setCellType(HSSFCell.CELL_TYPE_NUMERIC);
                            field.set(o, row.getCell(filedIndex.get(excelTitleMap.title())).getNumericCellValue());
                        } else {
                            row.getCell(filedIndex.get(excelTitleMap.title())).setCellType(HSSFCell.CELL_TYPE_STRING);
                            field.set(o, row.getCell(filedIndex.get(excelTitleMap.title())).getStringCellValue());
                        }
                    } catch (IllegalAccessException e) {
                        logger.error("{}", e);
                    }
                }
            });
            result.add(o);
        }
    }

    public static <T> ExcelData getExcelDataByAnnotation(List<T> data, Class clz) {
        Annotation clzAnnotation = clz.getAnnotation(Excel.class);
        ExcelData excelData = new ExcelData();
        if (clzAnnotation == null) {
            throw new DustBusinessException("找不到类注解！");
        }
        Excel excel = (Excel)clzAnnotation;
        String fileName = DateUtils.parseDate(new Date(), DateUtils.YYYYMMDDHHMMSS);

        if (StringUtil.isNoneBlank(excel.fileName())) {
            fileName = excel.fileName();
        }

        excelData.setFileName(fileName);
        excelData.setSheetName(excel.sheetName());

        List<Field> fields = Arrays.asList(clz.getDeclaredFields());
        LinkedHashMap<String, String> fieldMap = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> mergeFieldMap = new LinkedHashMap<>();
        HashMap<String, HashMap<Object, Object>> valuesMap = new HashMap<>(fields.size());
        fields.forEach(field -> {
            ExcelTitleMap excelTitleMap = field.getAnnotation(ExcelTitleMap.class);
            ExcelTitleGroup group = field.getAnnotation(ExcelTitleGroup.class);
            if (excelTitleMap != null) {
                fieldMap.put(field.getName(), excelTitleMap.title());
                ExcelValueMap excelValueMap = field.getAnnotation(ExcelValueMap.class);
                if (excelValueMap != null) {
                    JSONArray array = JSONArray.parseArray(excelValueMap.arrayJsonString());
                    HashMap<Object, Object> values = new HashMap<>();
                    array.forEach(valueMap -> {
                        values.putAll((Map)valueMap);
                    });
                    valuesMap.put(field.getName(), values);
                }
                ExcelValueEnum excelValueEnum = field.getAnnotation(ExcelValueEnum.class);
                if (excelValueEnum != null) {
                    try {
                        Method enumValues = excelValueEnum.enumClass().getMethod("values");
                        Method enumKey = excelValueEnum.enumClass().getMethod(excelValueEnum.keyMethod());
                        Method enumValue = excelValueEnum.enumClass().getMethod(excelValueEnum.valueMethod());
                        Object[] arr = (Object[])enumValues.invoke(null, null);
                        HashMap<Object, Object> values = new HashMap<>();
                        for (Object o : arr) {
                            values.put(enumKey.invoke(o, null), enumValue.invoke(o, null));
                        }
                        JSONArray array = JSONArray.parseArray(excelValueEnum.arrayJsonString());
                        array.forEach(valueMap -> {
                            values.putAll((Map)valueMap);
                        });
                        valuesMap.put(field.getName(), values);
                    } catch (Exception e) {
                        logger.error("{}", e);
                    }
                }
            } else {
                fieldMap.put(field.getName(), "");
            }
            if (group != null) {
                if (mergeFieldMap.get(group.title()) != null) {
                    mergeFieldMap.put(group.title(), mergeFieldMap.get(group.title()) + 1);
                } else {
                    mergeFieldMap.put(group.title(), 1);
                }
            }
        });

        data = data.stream().map(ele -> {
            List<Field> fs = Arrays.asList(clz.getDeclaredFields());
            fs.forEach(f -> {
                if (valuesMap.get(f.getName()) != null) {
                    try {
                        f.setAccessible(true);
                        if (valuesMap.get(f.getName()).get(f.get(ele)) != null) {
                            f.set(ele, valuesMap.get(f.getName()).get(f.get(ele)));
                        }
                    } catch (IllegalAccessException e) {
                        logger.error("{}", e);
                    }
                }
            });
            return ele;
        }).collect(Collectors.toList());

        excelData.setMergeFieldMap(mergeFieldMap);
        excelData.setFieldMap(fieldMap);
        excelData.setData(data);

        return excelData;
    }

    /**
     * 通过注解映射来导出excel
     * 
     * @param list
     * @param response
     */
    public static <T> void listToExcelByAnnotation(List<T> list, HttpServletResponse response, Class clz)
        throws Exception {

        ExcelData excelData = getExcelDataByAnnotation(list, clz);

        if (excelData.getData() == null || excelData.getData().size() == 0) {
            headToExcelByAnnotation(response, excelData.getMergeFieldMap(), excelData.getFieldMap(), clz);
            return;
        }

        // 设置response头信息
        setResponseExcelHeader(response, excelData.getFileName());

        // 创建工作簿并发送到浏览器
        try (OutputStream out = response.getOutputStream()) {
            listToExcel(excelData.getData(), excelData.getMergeFieldMap(), excelData.getFieldMap(), excelData.getSheetName(),
                65535, out);
        } catch (Exception e) {
            e.printStackTrace();
            // 如果是Exception，则直接抛出
            if (e instanceof Exception) {
                throw e;
                // 否则将其它异常包装成Exception再抛出
            } else {
                throw new Exception("导出Excel失败");
            }
        }
    }

    public static void headToExcelByAnnotation(HttpServletResponse response,
        LinkedHashMap<String, Integer> mergeFieldMap, LinkedHashMap<String, String> fieldMap, Class clz)
        throws Exception {
        Annotation clzAnnotation = clz.getAnnotation(Excel.class);
        if (clzAnnotation == null) {
            throw new DustBusinessException("找不到类注解！");
        }
        Excel excel = (Excel)clzAnnotation;
        String fileName = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
        if (StringUtil.isNoneBlank(excel.fileName())) {
            fileName = excel.fileName();
        }
        String sheetName = excel.sheetName();

        setResponseExcelHeader(response, fileName);

        // 创建工作簿并发送到浏览器
        try (OutputStream out = response.getOutputStream()) {
            headToExcel(mergeFieldMap, fieldMap, sheetName, 65535, out);
        } catch (Exception e) {
            e.printStackTrace();
            // 如果是Exception，则直接抛出
            if (e instanceof Exception) {
                throw e;
                // 否则将其它异常包装成Exception再抛出
            } else {
                throw new Exception("导出Excel失败");
            }
        }
    }

    /**
     * @param list
     *            数据源
     * @param fieldMap
     *            类的英文属性和Excel中的中文列名的对应关系
     * @param sheetSize
     *            每个工作表中记录的最大个数
     * @param response
     *            使用response可以导出到浏览器
     * @throws Exception
     * @MethodName : listToExcel
     * @Description : 导出Excel（导出到浏览器，可以自定义工作表的大小）
     */
    public static <T> void listToExcel(List<T> list, LinkedHashMap<String, Integer> mergeFieldMap,
        LinkedHashMap<String, String> fieldMap, String sheetName, int sheetSize, HttpServletResponse response)
        throws Exception {
        String fileName = sheetName;
        // 设置默认文件名为当前时间：年月日时分秒
        if (StringUtils.isEmpty(sheetName)) {
            fileName = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
            sheetName = fileName;
        }

        // 设置response头信息
        setResponseExcelHeader(response, fileName);

        // 创建工作簿并发送到浏览器
        try (OutputStream out = response.getOutputStream()) {
            listToExcel(list, mergeFieldMap, fieldMap, sheetName, sheetSize, out);
        } catch (Throwable e) {
            logger.info("导出Excel失败,{}", e);
            throw new DustBusinessException("导出Excel失败");
        }
    }

    /**
     * @param list
     *            数据源
     * @param fieldMap
     *            类的英文属性和Excel中的中文列名的对应关系
     * @param response
     *            使用response可以导出到浏览器
     * @throws Exception
     * @MethodName : listToExcel
     * @Description : 导出Excel（导出到浏览器，工作表的大小是2003支持的最大值）
     */
    public static <T> void listToExcel(List<T> list, LinkedHashMap<String, Integer> mergeFieldMap,
        LinkedHashMap<String, String> fieldMap, String sheetName, HttpServletResponse response) throws Exception {

        listToExcel(list, mergeFieldMap, fieldMap, sheetName, 65535, response);
    }

    public static Map<String, Object> excelToMap(InputStream in, List<String> list) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        try {
            // 根据Excel数据源创建WorkBook
            Workbook wb = Workbook.getWorkbook(in);
            // 获取工作表第一个
            Sheet sheet = wb.getSheet(0);
            if (Objects.isNull(sheet)) {
                return map;
            }
            // 读取第二列的数据
            for (int i = 1; i < sheet.getRows(); i++) {
                if (list.contains(sheet.getCell(0, i).getContents().trim())) {
                    continue;
                }
                map.put(sheet.getCell(0, i).getContents(),
                    sheet.getCell(1, i).getContents().trim());
            }
            System.out.println(map.size());
        } catch (Exception e) {
            e.printStackTrace();
            // 如果是Exception，则直接抛出
            if (e instanceof Exception) {
                throw e;
                // 否则将其它异常包装成Exception再抛出
            } else {
                e.printStackTrace();
                throw new Exception("导入Excel失败");
            }
        }
        return map;
    }

    /**
     * @param in
     *            ：承载着Excel的输入流
     * @param entityClass
     *            ：List中对象的类型（Excel中的每一行都要转化为该类型的对象）
     * @param fieldMap
     *            ：Excel中的中文列头和类的英文属性的对应关系Map
     * @param uniqueFields
     *            ：指定业务主键组合（即复合主键），这些列的组合不能重复
     * @return List
     * @throws Exception
     * @MethodName : excelToList
     * @Description : 将Excel转化为List
     */
    public static <T> Map<String, Object> excelToList(InputStream in, String sheetName, Class<T> entityClass,
        LinkedHashMap<String, String> fieldMap, String[] uniqueFields) throws Exception {
        // 定义要返回的list
        List<T> resultList = new ArrayList<T>();
        Map<String, Object> map = new HashMap<String, Object>();
        Map<String, String> errorMap = new HashMap<String, String>();
        try {
            // 根据Excel数据源创建WorkBook
            Workbook wb = Workbook.getWorkbook(in);
            // 获取工作表
            Sheet sheet = wb.getSheet(sheetName);
            if (Objects.isNull(sheet)) {
                return map;
            }

            if (sheet.getRows() > 5000) {
                errorMap.put("error", "本次共导入{}条数据，已超出5000条上限。为保证系统稳定，请重新导入。");
                return map;
            }

            // 获取工作表的有效行数
            int realRows = 0;
            for (int i = 0; i < sheet.getRows(); i++) {
                int nullCols = 0;
                for (int j = 0; j < sheet.getColumns(); j++) {
                    Cell currentCell = sheet.getCell(j, i);
                    if (Objects.isNull(currentCell) || "".equals(currentCell.getContents())) {
                        nullCols++;
                    }
                }
                if (nullCols == sheet.getColumns()) {
                    break;
                } else {
                    realRows++;
                }
            }

            // 如果Excel中没有数据则提示错误
            if (sheet.getRows() > 5000) {
                // return ;
                errorMap.put("error", "本次共导入{}条数据，已超出5000条上限。为保证系统稳定，请重新导入。");
            }

            Cell[] columnName = sheet.getRow(0);
            String[] excelFieldNames = new String[columnName.length];

            // 获取Excel中的列名
            for (int i = 0; i < columnName.length; i++) {
                excelFieldNames[i] = columnName[i].getContents().trim();
            }

            // 判断需要的字段在Excel中是否都存在
            boolean isExist = true;
            List<String> excelFieldList = Arrays.asList(excelFieldNames);
            for (String cnName : fieldMap.keySet()) {
                if (!excelFieldList.contains(cnName)) {
                    isExist = false;
                    break;
                }
            }

            // 如果有列名不存在，则抛出异常，提示错误
            if (!isExist) {
                throw new Exception("Excel中缺少必要的字段，或字段名称有误");
            }

            // 将列名和列号放入Map中,这样通过列名就可以拿到列号
            LinkedHashMap<String, Integer> colMap = new LinkedHashMap<String, Integer>();
            for (int i = 0; i < excelFieldNames.length; i++) {
                colMap.put(excelFieldNames[i], columnName[i].getColumn());
            }

            // 判断是否有重复行
            // 1.获取uniqueFields指定的列
            Cell[][] uniqueCells = new Cell[uniqueFields.length][];
            for (int i = 0; i < uniqueFields.length; i++) {
                int col = colMap.get(uniqueFields[i]);
                uniqueCells[i] = sheet.getColumn(col);
            }

            // 2.从指定列中寻找重复行
            for (int i = 1; i < realRows; i++) {
                int nullCols = 0;
                for (int j = 0; j < uniqueFields.length; j++) {
                    String currentContent = uniqueCells[j][i].getContents();
                    Cell sameCell =
                        sheet.findCell(currentContent, uniqueCells[j][i].getColumn(), uniqueCells[j][i].getRow() + 1,
                            uniqueCells[j][i].getColumn(), uniqueCells[j][realRows - 1].getRow(), true);
                    if (sameCell != null) {
                        nullCols++;
                    }
                }

                if (nullCols == uniqueFields.length) {
                    throw new Exception("Excel中有重复行，请检查");
                }
            }

            // 将sheet转换为list
            for (int i = 1; i < realRows; i++) {
                // 新建要转换的对象
                T entity = entityClass.newInstance();

                // 给对象中的字段赋值
                for (Entry<String, String> entry : fieldMap.entrySet()) {
                    // 获取中文字段名
                    String cnNormalName = entry.getKey();
                    // 获取英文字段名
                    String enNormalName = entry.getValue();
                    // 根据中文字段名获取列号
                    int col = colMap.get(cnNormalName);

                    // 获取当前单元格中的内容
                    String content = sheet.getCell(col, i).getContents().trim();

                    // 给对象赋值
                    setFieldValueByName(enNormalName, content, entity);
                }

                resultList.add(entity);
            }
            map.put("msg", errorMap);
            map.put("val", resultList);
        } catch (Exception e) {
            e.printStackTrace();
            // 如果是Exception，则直接抛出
            if (e instanceof Exception) {
                throw e;

                // 否则将其它异常包装成Exception再抛出
            } else {
                e.printStackTrace();
                throw new Exception("导入Excel失败");
            }
        }
        return map;
    }

    public static <T> Map<String, Object> excelToList(InputStream in, String sheetName, Class<T> entityClass,
        LinkedHashMap<String, String> fieldMap, int maxRows) throws Exception {
        // 定义要返回的list
        List<T> resultList = new ArrayList<T>();
        Map<String, Object> map = new HashMap<String, Object>();
        Map<String, String> errorMap = new HashMap<String, String>();
        try {
            // 根据Excel数据源创建WorkBook
            org.apache.poi.ss.usermodel.Workbook wb = WorkbookFactory.create(in);
            // 获取工作表
            org.apache.poi.ss.usermodel.Sheet sheet = wb.getSheet(sheetName);
            if (Objects.isNull(sheet)) {
                errorMap.put("error", "导入的Excel工作表sheet名称不对");
                map.put("msg", errorMap);
                map.put("val", resultList);
                return map;
            }

            // 获取工作表的有效行数(可能有一行全为空)
            int realRows = 0;
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                int nullCols = 0;
                Row row = sheet.getRow(i);
                for (int j = 0; j < row.getLastCellNum(); j++) {
                    org.apache.poi.ss.usermodel.Cell currentCell = row.getCell(j);
                    currentCell.setCellType(org.apache.poi.ss.usermodel.Cell.CELL_TYPE_STRING);
                    if (Objects.isNull(currentCell) || "".equals(currentCell.getStringCellValue())) {
                        nullCols++;
                    }
                }
                if (nullCols == row.getLastCellNum()) {
                    break;
                } else {
                    realRows++;
                }
            }

            // 如果Excel中没有数据则提示错误，需要删除表头行
            if ((realRows - 1) > maxRows) {
                errorMap.put("error", "本次共导入" + (realRows - 1) + "条数据，已超出" + maxRows + "条上限。为保证系统稳定，请重新导入。");
                map.put("msg", errorMap);
                map.put("val", resultList);
                return map;
            }

            if (realRows <= 1) {
                errorMap.put("error", "导入的数据至少有一条");
                map.put("msg", errorMap);
                map.put("val", resultList);
                return map;
            }

            Row titleRow = sheet.getRow(0);
            String[] excelFieldNames = new String[titleRow.getLastCellNum()];

            // 获取Excel中的列名
            for (int i = 0; i < titleRow.getLastCellNum(); i++) {
                titleRow.getCell(i).setCellType(org.apache.poi.ss.usermodel.Cell.CELL_TYPE_STRING);
                excelFieldNames[i] = titleRow.getCell(i).getStringCellValue().trim();
            }

            // 判断需要的字段在Excel中是否都存在
            boolean isExist = true;
            List<String> excelFieldList = Arrays.asList(excelFieldNames);
            for (String cnName : fieldMap.keySet()) {
                if (!excelFieldList.contains(cnName)) {
                    isExist = false;
                    break;
                }
            }

            // 如果有列名不存在，则抛出异常，提示错误
            if (!isExist) {
                throw new Exception("Excel中缺少必要的字段，或表头名称有误");
            }

            // 将列名和列号放入Map中,这样通过列名就可以拿到列号
            LinkedHashMap<String, Integer> colMap = new LinkedHashMap<String, Integer>();
            for (int i = 0; i < excelFieldNames.length; i++) {
                colMap.put(excelFieldNames[i], titleRow.getCell(i).getColumnIndex());
            }

            // 将sheet转换为list

            // 计算分成多少页
            int totalNum = sheet.getLastRowNum();
            int pageNum = totalNum%PAGE_SIZE == 0? totalNum/PAGE_SIZE : (totalNum/PAGE_SIZE + 1);

            List<ExcelReadParamDTO<T>> excelReadParams = new ArrayList<>(pageNum);

            for(int i=0;i<pageNum;i++){
                int start = i*PAGE_SIZE;
                int end = (i+1)*PAGE_SIZE >= totalNum?totalNum:((i+1)*PAGE_SIZE);

                ExcelReadParamDTO excelReadParam = new ExcelReadParamDTO();
                excelReadParam.setStart(start);
                excelReadParam.setEnd(end);
                excelReadParam.setColMap(colMap);
                excelReadParam.setEntityClass(entityClass);
                excelReadParam.setFieldMap(fieldMap);
                excelReadParam.setSheet(sheet);

                excelReadParams.add(excelReadParam);
            }

            List<List<T>> results = AsyncContextUtil.getExecutorEngine().execute(excelReadParams, new ExcelReadContentExecuteUnit<T>());

            results.stream().forEach(contentList->{
                resultList.addAll(contentList);
            });

            map.put("msg", errorMap);
            map.put("val", resultList);
        } catch (Throwable e) {
            logger.warn("导入Excel失败,{}", e);
            throw new DustBusinessException("导入Excel失败");

        }
        return map;
    }

    /**
     * 读取Excel内容转化为List集合
     * 
     * @param excelReadParam
     *            读取参数DTO
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T> List<T> excelContentConvertList(ExcelReadParamDTO excelReadParam) throws Exception {
        int start = excelReadParam.getStart();
        int end = excelReadParam.getEnd();
        Class<T> entityClass = excelReadParam.getEntityClass();
        LinkedHashMap<String, Integer> colMap = excelReadParam.getColMap();
        org.apache.poi.ss.usermodel.Sheet sheet = excelReadParam.getSheet();
        LinkedHashMap<String, String> fieldMap = excelReadParam.getFieldMap();
        List<T> resultList = new ArrayList<T>();
        for (int i = start+1; i <= end; i++) {
            T entity = entityClass.newInstance();
            Row contentRow = sheet.getRow(i);
            if (Objects.nonNull(contentRow)) {
                if (entity instanceof Map) {
                    Map<String, Object> maps = (Map)entity;
                    // 新建要转换的对象 给对象中的字段赋值
                    for (Entry<String, String> entry : fieldMap.entrySet()) {
                        // 获取中文字段名
                        String cnNormalName = entry.getKey();
                        // 获取英文字段名
                        String enNormalName = entry.getValue();
                        // 根据中文字段名获取列号
                        int col = colMap.get(cnNormalName);

                        // 获取当前单元格中的内容
                        contentRow.getCell(col).setCellType(org.apache.poi.ss.usermodel.Cell.CELL_TYPE_STRING);
                        String content = contentRow.getCell(col).getStringCellValue().trim();
                        maps.put(enNormalName, content);
                    }
                } else {
                    // 新建要转换的对象 给对象中的字段赋值
                    for (Entry<String, String> entry : fieldMap.entrySet()) {
                        // 获取中文字段名
                        String cnNormalName = entry.getKey();
                        // 获取英文字段名
                        String enNormalName = entry.getValue();
                        // 根据中文字段名获取列号
                        int col = colMap.get(cnNormalName);

                        // 获取当前单元格中的内容
                        contentRow.getCell(col).setCellType(org.apache.poi.ss.usermodel.Cell.CELL_TYPE_STRING);
                        String content = contentRow.getCell(col).getStringCellValue().trim();

                        // 给对象赋值
                        setFieldValueByName(enNormalName, content, entity);
                    }
                }
                resultList.add(entity);
            }
        }

        return resultList;
    }

    /**
     * 将excel转成list(.xls格式和.xlsx格式)</br>
     * list下标对应一行数据。map的key为excel的列的下标
     *
     * @return
     * @throws IOException
     */

    public static List<String> readExcel(InputStream is) throws IOException {
        List<Map<Integer, Object>> tmp = readExcel(is, ExcelUtils.Postfix.XLS);
        List<String> result = new ArrayList<>();
        if (tmp != null) {
            tmp.forEach(map -> {
                Collection<Object> arr = map.values();
                String str = Joiner.on(",").join(arr);
                String tempStr = Joiner.on(" ").join(arr);
                if (tempStr != null && StringUtils.isNotBlank(tempStr.trim())) {
                    result.add(str);
                }
            });
        }

        return result;
    }

    public static List<Map<Integer, Object>> readExcel(InputStream is, ExcelUtils.Postfix postfix) throws IOException {
        try {
            if (ExcelUtils.Postfix.XLS.equals(postfix)) {
                return read(WorkbookFactory.create(is));
            } else if (ExcelUtils.Postfix.XLSX.equals(postfix)) {
                return read(WorkbookFactory.create(is));
            }
        } catch (Exception e) {
            logger.error("{}", e);
        }
        return null;
    }

    private static List<Map<Integer, Object>> read(org.apache.poi.ss.usermodel.Workbook workbook) throws IOException {
        List<Map<Integer, Object>> list = new ArrayList<>();
        org.apache.poi.ss.usermodel.Sheet hssfSheet = workbook.getSheetAt(0);
        int rowstart = hssfSheet.getFirstRowNum();
        int rowEnd = hssfSheet.getLastRowNum();
        for (int i = rowstart; i <= rowEnd; i++) {
            Map<Integer, Object> rowMap = new HashMap<>();
            Row row = hssfSheet.getRow(i);
            if (null == row) {
                continue;
            }
            int cellStart = row.getFirstCellNum();
            int cellEnd = row.getLastCellNum();

            for (int k = cellStart; k <= cellEnd; k++) {
                org.apache.poi.ss.usermodel.Cell cell = row.getCell(k);
                if (null == cell) {
                    continue;
                }

                rowMap.put(k, getValue(cell));
            }
            list.add(rowMap);
        }
        return list;
    }

    private static Object getValue(org.apache.poi.ss.usermodel.Cell cell) {
        switch (cell.getCellType()) {
            case HSSFCell.CELL_TYPE_NUMERIC: // 数字
                // 上传时格式是指数型的数字处理
                DecimalFormat df = new DecimalFormat("0");
                return df.format(cell.getNumericCellValue());
            case HSSFCell.CELL_TYPE_STRING: // 字符串
                return cell.getStringCellValue().trim();
            case HSSFCell.CELL_TYPE_BOOLEAN: // Boolean
                return cell.getBooleanCellValue();
            case HSSFCell.CELL_TYPE_FORMULA: // 公式
                return cell.getCellFormula();
            case HSSFCell.CELL_TYPE_BLANK: // 空值
                return "";
            case HSSFCell.CELL_TYPE_ERROR: // 故障
                return "";
            default:
                return "";
        }
    }

    public enum Postfix {
        XLS(".xls"), XLSX(".xlsx");

        private String value;

        Postfix(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static String getPostfix(String value) {
            for (ExcelUtils.Postfix postfix : values()) {
                if (postfix.value.equals(value)) {
                    return postfix.getValue();
                }
            }
            return null;
        }
    }

    public static void writeExcel(HSSFWorkbook book, String path) throws IOException {
        File file = new File(path);
        FileOutputStream fos = new FileOutputStream(file);
        book.write(fos);
    }

    /* <-------------------------辅助的私有方法-----------------------------------------------> */

    /**
     * @param fieldName
     *            字段名
     * @param o
     *            对象
     * @return 字段值
     * @MethodName : getFieldValueByName
     * @Description : 根据字段名获取字段值
     */
    private static Object getFieldValueByName(String fieldName, Object o) throws Exception {

        Object value = null;
        Field field = getFieldByName(fieldName, o.getClass());

        if (field != null) {
            field.setAccessible(true);
            value = field.get(o);
            if (field.getType() == Date.class) {
                value = DateUtils.parseDate((Date)value, DateUtils.DATE_TIME_FORMAT);
            }
        } else {
            throw new Exception(o.getClass().getSimpleName() + "类不存在字段名 " + fieldName);
        }

        return value;
    }

    /**
     * @param fieldName
     *            字段名
     * @param clazz
     *            包含该字段的类
     * @return 字段
     * @MethodName : getFieldByName
     * @Description : 根据字段名获取字段
     */
    private static Field getFieldByName(String fieldName, Class<?> clazz) {
        // 拿到本类的所有字段
        Field[] selfFields = clazz.getDeclaredFields();

        // 如果本类中存在该字段，则返回
        for (Field field : selfFields) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }

        // 否则，查看父类中是否存在此字段，如果有则返回
        Class<?> superClazz = clazz.getSuperclass();
        if (superClazz != null && superClazz != Object.class) {
            return getFieldByName(fieldName, superClazz);
        }

        // 如果本类和父类都没有，则返回空
        return null;
    }

    /**
     * @param fieldNameSequence
     *            带路径的属性名或简单属性名
     * @param o
     *            对象
     * @return 属性值
     * @throws Exception
     * @MethodName : getFieldValueByNameSequence
     * @Description : 根据带路径或不带路径的属性名获取属性值 即接受简单属性名，如userName等，又接受带路径的属性名，如student.department.name等
     */
    private static Object getFieldValueByNameSequence(String fieldNameSequence, Object o) throws Exception {

        Object value = null;

        // 将fieldNameSequence进行拆分
        String[] attributes = fieldNameSequence.split("\\.");
        if (attributes.length == 1) {
            value = getFieldValueByName(fieldNameSequence, o);
        } else {
            // 根据属性名获取属性对象
            Object fieldObj = getFieldValueByName(attributes[0], o);
            String subFieldNameSequence = fieldNameSequence.substring(fieldNameSequence.indexOf(".") + 1);
            value = getFieldValueByNameSequence(subFieldNameSequence, fieldObj);
        }
        return value;

    }

    /**
     * @param fieldName
     *            字段名
     * @param fieldValue
     *            字段值
     * @param o
     *            对象
     * @MethodName : setFieldValueByName
     * @Description : 根据字段名给对象的字段赋值
     */
    private static void setFieldValueByName(String fieldName, Object fieldValue, Object o) throws Exception {

        Field field = getFieldByName(fieldName, o.getClass());
        if (field != null) {
            field.setAccessible(true);
            // 获取字段类型
            Class<?> fieldType = field.getType();

            // 根据字段类型给字段赋值
            if (String.class == fieldType) {
                field.set(o, String.valueOf(fieldValue));
            } else if ((Integer.TYPE == fieldType) || (Integer.class == fieldType)) {
                field.set(o, Integer.parseInt(fieldValue.toString()));
            } else if ((Long.TYPE == fieldType) || (Long.class == fieldType)) {
                field.set(o, Long.valueOf(fieldValue.toString()));
            } else if ((Float.TYPE == fieldType) || (Float.class == fieldType)) {
                field.set(o, Float.valueOf(fieldValue.toString()));
            } else if ((Short.TYPE == fieldType) || (Short.class == fieldType)) {
                field.set(o, Short.valueOf(fieldValue.toString()));
            } else if ((Double.TYPE == fieldType) || (Double.class == fieldType)) {
                field.set(o, Double.valueOf(fieldValue.toString()));
            } else if (Character.TYPE == fieldType) {
                if ((fieldValue != null) && (fieldValue.toString().length() > 0)) {
                    field.set(o, Character.valueOf(fieldValue.toString().charAt(0)));
                }
            } else if (Date.class == fieldType) {
                field.set(o, parseDate(fieldValue.toString()));
            } else {
                field.set(o, fieldValue);
            }
        } else {
            throw new Exception(o.getClass().getSimpleName() + "类不存在字段名 " + fieldName);
        }
    }

    /**
     * 日期型字符串转化为日期 格式 { "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss",
     * "yyyy/MM/dd HH:mm" }
     */
    public static Date parseDate(Object str) {
        if (str == null) {
            return null;
        }
        try {
            return DateUtils.parseDate(str.toString(), parsePatterns[0]);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @param ws
     * @MethodName : setColumnAutoSize
     * @Description : 设置工作表自动列宽和首行加粗
     */
    private static void setColumnAutoSize(XSSFSheet ws, int columnNum) {
        // 获取本列的最宽单元格的宽度
        for (int i = 0; i < columnNum; i++) {
            ws.autoSizeColumn(i);
        }
    }

    private static CellStyle cellStyle(XSSFSheet sheet) {
        XSSFCellStyle style = sheet.getWorkbook().createCellStyle();
        style.setAlignment(XSSFCellStyle.ALIGN_CENTER);
        style.setVerticalAlignment(XSSFCellStyle.VERTICAL_CENTER);
        return style;
    }

    /**
     * 设置列式表头
     * 
     * @param sheet
     * @param columnMap
     * @param excelMeta
     */
    private static void fillColumnHeader(XSSFSheet sheet, LinkedHashMap<String, String> columnMap,
        ExcelMeta excelMeta) {

        if (MapUtils.isNotEmpty(columnMap)) {
            columnMap.forEach((key, value) -> {
                XSSFRow row = sheet.createRow(excelMeta.getRowNo());

                XSSFCell keyCell = row.createCell(0);
                keyCell.setCellValue(new XSSFRichTextString(key));
                keyCell.setCellStyle(cellStyle(sheet));
                sheet.setColumnWidth(0, 30 * 150);

                XSSFCell valueCell = row.createCell(1);
                valueCell.setCellValue(new XSSFRichTextString(value));
                valueCell.setCellStyle(cellStyle(sheet));
                sheet.setColumnWidth(1, 30 * 150);

                excelMeta.setRowNo(excelMeta.getRowNo() + 1);
            });
        }
    }

    /**
     * 创建合并列的表头
     * 
     * @param sheet
     * @param mergeFieldMap
     * @return
     */
    private static void fillMergeFiledHeader(XSSFSheet sheet, LinkedHashMap<String, Integer> mergeFieldMap,
        ExcelMeta excelMeta) {
        int rowNo = excelMeta.getRowNo();
        // 填充表头
        if (!MapUtils.isEmpty(mergeFieldMap)) {
            int columnNum = 0;
            XSSFRow row = sheet.createRow(rowNo);
            for (Entry<String, Integer> entry : mergeFieldMap.entrySet()) {
                XSSFCell cell = row.createCell(columnNum);
                XSSFRichTextString val = new XSSFRichTextString(entry.getKey());
                cell.setCellValue(val);
                cell.setCellStyle(cellStyle(sheet));
                sheet.addMergedRegion(new CellRangeAddress(rowNo, rowNo, columnNum, columnNum + entry.getValue() - 1));
                sheet.setColumnWidth(columnNum, 30 * 150);
                columnNum = columnNum + entry.getValue();
            }
            rowNo++;
        }
        excelMeta.setRowNo(rowNo);
    }

    /**
     * 创建内容对应的列名的表头
     * 
     * @param sheet
     * @param fieldMap
     * @return
     */
    private static void fillFiledHeader(XSSFSheet sheet, LinkedHashMap<String, String> fieldMap,
        ExcelMeta excelMeta) {

        int rowNo = excelMeta.getRowNo();
        String[] enFields = excelMeta.getEnFields();
        String[] cnFields = excelMeta.getCnFields();

        // 二级表头
        int count = 0;
        for (Entry<String, String> entry : fieldMap.entrySet()) {
            enFields[count] = entry.getKey();
            cnFields[count] = entry.getValue();
            count++;
        }

        XSSFRow row = sheet.createRow(rowNo);
        for (int i = 0; i < cnFields.length; i++) {
            XSSFCell cell = row.createCell(i);
            XSSFRichTextString val = new XSSFRichTextString(cnFields[i]);
            cell.setCellValue(val);
            cell.setCellStyle(cellStyle(sheet));
            sheet.setColumnWidth(i, 30 * 150);
        }
        rowNo++;
        excelMeta.setRowNo(rowNo);
    }

    /**
     * 设置表头
     * 
     * @param sheet
     * @param mergeFieldMap
     * @param fieldMap
     * @throws Exception
     * @return
     */
    private static ExcelMeta fillSheetHeader(XSSFSheet sheet, LinkedHashMap<String, String> columnMap,
        LinkedHashMap<String, Integer> mergeFieldMap, LinkedHashMap<String, String> fieldMap) throws Exception {

        ExcelMeta excelMeta = new ExcelMeta();

        // 定义存放英文字段名和中文字段名的数组
        String[] enFields = new String[fieldMap.size()];
        String[] cnFields = new String[fieldMap.size()];
        int rowNo = 0;

        excelMeta.setCnFields(cnFields);
        excelMeta.setEnFields(enFields);
        excelMeta.setRowNo(rowNo);

        fillColumnHeader(sheet, columnMap, excelMeta);
        fillMergeFiledHeader(sheet, mergeFieldMap, excelMeta);
        fillFiledHeader(sheet, fieldMap, excelMeta);

        // 设置自动列宽
        // setColumnAutoSize(sheet, cnFields.length);

        return excelMeta;
    }

    /**
     * @param sheet
     *            工作表
     * @param list
     *            数据源
     * @param firstIndex
     *            开始索引
     * @param lastIndex
     *            结束索引
     * @MethodName : fillSheet
     * @Description : 向工作表中填充数据
     */
    private static <T> void fillSheetContents(XSSFSheet sheet, List<T> list, int firstIndex, int lastIndex,
        ExcelMeta excelMeta) throws Exception {

        int rowNo = excelMeta.getRowNo();
        String[] enFields = excelMeta.getEnFields();

        // 填充内容
        for (int index = firstIndex; index <= lastIndex; index++) {
            // 获取单个对象
            T item = list.get(index);
            XSSFRow row = sheet.createRow(rowNo);
            for (int i = 0; i < enFields.length; i++) {
                String fieldValue = "";
                // 如果时map,则通过key-value的形式取出
                if (item instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>)item;
                    Object objValue = map.get(enFields[i]);
                    fieldValue = objValue == null ? "" : objValue.toString();
                } else {
                    Object objValue = getFieldValueByNameSequence(enFields[i], item);
                    fieldValue = objValue == null ? "" : objValue.toString();
                }
                XSSFCell cell = row.createCell(i);
                cell.setCellValue(fieldValue);
                cell.setCellStyle(cellStyle(sheet));
                sheet.setColumnWidth(i, 30 * 150);
            }

            rowNo++;
        }

        excelMeta.setRowNo(rowNo);
        // 设置自动列宽
        // setColumnAutoSize(sheet, enFields.length);
    }

    // 解析对象,每个属性是一个Node
    private static List<ReportNode> parseObject(Object obj) throws Exception {

        if (!obj.getClass().isAnnotationPresent(ExcelType.class)) {
            logger.info("导出数据失败,找不到节点@ExcelType");
            throw new DustBusinessException("导出数据失败");
        }

        List<ReportNode> params = Lists.newArrayList();

        // 反射获取对象中的属性
        BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
        // 获取所有属性
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();

        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            String propertyName = propertyDescriptor.getName();
            if (!propertyName.equals("class")) {
                // 判断这个对象的每个属性
                Field field = ReflectionUtils.findField(obj.getClass(), propertyName);
                // 属性是否含有@ExcelField注解
                if (field.isAnnotationPresent(ExcelField.class)) {
                    Method readMethod = propertyDescriptor.getReadMethod();
                    ExcelField excelField = field.getAnnotation(ExcelField.class);
                    String name = excelField.name();
                    // 获取这个属性的值
                    Object ob = readMethod.invoke(obj);
                    if (Objects.nonNull(ob)) {
                        // ob是否为对象? Map? List? 其他?
                        ReportNode subNode = new ReportNode();
                        if (ExcelFieldTypeEnum.TYPE_FAIL_REASON.equals(excelField.type())) {// 单独处理失败原因
                            subNode.setName(null);
                            subNode.setOrder(excelField.order());
                            subNode.setValue(String.valueOf(String.valueOf(ob)));// 将原因值设置给name字段
                        } else {
                            subNode.setName(name);
                            subNode.setOrder(excelField.order());
                            subNode.setValue(String.valueOf(String.valueOf(ob)));
                        }
                        subNode.setAppendCell(excelField.appendCell());
                        params.add(subNode);
                    }
                }
            }
        }

        // 进行排序
        params.sort(Comparator.comparingInt(ReportNode::getOrder));

        return params;
    }

    // map里每个key-value都是一项
    private static List<ReportNode> parseMap(Object obj) throws Exception {

        // 子项
        List<ReportNode> params = Lists.newArrayList();

        Map<String, Object> map = (Map<String, Object>)obj;
        for (String key : map.keySet()) {
            Object value = map.get(key);
            if (Objects.nonNull(value)) {
                // 判断value是不是含有ExcelType注解,有表示是对象
                ReportNode subNode = new ReportNode();
                subNode.setName(key);
                subNode.setValue(String.valueOf(value));
                params.add(subNode);

            }
        }
        return params;
    }

    public static void main(String[] args) throws Exception {
        String json =
            "{\"modelId\":\"148\",\"modelName\":\"模块6\",\"callId\":\"SL19042016515783411710606\",\"inputInfo\":{\"base64Str\":\"\",\"identificationType\":\"id_handheld_proof\",\"liaison3\":\"\",\"liaison2\":\"\",\"mobile\":\"13824411469\",\"liaison1\":\"13824411467\"},\"baseInfo\":{\"gender\":\"\",\"birthday\":\"\",\"age\":0,\"queryDate\":\"2019-04-20\"},\"resultMaps\":{\"handheldImage\":{\"score\":null,\"failReason\":\"参数不足\"},\"liaisonCheck\":{\"failReason\":null,\"liaison1\":\"67\",\"liaison2\":null,\"liaison3\":null}}}";
        VerifyReportVo verifyReportVo = JSON.parseObject(json, VerifyReportVo.class);
        LinkedHashMap<String, Object> linkedHashMap = verifyReportVo.getResultMaps();
        LinkedHashMap<String, Object> linkedHashMap1 = Maps.newLinkedHashMap();
        Map<String, ResultParse> stringResultParseMap = Maps.newHashMap();
        stringResultParseMap.put("liaisonCheck", new ModelLiaisonCheckVoParse());
        // stringResultParseMap.put("riskCheckLabel",new ModelRiskCheckLabelVoParse());
        // stringResultParseMap.put("bankcardCheck",new ModelBankcardCheckVoParse());
        // stringResultParseMap.put("degreeCheck",new ModelDegreeCheckVoParse());
        stringResultParseMap.put("handheldImage", new ModelHandheldImageVoParse());
        for (String key : linkedHashMap.keySet()) {
            ResultParse resultParse = stringResultParseMap.get(key);
            Object o = linkedHashMap.get(key);
            Object obj = resultParse.parse((Map<String, Object>)o);
            linkedHashMap1.put(key, obj);
        }
        verifyReportVo.setResultMaps(linkedHashMap1);
        exportTable(verifyReportVo, new FileOutputStream(new File("D:\\aaa.xlsx")));
    }

    private static ExcelTableData buildData(Object object) throws Exception {

        if (Objects.isNull(object)) {
            throw new DustBusinessException("导出数据不能为空");
        }
        if (!object.getClass().isAnnotationPresent(ExcelType.class)) {
            logger.info("导出数据失败,找不到节点@ExcelType");
            throw new DustBusinessException("导出数据失败");
        }
        // 反射获取带有ExcelTitleMap的属性
        BeanInfo beanInfo = Introspector.getBeanInfo(object.getClass());

        // 获取所有属性
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();

        String title = "";
        String subTitle = "";
        List<ReportNode> contentItems = Lists.newArrayList();
        // 解析当前所有属性
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            String propertyName = propertyDescriptor.getName();
            if (!propertyName.equals("class")) {
                Field field = ReflectionUtils.findField(object.getClass(), propertyName);
                Method readMethod = propertyDescriptor.getReadMethod();
                // 获取属性的值
                Object obj = readMethod.invoke(object);
                // 有ExcelField才解析
                if (field.isAnnotationPresent(ExcelField.class)) {

                    ExcelField excelField = field.getAnnotation(ExcelField.class);

                    if (excelField.type().equals(ExcelFieldTypeEnum.TYPE_TITLE)) { // 父标题
                        title = (String)obj;
                    } else if (excelField.type().equals(ExcelFieldTypeEnum.TYPE_SUBTITLE)) {// 子标题
                        subTitle = excelField.prefix() + (Objects.isNull(obj) ? "" : (String)obj);
                    } else {// 内容

                        // 获取内容的order
                        int order = excelField.order();

                        if (Objects.nonNull(obj)) {// Map,List,Object
                            String name = excelField.name();
                            // 获取每行的子节点项
                            if (obj instanceof Map) {
                                if (excelField.type().equals(ExcelFieldTypeEnum.TYPE_SIMPLE)) {
                                    // 对带有ExcelFieldTypeEnum.TYPE_SIMPLE类型的单独处理,在模块中就是前端的输入信息,比如VerifyReportVo中的inputInfo对象
                                    // 头行
                                    ReportNode rowNode = new ReportNode();
                                    rowNode.setOrder(order);
                                    rowNode.setName(name);
                                    rowNode.setLeafNode(parseMap(obj));
                                    contentItems.add(rowNode);
                                } else {// map的value为一个对象,这个对象中含有ExcelType注解,比如：VerifyReportVo中的resultMaps
                                    Map<String, Object> map = (Map<String, Object>)obj;
                                    for (String key : map.keySet()) {
                                        Object value = map.get(key);
                                        if (Objects.nonNull(value)) {
                                            // 比如resultMaps中的每个value对象,比如:ModelBankcardCheckVo
                                            if (value.getClass().isAnnotationPresent(ExcelType.class)) {// key-Object
                                                                                                        // 这种情况,每个key-Object都是一个头行
                                                ExcelType excelType = value.getClass().getAnnotation(ExcelType.class);

                                                // 头行
                                                ReportNode rowNode1 = new ReportNode();
                                                rowNode1.setOrder(order);
                                                // 将typeName设置为头行标题
                                                rowNode1.setName(excelType.typeName());
                                                // 子项
                                                rowNode1.setLeafNode(parseObject(value));
                                                contentItems.add(rowNode1);
                                            } else {// 这里List,Map,String,基本属性
                                            }
                                        }
                                    }
                                }
                            } else {// 对象的话,比如VerifyReportVo中的BaseInfoVo对象
                                    // 行头节点
                                ReportNode rowNode = new ReportNode();
                                rowNode.setOrder(order);
                                ExcelType excelType = obj.getClass().getAnnotation(ExcelType.class);
                                rowNode.setName(excelType.typeName());
                                rowNode.setLeafNode(parseObject(obj));
                                contentItems.add(rowNode);
                            }
                        }
                    }

                }
            }
        }

        ExcelTableData excelTableData = new ExcelTableData();
        excelTableData.setSheetName(title);
        excelTableData.setTitle(title);
        excelTableData.setSubTitle(subTitle);
        contentItems.sort(Comparator.comparingInt(ReportNode::getOrder));
        excelTableData.setContents(contentItems);

        return excelTableData;
    }

    public static XSSFCellStyle getHeaderCellStyle(XSSFWorkbook book) throws Exception {
        XSSFCellStyle style = book.createCellStyle();
        XSSFFont font = book.createFont();
        font.setFontName("宋体");
        font.setBold(true);
        font.setFontHeightInPoints((short)15);
        font.setUnderline(FontUnderline.NONE);
        style.setFont(font);
        style.setAlignment(XSSFCellStyle.ALIGN_CENTER);
        style.setFillForegroundColor(new XSSFColor(new Color(153, 204, 255)));
        style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderLeft(BorderStyle.THIN);
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderRight(BorderStyle.THIN);
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderTop(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        return style;
    }

    public static XSSFCellStyle getSubTitleCellStyle(XSSFWorkbook book) throws Exception {
        XSSFCellStyle style = book.createCellStyle();
        XSSFFont font = book.createFont();
        font.setFontName("宋体");
        font.setBold(false);
        font.setFontHeightInPoints((short)11);
        font.setUnderline(FontUnderline.NONE);
        style.setFont(font);
        style.setAlignment(XSSFCellStyle.ALIGN_CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderLeft(BorderStyle.THIN);
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderRight(BorderStyle.THIN);
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderTop(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        return style;
    }

    public static XSSFCellStyle getRowHeaderCellStyle(XSSFWorkbook book) throws Exception {
        XSSFCellStyle style = book.createCellStyle();
        XSSFFont font = book.createFont();
        font.setFontName("宋体");
        font.setBold(true);
        font.setFontHeightInPoints((short)10);
        font.setUnderline(FontUnderline.NONE);
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new Color(153, 204, 255)));
        style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderLeft(BorderStyle.THIN);
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderRight(BorderStyle.THIN);
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderTop(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        return style;
    }

    public static XSSFCellStyle getBodyCellStyle(XSSFWorkbook book) throws Exception {
        XSSFCellStyle style = book.createCellStyle();
        XSSFFont font = book.createFont();
        font.setFontName("宋体");
        font.setBold(false);
        font.setFontHeightInPoints((short)10);
        font.setUnderline(FontUnderline.NONE);
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new Color(255, 255, 255)));
        style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
        style.setAlignment(XSSFCellStyle.ALIGN_CENTER);
        style.setVerticalAlignment(XSSFCellStyle.VERTICAL_CENTER);
        style.setWrapText(true);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderLeft(BorderStyle.THIN);
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderRight(BorderStyle.THIN);
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderTop(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        return style;
    }

    public static void exportTable(ExcelTableData excelTableData, OutputStream out) throws Exception {
        XSSFWorkbook book = new XSSFWorkbook();

        XSSFSheet sheet = book.createSheet(excelTableData.getSheetName());
        sheet.setDisplayGridlines(false);

        CellView cv = new CellView();
        // cv.setAutosize(true);
        cv.setSize(40 * 150);

        int marginLeft = 1;
        int marginTop = 2;

        // title
        XSSFCell title = sheet.createRow(0 + marginTop).createCell(0 + marginLeft);
        title.setCellValue(excelTableData.getTitle());
        CellRangeAddress cra = new CellRangeAddress(0 + marginTop, 0 + marginTop, 0 + marginLeft, 3 + marginLeft);
        sheet.addMergedRegion(cra);
        title.setCellStyle(getHeaderCellStyle(book));
        RegionUtil.setBorderBottom(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
        RegionUtil.setBorderLeft(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
        RegionUtil.setBorderRight(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
        RegionUtil.setBorderTop(XSSFCellStyle.BORDER_THIN, cra, sheet, book);

        // subtitle
        XSSFCell subtitle = sheet.createRow(1 + marginTop).createCell(0 + marginLeft);
        subtitle.setCellValue(excelTableData.getSubTitle());
        cra = new CellRangeAddress(1 + marginTop, 1 + marginTop, 0 + marginLeft, 3 + marginLeft);
        sheet.addMergedRegion(cra);
        subtitle.setCellStyle(getSubTitleCellStyle(book));
        RegionUtil.setBorderBottom(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
        RegionUtil.setBorderLeft(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
        RegionUtil.setBorderRight(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
        RegionUtil.setBorderTop(XSSFCellStyle.BORDER_THIN, cra, sheet, book);

        List<ReportNode> nodes = excelTableData.getContents();

        // 子项
        int row = 2;//
        if (Objects.nonNull(nodes) && !nodes.isEmpty()) {
            for (ReportNode reportItem : nodes) {
                // 分割行
                XSSFRow xrow = sheet.createRow(row + marginTop);
                XSSFCell itemHead = xrow.createCell(0 + marginLeft);
                itemHead.setCellValue("");
                cra = new CellRangeAddress(row + marginTop, row + marginTop, 0 + marginLeft, 3 + marginLeft);
                sheet.addMergedRegion(cra);
                itemHead.setCellStyle(getBodyCellStyle(book));
                RegionUtil.setBorderBottom(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
                RegionUtil.setBorderLeft(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
                RegionUtil.setBorderRight(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
                RegionUtil.setBorderTop(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
                row++;

                // 行头
                xrow = sheet.createRow(row + marginTop);
                XSSFCell br = xrow.createCell(0 + marginLeft);
                br.setCellValue(reportItem.getName());
                cra = new CellRangeAddress(row + marginTop, row + marginTop, 0 + marginLeft, 3 + marginLeft);
                sheet.addMergedRegion(cra);
                br.setCellStyle(getRowHeaderCellStyle(book));
                RegionUtil.setBorderBottom(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
                RegionUtil.setBorderLeft(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
                RegionUtil.setBorderRight(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
                RegionUtil.setBorderTop(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
                row++;

                // 子项
                List<ReportNode> leafNodes = reportItem.getLeafNode();
                if (Objects.nonNull(leafNodes) && !leafNodes.isEmpty()) {
                    if (leafNodes.size() >= 2) {
                        int cow = 0;
                        xrow = sheet.createRow(row + marginTop);
                        for (ReportNode leafNode : leafNodes) {
                            // name
                            XSSFCell label3 = xrow.createCell(cow + marginLeft);
                            label3.setCellValue(leafNode.getName());
                            label3.setCellStyle(getBodyCellStyle(book));
                            sheet.setColumnWidth(cow + marginLeft, cv.getSize());
                            cow++;

                            // value
                            XSSFCell label4 = xrow.createCell(cow + marginLeft);
                            label4.setCellValue(leafNode.getValue());
                            if (leafNode.getAppendCell() > 0) {
                                cra = new CellRangeAddress(row + marginTop, row + marginTop, cow + marginLeft,
                                    cow + marginLeft + leafNode.getAppendCell());
                                sheet.addMergedRegion(cra);
                                cow = cow + leafNode.getAppendCell();
                            }
                            label4.setCellStyle(getBodyCellStyle(book));
                            if (leafNode.getAppendCell() > 0) {
                                RegionUtil.setBorderBottom(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
                                RegionUtil.setBorderLeft(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
                                RegionUtil.setBorderRight(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
                                RegionUtil.setBorderTop(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
                                sheet.setColumnWidth(cow + marginLeft, cv.getSize());
                            }
                            sheet.setColumnWidth(cow + marginLeft, cv.getSize());
                            cow++;
                            if (cow % 4 == 0) {
                                row++;
                                xrow = sheet.createRow(row + marginTop);
                                cow = 0;
                            }
                        }
                        if (cow % 4 != 0) {
                            row++;// 出循环再加一次,表示下一行
                        }
                    } else {
                        if (leafNodes.size() >= 1) {
                            // 分情况,两个项,一个项
                            int cow = 0;
                            xrow = sheet.createRow(row + marginTop);
                            for (ReportNode leafNode : leafNodes) {
                                // 判断name是否为null,如果为空,表示value要占用整行
                                if (Objects.nonNull(leafNode.getName())) {
                                    XSSFCell label3 = xrow.createCell(cow + marginLeft);
                                    label3.setCellValue(leafNode.getName());
                                    label3.setCellStyle(getBodyCellStyle(book));
                                    sheet.setColumnWidth(cow + marginLeft, cv.getSize());
                                    cow++;

                                    XSSFCell label4 = xrow.createCell(cow + marginLeft);
                                    label4.setCellValue(leafNode.getValue());
                                    cra = new CellRangeAddress(row + marginTop, row + marginTop, cow + marginLeft,
                                        cow + marginLeft + 2);
                                    sheet.addMergedRegion(cra);
                                    label4.setCellStyle(getBodyCellStyle(book));
                                    RegionUtil.setBorderBottom(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
                                    RegionUtil.setBorderLeft(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
                                    RegionUtil.setBorderRight(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
                                    RegionUtil.setBorderTop(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
                                    sheet.setColumnWidth(cow + marginLeft, cv.getSize());
                                    cow++;
                                } else {
                                    XSSFCell label3 = xrow.createCell(cow + marginLeft);
                                    label3.setCellValue(leafNode.getValue());
                                    cra = new CellRangeAddress(row + marginTop, row + marginTop, cow + marginLeft,
                                        cow + marginLeft + 3);
                                    sheet.addMergedRegion(cra);
                                    label3.setCellStyle(getBodyCellStyle(book));
                                    RegionUtil.setBorderBottom(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
                                    RegionUtil.setBorderLeft(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
                                    RegionUtil.setBorderRight(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
                                    RegionUtil.setBorderTop(XSSFCellStyle.BORDER_THIN, cra, sheet, book);
                                    sheet.setColumnWidth(cow + marginLeft, cv.getSize());
                                    cow++;
                                }
                            }
                        }
                        row++;
                    }
                }
            }
        }

        // 设置自动列宽
        // setColumnAutoSize(sheet, 5);
        book.write(out);
        out.close();
    }

    public static void exportTable(Object obj, OutputStream out) throws Exception {
        ExcelTableData excelTableData = buildData(obj);
        exportTable(excelTableData, out);
    }

    public static void exportTableToWeb(Object obj, HttpServletResponse response, String fileName) throws Exception {
        if (StringUtils.isEmpty(fileName)) {
            fileName = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
        }
        setResponseExcelHeader(response, fileName);
        exportTable(obj, response.getOutputStream());
    }

    public static void setResponseExcelHeader(HttpServletResponse response, String fileName) throws Exception {
        // 设置response头信息
        response.reset();
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-disposition",
            "attachment; filename=" + java.net.URLEncoder.encode(fileName, "UTF-8") + ".xlsx");
        response.setHeader("Content-Transfer-Encoding", "binary");
        response.setHeader("Access-Control-Expose-Headers", "Content-disposition");
    }

    public static MultipartFile read(HttpServletRequest httpServletRequest) {
        MultipartResolver resolver = (SpringContextUtil.getBean("multipartResolver"));
        String enctype = httpServletRequest.getContentType();
        if (StringUtils.isNotBlank(enctype) && enctype.contains("multipart/form-data")) {
            httpServletRequest = resolver.resolveMultipart(httpServletRequest);
        }
        MultipartHttpServletRequest multipartRequest =
            WebUtils.getNativeRequest(httpServletRequest, MultipartHttpServletRequest.class);
        if (multipartRequest == null) {
            throw new DustBusinessException("找不到上传文件");
        }

        List<MultipartFile> files = multipartRequest.getFiles("file");
        if (files.isEmpty()) {
            throw new DustBusinessException("找不到上传文件");
        }

        if (files.size() > 1) {
            throw new DustBusinessException("通过文件名找到多个映射文件");
        }

        MultipartFile file = files.get(0);
        long fileSize = file.getSize();
        if (fileSize > 1 * 1024 * 1024) {
            throw new DustBusinessException("errors.upload.profile.oversize");
        }
        return file;
    }

    public static <T> ExcelList getExcelData(HttpServletRequest httpServletRequest, String sheetName,
        Class<T> entityClass, LinkedHashMap<String, String> fieldMap, int maxRows) throws Exception {
        MultipartFile file = read(httpServletRequest);
        Map<String, Object> result =
            ExcelUtils.excelToList(file.getInputStream(), sheetName, entityClass, fieldMap, maxRows);

        Map<String, Object> msgMap = (Map<String, Object>)result.get("msg");
        List<LinkedHashMap<String, Object>> resultList = (List<LinkedHashMap<String, Object>>)result.get("val");

        if (!MapUtils.isEmpty(msgMap)) {
            throw new DustBusinessException((String)msgMap.get("error"));
        } else {
            ExcelList<LinkedHashMap<String, Object>> excelList = new ExcelList<>();
            resultList.stream().forEach(stringObjectLinkedHashMap -> excelList.add(stringObjectLinkedHashMap));
            excelList.setFileName(file.getOriginalFilename());
            return excelList;
        }
    }

    public static void exportBill() {

    }

    /**
     * 用来生成Table表格的demo案例
     * 
     * @param response
     * @throws Exception
     */
    public static void demoExportTable(HttpServletResponse response) throws Exception {
        ExcelUtils.setResponseExcelHeader(response, "导出中午");

        ExcelTableData excelTableData = new ExcelTableData();

        excelTableData.setSheetName("aaa");
        excelTableData.setTitle("bb");
        excelTableData.setSubTitle("fff");

        List<ReportNode> typeNodes = Lists.newArrayList();

        ReportNode reportNode = new ReportNode();
        reportNode.setName("title1");
        List<ReportNode> title1Leaf = Lists.newArrayList();
        reportNode.setLeafNode(title1Leaf);
        ReportNode reportNode_1 = new ReportNode();
        reportNode_1.setName("a");
        reportNode_1.setValue("b");
        title1Leaf.add(reportNode_1);
        ReportNode reportNode_2 = new ReportNode();
        reportNode_2.setName("a");
        reportNode_2.setValue("b");
        title1Leaf.add(reportNode_2);

        ReportNode reportNode1 = new ReportNode();
        reportNode1.setName("title2");
        List<ReportNode> title2Leaf = Lists.newArrayList();
        reportNode1.setLeafNode(title2Leaf);
        ReportNode reportNode1_1 = new ReportNode();
        reportNode1_1.setName("aa");
        reportNode1_1.setValue("bb");
        title2Leaf.add(reportNode1_1);
        ReportNode reportNode2_2 = new ReportNode();
        reportNode2_2.setName("aa");
        reportNode2_2.setValue("bb");
        title2Leaf.add(reportNode2_2);

        ReportNode reportNode2 = new ReportNode();
        reportNode2.setName("title3");
        List<ReportNode> title3Leaf = Lists.newArrayList();
        reportNode2.setLeafNode(title3Leaf);

        typeNodes.add(reportNode);
        typeNodes.add(reportNode1);
        typeNodes.add(reportNode2);

        excelTableData.setContents(typeNodes);
        // 导出excel
        ExcelUtils.exportTable(excelTableData, response.getOutputStream());
    }

}
