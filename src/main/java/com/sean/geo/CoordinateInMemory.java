package com.sean.geo;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * 经纬度-地址信息
 * 
 */
public class CoordinateInMemory {

	/**
	 * 位置信息元素
	 */
	public static class Loc {
		/** 省份 */
		public final String province;
		/** 城市 */
		public final String city;
		/** 区/县 */
		public final String district;

		/**
		 * 创建位置信息元素
		 * 
		 * @param province
		 *            省份
		 * @param city
		 *            城市
		 * @param district
		 *            区/县
		 */
		public Loc(String province, String city, String district) {
			this.province = province;
			this.city = city;
			this.district = district;
		}

		@Override
		public String toString() {
			return String.format("%s-%s-%s", province, city, district);
		}
	}

	/**
	 * 爬取经纬度-地址信息
	 * 
	 * @param args
	 *            [已爬取的数据文件路径, 导出数据文件目标路径]
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println(String.format("Usage: %s load_file_path export_file_path [end_level]",
					CoordinateInMemory.class.getName()));
			System.exit(1);
		}
		File out = new File(args[1]);
		if (out.exists()) {
			System.out.println(String.format("export_file_path '%s' already exists", args[1]));
			System.exit(1);
		}
		if (args.length > 2) {
			int minLevel = Integer.parseInt(args[2].trim(), 10);
			if (minLevel > LocMatrix.START_LEVEL || minLevel < 1) {
				System.out.println(String.format("Illegal end_level '%s'. Range 1 - %d. Default %d", args[2],
						LocMatrix.START_LEVEL, LocMatrix.END_LEVEL));
				System.exit(1);
			}
			LocMatrix.END_LEVEL = minLevel;
		}
		continueCrawl(new File(args[0]), out);
	}

	/**
	 * 解析指定坐标的位置信息
	 * 
	 * @param lng
	 *            百度经度
	 * @param lat
	 *            百度纬度
	 * @return 位置信息元素
	 */
	public static Loc getLocation(double lng, double lat) {
		if (ROOT == null || !ROOT.contents(lng, lat)) {
			return null;
		}
		Loc result = ROOT.getLoc(lng, lat);
		int level = ROOT.level;
		while (level > LocMatrix.END_LEVEL) {
			Long key = ROOT.getNextLevelKey(lng, lat);
			if (key == null) {
				break;
			}
			LocMatrix matrix = LOC_MAP.get(key);
			if (matrix == null) {
				break;
			}
			Loc retryResult = matrix.getLoc(lng, lat);
			if (retryResult == null) {
				break;
			}
			result = retryResult;
			level--;
		}
		return result;
	}

	/**
	 * 加载经纬度-地址信息
	 * 
	 * @param in
	 *            字符输入流
	 * @throws IOException
	 */
	public static void load(Reader in) throws IOException {
		load(in, false);
	}

	/**
	 * 爬取经纬度-地址信息
	 * 
	 * @param loadFile
	 *            源文件
	 * @param exportFile
	 *            输出文件
	 * @throws IOException
	 */
	public static void continueCrawl(File loadFile, File exportFile) throws IOException {
		if (loadFile.canRead()) {
			FileInputStream in = new FileInputStream(loadFile);
			Reader reader = new InputStreamReader(in, CHARACTER_ENCODING);
			load(reader, true);
			reader.close();
			in.close();
		}
		int dictNum = 0;
		List<LocMatrix> runing = new ArrayList<LocMatrix>();
		FileOutputStream out = new FileOutputStream(exportFile);
		Writer writer = new OutputStreamWriter(out, CHARACTER_ENCODING);
		BufferedWriter bfout = new BufferedWriter(writer);
		if (ROOT == null) {
			ROOT = new LocMatrix(73 * LocMatrix.BASE, (136 - 73) * LocMatrix.BASE
					/ LocMatrix.getRange(LocMatrix.START_LEVEL), 3 * LocMatrix.BASE, (54 - 3) * LocMatrix.BASE
					/ LocMatrix.getRange(LocMatrix.START_LEVEL), LocMatrix.START_LEVEL);
		}
		bfout.write(ROOT.exportRootMeta());
		bfout.write('\n');
		bfout.flush();
		exportAccessCount(bfout);
		dictNum = rootSplitCrawl(dictNum, bfout);
		dictNum = exportLocationDict(bfout, dictNum);
		runing.add(ROOT);
		long total = ROOT.getNeedNextLevelNum();
		long passed = 0;
		for (int i = 0; i < runing.size(); i++) {
			LocMatrix p = runing.get(i);
			for (int parsed = 0; parsed < p.loc.length; parsed++) {
				if (p.loc[parsed] >= 0) {
					continue;
				}
				LocMatrix test = p.createNextLevel(parsed);
				if (test == null) {
					passed++;
					continue;
				}
				Long nextKey = test.getCatchKey();
				LocMatrix item = LOC_MAP.get(nextKey);
				if (item == null) {
					test.crawl();
					LOC_MAP.put(nextKey, test);
					exportAccessCount(bfout);
					item = test;
				}
				dictNum = exportLocationDict(bfout, dictNum);
				runing.add(item);
				exportLocMatrix(bfout, item);
				passed++;
				total += item.getNeedNextLevelNum();
				System.out.println(String.format(
						"%s Block percent : %f%%(%d, %d) Api access times : %d Coordinate cache size : %d",
						currentTime(), ((double) passed) * 100 / total, passed, total, LocMatrix.API_ACCESS_COUNT,
						LocMatrix.LNG_LAT_CACHE.size()));
			}
		}
		exportAccessCount(bfout);
		bfout.close();
		writer.close();
		out.close();
	}

