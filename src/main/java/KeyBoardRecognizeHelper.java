import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import utils.BaiduOcr;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class KeyBoardRecognizeHelper {

    private static final String url = "http://192.144.227.111/ocr";
    private final String filePath;
    private BaiduOcr baiduOcr;

    public KeyBoardRecognizeHelper(String filePath) throws Exception {
        this.filePath = filePath;
        this.baiduOcr = new BaiduOcr();
    }

    private JSONArray getOCRResults(String imagePath) {
        File rawImage = FileUtil.file(imagePath);
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("file", rawImage);
        String result = HttpUtil.post(url, paramMap);
        JSONObject json = JSON.parseObject(result);
        // get the result
        return json.getJSONArray("res");
    }

    /**
     * 计算两点之间的距离
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    private double calculateDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    private boolean isCompleteLetterList(ArrayList<Object> LetterList, boolean upperCase) {
        if (LetterList.size() < 26) return false;
        boolean[] letterExist = new boolean[26];
        Arrays.fill(letterExist, false);
        char base = upperCase ? 'A' : 'a';
        for (Object o : LetterList) {
            JSONArray item = (JSONArray) o;
            letterExist[((String) item.get(0)).charAt(0) - base] = true;
        }
        for (boolean b : letterExist) {
            if (!b) return false;
        }
        return true;
    }

    private final String[] keyBoardOrder = new String[]{"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "A", "S", "D", "F", "G", "H", "J", "K", "L", "Z", "X", "C", "V", "B", "N", "M"};

    private String getNextKeyboardLetter(String letter) {
        int index = Arrays.asList(keyBoardOrder).indexOf(letter);
        if (index == keyBoardOrder.length - 1) {
            return keyBoardOrder[0];
        }
        return keyBoardOrder[index + 1];
    }

    /**
     * 键盘识别
     *
     * @return 返回键盘各个字母的坐标 格式为
     * "A":[leftTopX,leftTopY,rightBottomX,rightBottomY]
     * "K":[leftTopX,leftTopY,rightBottomX,rightBottomY]
     */
    public ArrayList<BaiduOcr.Letter> KeyBoardRecognize() throws Exception {
        ArrayList<BaiduOcr.Letter> res = baiduOcr.ocrPicture(this.filePath);
        int startIndex = 0;
        ArrayList<BaiduOcr.Letter> keyBoard = new ArrayList<>();
        for (BaiduOcr.Letter letter : res) {
            // 遍历识别到的列表
            if (letter.word.equals("Q")) {
                // 如果是Q键，则先清除记录的列表，然后记录Q键的位置
                keyBoard.clear();
                keyBoard.add(letter);
            } else {
                if (keyBoard.size() == 0) continue;
                // 如果是其他键且是键盘上下一个键，则记录到列表中
                BaiduOcr.Letter lastLetter = keyBoard.get(keyBoard.size() - 1);
                String nextLetter = getNextKeyboardLetter(lastLetter.word);
                if (nextLetter.equals(letter.word)) {
                    keyBoard.add(letter);
                }
            }
        }
        for (BaiduOcr.Letter letter : keyBoard) {
            System.out.println(letter);
        }
        return keyBoard;
    }

    public static void main(String[] args) throws Exception {
        new KeyBoardRecognizeHelper("C:\\Users\\zhaolin\\Desktop\\fsm-project\\src\\main\\resources\\image\\keyboard6.png").KeyBoardRecognize();
    }

}
