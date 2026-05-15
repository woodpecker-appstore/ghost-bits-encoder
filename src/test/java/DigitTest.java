import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DigitTest {
    public static void main(String[] args) {
        Map<Integer, List<String>> hexChars = new TreeMap<>();


        // 打印结果
        for (int value = 0; value <= 15; value++) {
            String hex = Integer.toHexString(value).toUpperCase();
            List<String> chars = hexChars.get(value);

            System.out.println("数值 " + value + " (0x" + hex + "):");
            for (String info : chars) {
                System.out.println("  " + info);
            }
            System.out.println();
        }
    }

    private static void processCharacter(int cp, Map<Integer, List<String>> hexChars) {
        // 检查是否为有效字符
        if (!Character.isDefined(cp)) return;

        int digitResult = Character.digit(cp, 16);

        if (digitResult >= 0) {
            String charStr = new String(Character.toChars(cp));
            String block = Character.UnicodeBlock.of(cp).toString().replace("_", " ");
            hexChars.get(digitResult).add(
                    String.format("'%s' (U+%04X) - %s", charStr, cp, block)
            );
        }
    }
}