	/**
	 * 绘制根矩阵
	 * 
	 * @param format
	 *            图像格式
	 * @param output
	 *            输出文件
	 * @param bg
	 *            背景色
	 * @param color
	 *            前景色
	 * @param drawDepth
	 *            绘制精度深度
	 * @param exportDepth
	 *            输出精度深度
	 * @throws IOException
	 */
	public static void exportCityImage(String format, File output, Color bg, Color color, int drawDepth, int exportDepth)
			throws IOException {
		if (ROOT == null) {
			return;
		}
		BufferedImage image = new BufferedImage(ROOT.lngLen * pow(LocMatrix.STEP, drawDepth - 1), ROOT.latLen
				* pow(LocMatrix.STEP, drawDepth - 1), BufferedImage.TYPE_3BYTE_BGR);
		for (int i = 0; i < image.getWidth(); i++) {
			for (int j = 0; j < image.getHeight(); j++) {
				image.setRGB(i, j, bg.getRGB());
			}
		}
		drawRect(0, 0, ROOT, ROOT.level - drawDepth + 1, image, color);
		image.flush();
		BufferedImage oi = image;
		if (drawDepth != exportDepth) {
			int sw = ROOT.lngLen * pow(LocMatrix.STEP, exportDepth - 1);
			int sh = ROOT.latLen * pow(LocMatrix.STEP, exportDepth - 1);
			Image scale = image.getScaledInstance(sw, sh, Image.SCALE_AREA_AVERAGING);
			oi = new BufferedImage(sw, sh, BufferedImage.TYPE_3BYTE_BGR);
			Graphics g = oi.getGraphics();
			g.drawImage(scale, 0, 0, null);
			g.dispose();
		}
		ImageIO.write(oi, format, output);
	}

	/**
	 * 格式化根矩阵元数据
	 * 
	 * @param lngStart
	 *            起始经度
	 * @param latStart
	 *            起始纬度
	 * @param lngEnd
	 *            经度范围
	 * @param latEnd
	 *            纬度范围
	 * @return 根矩阵元数据
	 */
	public static String formatRootMeta(int lngStart, int latStart, int lngEnd, int latEnd) {
		return new LocMatrix(lngStart * LocMatrix.BASE, (lngEnd - lngStart) * LocMatrix.BASE
				/ LocMatrix.getRange(LocMatrix.START_LEVEL), latStart * LocMatrix.BASE, (latEnd - latStart)
				* LocMatrix.BASE / LocMatrix.getRange(LocMatrix.START_LEVEL), LocMatrix.START_LEVEL).exportRootMeta();
	}

	/**
	 * GCJ02(高德/谷歌)坐标转BD09(百度)坐标
	 * 
	 * @param cood
	 *            [GCJ02经度, GCJ02纬度]
	 * @return [BD09经度, BD09纬度]
	 */
	public static double[] convertGoogle2Baidu(double[] cood) {
		double lng = cood[0];
		double lat = cood[1];
		double z = Math.sqrt(lng * lng + lat * lat) + 0.00002D * Math.sin(lat * Math.PI);
		double theta = Math.atan2(lat, lng) + 0.000003D * Math.cos(lng * Math.PI);
		return new double[] { z * Math.cos(theta) + 0.0065D, z * Math.sin(theta) + 0.006D };
	}

	/**
	 * BD09(百度)坐标转GCJ02(高德/谷歌)坐标
	 * 
	 * @param cood
	 *            [BD09经度, BD09纬度]
	 * @return [GCJ02经度, GCJ02纬度]
	 */
	public static double[] convertBaidu2Google(double[] cood) {
		double lng = cood[0] - 0.0065D;
		double lat = cood[1] - 0.006D;
		double z = Math.sqrt(lng * lng + lat * lat) + 0.00002D * Math.sin(lat * Math.PI);
		double theta = Math.atan2(lat, lng) - 0.000003D * Math.cos(lng * Math.PI);
		return new double[] { z * Math.cos(theta), z * Math.sin(theta) };
	}

	/**
	 * WGS84(GPS)坐标转GCJ02(高德/谷歌)坐标
	 * 
	 * @param cood
	 *            [GPS经度, GPS纬度]
	 * @return [GCJ02经度, GCJ02纬度]
	 */
	public static double[] convertGps2Google(double[] cood) {
		double lat = cood[1];
		double lng = cood[0];
		double ee = 0.00669342162296594323D;
		double a = 6378245.0D;
		if (lng < 72.004D || lng > 137.8347D || lat < 0.8293D || lat > 55.8271D) {
			return cood;
		}
		double subLng = lng - 105.0D;
		double subLat = lat - 35.0D;
		double dLat = -100.0D + 2.0D * subLng + 3.0D * subLat + 0.2D * subLat * subLat + 0.1D * subLng * subLat + 0.2D
				* Math.sqrt(Math.abs(subLng));
		dLat += (20.0D * Math.sin(6.0 * subLng * Math.PI) + 20.0D * Math.sin(2.0D * subLng * Math.PI)) * 2.0D / 3.0D;
		dLat += (20.0D * Math.sin(subLat * Math.PI) + 40.0D * Math.sin(subLat / 3.0D * Math.PI)) * 2.0D / 3.0D;
		dLat += (160.0D * Math.sin(subLat / 12.0D * Math.PI) + 320D * Math.sin(subLat * Math.PI / 30.0D)) * 2.0D / 3.0D;
		double dLon = 300.0D + subLng + 2.0D * subLat + 0.1D * subLng * subLng + 0.1D * subLng * subLat + 0.1D
				* Math.sqrt(Math.abs(subLng));
		dLon += (20.0D * Math.sin(6.0D * subLng * Math.PI) + 20.0D * Math.sin(2.0D * subLng * Math.PI)) * 2.0D / 3.0D;
		dLon += (20.0D * Math.sin(subLng * Math.PI) + 40.0D * Math.sin(subLng / 3.0D * Math.PI)) * 2.0D / 3.0D;
		dLon += (150.0D * Math.sin(subLng / 12.0D * Math.PI) + 300.0D * Math.sin(subLng / 30.0D * Math.PI)) * 2.0D / 3.0D;
		double radLat = lat / 180.0D * Math.PI;
		double magic = Math.sin(radLat);
		magic = 1 - ee * magic * magic;
		double sqrtMagic = Math.sqrt(magic);
		dLat = (dLat * 180.0D) / ((a * (1 - ee)) / (magic * sqrtMagic) * Math.PI);
		dLon = (dLon * 180.0D) / (a / sqrtMagic * Math.cos(radLat) * Math.PI);
		return new double[] { lng + dLon, lat + dLat };
	}

	/** 地址矩阵映射表 */
	private static final Map<Long, LocMatrix> LOC_MAP = new LinkedHashMap<Long, LocMatrix>();

	/** 根地址矩阵 */
	private static LocMatrix ROOT = null;

	/** 根地址矩阵拆分步长 */
	private static final int ROOT_SPLIT_STEP = 30;

