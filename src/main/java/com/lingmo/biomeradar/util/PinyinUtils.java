package com.lingmo.biomeradar.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 轻量拼音工具：覆盖本 mod 用到的中文条目，支持全拼和首字母简写搜索。
 * 例如 "平原" -> ["pingyuan", "py"]，搜 "ping" 或 "py" 都能命中。
 */
public final class PinyinUtils {

	private static final Map<String, String> CHAR_PINYIN = new HashMap<>();
	static {
		// 群系/结构名用到的中文字（单字 -> 拼音）
		add("平", "ping"); add("原", "yuan");
		add("沙", "sha");  add("漠", "mo");
		add("森", "sen");  add("林", "lin");
		add("雪", "xue");  add("泽", "ze");
		add("滴", "di");   add("水", "shui"); add("石", "shi"); add("洞", "dong"); add("窟", "ku");
		add("繁", "fan");  add("茂", "mao");
		add("山", "shan"); add("地", "di");
		add("滩", "tan");
		add("河", "he");   add("流", "liu");
		add("海", "hai");  add("洋", "yang");
		add("群", "qun");  add("系", "xi");
		add("积", "ji");
		add("龙", "long"); add("珠", "zhu");
		add("阿", "a");    add("吉", "ji");   add("萨", "sa");
		add("那", "na");   add("美", "mei");  add("克", "ke");  add("星", "xing"); add("川", "chuan");
		add("圣", "sheng");
		add("界", "jie");  add("王", "wang"); add("神", "shen"); add("丘", "qiu"); add("陵", "ling");
		add("精", "jing"); add("时", "shi");  add("光", "guang"); add("屋", "wu");
		add("冥", "ming"); add("届", "jie");  add("府", "fu");
		add("第", "di");   add("一", "yi");   add("二", "er");   add("三", "san");
		add("魔", "mo");
		add("岩", "yan");  add("带", "dai");
		add("大", "da");   add("长", "chang"); add("老", "lao"); add("家", "jia");
		add("沙", "sha");  add("鲁", "lu");   add("竞", "jing"); add("技", "ji");
		add("格", "ge");   add("博", "bo");   add("士", "shi"); add("研", "yan"); add("究", "jiu"); add("所", "suo");
		add("比", "bi");   add("克", "ke");
		add("龟", "gui");  add("仙", "xian");
		add("红", "hong"); add("缎", "duan"); add("军", "jun");  add("团", "tuan"); add("塔", "ta");
		add("特", "te");   add("兰", "lan");  add("斯", "si");   add("飞", "fei");  add("船", "chuan");
		add("贝", "bei");  add("吉", "ji");   add("太", "tai");  add("空", "kong"); add("舱", "cang");
		add("村", "cun");
		add("巴", "ba");   add("迪", "di");
		add("弗", "fu");   add("利", "li");   add("萨", "sa");
		add("悟", "wu");   add("空", "kong"); add("之", "zhi");
		add("雅", "ya");   add("木", "mu");   add("茶", "cha");
		add("殿", "dian"); add("入", "ru");   add("口", "kou");
		add("人", "ren");  add("类", "lei");
		add("村", "cun");
		add("宝", "bao");  add("藏", "zang");
		add("冰", "bing"); add("川", "chuan");
		add("冻", "dong");
		add("针", "zhen"); add("叶", "ye");
		add("稀", "xi");   add("疏", "shu");
		add("落", "luo");  add("叶", "ye");
		add("桦", "hua");
		add("黑", "hei");  add("橡", "xiang");
		add("丛", "cong"); add("林", "lin");
		add("芒", "mang"); add("草", "cao");
		add("台", "tai");  add("原", "yuan");
		add("恶", "e");    add("地", "di");
		add("荒", "huang");add("芜", "wu");
		add("风", "feng"); add("啸", "xiao");
		add("沙", "sha");  add("丘", "qiu");
		add("恶", "e");    add("原", "yuan");
		add("碎", "sui");  add("片", "pian");
		add("漂", "piao"); add("浮", "fu");
		add("冻", "dong"); add("洋", "yang");
		add("深", "shen");  add("寒", "han");
		add("暖", "nuan");  add("温", "wen");
		add("河", "he");    add("口", "kou");
		add("椰", "ye");    add("子", "zi");
		add("原", "yuan");  add("野", "ye");
		add("花", "hua");
		add("草", "cao");   add("坪", "ping");
		add("沼", "zhao");  add("泽", "ze");
		add("湿", "shi");   add("原", "yuan");
		add("泥", "ni");    add("滩", "tan");
		add("岸", "an");
		add("崖", "ya");    add("壁", "bi");
		add("峰", "feng");
		add("岭", "ling");
		add("谷", "gu");
		add("洞", "dong");
		add("穴", "xue");
		add("泉", "quan");
		add("湖", "hu");
		add("泊", "po");
		add("湾", "wan");
		add("角", "jiao");
		add("岬", "jia");
		add("滩", "tan");
		add("涂", "tu");
		add("泽", "ze");
		add("淀", "dian");
		add("泊", "po");
		add("溪", "xi");
		add("涧", "jian");
		add("瀑", "pu");    add("布", "bu");
		add("池", "chi");
		add("塘", "tang");
		add("井", "jing");
		add("泉", "quan");
		add("洲", "zhou");
		add("渚", "zhu");
		add("汀", "ting");
		add("浦", "pu");
		add("滨", "bin");
		add("浪", "lang");
		add("涛", "tao");
		add("潮", "chao");
		add("汐", "xi");
		add("汛", "xun");
		add("涝", "lao");
		add("旱", "han");
		add("霖", "lin");
		add("霜", "shuang");
		add("露", "lu");
		add("雾", "wu");
		add("霞", "xia");
		add("霾", "mai");
		add("雷", "lei");
		add("电", "dian");
		add("雨", "yu");
		add("雪", "xue");
		add("雹", "bao");
		add("霰", "xian");
		add("虹", "hong");
		add("霓", "ni");
		add("晕", "yun");
		add("华", "hua");
		add("曙", "shu");
		add("曦", "xi");
		add("昏", "hun");
		add("晓", "xiao");
		add("晨", "chen");
		add("昏", "hun");
		add("昼", "zhou");
		add("夜", "ye");
		add("宵", "xiao");
		add("更", "geng");
		add("鼓", "gu");
		add("钟", "zhong");
		add("锣", "luo");
		add("鼓", "gu");
		add("号", "hao");
		add("角", "jiao");
		add("哨", "shao");
		add("笛", "di");
		add("笙", "sheng");
		add("箫", "xiao");
		add("琴", "qin");
		add("瑟", "se");
		add("琵琶", "pipa");
		add("筝", "zheng");
		add("筑", "zhu");
		add("竽", "yu");
		add("篪", "chi");
		add("埙", "xun");
		add("鼓", "gu");
		add("柷", "zhu");
		add("敔", "yu");
	}

	private static void add(String ch, String py) {
		CHAR_PINYIN.put(ch, py);
	}

	/** 输入中文短语，返回 [全拼无空格, 首字母]，例如 "平原" -> ["pingyuan","py"]；非中文原样返回空列表 */
	public static List<String> toPinyinForms(String zh) {
		final List<String> result = new ArrayList<>();
		if (zh == null || zh.isEmpty()) {
			return result;
		}
		StringBuilder full = new StringBuilder();
		StringBuilder initials = new StringBuilder();
		boolean hasAnyChinese = false;
		for (int i = 0; i < zh.length(); i++) {
			final String ch = String.valueOf(zh.charAt(i));
			final String py = CHAR_PINYIN.get(ch);
			if (py != null) {
				hasAnyChinese = true;
				full.append(py);
				initials.append(py.charAt(0));
			}
		}
		if (hasAnyChinese) {
			result.add(full.toString());
			result.add(initials.toString());
		}
		return result;
	}

	private PinyinUtils() {}
}
