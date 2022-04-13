package utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BaiduOcr {
    public String accessToken;

    /**
     * 获取权限token
     *
     * @return 返回示例：
     * {
     * "access_token": "24.460da4889caad24cccdb1fea17221975.2592000.1491995545.282335-1234567",
     * "expires_in": 2592000
     * }
     */
    public String getAuth() throws Exception {
        // 官网获取的 API Key 更新为你注册的
        String clientId = "FRkbOAFk7B2NqfTT7a4av4Qj";
        // 官网获取的 Secret Key 更新为你注册的
        String clientSecret = "ffekElRYZvasKWjsLqbR46pQLXXsVLki";
        return getAuth(clientId, clientSecret);
    }

    /**
     * 获取API访问token
     * 该token有一定的有效期，需要自行管理，当失效时需重新获取.
     * 有效期好像是30天？？？
     *
     * @param ak - 百度云官网获取的 API Key
     * @param sk - 百度云官网获取的 Secret Key
     * @return assess_token 示例：
     * "24.460da4889caad24cccdb1fea17221975.2592000.1491995545.282335-1234567"
     */
    public String getAuth(String ak, String sk) throws Exception {
        // 获取token地址
        String authHost = "https://aip.baidubce.com/oauth/2.0/token?";
        String getAccessTokenUrl = authHost
                // 1. grant_type为固定参数
                + "grant_type=client_credentials"
                // 2. 官网获取的 API Key
                + "&client_id=" + ak
                // 3. 官网获取的 Secret Key
                + "&client_secret=" + sk;

        HttpResponse response = HttpRequest.post(getAccessTokenUrl).execute();
        if (response.isOk()) {
            String res = response.body();
            JSONObject jsonObject = JSONObject.parseObject(res);
            return jsonObject.getString("access_token");
        } else {
            throw new Exception("ocr识别获取百度云access_token失败");
        }
    }

    private String getImage(String imagePath) throws IOException {
        byte[] data = null;
        // 读取图片字节数组
        InputStream in = new FileInputStream(imagePath);
        data = new byte[in.available()];
        in.read(data);
        in.close();
        // 对字节数组Base64编码和URLEncode编码
        return URLEncoder.encode(Base64.encode(data), StandardCharsets.UTF_8);
    }

    public static class Letter {
        public String word;
        public int leftTopX;
        public int leftTopY;
        public int rightBottomX;
        public int rightBottomY;

        public Letter(String word, int leftTopX, int leftTopY, int rightBottomX, int rightBottomY) {
            this.word = word;
            this.leftTopX = leftTopX;
            this.leftTopY = leftTopY;
            this.rightBottomX = rightBottomX;
            this.rightBottomY = rightBottomY;
        }

        public String toString() {
            return word + " " + leftTopX + " " + leftTopY + " " + rightBottomX + " " + rightBottomY;
        }
    }

    public ArrayList<Letter> ocrPicture(String imagePath) throws Exception {
        ArrayList<Letter> result = new ArrayList<>();
        String requestUrl = "https://aip.baidubce.com/rest/2.0/ocr/v1/accurate?access_token=" + this.accessToken;
        HttpRequest request = HttpRequest.post(requestUrl).header("content-type", "application/x-www-form-urlencoded").body("image=" + getImage(imagePath) + "&language_type=CHN_ENG&recognize_granularity=small&eng_granularity=letter");
        HttpResponse response = request.execute();
        if (response.isOk()) {
            JSONObject res = JSONObject.parseObject(response.body());
            JSONArray wordsResult = res.getJSONArray("words_result");
            for (Object word : wordsResult) {
                JSONArray chars = ((JSONObject) word).getJSONArray("chars");
                for (Object c : chars) {
                    String ch = ((JSONObject) c).getString("char");
                    if (ch.charAt(0) <= 'z' && ch.charAt(0) >= 'a' ||
                            ch.charAt(0) <= 'Z' && ch.charAt(0) >= 'A') {
                        JSONObject location = (JSONObject) (((JSONObject) c).get("location"));
                        int left = (int) location.get("left");
                        int top = (int) location.get("top");
                        int width = (int) location.get("width");
                        int height = (int) location.get("height");
                        result.add(new Letter(ch, left, top, left + width, top + height));
                    }
                }
            }
//            for (Letter letter : result) {
//                System.out.println(letter);
//            }
//            System.out.println(result.size());
        } else {
            return ocrPicture(imagePath);
        }
        return result;
    }

    public BaiduOcr() throws Exception {
        this.accessToken = getAuth();
    }

    public static void main(String[] args) throws Exception {
        BaiduOcr baiduOcr = new BaiduOcr();
        baiduOcr.ocrPicture("C:\\Users\\zhaolin\\Desktop\\fsm-project\\src\\main\\resources\\image\\keyboard6.png");
    }
}