	/**
	 * 加载经纬度-地址信息
	 * 
	 * @param in
	 *            字符输入流
	 * @param needRootCache
	 *            是否需要保存根矩阵的分片缓存
	 * @throws IOException
	 */
	private static void load(Reader in, boolean needRootCache) throws IOException {
		BufferedReader bin = new BufferedReader(in);
		while (bin.ready()) {
			String line = bin.readLine();
			if (line == null || line.length() == 0) {
				continue;
			}
			if (line.startsWith(LocMatrix.ROOT_PREFIX)) {
				ROOT = LocMatrix.parseRootMeta(line);
			} else if (line.startsWith(LocMatrix.LOC_DICT_PREFIX)) {
				LocMatrix.parseLocationDict(line);
			} else if (line.startsWith(LocMatrix.ACCESS_COUNT_PREFIX)) {
				LocMatrix.parseAccessCount(line);
			} else {
				LocMatrix matrix = LocMatrix.parseLocMatrix(line);
				if (matrix.level == ROOT.level) {
					copyToRoot(matrix, (matrix.lngStart - ROOT.lngStart) / LocMatrix.getRange(ROOT.level),
							(matrix.latStart - ROOT.latStart) / LocMatrix.getRange(ROOT.level));
					if (needRootCache) {
						LOC_MAP.put(matrix.getCatchKey(), matrix);
					}
				} else {
					LOC_MAP.put(matrix.getCatchKey(), matrix);
				}
			}
		}
	}

	/**
	 * 分片爬取根矩阵
	 * 
	 * @param dictNum
	 *            已输出的字典条目数
	 * @param bfout
	 *            输出字符流
	 * @return 合计输出的字典项条目数
	 * @throws IOException
	 */
	private static int rootSplitCrawl(int dictNum, BufferedWriter bfout) throws IOException {
		HashSet<Long> rootKeySet = new HashSet<Long>();
		int range = LocMatrix.getRange(ROOT.level);
		int latSplit = (ROOT.latLen / ROOT_SPLIT_STEP) + (ROOT.latLen % ROOT_SPLIT_STEP == 0 ? 0 : 1);
		int lngSplit = (ROOT.lngLen / ROOT_SPLIT_STEP) + (ROOT.lngLen % ROOT_SPLIT_STEP == 0 ? 0 : 1);
		for (int i = 0; i < latSplit; i++) {
			int latBase = i * ROOT_SPLIT_STEP;
			for (int j = 0; j < lngSplit; j++) {
				int lngBase = j * ROOT_SPLIT_STEP;
				LocMatrix meta = new LocMatrix(ROOT.lngStart + (range * lngBase), Math.min(ROOT_SPLIT_STEP, ROOT.lngLen
						- lngBase), ROOT.latStart + (range * latBase),
						Math.min(ROOT_SPLIT_STEP, ROOT.latLen - latBase), ROOT.level);
				Long key = meta.getCatchKey();
				rootKeySet.add(key);
				LocMatrix matrix = LOC_MAP.get(key);
				if (matrix == null) {
					meta.crawl();
					LOC_MAP.put(key, meta);
					copyToRoot(meta, lngBase, latBase);
					exportAccessCount(bfout);
					matrix = meta;
				}
				dictNum = exportLocationDict(bfout, dictNum);
				exportLocMatrix(bfout, matrix);
				System.out.println(String.format(
						"%s Root percent : %f%%(%d, %d) Api access times : %d Coordinate cache size : %d",
						currentTime(), ((double) (lngSplit * i + j + 1)) * 100 / (latSplit * lngSplit), lngSplit * i
								+ j + 1, latSplit * lngSplit, LocMatrix.API_ACCESS_COUNT,
						LocMatrix.LNG_LAT_CACHE.size()));
			}
		}
		LocMatrix.CACHE_ROOT = ROOT;
		for (Long key : rootKeySet) {
			LOC_MAP.remove(key);
		}
		return dictNum;
	}

	/**
	 * 将一个地理区块矩阵的分片合并至根矩阵
	 * 
	 * @param matrix
	 *            地理区块矩阵分片
	 * @param lngBase
	 *            经度起始偏移量
	 * @param latBase
	 *            纬度起始偏移量
	 */
	private static void copyToRoot(LocMatrix matrix, int lngBase, int latBase) {
		for (int i = 0; i < matrix.latLen; i++) {
			for (int j = 0; j < matrix.lngLen; j++) {
				ROOT.loc[ROOT.convertIndex(j + lngBase, i + latBase)] = matrix.loc[matrix.convertIndex(j, i)];
			}
		}
	}

	/**
	 * 输出一个地理区块矩阵
	 * 
	 * @param bfout
	 *            输出字符流
	 * @param m
	 *            地理区块矩阵
	 * @throws IOException
	 */
	private static void exportLocMatrix(BufferedWriter bfout, LocMatrix m) throws IOException {
		bfout.write(m.exportLocMatrix());
		bfout.write("\n");
		bfout.flush();
		System.out.println(String.format("%s Exported %d", currentTime(), (long) m.getCatchKey()));
	}

	/**
	 * 输出位置信息字典
	 * 
	 * @param bfout
	 *            输出字符流
	 * @param dictNum
	 *            已输出的字典条目数
	 * @return 合计输出的字典项条目数
	 * @throws IOException
	 */
	private static int exportLocationDict(BufferedWriter bfout, int dictNum) throws IOException {
		if (dictNum < LocMatrix.LOC_DICT.size()) {
			bfout.write(LocMatrix.exportLocationDict(dictNum));
			bfout.write("\n");
			bfout.flush();
			dictNum = LocMatrix.LOC_DICT.size();
		}
		return dictNum;
	}

	/**
	 * 输出API访问次数
	 * 
	 * @param bfout
	 *            输出字符流
	 * @throws IOException
	 */
	private static void exportAccessCount(BufferedWriter bfout) throws IOException {
		bfout.write(LocMatrix.exportAccessCount());
		bfout.write("\n");
		bfout.flush();
	}

	/**
	 * 获取一个地理区块矩阵
	 * 
	 * @param lngStart
	 *            起始经度
	 * @param lngLen
	 *            经度偏移范围
	 * @param latStart
	 *            起始纬度
	 * @param latLen
	 *            纬度偏移范围
	 * @param level
	 *            缩放级别
	 * @return 地理区块矩阵
	 */
	private static LocMatrix getMatrix(int lngStart, int lngLen, int latStart, int latLen, int level) {
		return LOC_MAP.get(new LocMatrix(lngStart, lngLen, latStart, latLen, level).getCatchKey());
	}

	/**
	 * 乘幂
	 * 
	 * @param a
	 *            底数
	 * @param b
	 *            幂
	 * @return 乘幂
	 */
	private static int pow(int a, int b) {
		if (b == 0) {
			return 1;
		}
		if (b % 2 == 0) {
			return pow(a, b / 2) * pow(a, b / 2);
		} else {
			return pow(a, (b - 1) / 2) * pow(a, (b - 1) / 2) * a;
		}
	}

