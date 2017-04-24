package com.sean.geo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.GZIPInputStream;


/**
 * 经纬度转城市工具
 * 
 */
public class CoordinateUtil {
	static {
		try {
			loadData("/geo/73000-504-3000-408-4.2.loc3.gz");
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}

	private static void loadData(String loadPath) throws IOException {
		InputStream in = CoordinateUtil.class.getResourceAsStream(loadPath);
		GZIPInputStream zin = new GZIPInputStream(in);
		Reader reader = new InputStreamReader(zin, "UTF-8");
		CoordinateInMemory.load(reader);
		reader.close();
		zin.close();
		in.close();
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
	public static CoordinateInMemory.Loc getLocation(double lng, double lat) {
		return CoordinateInMemory.getLocation(lng, lat);
	}

	/**
	 * GCJ02(高德/谷歌)坐标转BD09(百度)坐标
	 * 
	 * @param cood
	 *            [GCJ02经度, GCJ02纬度]
	 * @return [BD09经度, BD09纬度]
	 */
	public static double[] convertGoogle2Baidu(double[] cood) {
		return CoordinateInMemory.convertGoogle2Baidu(cood);
	}

	/**
	 * BD09(百度)坐标转GCJ02(高德/谷歌)坐标
	 * 
	 * @param cood
	 *            [BD09经度, BD09纬度]
	 * @return [GCJ02经度, GCJ02纬度]
	 */
	public static double[] convertBaidu2Google(double[] cood) {
		return CoordinateInMemory.convertBaidu2Google(cood);
	}

	/**
	 * WGS84(GPS)坐标转GCJ02(高德/谷歌)坐标
	 * 
	 * @param cood
	 *            [GPS经度, GPS纬度]
	 * @return [GCJ02经度, GCJ02纬度]
	 */
	public static double[] convertGps2Google(double[] cood) {
		return CoordinateInMemory.convertGps2Google(cood);
	}

	public static void main(String[] args) throws IOException {
		test(120.1755387337135, 30.25405642113672);
		/*test(116.2951598265899, 39.95972702907648);
		test(118.3828603663653, 31.32772541142192);
		test(120.7552447386641, 27.96080782203826);
		test(119.914972170886, 32.47611234948089);*/
		pause();
	}

	/**
	 * 输出城市信息
	 * 
	 * @param lng
	 *            经度 (百度坐标系BD09)
	 * @param lat
	 *            纬度 (百度坐标系BD09)
	 */
	private static void test(double lng, double lat) {
		CoordinateInMemory.Loc loc = CoordinateUtil.getLocation(lng, lat);
		if (loc != null) {
			System.out.println(String.format("province : %s, city : %s, district : %s", loc.province, loc.city,loc.district));
		}
	}

	/**
	 * 暂停, 等待标准输入
	 * 
	 * @throws IOException
	 */
	private static void pause() throws IOException {
		if (System.in.read() != 0) {
			System.out.println("Stop.");
		}
	}
}