package com.sean.geo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

/**
 * 内存IP库
 * 
 */
public class IpMemoryDB {

	/** 以IP规则的起始索引为依据的比较器 */
	private static Comparator<IpItem> startComparator = new Comparator<IpItem>() {
		public int compare(IpItem o1, IpItem o2) {
			return o1.start == o2.start ? 0 : (o1.start < o2.start ? -1 : 1);
		}
	};
	/** 以IP规则的截止索引为依据的比较器 */
	private static Comparator<IpItem> endComparator = new Comparator<IpItem>() {
		public int compare(IpItem o1, IpItem o2) {
			return o1.end == o2.end ? 0 : (o1.end < o2.end ? -1 : 1);
		}
	};

	static {
		try {
			loadData("/geo/ip_data.sql.gz");
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
	/** 已排序的内存IP库 */
	private static IpItem[] db;
	/** 起始IP在SQL记录中的位置索引 : 1 */
	private static final int START_IP_INDEX = 1;
	/** 截止IP在SQL记录中的位置索引 : 2 */
	private static final int END_IP_INDEX = 2;
	/** 省份/国家信息在SQL记录中的位置索引 : 3 */
	private static final int PROVINCE_INDEX = 3;
	/** 城市信息在SQL记录中的位置索引 : 4 */
	private static final int CITY_INDEX = 4;

	private static void loadData(String loadPath) throws IOException {
		InputStream in = IpMemoryDB.class.getResourceAsStream(loadPath);
		GZIPInputStream zin = new GZIPInputStream(in);
		Reader reader = new InputStreamReader(zin, "UTF-8");
		loadData(reader);
		reader.close();
		zin.close();
		in.close();
	}

	/**
	 * 从字符流中加载IP库
	 * 
	 * @param dataFileReader
	 *            IP库数据文件(SQL)
	 * @return [IP库总记录数, 有效的IP库记录数, 涵盖的有效IP数]
	 * @throws IOException
	 *             字符流读取异常
	 */
	public static synchronized long[] loadData(Reader dataFileReader) throws IOException {
		// 将字符流中的转为字符串
		String input = readerToString(dataFileReader);
		TreeSet<IpItem> ipset = new TreeSet<IpItem>(startComparator);
		// 城市/省份/国家等文字信息缓存
		HashMap<String, String> cache = new HashMap<String, String>();
		// 循环加载每条SQL
		int startIndex = 0;
		while (startIndex >= 0) {
			startIndex = parseRecords(input, startIndex, ipset, cache);
		}
		int metaSize = ipset.size();
		// 校验数据的有效性
		long ip = -1;
		for (Iterator<IpItem> iterator = ipset.iterator(); iterator.hasNext();) {
			IpItem ipItem = iterator.next();
			/*
			 * 当前记录的起始IP地址低于上一条记录的截至IP地址时，存在重复的IP区间，舍弃当前记录
			 * 或者当前记录的截至IP地址低于前记录的起始IP地址时，当前记录无效，舍弃当前记录
			 */
			if (ipItem.start < ip || ipItem.end < ipItem.start) {
				iterator.remove();
			} else {
				ip = ipItem.end;
			}
		}
		db = ipset.toArray(new IpItem[0]);
		long ipcount = 0;
		for (IpItem item : db) {
			ipcount += item.end + 1 - item.start;
		}
		return new long[] { metaSize, db.length, ipcount };
	}

	/**
	 * 根据IP获取城市信息
	 * 
	 * @param ip
	 *            xxx.xxx.xxx.xxx格式的IP字符串
	 * @return [省份/国家信息, 城市信息]<br>
	 *         IP库中未收录的IP : null
	 */
	public static String[] getCity(String ip) {
		String[] ips = ip.split("\\.");
		return getCity((parseLong(ips[0], 10) << 24) + (parseLong(ips[1], 10) << 16) + (parseLong(ips[2], 10) << 8)
				+ parseLong(ips[3], 10));
	}

	/**
	 * 根据IP获取城市信息
	 * 
	 * @param ip
	 *            IP地址(整数)
	 * @return [省份/国家信息, 城市信息]<br>
	 *         IP库中未收录的IP : null
	 */
	public static String[] getCity(long ip) {
		IpItem key = new IpItem();
		key.start = ip;
		key.end = key.start;
		IpItem rule = find(db, key);
		return rule == null ? null : new String[] { rule.province, rule.city };
	}

	/**
	 * 解析每条SQL
	 * 
	 * @param input
	 *            原始SQL
	 * @param is
	 *            当前INSERT语句的起点索引
	 * @param ipset
	 *            IP规则集合
	 * @param cache
	 *            城市/省份/国家等文字信息缓存
	 * @return 当前INSERT语句的结束索引
	 */
	private static int parseRecords(String input, int is, TreeSet<IpItem> ipset, HashMap<String, String> cache) {
		if (is < 0) {
			return -1;
		}
		is = input.indexOf(") values (", is);
		if (is >= 0) {
			int es = input.indexOf(");", is);
			if (es > is) {
				String[] records = input.substring(is + 10, es).split("\\),\\(");
				for (String record : records) {
					String[] items = record.split(",");
					IpItem item = new IpItem();
					item.start = Long.parseLong(items[START_IP_INDEX], 10);
					item.end = Long.parseLong(items[END_IP_INDEX], 10);
					item.province = useCache(items[PROVINCE_INDEX], cache);
					item.city = useCache(items[CITY_INDEX], cache);
					if (equals("局域网", item.province) || equals("局域网", item.city)) {
						continue;
					}
					ipset.add(item);
				}
				return es;
			}
		}
		return -1;
	}

	/**
	 * 重用相同的字符串
	 * 
	 * @param key
	 *            字符串
	 * @param cache
	 *            字符串缓存
	 * @return 重用的字符串
	 */
	private static String useCache(String key, HashMap<String, String> cache) {
		if (!cache.containsKey(key)) {
			if ("NULL".equalsIgnoreCase(key)) {
				cache.put(key, null);
			} else {
				cache.put(key, key.replace("'", ""));
			}
		}
		return cache.get(key);
	}

	/**
	 * 查找IP对应的规则
	 * 
	 * @param db
	 *            已排序的内存IP库
	 * @param key
	 *            格式化的IP
	 * @return IP对应的规则<br>
	 *         未找到时 : null
	 */
	private static IpItem find(IpItem[] db, IpItem key) {
		int si = Arrays.binarySearch(db, key, startComparator);
		// 若某条IP规则的起始IP与当前IP相等，则返回该IP规则
		if (si >= 0) {
			return db[si];
		}
		int ei = Arrays.binarySearch(db, key, endComparator);
		// 若某条IP规则的截至IP与当前IP相等，则返回该IP规则
		if (ei >= 0) {
			return db[ei];
		} else {
			// 若某条IP规则的起始IP < 当前IP < 该IP规则的截至IP，则返回该IP规则
			if (si + 1 == ei) {
				return db[-ei - 1];
			} else {
				return null;
			}
		}
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

	/**
	 * 解析数字
	 * 
	 * @param num
	 *            表示数字的字符
	 * @param radix
	 *            进制
	 * @return 数字
	 */
	private static long parseLong(String num, int radix) {
		try {
			return Long.parseLong(num, radix);
		} catch (Exception e) {
			return -1;
		}
	}

	private static boolean equals(String str1, String str2) {
		return str1 == null ? str2 == null : str1.equals(str2);
	}

	/**
	 * IP规则
	 */
	private static class IpItem {
		/** 起始IP */
		long start;
		/** 截止IP */
		long end;
		/** 省份/国家信息 */
		String province;
		/** 城市信息 */
		String city;
	}

	public static void main(String[] args) throws IOException {
		System.out.println(getCity("211.151.238.51")[0] + "," + getCity("211.151.238.51")[1]);
		System.out.println(getCity("192.168.1.1")[0] + "," + getCity("192.168.1.1")[1]);
		if (System.in.read() != 0) {
			System.out.println("Stop.");
		}
	}
}
