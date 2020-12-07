package com.rao.cloud.mall.common.utils;

import com.alibaba.fastjson.JSON;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.format.Alignment;
import jxl.format.VerticalAlignment;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.*;

@Slf4j
public class ExcelUtils {


    /**
     * @param list      数据源
     * @param fieldMap  类的英文属性和Excel中的中文列名的对应关系 如果需要的是引用对象的属性，则英文属性使用类似于EL表达式的格式
     *                  如：list中存放的都是student，student中又有college属性，而我们需要学院名称，则可以这样写
     *                  fieldMap.put("college.collegeName","学院名称")
     * @param sheetName 工作表的名称
     * @param sheetSize 每个工作表中记录的最大个数
     * @param out       导出流
     * @throws Exception
     * @MethodName : listToExcel
     * @Description : 导出Excel（可以导出到本地文件系统，也可以导出到浏览器，可自定义工作表大小）
     */
    public static <T> void listToExcel(List<T> list, LinkedHashMap<String,Integer> mergeFieldMap, LinkedHashMap<String, String> fieldMap, String sheetName,
                                       int sheetSize, OutputStream out) throws Exception {

        if (CollectionUtils.isEmpty(list)) {
            throw new BusinessException(null,"导出数据为空");
        }

        if (sheetSize > 65535 || sheetSize < 1) {
            sheetSize = 65535;
        }

        // 创建工作簿并发送到OutputStream指定的地方
        WritableWorkbook wwb = null;
        try {
            wwb = Workbook.createWorkbook(out);

            // 因为2003的Excel一个工作表最多可以有65536条记录，除去列头剩下65535条
            // 所以如果记录太多，需要放到多个工作表中，其实就是个分页的过程
            // 1.计算一共有多少个工作表
            double sheetNum = Math.ceil(list.size() / new Integer(sheetSize).doubleValue());

            // 2.创建相应的工作表，并向其中填充数据
            for (int i = 0; i < sheetNum; i++) {
                // 如果只有一个工作表的情况
                if (1 == sheetNum) {
                    WritableSheet sheet = wwb.createSheet(sheetName, i);
                    fillSheet(sheet, list, mergeFieldMap, fieldMap, 0, list.size() - 1);

                    // 有多个工作表的情况
                } else {
                    WritableSheet sheet = wwb.createSheet(sheetName + (i + 1), i);

                    // 获取开始索引和结束索引
                    int firstIndex = i * sheetSize;
                    int lastIndex =
                            (i + 1) * sheetSize - 1 > list.size() - 1 ? list.size() - 1 : (i + 1) * sheetSize - 1;
                    // 填充工作表
                    fillSheet(sheet, list, mergeFieldMap, fieldMap, firstIndex, lastIndex);
                }
            }
            wwb.write();

        } catch (Exception e) {
            log.warn("导出Excel失败={}", JSON.toJSONString(e));
            throw new BusinessException(null,"导出Excel失败");
        } finally {
            if (null != wwb) {
                wwb.close();
            }
        }

    }

