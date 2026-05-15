import com.alibaba.fastjson.JSON;
import util.GhostBits;
import util.Json;

import java.util.Random;

public class FastJsonTest {
    public static void main(String[] args) {
        String fj = "{\"\\u๐༠໗໘\\u๐꘠꤇๘\":{\"\\u༠꣐੪٠\\u๐٠꣗４\\u੦໐７۹\\u๐꣐۷꘠\\u꤀٠๖໕\":\"\\u０꤀꘦A\\u꘠໐６༡\\u༠۰７６\\u໐༠꣖꤁\\u੦꣐꤂E\\u٠๐۶༩\\u٠꤀６F\\u٠۰꘢E\\u੦੦۴꣙\\u０꣐꘦E\\u༠꣐໗๐\\u༠๐７۵\\u٠੦๗꣔\\u꣐꣐۵༣\\u๐꘠۷꤄\\u٠꣐༧๒\\u۰０໖５\\u꤀༠੬١\\u꤀๐๖D\",\"\\u໐٠꣔꘠\\u꣐꣐꘧۴\\u໐੦٧꤉\\u꤀꤀꤇꣐\\u꣐۰੬꘥\":\"\\u༠꣐๖F\\u꣐༠꤇꣒\\u໐۰٦꤇\\u０໐༢E\\u๐๐꘦༡\\u໐๐７໐\\u۰０꘦༡\\u๐０໖꤃\\u٠꤀๖８\\u٠੦๖༥\\u༠٠꘢E\\u０꤀۶٣\\u੦๐༦F\\u٠０꤆D\\u０꘠໖D\\u꣐꘠໖F\\u꤀꘠۶E\\u๐٠໗໓\\u꣐۰２E\\u๐੦꤆꤉\\u๐੦໖F\\u０٠໒E\\u੦꘠６๙\\u۰੦٦E\\u０۰๗０\\u۰０꤇꘥\\u༠੦７๔\\u꣐۰꣒E\\u꤀๐໔٣\\u꣐੦꣖༨\\u٠０꘦١\\u０꤀꘧۲\\u꘠๐༥੩\\u０໐໖٥\\u꣐੦۷໑\\u꘠꘠７๕\\u٠꤀۶੫\\u༠꣐੬E\\u꣐꤀໖꤃\\u੦０੬５\\u꤀٠４໙\\u༠༠੬E\\u٠੦꣗๐\\u༠٠꘧੫\\u໐꣐꣗໔\\u੦٠໕੩\\u༠੦꘧੪\\u੦๐꘧༢\\u٠۰꤆꘥\\u꣐໐੬١\\u꣐۰๖D\"}}";

         //fj = Json.FjGhostBits(Json.encodeStringValuesToUnicode(fj));
//
//        String fj = "{\"\\u00４０type\":\"java.awt.Rectangle\"}";
//
        System.out.println(fj);
        Object obj = JSON.parse(fj);
        System.out.println(obj);
    }

    private static final Random random = new Random();

    public static int generateFast() {
        int r = random.nextInt(50);
        if (r < 16) return 32 + r;        // 32-47
        if (r == 16) return 48;            // 48
        if (r < 24) return 58 + (r - 17);  // 58-64 (r=17→58, r=23→64)
        return 71 + (r - 24);              // 71-96 (r=24→71, r=49→96)
    }

}