	/**
	 * 绘制矩阵
	 * 
	 * @param x
	 *            起始横坐标
	 * @param y
	 *            起始纵坐标
	 * @param matrix
	 *            矩阵
	 * @param endLevel
	 *            绘制截至精度
	 * @param image
	 *            图形对象
	 * @param c
	 *            绘图颜色
	 */
	private static void drawRect(int x, int y, LocMatrix matrix, int endLevel, BufferedImage image, Color c) {
		for (int i = 0; i < matrix.latLen; i++) {
			for (int j = 0; j < matrix.lngLen; j++) {
				if (matrix.level > endLevel) {
					LocMatrix next = matrix.createNextLevel(matrix.convertIndex(j, i));
					next = next == null ? null : LOC_MAP.get(next.getCatchKey());
					if (next != null) {
						drawRect((x + j) * LocMatrix.STEP, (y + i) * LocMatrix.STEP, next, endLevel, image, c);
					}
				} else {
					if (matrix.loc[matrix.convertIndex(j, i)] < 0) {
						image.setRGB(x + j, image.getHeight() - y - i - 1, c.getRGB());
					}
				}
			}
		}
	}

	/**
	 * 地理区块矩阵
	 */
	private static class LocMatrix {
		/** 地理位置精度 1000代表 0.001度 约130米 */
		private static int BASE = 1000;// 该值最大支持11930464, 以保证bit_length(180 * BASE) <= 30
		/** 基准精度范围 */
		private static int BASE_RANGE = 1;
		/** 起始缩放级别 */
		private static int START_LEVEL = 4;// 该值最大支持7，以保证bit_length(START_LEVEL) <= 3
		/** 最终缩放级别 */
		private static int END_LEVEL = 2;
		/** 精度缩放倍数 */
		private static int STEP = 5;

		/** 位置信息-索引映射表 */
		private static final Map<String, Integer> LOC_INDEX = new HashMap<String, Integer>();
		/** 位置信息字典 */
		private static final List<Loc> LOC_DICT = new ArrayList<Loc>();
		/** 坐标-位置索引缓存 */
		private static final Map<Long, Integer> LNG_LAT_CACHE = new HashMap<Long, Integer>();
		/** 缓存矩阵根 */
		private static LocMatrix CACHE_ROOT = null;

		/** 数据分隔符 */
		private static final String SPLIT = ",";
		/** 子数据分隔符 */
		private static final String SUB_SPLIT = ":";
		/** 根矩阵字典前缀 */
		private static final String ROOT_PREFIX = "!";
		/** 位置信息字典前缀 */
		private static final String LOC_DICT_PREFIX = "#";
		/** API访问次数前缀 */
		private static final String ACCESS_COUNT_PREFIX = "*";
		/** API访问次数 */
		private static long API_ACCESS_COUNT = 0L;

		/** 起始经度 */
		int lngStart;
		/** 起始纬度 */
		int latStart;
		/** 经度偏移范围 */
		int lngLen;
		/** 纬度偏移范围 */
		int latLen;
		/** 缩放级别 */
		int level;
		/** 位置信息索引 */
		int[] loc;

		/**
		 * 创建地理区块矩阵
		 * 
		 * @param lngStart
		 *            起始经度
		 * @param lngLen
		 *            经度偏移范围
		 * @param latStart
		 *            起始纬度
		 * @param latLen
		 *            纬度偏移范围
		 * @param level
		 *            缩放级别
		 */
		public LocMatrix(int lngStart, int lngLen, int latStart, int latLen, int level) {
			this.lngStart = lngStart;
			this.lngLen = lngLen;
			this.latStart = latStart;
			this.latLen = latLen;
			this.level = Math.max(END_LEVEL, Math.min(level, START_LEVEL));
			loc = new int[latLen * lngLen];
			for (int i = 0; i < latLen; i++) {
				for (int j = 0; j < lngLen; j++) {
					setCheckNextLevel(j, i);
				}
			}
		}

		/**
		 * 爬取位置信息
		 */
		public void crawl() {
			System.out.println(String.format("%s Crawl %d(%d-%d-%d-%d-%d)", currentTime(), (long) getCatchKey(),
					lngStart, lngLen, latStart, latLen, level));
			int range = getRange(level);
			int[] left = createItems(lngStart - range, 1, latStart, latLen, range,
					getMatrix(lngStart - (range * lngLen), lngLen, latStart, latLen, level));
			int[] top = createItems(lngStart, lngLen, latStart - range, 1, range,
					getMatrix(lngStart, lngLen, latStart - (range * latLen), latLen, level));
			int[] right = createItems(lngStart + (lngLen * range), 1, latStart, latLen, range,
					getMatrix(lngStart + (range * lngLen), lngLen, latStart, latLen, level));
			int[] bottom = createItems(lngStart, lngLen, latStart + (latLen * range), 1, range,
					getMatrix(lngStart, lngLen, latStart + (range * latLen), latLen, level));
			for (int i = 0; i < latLen; i++) {
				for (int j = 0; j < lngLen; j++) {
					loc[convertIndex(j, i)] = parseLoc(lngStart + (j * range), latStart + (i * range), false);
				}
			}
			for (int i = 0; i < latLen; i++) {
				for (int j = 0; j < lngLen; j++) {
					if (!isSameAround(loc[convertIndex(j, i)], j == 0 ? left[i] : loc[convertIndex(j - 1, i)],
							i == 0 ? top[j] : loc[convertIndex(j, i - 1)], j == lngLen - 1 ? right[i]
									: loc[convertIndex(j + 1, i)],
							i == latLen - 1 ? bottom[j] : loc[convertIndex(j, i + 1)])) {
						setCheckNextLevel(j, i);
					}
				}
			}
		}

		/**
		 * 解析一组位置信息
		 * 
		 * @param lngStart
		 *            起始经度
		 * @param lngLen
		 *            经度偏移范围
		 * @param latStart
		 *            起始纬度
		 * @param latLen
		 *            纬度偏移范围
		 * @param range
		 *            精度范围
		 * @return 位置信息索引组
		 */
		private static int[] createItems(int lngStart, int lngLen, int latStart, int latLen, int range, LocMatrix m) {
			if (latLen == 1) {
				int[] result = new int[lngLen];
				for (int i = 0; i < lngLen; i++) {
					Integer loc = getLocCache(lngStart + (i * range), latStart, m);
					if (loc != null) {
						result[i] = loc;
					} else {
						result[i] = parseLoc(lngStart + (i * range), latStart, true);
					}
				}
				return result;
			} else {
				int[] result = new int[latLen];
				for (int i = 0; i < latLen; i++) {
					Integer loc = getLocCache(lngStart, latStart + (i * range), m);
					if (loc != null) {
						result[i] = loc;
					} else {
						result[i] = parseLoc(lngStart, latStart + (i * range), true);
					}
				}
				return result;
			}
		}