    /**
     * @param sheet      工作表
     * @param list       数据源
     * @param fieldMap   中英文字段对应关系的Map
     * @param firstIndex 开始索引
     * @param lastIndex  结束索引
     * @MethodName : fillSheet
     * @Description : 向工作表中填充数据
     */
    private static <T> void fillSheet(WritableSheet sheet, List<T> list, LinkedHashMap<String,Integer> mergeFieldMap, LinkedHashMap<String, String> fieldMap,
                                      int firstIndex, int lastIndex) throws Exception {
        WritableCellFormat format = new WritableCellFormat();
        format.setAlignment(Alignment.CENTRE);
        format.setVerticalAlignment(VerticalAlignment.CENTRE);

        // 定义存放英文字段名和中文字段名的数组
        String[] enFields = new String[fieldMap.size()];
        String[] cnFields = new String[fieldMap.size()];

        int rowNo = 0;

        // 填充表头
        if(!MapUtils.isEmpty(mergeFieldMap)) {
            int columnNum = 0;
            for (Map.Entry<String, Integer> entry : mergeFieldMap.entrySet()) {
                Label label = new Label(columnNum, rowNo, entry.getKey());
                label.setCellFormat(format);
                sheet.addCell(label);
                sheet.mergeCells(columnNum, rowNo, columnNum + entry.getValue() - 1, rowNo);
                columnNum = columnNum + entry.getValue();
            }
            rowNo++;
        }

        // 二级表头
        int count = 0;
        for (Map.Entry<String, String> entry : fieldMap.entrySet()) {
            enFields[count] = entry.getKey();
            cnFields[count] = entry.getValue();
            count++;
        }
        for (int i = 0; i < cnFields.length; i++) {
            Label label = new Label(i, rowNo, cnFields[i]);
            label.setCellFormat(format);
            sheet.addCell(label);
        }
        rowNo++;

        // 填充内容
        for (int index = firstIndex; index <= lastIndex; index++) {
            // 获取单个对象
            T item = list.get(index);
            for (int i = 0; i < enFields.length; i++) {
                String fieldValue = "";
                if(item instanceof Map){//如果时map,则通过key-value的形式取出
                    Map<String,Object> map = (Map<String, Object>) item;
                    Object objValue = map.get(enFields[i]);
                    fieldValue = objValue == null ? "" : objValue.toString();
                }else {
                    Object objValue = getFieldValueByNameSequence(enFields[i], item);
                    fieldValue = objValue == null ? "" : objValue.toString();
                }
                Label label = new Label(i, rowNo, fieldValue);
                label.setCellFormat(format);
                sheet.addCell(label);
            }

            rowNo++;
        }

        // 设置自动列宽
        setColumnAutoSize(sheet, 5);
    }

    /**
     * @param fieldNameSequence 带路径的属性名或简单属性名
     * @param o                 对象
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
     * @param ws
     * @MethodName : setColumnAutoSize
     * @Description : 设置工作表自动列宽和首行加粗
     */
    private static void setColumnAutoSize(WritableSheet ws, int extraWith) {
        // 获取本列的最宽单元格的宽度
        for (int i = 0; i < ws.getColumns(); i++) {
            int colWith = 0;
            for (int j = 0; j < ws.getRows(); j++) {
                String content = ws.getCell(i, j).getContents().toString();
                int cellWith = content.length();
                if (colWith < cellWith) {
                    colWith = cellWith;
                }
            }
            // 设置单元格的宽度为最宽宽度+额外宽度
            ws.setColumnView(i, colWith + extraWith);
        }

    }

