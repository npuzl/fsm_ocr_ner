import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * GUI文本元素文字识别
 * 在服务端调用了腾讯云的接口
 * 因为腾讯云的ocr的java SDK实在是太垃了，好久没更新，所以用Python在服务器上部署了一套，和命名实体识别放一块了
 */
public class GUITextRecognizeHelper {
    // TODO change the image and txt directory
    private static final String imageDirectory = "C:\\Users\\zhaolin\\Desktop\\fsm-project\\src\\main\\resources\\image\\";
    private static final String txtDirectory = "C:\\Users\\zhaolin\\Desktop\\fsm-project\\src\\main\\resources\\txt\\";
    private static final String url = "http://192.144.227.111/ocr";
    //    private static final String url = "http://localhost:5000/ocr";
    /**
     * 上一步中未识别到的文本但是在ocr中识别到的文本 是否要加入到识别结果的txt中
     */
    private boolean appendMissingText = false;

    public GUITextRecognizeHelper() {

    }

    public GUITextRecognizeHelper(boolean appendMissingText) {
        this.appendMissingText = appendMissingText;
    }

    private ArrayList<HashMap<String, Object>> readInputFile(String filepath) {
        String TxtContent = new FileReader(filepath).readString();
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        String[] table = TxtContent.split("\n");
        String[] lines = Arrays.copyOfRange(table, 1, table.length);
        for (String line : lines) {
            line = line.replace("\r", "");
            String[] lineArr = line.split("\t");
            HashMap<String, Object> map = new HashMap<>();
            map.put("id", lineArr[0]);
            map.put("leftTopX", lineArr[1]);
            map.put("leftTopY", lineArr[2]);
            map.put("rightBottomX", lineArr[3]);
            map.put("rightBottomY", lineArr[4]);
            map.put("elementType", lineArr[5]);
            list.add(map);
        }
        return list;
    }

    private void writeOutputFile(String filepath, ArrayList<HashMap<String, Object>> result) {
        FileWriter writer = new FileWriter(filepath);
        StringBuilder sb = new StringBuilder("元素编号\t左上角横坐标\t左上角纵坐标\t右下角横坐标\t右下角纵坐标\t元素类型\t文本信息\r\n");
        for (HashMap<String, Object> map : result) {
            StringBuilder tempSb = new StringBuilder();
            tempSb.append(map.get("id")).append("\t");
            tempSb.append(map.get("leftTopX")).append("\t");
            tempSb.append(map.get("leftTopY")).append("\t");
            tempSb.append(map.get("rightBottomX")).append("\t");
            tempSb.append(map.get("rightBottomY")).append("\t");
            tempSb.append(map.get("elementType")).append("\t");
            tempSb.append(map.get("text")).append("\r\n");
            sb.append(tempSb);
        }

        writer.write(sb.toString());
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

    /**
     * 调用腾讯云的ocr接口，获取图像中文本信息
     *
     * @param txt 文本文件路径
     */
    public void GUITextRecognize(String txt) {
        String filename = txt.split("\\.")[0];
        ArrayList<HashMap<String, Object>> list = readInputFile(txtDirectory + txt);

        JSONArray res = getOCRResults(imageDirectory + filename + ".jpg");

        ArrayList<HashMap<String, Object>> addList = new ArrayList<>();
        res:
        for (Object arr : res) {
            JSONArray subarr = (JSONArray) arr;
            int xmin = subarr.getInteger(1);
            int ymin = subarr.getInteger(2);
            int xmax = subarr.getInteger(3);
            int ymax = subarr.getInteger(4);
            for (HashMap<String, Object> map : list) {
                int leftTopX = Integer.parseInt(map.get("leftTopX").toString());
                int leftTopY = Integer.parseInt(map.get("leftTopY").toString());
                int rightBottomX = Integer.parseInt(map.get("rightBottomX").toString());
                int rightBottomY = Integer.parseInt(map.get("rightBottomY").toString());

                if (calculateDistance(leftTopX, leftTopY, xmin, ymin) <= 100 && calculateDistance(rightBottomX, rightBottomY, xmax, ymax) <= 100) {
                    map.put("text", subarr.getString(0));
                    map.put("leftTopX", xmin);
                    map.put("leftTopY", ymin);
                    map.put("rightBottomX", xmax);
                    map.put("rightBottomY", ymax);
                    continue res;
                }
            }
            // 执行到这里代表在上一步的匹配中没有匹配到，要加进去
            if (this.appendMissingText) {
                addList.add(new HashMap<String, Object>() {
                    {
                        put("id", Integer.parseInt((String) list.get(list.size() - 1).get("id")) + 1);
                        put("text", subarr.getString(0));
                        put("leftTopX", xmin);
                        put("leftTopY", ymin);
                        put("rightBottomX", xmax);
                        put("rightBottomY", ymax);
                        put("elementType", "Text");
                    }
                });
            }
        }
        list.addAll(addList);
        writeOutputFile(txtDirectory + filename + "_ocr_result.txt", list);

    }

    public static void main(String[] args) {
        // test
        new GUITextRecognizeHelper(true).GUITextRecognize("000037.txt");
    }
}
