
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.http.HttpRequest;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import utils.RegexValidator;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * 命名实体识别，使用的是lac的api
 * lac项目地址：https://github.com/baidu/lac
 * 可以识别人名，地名，机构名，时间 以及词性
 */
public class NamedEntityRecognizeHelper {
    // TODO change the txt directory
    private static final String txtDirectory = "C:\\Users\\zhaolin\\Desktop\\fsm-project\\src\\main\\resources\\txt\\";
    private static final String url = "http://192.144.227.111/entity";
    /**
     * entity hashmap
     */
    private static final HashMap<String, String> entityMap = new HashMap<>() {
        {
            put("n", "普通名词");
            put("f", "方位名词");
            put("s", "处所名词");
            put("nw", "作品名");
            put("nz", "其他专名");
            put("v", "普通动词");
            put("vd", "动副词");
            put("vn", "名动词");
            put("a", "形容词");
            put("ad", "副形词");
            put("an", "名形词");
            put("d", "副词");
            put("m", "数量词");
            put("q", "量词");
            put("r", "代词");
            put("p", "介词");
            put("c", "连词");
            put("u", "助词");
            put("xc", "其他虚词");
            put("w", "标点符号");
            put("PER", "Name");
            put("LOC", "Location");
            put("ORG", "Organization");
            put("TIME", "Time");
        }
    };

    /**
     * read from a txt file and put into a list of hashmap
     *
     * @param filepath txt file path
     * @return list of hashmap
     */
    private ArrayList<HashMap<String, Object>> readInputFile(String filepath) {
        String TxtContent = new FileReader(filepath).readString();
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        String[] table = TxtContent.split("\n");
        String[] lines = Arrays.copyOfRange(table, 1, table.length);
        for (String line : lines) {
            line = line.replace("\r", "");
            String[] lineArr = line.split("\t");
            HashMap<String, Object> map = new HashMap<>();
            System.out.println(lineArr);
            map.put("id", lineArr[0]);
            map.put("leftTopX", lineArr[1]);
            map.put("leftTopY", lineArr[2]);
            map.put("rightBottomX", lineArr[3]);
            map.put("rightBottomY", lineArr[4]);
            map.put("elementType", lineArr[5]);
            map.put("text", lineArr[6]);
            list.add(map);
        }
        return list;
    }


    /**
     * write the result into a txt file
     *
     * @param filepath txt file path
     * @param result   result
     */
    private void writeOutputFile(String filepath, ArrayList<HashMap<String, Object>> result) {
        FileWriter writer = new FileWriter(filepath);
        StringBuilder sb = new StringBuilder("元素编号\t左上角横坐标\t左上角纵坐标\t右下角横坐标\t右下角纵坐标\t元素类型\t文本信息\t语义\r\n");
        for (HashMap<String, Object> map : result) {
            StringBuilder tempSb = new StringBuilder();
            tempSb.append(map.get("id")).append("\t");
            tempSb.append(map.get("leftTopX")).append("\t");
            tempSb.append(map.get("leftTopY")).append("\t");
            tempSb.append(map.get("rightBottomX")).append("\t");
            tempSb.append(map.get("rightBottomY")).append("\t");
            tempSb.append(map.get("elementType")).append("\t");
            tempSb.append(map.get("text")).append("\t");
            tempSb.append(map.get("entity")).append("\r\n");
            sb.append(tempSb);
        }
        writer.write(sb.toString());
    }

    /**
     * use lac to named entity recognize
     *
     * @param txt txt file name
     */
    public void NamedEntityRecognizeAPI(String txt) {
        String filename = txt.split("\\.")[0];
        ArrayList<HashMap<String, Object>> list = readInputFile(txtDirectory + txt);
        for (HashMap<String, Object> map : list) {
            String text = (String) map.get("text");
            if (text.equals("NULL")) {
                map.put("entity", "NULL");
                continue;
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("text", text);
            // post to the server
            String result = HttpRequest.post(url).body(jsonObject.toJSONString()).execute().body();
            JSONObject resultJson = JSON.parseObject(result);
            JSONArray resultArray = resultJson.getJSONArray("res");
            // segmentation is the segmentation of the text. May be useful in the future.
            JSONArray segmentation = (JSONArray) resultArray.get(0);
            JSONArray entity = (JSONArray) resultArray.get(1);
            String entityResult;
            if (entity.size() == 1 && ((entityMap.get((String) entity.get(0)).equals("Name")
                    || entityMap.get((String) entity.get(0)).equals("Location")
                    || entityMap.get((String) entity.get(0)).equals("Organization")
                    || entityMap.get((String) entity.get(0)).equals("Time")))) {
                entityResult = entityMap.get((String) entity.get(0));
            } else {
                entityResult = RegexValidator.regexValidator(map.get("text").toString());
            }
            map.put("entity", entityResult);
        }
        writeOutputFile(txtDirectory + filename + "_entity_result.txt", list);
    }

    public static void main(String[] args) {
        new NamedEntityRecognizeHelper().NamedEntityRecognizeAPI("000037_ocr_result.txt");
    }
}