		/**
		 * 解析位置信息
		 * 
		 * @param lng
		 *            经度
		 * @param lat
		 *            纬度
		 * @param needAddCache
		 *            是否存入坐标-位置索引缓存
		 * @return 位置信息索引
		 */
		private static int parseLoc(int lng, int lat, boolean needAddCache) {
			long time = System.currentTimeMillis();
			Integer cache = getLocCache(lng, lat);
			if (cache != null) {
				if (!needAddCache) {
					removeLocCache(lng, lat);
				}
				return cache;
			}
			String[] loc = null;
			boolean isRetry = false;
			while (loc == null) {
				if (isRetry) {
					System.err.println(String.format("%s Retry (%f, %f)", currentTime(), lng / (double) BASE, lat
							/ (double) BASE));
				}
				loc = getLocation(lng / (double) BASE, lat / (double) BASE, TIME_OUT);
				time += API_ACCESS_SLEEP_TIME - System.currentTimeMillis();
				if (time > 0) {
					try {
						Thread.sleep(time);
					} catch (InterruptedException e) {
						e.printStackTrace(System.err);
					}
				}
				isRetry = true;
				time = System.currentTimeMillis();
			}
			int result = getLocIndex(loc[3], loc[2], loc[1]);
			if (needAddCache) {
				addLocCache(lng, lat, result);
			}
			API_ACCESS_COUNT++;
			return result;
		}

		/**
		 * 获取位置信息缓存
		 * 
		 * @param lng
		 *            经度
		 * @param lat
		 *            纬度
		 * @return 位置信息索引
		 */
		private static Integer getLocCache(int lng, int lat) {
			Integer cache = LNG_LAT_CACHE.get(getLocCacheKey(lng, lat));
			if (cache == null) {
				return getLocCache(lng, lat, CACHE_ROOT);
			} else {
				return cache;
			}
		}

		/**
		 * 清除位置信息缓存
		 * 
		 * @param lng
		 *            经度
		 * @param lat
		 *            纬度
		 */
		private static void removeLocCache(int lng, int lat) {
			LNG_LAT_CACHE.remove(getLocCacheKey(lng, lat));
		}

		/**
		 * 从指定矩阵中获取位置信息缓存
		 * 
		 * @param lng
		 *            经度
		 * @param lat
		 *            纬度
		 * @param matrix
		 *            地理区块矩阵
		 * @return 位置信息索引
		 */
		private static Integer getLocCache(int lng, int lat, LocMatrix matrix) {
			if (matrix == null) {
				return null;
			}
			int range = getRange(matrix.level);
			int lngIdx = (lng - matrix.lngStart) / range;
			int latIdx = (lat - matrix.latStart) / range;
			if (!matrix.inRange(lngIdx, latIdx)) {
				return null;
			}
			if (lngIdx * range + matrix.lngStart == lng && latIdx * range + matrix.latStart == lat) {
				return abs(matrix.loc[matrix.convertIndex(lngIdx, latIdx)]);
			} else {
				matrix = matrix.createNextLevel(matrix.convertIndex(lngIdx, latIdx));
				if (matrix == null) {
					return null;
				} else {
					return getLocCache(lng, lat, LOC_MAP.get(matrix.getCatchKey()));
				}
			}
		}

		/**
		 * 缓存位置信息
		 * 
		 * @param lng
		 *            经度
		 * @param lat
		 *            纬度
		 * @param loc
		 *            位置信息索引
		 */
		private static void addLocCache(int lng, int lat, int loc) {
			LNG_LAT_CACHE.put(getLocCacheKey(lng, lat), loc);
		}

		/**
		 * 获取位置信息缓存Key
		 * 
		 * @param lng
		 *            经度
		 * @param lat
		 *            纬度
		 * @return 位置信息缓存Key
		 */
		private static Long getLocCacheKey(int lng, int lat) {
			return (((long) lng) << 32) + lat;
		}

		/**
		 * 检测地理区块的行政区划是否与相邻地理区块相同
		 * 
		 * @param x
		 *            待检测的地理区块
		 * @param left
		 *            西侧地理区块
		 * @param top
		 *            南侧地理区块
		 * @param right
		 *            东侧地理区块
		 * @param bottom
		 *            北侧地理区块
		 * @return true : 行政区划相同, false : 行政区划不同
		 */
		private static boolean isSameAround(int x, int left, int top, int right, int bottom) {
			return abs(x) == abs(left) && abs(x) == abs(top) && abs(x) == abs(right) && abs(x) == abs(bottom);
		}

		/**
		 * 输出API访问次数
		 * 
		 * @return 格式化的API访问次数
		 */
		public static String exportAccessCount() {
			return String.format("%s%d", ACCESS_COUNT_PREFIX, API_ACCESS_COUNT);
		}

		/**
		 * 解析API访问次数
		 * 
		 * @param dict
		 *            格式化的API访问次数
		 */
		public static void parseAccessCount(String dict) {
			API_ACCESS_COUNT = Long.parseLong(dict.substring(ACCESS_COUNT_PREFIX.length()), 10);
		}

		/**
		 * 输出位置信息字典
		 * 
		 * @param start
		 *            输出起始位置
		 * @return 格式化的位置信息字典
		 */
		public static String exportLocationDict(int start) {
			StringBuilder sb = new StringBuilder(LOC_DICT_PREFIX);
			for (int i = start; i < LOC_DICT.size(); i++) {
				Loc loc = LOC_DICT.get(i);
				sb.append(loc.province);
				sb.append(SUB_SPLIT);
				sb.append(loc.city);
				sb.append(SUB_SPLIT);
				sb.append(loc.district);
				sb.append(SPLIT);
			}
			return sb.toString();
		}

