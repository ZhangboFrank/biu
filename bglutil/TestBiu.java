package bglutil;

public class TestBiu {
	
	private static String[] base64Encode = new String[]{"base64Encode","glacier"};
	private static String[] base64Decode = new String[]{"base64Decode","Z2xhY2llcg=="};
	
	public static void main(String[] args) throws Exception{
		Biu.coreV2(base64Encode);
		Biu.coreV2(base64Decode);
	}
}
