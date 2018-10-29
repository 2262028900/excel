package com.mukitech.web.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class ImportExcelUtil<T> {

	public Class<T> clazz;

	public ImportExcelUtil(Class<T> clazz) {
		this.clazz = clazz;
	}

	/**
	 * 对excel表单默认第一个索引名转换成list
	 * 
	 * @param input
	 *            输入流
	 * @return 转换后集合
	 */
	public  List<T> importExcel(InputStream input) throws Exception {
		return importExcel(StringUtils.EMPTY, input);
	}

	/**
	 * 对excel表单指定表格索引名转换成list
	 * 
	 * @param sheetName
	 *            表格索引名
	 * @param input
	 *            输入流
	 * @return 转换后集合
	 */
	public  List<T> importExcel(String sheetName, InputStream input) throws Exception {
		List<T> list = new ArrayList<T>();

		Workbook workbook = WorkbookFactory.create(input);
		Sheet sheet = null;
		if (StringUtils.isNotEmpty(sheetName)) {
			// 如果指定sheet名,则取指定sheet中的内容.
			sheet = workbook.getSheet(sheetName);
		} else {
			// 如果传入的sheet名不存在则默认指向第1个sheet.
			sheet = workbook.getSheetAt(0);
		}

		if (sheet == null) {
			throw new IOException("文件sheet不存在");
		}

		int rows = sheet.getPhysicalNumberOfRows();

		if (rows > 0) {
			// 默认序号
			int serialNum = 0;
			// 有数据时才处理 得到类的所有field.
			Field[] allFields = clazz.getDeclaredFields();
			// 定义一个map用于存放列的序号和field.
			Map<Integer, Field> fieldsMap = new HashMap<Integer, Field>();
			for (int col = 0; col < allFields.length; col++) {
				Field field = allFields[col];
				// 将有注解的field存放到map中.
				if (field.isAnnotationPresent(Excel.class)) {
					// 设置类的私有字段属性可访问.
					field.setAccessible(true);
					fieldsMap.put(++serialNum, field);
				}
			}
			for (int i = 1; i < rows; i++) {
				// 从第2行开始取数据,默认第一行是表头.
				Row row = sheet.getRow(i);
				int cellNum = serialNum;
				T entity = null;
				for (int j = 0; j < cellNum; j++) {
					Cell cell = row.getCell(j);
					if (cell == null) {
						continue;
					} else {
						// 先设置Cell的类型，然后就可以把纯数字作为String类型读进来了
						row.getCell(j).setCellType(CellType.STRING);
						cell = row.getCell(j);
					}

					String c = cell.getStringCellValue();
					if (StringUtils.isEmpty(c)) {
						continue;
					}

					// 如果不存在实例则新建.
					entity = (entity == null ? clazz.newInstance() : entity);
					// 从map中得到对应列的field.
					Field field = fieldsMap.get(j + 1);
					// 取得类型,并根据对象类型设置值.
					Class<?> fieldType = field.getType();
					if (String.class == fieldType) {
						field.set(entity, String.valueOf(c));
					} else if ((Integer.TYPE == fieldType) || (Integer.class == fieldType)) {
						field.set(entity, Integer.parseInt(c));
					} else if ((Long.TYPE == fieldType) || (Long.class == fieldType)) {
						field.set(entity, Long.valueOf(c));
					} else if ((Float.TYPE == fieldType) || (Float.class == fieldType)) {
						field.set(entity, Float.valueOf(c));
					} else if ((Short.TYPE == fieldType) || (Short.class == fieldType)) {
						field.set(entity, Short.valueOf(c));
					} else if ((Double.TYPE == fieldType) || (Double.class == fieldType)) {
						field.set(entity, Double.valueOf(c));
					} else if (Character.TYPE == fieldType) {
						if ((c != null) && (c.length() > 0)) {
							field.set(entity, Character.valueOf(c.charAt(0)));
						}
					} else if (java.util.Date.class == fieldType) {
						if (cell.getCellTypeEnum() == CellType.NUMERIC) {
							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							cell.setCellValue(sdf.format(cell.getNumericCellValue()));
							c = sdf.format(cell.getNumericCellValue());
						} else {
							c = cell.getStringCellValue();
						}
					} else if (java.math.BigDecimal.class == fieldType) {
						c = cell.getStringCellValue();
					}
				}
				if (entity != null) {
					list.add(entity);
				}
			}
		}

		return list;
	}

}