		/**
		 * 解析位置信息字典
		 * 
		 * @param dict
		 *            格式化的位置信息字典
		 */
		public static void parseLocationDict(String dict) {
			String[] locs = dict.split(SPLIT);
			if (locs.length > 0 && locs[0] != null && locs[0].startsWith(LocMatrix.LOC_DICT_PREFIX)) {
				locs[0] = locs[0].substring(LOC_DICT_PREFIX.length());
			}
			for (String loc : locs) {
				String[] items = loc.split(SUB_SPLIT, 3);
				getLocIndex(items[0], items[1], items.length > 2 ? items[2] : "");
			}
		}

		/**
		 * 输出根矩阵元数据
		 * 
		 * @return 根矩阵元数据
		 */
		public String exportRootMeta() {
			StringBuilder sb = new StringBuilder(ROOT_PREFIX);
			sb.append(exportInt(lngStart));
			sb.append(SUB_SPLIT);
			sb.append(exportInt(lngLen));
			sb.append(SUB_SPLIT);
			sb.append(exportInt(latStart));
			sb.append(SUB_SPLIT);
			sb.append(exportInt(latLen));
			sb.append(SUB_SPLIT);
			sb.append(exportInt(level));
			sb.append(SUB_SPLIT);
			sb.append(exportInt(BASE));
			sb.append(SUB_SPLIT);
			sb.append(exportInt(BASE_RANGE));
			sb.append(SUB_SPLIT);
			sb.append(exportInt(START_LEVEL));
			sb.append(SUB_SPLIT);
			sb.append(exportInt(END_LEVEL));
			sb.append(SUB_SPLIT);
			sb.append(exportInt(STEP));
			return sb.toString();
		}

		/**
		 * 解析根矩阵元数据
		 * 
		 * @param metaStr
		 *            根矩阵元数据
		 * @return 根矩阵
		 */
		public static LocMatrix parseRootMeta(String metaStr) {
			String[] meta = metaStr.split(SUB_SPLIT);
			if (meta.length > 0 && meta[0] != null && meta[0].startsWith(ROOT_PREFIX)) {
				meta[0] = meta[0].substring(ROOT_PREFIX.length());
			}
			if (meta.length > 5) {
				BASE = parseInt(meta[5]);
			}
			if (meta.length > 6) {
				BASE_RANGE = parseInt(meta[6]);
			}
			if (meta.length > 7) {
				START_LEVEL = parseInt(meta[7]);
			}
			if (meta.length > 8) {
				END_LEVEL = parseInt(meta[8]);
			}
			if (meta.length > 9) {
				STEP = parseInt(meta[9]);
			}
			return new LocMatrix(parseInt(meta[0]), parseInt(meta[1]), parseInt(meta[2]), parseInt(meta[3]),
					parseInt(meta[4]));
		}

		/**
		 * 输出地理区块矩阵
		 *
		 * @return 格式化的地理区块矩阵
		 */
		public String exportLocMatrix() {
			StringBuilder sb = new StringBuilder();
			sb.append(exportInt(lngStart));
			sb.append(SUB_SPLIT);
			sb.append(exportInt(lngLen));
			sb.append(SUB_SPLIT);
			sb.append(exportInt(latStart));
			sb.append(SUB_SPLIT);
			sb.append(exportInt(latLen));
			sb.append(SUB_SPLIT);
			sb.append(exportInt(level));
			sb.append(SPLIT);
			for (int idx : loc) {
				sb.append(exportInt(idx));
				sb.append(SUB_SPLIT);
			}
			return sb.toString();
		}

		/**
		 * 解析地理区块矩阵
		 * 
		 * @param matrix
		 *            格式化的地理区块矩阵
		 * @return 地理区块矩阵
		 */
		public static LocMatrix parseLocMatrix(String matrix) {
			int splitIndex = matrix.indexOf(SPLIT);
			String[] meta = matrix.substring(0, splitIndex).split(SUB_SPLIT);
			LocMatrix result = new LocMatrix(parseInt(meta[0]), parseInt(meta[1]), parseInt(meta[2]),
					parseInt(meta[3]), parseInt(meta[4]));
			String[] indexs = matrix.substring(splitIndex + 1).split(SUB_SPLIT);
			for (int i = 0; i < indexs.length && i < result.loc.length; i++) {
				result.loc[i] = parseInt(indexs[i]);
			}
			return result;
		}

		/** 格式化64进制整数字典 */
		private static final char[] INT_DICT = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ@_"
				.toCharArray();

		/**
		 * 将整数按64进制格式化
		 * 
		 * @param x
		 *            整数
		 * @return 64进制字符
		 */
		private static String exportInt(int x) {
			if (x == 0) {
				return "0";
			}
			char[] b = new char[7];
			int i = 7;
			boolean n = x < 0;
			if (n) {
				x = -x;
			}
			while (x != 0) {
				i--;
				b[i] = INT_DICT[x & 63];
				x = x >> 6;
			}
			if (n) {
				i--;
				b[i] = '-';
			}
			return new String(b, i, b.length - i);
		}

		/**
		 * 解析64进制整数
		 * 
		 * @param number
		 *            64进制字符
		 * @return 整数
		 */
		private static int parseInt(String number) {
			char[] b = number.toCharArray();
			int i = 0;
			boolean n = false;
			if (b[i] == '-') {
				n = true;
				i++;
			}
			int result = 0;
			for (; i < b.length; i++) {
				result = result << 6;
				if (b[i] >= '0' && b[i] <= '9') {
					result += b[i] - '0';
				} else if (b[i] >= 'a' && b[i] <= 'z') {
					result += b[i] - 'a' + 10;
				} else if (b[i] >= 'A' && b[i] <= 'Z') {
					result += b[i] - 'A' + 36;
				} else if (b[i] == '@') {
					result += 62;
				} else if (b[i] == '_') {
					result += 63;
				} else {
					result = result >> 6;
				}
			}
			return n ? -result : result;
		}

		/**
		 * 将区块标记为需要缩放解析
		 * 
		 * @param lngIdx
		 *            经度偏移量
		 * @param latIdx
		 *            纬度偏移量
		 */
		public void setCheckNextLevel(int lngIdx, int latIdx) {
			int idx = convertIndex(lngIdx, latIdx);
			if (loc[idx] >= 0) {
				loc[idx] = -loc[idx] - 1;
			}
		}

		/**
		 * 将坐标偏移量转换为位置索引偏移量
		 * 
		 * @param lngIdx
		 *            经度偏移量
		 * @param latIdx
		 *            纬度偏移量
		 * @return 位置索引偏移量
		 */
		private int convertIndex(int lngIdx, int latIdx) {
			return latIdx * lngLen + lngIdx;
		}