    /**
     * @param fieldName 字段名
     * @param o         对象
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
                value = DateUtils.parseDateForString((Date) value, DateUtils.YYYY_MM_DD_T_HH_MM_SS_SSS);
            }
        } else {
            throw new Exception(o.getClass().getSimpleName() + "类不存在字段名 " + fieldName);
        }

        return value;
    }

    /**
     * @param fieldName 字段名
     * @param clazz     包含该字段的类
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

    public static <T> List<T> getExcelData(MultipartFile file,String sheetName, Class<T> entityClass,
                             LinkedHashMap<String, String> fieldMap,int maxRows)throws Exception{

        if (Objects.isNull(file)) {
            throw new BusinessException(null,"模板导入失败，请尝试重新导入");
        }

        Map<String,Object> result = ExcelUtils.excelToList(file.getInputStream(),sheetName, entityClass,fieldMap,maxRows);

        Map<String,Object> msgMap = (Map<String, Object>) result.get("msg");
        List<T> resultList = (List<T>) result.get("val");

        if(!MapUtils.isEmpty(msgMap)){
            throw new BusinessException((String) msgMap.get("error"));
        }else{
            return resultList;
        }
    }

    public static <T> Map<String, Object> excelToList(InputStream in, String sheetName, Class<T> entityClass,
                                                      LinkedHashMap<String, String> fieldMap, int maxRows) throws Exception {
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
                errorMap.put("error", "导入的Excel工作表sheet名称不对");
                map.put("msg", errorMap);
                map.put("val", resultList);
                return map;
            }

            // 统计真实有效的行
            int realRows = 0;
            for (int i = 0; i < sheet.getRows(); i++) {
                int nullCols = 0;
                for (int j = 0; j < sheet.getColumns(); j++) {
                    Cell currentCell = sheet.getCell(j, i);
                    if (Objects.isNull(currentCell) || StringUtils.isEmpty(currentCell.getContents())) {
                        nullCols++;
                    }
                }
                if (nullCols != sheet.getColumns()) {
                    realRows++;
                }
            }

            if(realRows<=1){
                errorMap.put("error", "导入的数据至少有一条");
                map.put("msg", errorMap);
                map.put("val", resultList);
                return map;
            }

            if (realRows > maxRows) {
                errorMap.put("error", "本次共导入"+realRows+"条数据，已超出"+maxRows+"条上限。为保证系统稳定，请重新导入。");
                map.put("msg", errorMap);
                map.put("val", resultList);
                return map;
            }

            //获取第0行
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
                if (!excelFieldList.contains(fieldMap.get(cnName))) {
                    isExist = false;
                    break;
                }
            }

            // 如果有列名不存在，则抛出异常，提示错误
            if (!isExist) {
                errorMap.put("error", "Excel中缺少必要的字段，或表头名称有误");
                map.put("msg", errorMap);
                map.put("val", resultList);
                return map;
            }

            // 将列名和列号放入Map中,这样通过列名就可以拿到列号
            LinkedHashMap<String, Integer> colMap = new LinkedHashMap<String, Integer>();
            for (int i = 0; i < excelFieldNames.length; i++) {
                colMap.put(excelFieldNames[i], columnName[i].getColumn());
            }

            // 将sheet转换为list
            for (int i = 1; i < sheet.getRows(); i++) {
                //排除全为空的行
                T entity = entityClass.newInstance();
                if (entity instanceof Map) {
                    Map<String, Object> maps = (Map) entity;
                    //  新建要转换的对象 给对象中的字段赋值
                    for (Map.Entry<String, String> entry : fieldMap.entrySet()) {
                        // 获取中文字段名
                        String cnNormalName = entry.getValue();
                        // 获取英文字段名
                        String enNormalName = entry.getKey();
                        // 根据中文字段名获取列号
                        int col = colMap.get(cnNormalName);

                        // 获取当前单元格中的内容
                        String content = sheet.getCell(col, i).getContents().toString().trim();
                        maps.put(enNormalName, content);
                    }
                } else {
                    //  新建要转换的对象 给对象中的字段赋值
                    for (Map.Entry<String, String> entry : fieldMap.entrySet()) {
                        // 获取中文字段名
                        String cnNormalName = entry.getValue();
                        // 获取英文字段名
                        String enNormalName = entry.getKey();
                        // 根据中文字段名获取列号
                        int col = colMap.get(cnNormalName);

                        // 获取当前单元格中的内容
                        String content = sheet.getCell(col, i).getContents().toString().trim();

                        // 给对象赋值
                        setFieldValueByName(enNormalName, content, entity);
                    }
                }
                resultList.add(entity);
            }
            map.put("msg", errorMap);
            map.put("val", resultList);

            wb.close();
        } catch (Exception e) {
            log.warn("导入Excel失败,{}",e);
            throw new Exception("导入Excel失败");
        }
        return map;
    }

    /**
     * @param fieldName  字段名
     * @param fieldValue 字段值
     * @param o          对象
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
                field.set(o, DateUtils.parseDate(fieldValue.toString(),DateUtils.YYYY_MM_DD));
            } else {
                field.set(o, fieldValue);
            }
        } else {
            throw new Exception(o.getClass().getSimpleName() + "类不存在字段名 " + fieldName);
        }
    }

}