		/**
		 * 判断地理区块是否包含指定坐标
		 * 
		 * @param lng
		 *            百度经度
		 * @param lat
		 *            百度纬度
		 * @return true : 包含, false : 不包含
		 */
		public boolean contents(double lng, double lat) {
			return inRange(convertIndex(lng, lngStart), convertIndex(lat, latStart));
		}

		/**
		 * 判断地理区块是否包含某点
		 * 
		 * @param lngIdx
		 *            经度偏移量
		 * @param latIdx
		 *            纬度偏移量
		 * @return true : 包含, false : 不包含
		 */
		private boolean inRange(int lngIdx, int latIdx) {
			return lngIdx >= 0 && lngIdx < lngLen && latIdx >= 0 && latIdx < latLen;
		}

		/**
		 * 获取包含指定坐标的下一缩放级别地理区块名称
		 * 
		 * @param lng
		 *            百度经度
		 * @param lat
		 *            百度纬度
		 * @return 地理区块名称
		 */
		public Long getNextLevelKey(double lng, double lat) {
			int lngIdx = convertIndex(lng, lngStart);
			int latIdx = convertIndex(lat, latStart);
			int index = convertIndex(lngIdx, latIdx);
			if (inRange(lngIdx, latIdx) && loc[index] < 0) {
				LocMatrix next = createNextLevel(index);
				return next == null ? null : next.getCatchKey();
			}
			return null;
		}

		/**
		 * 创建某点的下一缩放级别地理区块矩阵
		 * @return 地理区块矩阵
		 */
		public LocMatrix createNextLevel(int index) {
			if (level <= END_LEVEL) {
				return null;
			}
			int lngIdx = index % lngLen;
			int latIdx = index / lngLen;
			int range = getRange(level);
			int sub = (STEP - 1) * getRange(level - 1) / 2;
			return new LocMatrix(lngStart + (range * lngIdx) - sub, STEP, latStart + (range * latIdx) - sub, STEP,
					level - 1);
		}

		/**
		 * 根据偏移查找位置信息
		 * 
		 * @param lngIdx
		 *            经度偏移量
		 * @param latIdx
		 *            纬度偏移量
		 * @return 位置信息元素
		 */
		private Loc findLocByIdx(int lngIdx, int latIdx) {
			if (!inRange(lngIdx, latIdx)) {
				return null;
			}
			return LOC_DICT.get(LocMatrix.abs(loc[convertIndex(lngIdx, latIdx)]));
		}

		/**
		 * 解析指定坐标的位置信息
		 * 
		 * @param lng
		 *            百度经度
		 * @param lat
		 *            百度纬度
		 * @return 位置信息元素
		 */
		public Loc getLoc(double lng, double lat) {
			return findLocByIdx(convertIndex(lng, lngStart), convertIndex(lat, latStart));
		}

		/**
		 * 将经纬度转换为坐标偏移量
		 * 
		 * @param x
		 *            经纬度
		 * @param start
		 *            基准坐标
		 * @return 坐标偏移量
		 */
		private int convertIndex(double x, int start) {
			return (int) Math.round((Math.round(x * BASE) - start) / (double) getRange(level));
		}

		/**
		 * 获取地理区块缓存Key
		 * 
		 * @return 缓存Key
		 */
		public Long getCatchKey() {
			return (((((long) lngStart) << 30) + latStart) << 3) + level;
		}

		/**
		 * 取位置信息索引绝对值
		 * 
		 * @param x
		 *            原始索引
		 * @return 位置信息索引
		 */
		public static int abs(int x) {
			if (x >= 0) {
				return x;
			} else {
				return -x - 1;
			}
		}

		/**
		 * 获取精度范围
		 *
		 * @param level
		 *            精度级别(1-n)
		 * @return 精度范围
		 */
		public static int getRange(int level) {
			int result = BASE_RANGE;
			for (int i = 1; i < level && i < START_LEVEL; i++) {
				result *= STEP;
			}
			return result;
		}

		/**
		 * 根据位置信息获取位置信息索引
		 * 
		 * @param province
		 *            省份
		 * @param city
		 *            城市
		 * @param district
		 *            区/县
		 * @return 位置信息索引
		 */
		public static int getLocIndex(String province, String city, String district) {
			Loc loc = new Loc(province == null ? "" : province.trim(), city == null ? "" : city.trim(),
					district == null ? "" : district.trim());
			String key = loc.toString();
			Integer index = LOC_INDEX.get(key);
			if (index == null) {
				index = LOC_DICT.size();
				LOC_DICT.add(loc);
				LOC_INDEX.put(key, index);
			}
			return index;
		}

		/**
		 * 取得需要缩放爬取的地理区块数量
		 * 
		 * @return 需要缩放爬取的地理区块数量
		 */
		public int getNeedNextLevelNum() {
			if (level <= END_LEVEL) {
				return 0;
			}
			int sum = 0;
			for (int l : loc) {
				if (l < 0) {
					sum++;
				}
			}
			return sum;
		}
	}

	/** --------------------------百度地图API访问函数-------------------------- */

	/**
	 * 根据百度坐标获取地址信息
	 * 
	 * @param lng
	 *            百度经度
	 * @param lat
	 *            百度纬度
	 * @param timeOut
	 *            超时时间(单位:ms)
	 * @return 地址信息[街道, 区/县, 市, 省]<br>
	 *         超时或异常时:null
	 */
	private static String[] getLocation(double lng, double lat, long timeOut) {
		long[] pixel = lngLatToPixel(lng, lat, 18);
		String url = String.format(GET_CITY_URL, pixel[0], pixel[1]);
		String[] result = getResultString(url, "", timeOut, CHARACTER_ENCODING);
		if (!String.valueOf(HTTP_SC_OK).equals(result[0])) {
			return null;
		}
		int addressDetailstart = result[1].indexOf(DETAIL_START);
		if (addressDetailstart < 0) {
			return null;
		}
		addressDetailstart += DETAIL_START.length();
		int streetStart = result[1].indexOf(STREET_START, addressDetailstart) + STREET_START.length();
		int districtStart = result[1].indexOf(DISTRICT_START, addressDetailstart) + DISTRICT_START.length();
		int cityStart = result[1].indexOf(CITY_START, addressDetailstart) + CITY_START.length();
		int provinceStart = result[1].indexOf(PROVINCE_START, addressDetailstart) + PROVINCE_START.length();
		String[] location = new String[] { result[1].substring(streetStart, result[1].indexOf(END, streetStart)),
				result[1].substring(districtStart, result[1].indexOf(END, districtStart)),
				result[1].substring(cityStart, result[1].indexOf(END, cityStart)),
				result[1].substring(provinceStart, result[1].indexOf(END, provinceStart)) };
		return location;
	}

	/**
	 * 发送GET请求，并返回响应字符串
	 * 
	 * @param url
	 *            请求地址
	 * @param params
	 *            请求参数
	 * @param maxTimeMillis
	 *            超时时间(单位:ms)
	 * @param characterEncoding
	 *            通信字符编码
	 * @return [HTTP状态码, 响应字符串(状态码不为200时:null)]
	 */
	private static String[] getResultString(String url, String params, long maxTimeMillis, String characterEncoding) {
		if (DEBUG_MODE) {
			System.err.println(String.format("%s Request[%s, %s]", currentTime(), url, params));
		}
		long time = System.currentTimeMillis();
		String response = "";
		int code = 0;
		HttpURLConnection conn = null;
		String query = params != null && params.trim().length() > 0 ? String.format(url.indexOf('?') >= 0 ? "%s&%s"
				: "%s?%s", url, params) : url;
		try {
			conn = (HttpURLConnection) new URL(query).openConnection();
			conn.setReadTimeout((int) maxTimeMillis);
			code = conn.getResponseCode();
			time = System.currentTimeMillis() - time;
			if (code != HTTP_SC_OK) {
				System.err.println(String.format("%s Request[%s]; Cost[%dms]; Response[%d]", currentTime(), query,
						time, code));
				return new String[] { String.valueOf(code), response };
			}
			Reader reader = new InputStreamReader(conn.getInputStream(), characterEncoding);
			response = readerToString(reader);
			reader.close();
		} catch (IOException e) {
			System.err.println(String.format("%s Request URL: %s failed. ", currentTime(), url));
			e.printStackTrace(System.err);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
		if (DEBUG_MODE) {
			System.err.println(String.format("%s Request[%s, %s]; Cost[%dms]; Response[%d, %s]", currentTime(), url,
					params, time, code, response));
		}
		return new String[] { String.valueOf(code), response == null ? "" : response };
	}

	private static long[] lngLatToPixel(double lng, double lat, int zoom) {
		double[] mercator = convertLL2MC(new double[] { lng, lat });
		return new long[] { (long) Math.ceil(mercator[0] * Math.pow(2, zoom - 18)),
				(long) Math.ceil(mercator[1] * Math.pow(2, zoom - 18)) };
	}

	private static double[] convertLL2MC(double[] fromPoint) {
		fromPoint[0] = fix(fromPoint[0], 180);
		fromPoint[1] = fix(fromPoint[1], 90);
		fromPoint[1] = fromPoint[1] > 74.0D ? 74.0D : fromPoint[1] < -74.0D ? -74.0D : fromPoint[1];
		double[] factor = LL2MC[5 - Math.min(((int) Math.floor(Math.abs(fromPoint[1]))) / 15, 5)];
		double x = factor[0] + factor[1] * Math.abs(fromPoint[0]);
		double temp = Math.abs(fromPoint[1]) / factor[9];
		double y = 0.0D;
		for (int i = 2; i <= 8; i++) {
			y += factor[i] * Math.pow(temp, i - 2);
		}
		x *= (fromPoint[0] < 0 ? -1 : 1);
		y *= (fromPoint[1] < 0 ? -1 : 1);
		return new double[] { x, y };
	}

	private static double fix(double point, int halfRange) {
		while (point > halfRange) {
			point -= 2 * halfRange;
		}
		while (point < -halfRange) {
			point += 2 * halfRange;
		}
		return point;
	}

	private static String readerToString(Reader in) throws IOException {
		StringBuilder sb = new StringBuilder();
		int readLength = 0;
		char[] buff = new char[4096];
		while ((readLength = in.read(buff)) >= 0) {
			sb.append(buff, 0, readLength);
		}
		return sb.toString();
	}

	private static String currentTime() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
	}

	/** 字符集 : UTF-8 */
	private static final String CHARACTER_ENCODING = "UTF-8";
	/** 成功 */
	private static final int HTTP_SC_OK = 200;
	/** 超时时间(单位:ms) */
	private static final long TIME_OUT = 1000L;

	private static final double[][] LL2MC = {
			{ -0.00157021024440, 1.113207020616939e+005, 1.704480524535203e+015, -1.033898737604234e+016,
					2.611266785660388e+016, -3.514966917665370e+016, 2.659570071840392e+016, -1.072501245418824e+016,
					1.800819912950474e+015, 82.5 },
			{ 8.277824516172526e-004, 1.113207020463578e+005, 6.477955746671608e+008, -4.082003173641316e+009,
					1.077490566351142e+010, -1.517187553151559e+010, 1.205306533862167e+010, -5.124939663577472e+009,
					9.133119359512032e+008, 67.5 },
			{ 0.00337398766765, 1.113207020202162e+005, 4.481351045890365e+006, -2.339375119931662e+007,
					7.968221547186455e+007, -1.159649932797253e+008, 9.723671115602145e+007, -4.366194633752821e+007,
					8.477230501135234e+006, 52.5 },
			{ 0.00220636496208, 1.113207020209128e+005, 5.175186112841131e+004, 3.796837749470245e+006,
					9.920137397791013e+005, -1.221952217112870e+006, 1.340652697009075e+006, -6.209436990984312e+005,
					1.444169293806241e+005, 37.5 },
			{ -3.441963504368392e-004, 1.113207020576856e+005, 2.782353980772752e+002, 2.485758690035394e+006,
					6.070750963243378e+003, 5.482118345352118e+004, 9.540606633304236e+003, -2.710553267466450e+003,
					1.405483844121726e+003, 22.5 },
			{ -3.218135878613132e-004, 1.113207020701615e+005, 0.00369383431289, 8.237256402795718e+005,
					0.46104986909093, 2.351343141331292e+003, 1.58060784298199, 8.77738589078284, 0.37238884252424,
					7.45 } };

	private static final String GET_CITY_URL = "http://api.map.baidu.com/?qt=rgc&ie=utf-8&oue=0&x=%d&y=%d";
	private static final String DETAIL_START = "\"address_detail\":{";
	private static final String STREET_START = "\"street\":\"";
	private static final String DISTRICT_START = "\"district\":\"";
	private static final String CITY_START = "\"city\":\"";
	private static final String PROVINCE_START = "\"province\":\"";
	private static final String END = "\"";

	private static final boolean DEBUG_MODE = false;
	private static final long API_ACCESS_SLEEP_TIME = 20L;
}